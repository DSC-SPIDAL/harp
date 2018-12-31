/*
 * commit 9eaaa2365fde01f1a8afd6a435d1738c4da204f1
 * Author: pengb <pengb@indiana.edu>
 * Date:   Wed Nov 28 16:32:23 2018 -0500
 *
 *     bloackScheduler:: Verify the effectiveness of halftrick
 *
 * Features:
 *      1Byte binid,  DMatrixCompactBlockDense
 *      HalfTrick
 *      1 Column Block Scheduler,
 *
 * Based on bnf code, this code targets to validate
 * the idea of feature prunning, i.e., prunning inpromising
 * features during the process of buildhist.
 *
 *
 */

/*!
 * Copyright 2014 by Contributors
 * \file updater_HistMakerFTPrune.cc
 * \brief use histogram counting to construct a tree
 * \author Tianqi Chen
 */
#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
#include <unordered_set>
#include <algorithm>
#include "../data/compact_dmatrix.h"
#include "../common/sync.h"
#include "../common/quantile.h"
#include "../common/group_data.h"
#include "./updater_basemaker-inl.h"
#include <fstream>
#include "../common/debug.h"
#include "../common/hist_util.h"
#include "./fast_hist_param.h"
#include "../common/random.h"
#include "../common/bitmap.h"
#include "../common/sync.h"
#include "../common/row_set.h"

#include <dmlc/timer.h>

#ifndef USE_OMP_BUILDHIST
#include "tbb/tick_count.h"
#include "tbb/task.h"
#include "tbb/scalable_allocator.h"
#include "tbb/task_scheduler_init.h"
#endif

//#define _INIT_PER_TREE_ 1
namespace xgboost {
namespace tree {

using xgboost::common::HistCutMatrix;

DMLC_REGISTRY_FILE_TAG(updater_ftprune);

template<typename TStats>
class HistMakerFTPrune: public BaseMaker {
 public:

 HistMakerFTPrune(){

      this->isInitializedHistIndex = false;
#ifdef USE_COMPACT
      p_hmat = new DMatrixCompactBlockDense();
#endif
  }

  ~HistMakerFTPrune(){
#ifdef USE_COMPACT
    delete p_hmat;
#endif
  }

  TimeInfo getTimeInfo() override{
      tminfo.posset_time -= tminfo.buildhist_time;
      return tminfo;
  }

  void Update(HostDeviceVector<GradientPair> *gpair,
              DMatrix *p_fmat,
              const std::vector<RegTree*> &trees) override {
    TStats::CheckInfo(p_fmat->Info());
    // rescale learning rate according to size of trees
    float lr = param_.learning_rate;
    param_.learning_rate = lr / trees.size();

    #ifndef USE_OMP_BUILDHIST
    // init param for scheduling
    this->gpair_ = gpair;
    #endif
    blockSize_ = param_.block_size * 1024;

    // build tree
    for (auto tree : trees) {
      this->Update(gpair->ConstHostVector(), p_fmat, tree);
    }
    param_.learning_rate = lr;


  }

 protected:

#ifndef USE_OMP_BUILDHIST
  /*
   * Task Scheduler
   */
    struct TreeNode {
        //block info
        DMatrixCompactColBlockDense blk_;
        //todo move into blk_
        int fid_;
    
        //task graph
        std::vector<TreeNode*> children_;
    
        void init(DMatrixCompactBlockDense& dmat, int fid, int blockid, int blockSize){
            
            children_.clear();

            fid_ = fid;
            if ( blockid >= 0){
                blk_ = dmat[fid].getBlock(blockid, blockSize);
            }
            else{
                //root only
                blk_.setEmpty();
            }
        }
    
        inline void addChild(TreeNode* p){
            children_.push_back(p);
        }
        inline TreeNode* getChild(int id){
            return children_[id];
        }
        inline int getChildNum(){
            return children_.size();
        }
    
        static void printTree(TreeNode* root, int level){
            //print self
            std::cout << "level[" << level << "] fid:" << 
                root->fid_ << ",blk:" << root->blk_.base_rowid_ << ",len:" << root->blk_.len_ << "\n";
    
            //print children
            int childNum = root->getChildNum();
            level++;
            for(int i=0; i < childNum; i++){
                TreeNode* p = root->getChild(i);
                printTree(p, level);
            }
        }

        static int getTreeDepth(TreeNode* root){
            int childNum = root->getChildNum();
            int depth = 0;

            for(int i=0; i < childNum; i++){
                TreeNode* p = root->getChild(i);
                int childdepth = getTreeDepth(p);
                if (childdepth > depth)
                    depth = childdepth;
            }

            return depth + 1;
        }
    
    };

    class TreeMaker {
    public:
        static TreeNode* allocate_node() {
            return tbb::scalable_allocator<TreeNode>().allocate(1);
            //return true? tbb::scalable_allocator<TreeNode>().allocate(1) : new TreeNode;
        }
        static TreeNode* create(DMatrixCompactBlockDense& dmat, int blocksize) {
            TreeNode* root = allocate_node();
            root->init(dmat, -1, -1, blocksize);
    
            int fsetSize = dmat.Size() ; 
            for(int fid = 0; fid < fsetSize; fid++){
                int blocknum = dmat[fid].getBlockNum(blocksize);

                TreeNode* head = allocate_node();
                head->init(dmat, fid, 0, blocksize);
                root->addChild(head);
                for(int blkid = 1 ; blkid < blocknum; blkid++){
                    TreeNode* n = allocate_node();
                    n->init(dmat, fid, blkid, blocksize);
                    head->addChild(n);
                    //add to the tail
                    head = n;
                }
            }
            //create the task root node
            return root;
        }
    };

#endif


    /*! \brief a single histogram */
  struct HistUnit {
    /*! \brief cutting point of histogram, contains maximum point */
    const bst_float *cut;
    /*! \brief content of statistics data */
    TStats *data;
    /*! \brief size of histogram */
    unsigned size;
    // default constructor
    HistUnit() = default;
    // constructor
    HistUnit(const bst_float *cut, TStats *data, unsigned size)
        : cut(cut), data(data), size(size) {}
    /*! \brief add a histogram to data */
    inline void Add(bst_float fv,
                    const std::vector<GradientPair> &gpair,
                    const MetaInfo &info,
                    const bst_uint ridx) {
      unsigned i = std::upper_bound(cut, cut + size, fv) - cut;
      CHECK_NE(size, 0U) << "try insert into size=0";
      CHECK_LT(i, size);
      data[i].Add(gpair, info, ridx);
    }
    
  };
  /*! \brief a set of histograms from different index */
  struct HistSet {
    /*! \brief the index pointer of each histunit */
    const unsigned *rptr;
    /*! \brief cutting points in each histunit */
    const bst_float *cut;
    /*! \brief data in different hist unit */
    std::vector<TStats> data;

    //add fset size
    size_t fsetSize;
    size_t nodeSize;
    size_t featnum;

    /*
     * GHSum is the model, preallocated
     * Layout <fid, nid, binid>
     *
     */


    /*! \brief */
    inline HistUnit operator[](size_t fid) {
      return HistUnit(cut + rptr[fid],
                      &data[0] + rptr[fid],
                      rptr[fid+1] - rptr[fid]);
    }

    /*
     * new interface without duplicate cut for each thread
     * all threads share the cut 
     */
    inline HistUnit GetHistUnit(size_t fid, size_t nid) {
      return HistUnit(cut + rptr[fid],
                      #ifdef MODELLAYOUT_NFB
                      &data[0] + rptr[fid] + nid*(fsetSize),
                      #endif
                      &data[0] + rptr[fid]*nodeSize + nid*(rptr[fid+1]-rptr[fid]),
                      rptr[fid+1] - rptr[fid]);
    }


  };
  // thread workspace
  struct ThreadWSpace {
    /*! \brief actual unit pointer */
    std::vector<unsigned> rptr;
    /*! \brief cut field */
    std::vector<bst_float> cut;
    std::vector<bst_float> min_val;
    // per thread histset
    std::vector<HistSet> hset;
    // initialize the hist set
    void Init(const TrainParam &param, int nthread, int nodesize) {
      hset.resize(nthread);
      // cleanup statistics
      for (int tid = 0; tid < nthread; ++tid) {
        for (size_t i = 0; i < hset[tid].data.size(); ++i) {
          hset[tid].data[i].Clear();
        }
        hset[tid].rptr = dmlc::BeginPtr(rptr);
        hset[tid].cut = dmlc::BeginPtr(cut);
        hset[tid].fsetSize = rptr.back();
        hset[tid].featnum = rptr.size() - 2;
        hset[tid].nodeSize = nodesize;
        hset[tid].data.resize(cut.size() * nodesize, TStats(param));

        LOG(CONSOLE)<< "Init hset: rptrSize:" << rptr.size() <<
            ",cutSize:" <<  cut.size() <<",nodesize:" << nodesize <<
            ",fsetSize:" << rptr.back() << ",max_depth:" << param.max_depth;
      }
    }

    void saveGHSum(int treeid, int depth, int nodecnt){
      std::ostringstream ss;
      ss << "ghsum_" << treeid << "_" << depth;

      std::ofstream write;
      write.open(ss.str());
 
      int pagesize = hset[0].data.size() / nodecnt;

      LOG(CONSOLE) << "saveGHSUM(" << treeid << "," << depth << "," 
          << nodecnt << ") pagesize=" << pagesize <<
          "fset=" << hset[0].featnum;

      for(int i=0; i< nodecnt; i++){
          write << "NODE:" << i << "\n";

          for (int fid = 0; fid < hset[0].featnum; fid++){
            write << "\tF:" << fid << "\t";

            int sumlen = hset[0].rptr[fid+1] - hset[0].rptr[fid]; 
            for (int j=0; j < sumlen; j++){ 
              auto offset = j + i*pagesize + hset[0].rptr[fid];
              write << hset[0].data[offset].sum_grad <<"," << hset[0].data[offset].sum_hess << " ";
            }
            write << "\n";
          }
      }

      write.close();
    }

    /*! \brief clear the workspace */
    inline void Clear() {
      cut.clear(); rptr.resize(1); rptr[0] = 0;
    }
    /*! \brief total size */
    inline size_t Size() const {
      return rptr.size() - 1;
    }
  };

  struct HistEntry {
    typename HistMakerFTPrune<TStats>::HistUnit hist;
    unsigned istart;

    /* OptApprox:: init bindid in pmat */
    inline unsigned GetBinId(bst_float fv) {
      unsigned start = 0;
      while (start < hist.size && !(fv < hist.cut[start])) ++start;
      //CHECK_NE(start, hist.size);
      if(start == hist.size) start--;
      return start;
    }

    inline void AddWithIndex(unsigned binid,
                    GradientPair gstats) {
        //hist.data[binid].Add(gstats);
    #ifdef USE_BINID
        hist.data[binid].Add(gstats);
    #endif
    }
    inline void AddWithIndex(unsigned binid,
                    const std::vector<GradientPair> &gpair,
                    const MetaInfo &info,
                    const bst_uint ridx) {
    #ifdef USE_BINID
      //CHECK_NE(binid, hist.size);
      hist.data[binid].Add(gpair, info, ridx);
    #endif
    }


    /*!
     * \brief add a histogram to data,
     * do linear scan, start from istart
     */
    inline void Add(bst_float fv,
                    const std::vector<GradientPair> &gpair,
                    const MetaInfo &info,
                    const bst_uint ridx) {
      while (istart < hist.size && !(fv < hist.cut[istart])) ++istart;
      CHECK_NE(istart, hist.size);
      hist.data[istart].Add(gpair, info, ridx);
    }
    /*!
     * \brief add a histogram to data,
     * do linear scan, start from istart
     */
    inline void Add(bst_float fv,
                    GradientPair gstats) {
      if (fv < hist.cut[istart]) {
        hist.data[istart].Add(gstats);
      } else {
        while (istart < hist.size && !(fv < hist.cut[istart])) ++istart;
        if (istart != hist.size) {
          hist.data[istart].Add(gstats);
        } else {
          LOG(INFO) << "fv=" << fv << ", hist.size=" << hist.size;
          for (size_t i = 0; i < hist.size; ++i) {
            LOG(INFO) << "hist[" << i << "]=" << hist.cut[i];
          }
          LOG(FATAL) << "fv=" << fv << ", hist.last=" << hist.cut[hist.size - 1];
        }
      }
    }
  };
 

#ifndef USE_OMP_BUILDHIST
     void BuildHistWithBlock(DMatrixCompactColBlockDense &block, int fid){
    if (block.size() == 0) return;

    const std::vector<GradientPair>& gpair = this->gpair_->ConstHostVector();

    // initialize sbuilder for use
    //std::vector<HistEntry> &hbuilder = this->thread_hist_[fid];
    std::vector<HistEntry> hbuilder;
    hbuilder.resize(this->num_nodes_);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid, nid);
    }



    #ifdef USE_HALFTRICK
    #define CHECKHALFCOND (nid>=0 && (nid&1)==0)
    #else
    #define CHECKHALFCOND (nid>=0)
    #endif

    //one block
    if (TStats::kSimpleStats != 0 && this->param_.cache_opt != 0) {
      constexpr bst_uint kBuffer = 32;
      bst_uint align_length = block.size() / kBuffer * kBuffer;
      int buf_position[kBuffer];
      GradientPair buf_gpair[kBuffer];
      for (bst_uint j = 0; j < align_length; j += kBuffer) {
        #pragma ivdep  
        for (bst_uint i = 0; i < kBuffer; ++i) {
          bst_uint ridx = block._index(j+i);
          //buf_position[i]= this->DecodePosition(ridx);
          buf_position[i]= this->position_[ridx];
          buf_gpair[i] = gpair[ridx];
        }
        for (bst_uint i = 0; i < kBuffer; ++i) {
          const int nid = buf_position[i];
          if (CHECKHALFCOND) {
            hbuilder[nid].AddWithIndex(block._binid(j+i), buf_gpair[i]);
          }
        }
      }
      for (bst_uint j = align_length; j < block.size(); ++j) {
        const bst_uint ridx = block._index(j);
        //const int nid = this->DecodePosition(ridx);
        const int nid = this->position_[ridx];

        if (CHECKHALFCOND) {
          hbuilder[nid].AddWithIndex(block._binid(j), gpair[ridx]);
        }
      }
    } else {
      //#pragma ivdep
      //#pragma omp simd
      for (bst_uint j = 0; j < block.size(); ++j) {
        const bst_uint ridx = block._index(j);
        //const int nid = this->DecodePosition(ridx);
        const int nid = this->position_[ridx];
        if (CHECKHALFCOND) {
          hbuilder[nid].AddWithIndex(block._binid(j), gpair[ridx]);
        }
      }
    }

    #ifdef USE_HALFTRICK
    double _tstart = dmlc::GetTime();
    //get the right node
    const unsigned nid_start = this->qexpand_[0];
    if (nid_start == 0)
        return;

    CHECK_NE(nid_start % 2, 0);
    unsigned nid_parent = (nid_start+1)/2-1;
    for (size_t i = 0; i < this->qexpand_.size(); i+=2) {
      const unsigned nid = this->qexpand_[i];
      auto parent_hist = this->wspace_.hset[0].GetHistUnit(fid, nid_parent + i/2);
      #pragma ivdep
      #pragma omp simd
      for(int j=0; j < hbuilder[nid].hist.size; j++){
        hbuilder[nid].hist.data[j].SetSubstract(
                parent_hist.data[j],
                hbuilder[nid+1].hist.data[j]);
      }

    }
    this->tminfo.aux_time[0] += dmlc::GetTime() - _tstart;
    #endif

  }


  /*
   * tbb task
   */
    class BuildHistTask: public tbb::task {
        TreeNode* root;
        HistMakerFTPrune* ctx;
        bool is_continuation;

    public:
        BuildHistTask( TreeNode* root_, HistMakerFTPrune* ctx_ ) : 
            root(root_), ctx(ctx_), is_continuation(false){}

        task* execute() /*override*/ {
    
            tbb::task* next = NULL;
            if( !is_continuation ) {
                if (root->blk_.size() <= 0){
                    //root
                    int childNum = root->getChildNum();
                    if (childNum > 0){
                        int count = 1; 
                        tbb::task_list list;
     
                        for(int i=0; i < childNum; i++){
                            TreeNode* p = root->getChild(i);
                            ++count;
                            list.push_back( *new( allocate_child() ) BuildHistTask(p, ctx) );
                        }
                        set_ref_count(count);
                        //std::cout << "Start spawn_all :" << childNum << "\n";
                        spawn_and_wait_for_all(list);
                        //std::cout << "End spawn_all :" << childNum << "\n";
                    }
                }
                else{
                    //std::cout << "StartNode off:" << root->off_ << "\n";
                    //do this block
                    ctx->BuildHistWithBlock(root->blk_, root->fid_);
    
                    //go to next child
                    if (root->getChildNum() == 1 ){
                        TreeNode* p = root->getChild(0);
                        auto* pchild = new( allocate_child() ) BuildHistTask(p,ctx);
     
                        recycle_as_continuation();
                        is_continuation = true;
                        set_ref_count(1);
                        //spawn(*pchild);
                        next = pchild;
                    }
                    //std::cout << "EndNode off:" << root->off_ << "\n";
                }
            }
            return next;
        }
    };

#endif

 /* --------------------------------------------------
  * data members
  */ 

  // pruning 
  // nexpand x feature_num
  std::vector<std::vector<unsigned>> best_ftidx_;
  std::vector<unsigned> best_final_;

  // workspace of thread
  ThreadWSpace wspace_;
  // reducer for histogram
  rabit::Reducer<TStats, TStats::Reduce> histred_;
  // set of working features
  std::vector<bst_uint> fwork_set_;
  std::vector<bst_uint> fsplit_set_;

  // cached dmatrix where we initialized the feature on.
  const DMatrix* cache_dmatrix_{nullptr};
  // feature helper
  BaseMaker::FMetaHelper feat_helper_;
  // temp space to map feature id to working index
  std::vector<int> feat2workindex_;
  // set of index from fset that are current work set
  std::vector<bst_uint> work_set_;
  // set of index from that are split candidates.
//  std::vector<bst_uint> fsplit_set_;
  // used to hold statistics
  std::vector<std::vector<TStats> > thread_stats_;
  // used to hold start pointer
  std::vector<std::vector<HistEntry> > thread_hist_;
  // node statistics
  std::vector<TStats> node_stats_;
  //HistCutMatrix
  HistCutMatrix cut_;
  // flag of initialization
  bool isInitializedHistIndex;
 
  //size_t blockSize_{256*1024};
  //size_t blockSize_{0};
  int blockSize_;

  #ifndef USE_OMP_BUILDHIST
  //task graph
  TreeNode* tg_root;
  //std::vector<GradientPair>& gpair_;
  HostDeviceVector<GradientPair> *gpair_{nullptr};

  int num_nodes_;
  #endif

 // hist mat compact
#ifdef USE_COMPACT
  DMatrixCompactBlockDense* p_hmat;
#endif

  //for predict cache
  const RegTree* p_last_tree_;
  int treeid_{0};

  /* -------------------------------------------------
   * functions
   */


  // update function implementation
  void Update(const std::vector<GradientPair> &gpair,
                      DMatrix *p_fmat,
                      RegTree *p_tree) {
    this->InitData(gpair, *p_fmat, *p_tree);
    this->InitWorkSet(p_fmat, *p_tree, &fwork_set_);
    // mark root node as fresh.
    for (int i = 0; i < p_tree->param.num_roots; ++i) {
      (*p_tree)[i].SetLeaf(0.0f, 0);
    }

    /*
     * Initialize the histogram and DMatrixCompact
     */
    //printVec("ResetPos::fwork_set=", fwork_set_);
    // reset and propose candidate split
    this->ResetPosAndPropose(gpair, p_fmat, fwork_set_, *p_tree);
    //printtree(p_tree, "ResetPosAndPropose");
    // initialize the histogram only
    this->InitializeHist(gpair, p_fmat, fwork_set_, *p_tree);

    for (int depth = 0; depth < param_.max_depth; ++depth) {


      printVec("qexpand:", this->qexpand_);
      // create histogram
      double _tstart = dmlc::GetTime();
      this->CreateHist(gpair, fwork_set_, *p_tree);
      this->tminfo.posset_time += dmlc::GetTime() - _tstart;

      //printVec("position:", this->position_);
      //printtree(p_tree, "After CreateHist");

#ifdef USE_DEBUG_SAVE
      this->wspace_.saveGHSum(treeid_, depth, this->qexpand_.size());
#endif


      // find split based on histogram statistics
      this->FindSplit(depth, gpair, fwork_set_, p_tree);

      //printtree(p_tree, "FindSplit");

      // reset position after split
      this->ResetPositionAfterSplit(NULL, *p_tree);
      //printtree(p_tree, "ResetPositionAfterSPlit");


      this->UpdateQueueExpand(*p_tree);
      //printtree(p_tree, "UpdateQueueExpand");

      // if nothing left to be expand, break
      if (qexpand_.size() == 0) break;
    }

    if(this->fsplit_set_.size() > 0){
        //update the position for update cache
        //printVec("before updatepos:", this->position_);
        double _tstart = dmlc::GetTime();
        this->CreateHist(gpair, fwork_set_, *p_tree);
        this->tminfo.posset_time += dmlc::GetTime() - _tstart;
        //printVec("after updatepos:", this->position_);
    }

    for (size_t i = 0; i < qexpand_.size(); ++i) {
      const int nid = qexpand_[i];
      (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
    }

    /* optApprox */
    //reset the binid to fvalue in this tree
    ResetTree(*p_tree);
    treeid_ ++;
  }

 private:
    void EnumerateSplit(const HistUnit &hist,
                             const TStats &node_sum,
                             bst_uint fid,
                             SplitEntry *best,
                             TStats *left_sum) {
    if (hist.size == 0) return;

    double root_gain = node_sum.CalcGain(param_);
    TStats s(param_), c(param_);
    for (int i = 0; i < hist.size; ++i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i, false)) {
            *left_sum = s;
          }
        }
      }
    }
    s.Clear();
    for (int i = hist.size - 1; i >= 0; --i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i-1, true)) {
            *left_sum = c;
          }
        }
      }
    }

    //debug
    //printSplit(*best);

   
  }
   void FindSplit(int depth,
                        const std::vector<GradientPair> &gpair,
                        const std::vector <bst_uint> &fset,
                        RegTree *p_tree) {
    
    const size_t num_feature = fset.size();
    //printInt("FindSplit::num_feature = ", num_feature);
    //printVec("FindSplit::fset=", fset);
    // get the best split condition for each node
    std::vector<SplitEntry> sol(qexpand_.size());
    std::vector<TStats> left_sum(qexpand_.size());
    auto nexpand = static_cast<bst_omp_uint>(qexpand_.size());
    best_final_.clear();
    best_final_.resize(nexpand);

    #pragma omp parallel for schedule(dynamic, 1)
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      CHECK_EQ(node2workindex_[nid], static_cast<int>(wid));
      SplitEntry &best = sol[wid];
      //TStats &node_sum = wspace_.hset[0][num_feature + wid * (num_feature + 1)].data[0];
      //TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, wid).data[0];
      TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, nid).data[0];
      for (size_t i = 0; i < fset.size(); ++i) {
        //int fid = this->fset[i];
        //int offset = this->feat2workindex_[fid];
        //EnumerateSplit(this->wspace_.hset[0][i + wid * (num_feature+1)],
        //EnumerateSplit(this->wspace_.hset[0].GetHistUnit(i, wid),
        //               node_sum, fset[i], &best, &left_sum[wid]);
        EnumerateSplit(this->wspace_.hset[0].GetHistUnit(i, nid),
                       node_sum, fset[i], &best, &left_sum[wid]);
        //EnumerateSplit(this->wspace_.hset[0][offset + wid * (num_feature+1)],
        //               node_sum, fid, &best, &left_sum[wid]);

        //printSplit(best, fset[i], nid); 
      }

    }

    // get the best result, we can synchronize the solution
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      const SplitEntry &best = sol[wid];

      //check current prune hits
      //save best splitindex for each node to best_final_
      //best_final_.push_back(sol[wid].SplitIndex());
      auto idx = std::find(best_ftidx_[wid].begin(), best_ftidx_[wid].end(), sol[wid].SplitIndex()) - best_ftidx_[wid].begin();

      best_final_.push_back(idx);

      //const TStats &node_sum = wspace_.hset[0][num_feature + wid * (num_feature + 1)].data[0];
      ///const TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, wid).data[0];
      const TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, nid).data[0];
      this->SetStats(p_tree, nid, node_sum);
      // set up the values
      p_tree->Stat(nid).loss_chg = best.loss_chg;
      // now we know the solution in snode[nid], set split
      
      
     
      if (best.loss_chg > kRtEps) {

        p_tree->AddChilds(nid);

        (*p_tree)[nid].SetSplit(best.SplitIndex(),
                                 best.split_value, best.DefaultLeft());
        // mark right child as 0, to indicate fresh leaf
        (*p_tree)[(*p_tree)[nid].LeftChild()].SetLeaf(0.0f, 0);
        (*p_tree)[(*p_tree)[nid].RightChild()].SetLeaf(0.0f, 0);
        // right side sum
        TStats right_sum;
        right_sum.SetSubstract(node_sum, left_sum[wid]);
        this->SetStats(p_tree, (*p_tree)[nid].LeftChild(), left_sum[wid]);
        this->SetStats(p_tree, (*p_tree)[nid].RightChild(), right_sum);
      } else {
        (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
        #ifdef USE_HALFTRICK
        //add empty childs anyway to keep the node id as the same as the fll
        //binary tree
        p_tree->AddChilds(nid);
        (*p_tree)[(*p_tree)[nid].LeftChild()].SetLeaf(0.0f, 0);
        (*p_tree)[(*p_tree)[nid].RightChild()].SetLeaf(0.0f, 0);
        #endif
 
      }
    }

    //prune test
    printVec("best_final_:", best_final_);

  }

  void SetStats(RegTree *p_tree, int nid, const TStats &node_sum) {
    p_tree->Stat(nid).base_weight = static_cast<bst_float>(node_sum.CalcWeight(param_));
    p_tree->Stat(nid).sum_hess = static_cast<bst_float>(node_sum.sum_hess);
    node_sum.SetLeafVec(param_, p_tree->Leafvec(nid));
  }

  // initialize the work set of tree
  void InitWorkSet(DMatrix *p_fmat,
                   const RegTree &tree,
                   std::vector<bst_uint> *p_fset){
    if (p_fmat != cache_dmatrix_) {
      feat_helper_.InitByCol(p_fmat, tree);
      cache_dmatrix_ = p_fmat;
    }
    feat_helper_.SyncInfo();

    /*
     * These codes will change the contents and order of the fwork_set
     * Therefore, the initialized cut_ which depends on the fid order
     * may have trouble for new trees.
     */
    //feat_helper_.SampleCol(this->param_.colsample_bytree, p_fset);
    p_fset->resize(tree.param.num_feature);
    for (size_t i = 0; i < p_fset->size(); ++i) {
      (*p_fset)[i] = static_cast<unsigned>(i);
    }
 
  }

  void ResetPositionAfterSplit(DMatrix *p_fmat,
                               const RegTree &tree){
    this->GetSplitSet(this->qexpand_, tree, &this->fsplit_set_);
  }

  void ResetPosAndPropose(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const std::vector<bst_uint> &fset,
                          const RegTree &tree) {
    const MetaInfo &info = p_fmat->Info();
    // fill in reverse map
    feat2workindex_.resize(tree.param.num_feature);
    std::fill(feat2workindex_.begin(), feat2workindex_.end(), -1);
    work_set_.clear();
    for (auto fidx : fset) {
      if (feat_helper_.Type(fidx) == 2) {
        feat2workindex_[fidx] = static_cast<int>(work_set_.size());
        work_set_.push_back(fidx);
      } else {
        feat2workindex_[fidx] = -2;
      }
    }
    const size_t work_set_size = work_set_.size();

    if (!isInitializedHistIndex && this->qexpand_.size() == 1) {
        cut_.Init(p_fmat,param_.max_bin /*256*/);

        /*
         * Debug on cut, higgs feature fid=8
         * 0.0
         * 1.0865380764007568
         * 2.1730761528015137
         *
         */
        if(1){
            auto a = cut_[8];
            std::cout << "higgs[8] cut size:" << a.size << "=" ;
            for (size_t i = 0; i < a.size; ++i) {
                std::cout << a.cut[i] << ",";
            }
            std::cout << "min_val=" << cut_.min_val[8] << "\n";

        }

        CHECK_EQ(this->qexpand_.size(), 1);

        // now we get the final result of sketch, setup the cut
        this->wspace_.cut.clear();
        this->wspace_.rptr.clear();
        this->wspace_.rptr.push_back(0);
        for (size_t wid = 0; wid < this->qexpand_.size(); ++wid) {
            
          for (unsigned int fid : fset) {
            int offset = feat2workindex_[fid];
            if (offset >= 0) {
              auto a = cut_[fid];

              for (size_t i = 0; i < a.size; ++i) {
                this->wspace_.cut.push_back(a.cut[i]);
              }
              // push a value that is greater than anything
              //if (a.size != 0) {
              //  bst_float cpt = a.cut[a.size - 1];
              //  // this must be bigger than last value in a scale
              //  bst_float last = cpt + fabs(cpt) + kRtEps;
              //  this->wspace_.cut.push_back(last);
              //}
              this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));

              //add minval
              this->wspace_.min_val.push_back(cut_.min_val[fid]);

            } else {
              CHECK_EQ(offset, -2);
              bst_float cpt = feat_helper_.MaxValue(fid);
              this->wspace_.cut.push_back(cpt + fabs(cpt) + kRtEps);
              this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
            }
 
          }

          //this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
          // reserve last value for global statistics
          this->wspace_.cut.push_back(0.0f);
          this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
        }
        CHECK_EQ(this->wspace_.rptr.size(),
                 (fset.size() + 1) * this->qexpand_.size() + 1);
    }
  }


  /* OptApprox:: init bindid in pmat */

  void InitHistCol(const SparsePage::Inst &col,
                            const RegTree &tree,
                            const std::vector<bst_uint> &fset,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {

    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);

    //LOG(CONSOLE) << "InitHistCol: num_nodes=" << tree.param.num_nodes <<
    //        ", qexpand.size=" << this->qexpand_.size() ;

    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      const unsigned wid = this->node2workindex_[nid];
      hbuilder[nid].istart = 0;
      //hbuilder[nid].hist = this->wspace_.hset[0][fid_offset + wid * (fset.size()+1)];
      //hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset,wid);
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset,nid);
    }
    for (auto& c : col) {
      const bst_uint ridx = c.index;
      const int nid = this->position_[ridx];
      if (nid >= 0) {
          // update binid in pmat
          unsigned binid = hbuilder[nid].GetBinId(c.fvalue);
          c.addBinid(binid);
      }
    }
  } 


  void InitHistIndex( DMatrix *p_fmat,
                      const std::vector<bst_uint> &fset,
                     const RegTree &tree){

      const auto nsize = static_cast<bst_omp_uint>(fset.size());
      std::cout  << "InitHistIndex : fset.size=" << nsize << "\n";
      
      thread_hist_.resize(omp_get_max_threads());

      // start accumulating statistics
      for (const auto &batch : p_fmat->GetSortedColumnBatches()) {
        // start enumeration
        //const auto nsize = static_cast<bst_omp_uint>(fset.size());
        #pragma omp parallel for schedule(dynamic, 1)
        for (bst_omp_uint i = 0; i < nsize; ++i) {
          int fid = fset[i];
          int offset = feat2workindex_[fid];
          if (offset >= 0) {
            this->InitHistCol(batch[fid], tree,
                                fset, offset,
                                &thread_hist_[omp_get_thread_num()]);
          }
        }
      }
  }

/*
 * halftrick 
 *
 */
  #ifdef USE_HALFTRICK
  //#define CHECKHALFCOND (nid>=0 && (nid&1)==0)
  #define CHECKHALFCOND ((nid & 0x80000001) ==0)
  #else
  #define CHECKHALFCOND (nid>=0)
  #endif

  //void UpdateHistColWithIndex_BlockOrder(const std::vector<GradientPair> &gpair,
  void UpdateHistColWithIndex(const std::vector<GradientPair> &gpair,
                            const DMatrixCompactColBlockDense &col,
                            const MetaInfo &info,
                            const RegTree &tree,
                            const std::vector<bst_uint> &fset,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {
    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      const unsigned wid = this->node2workindex_[nid];
      hbuilder[nid].istart = 0;
      //hbuilder[nid].hist = this->wspace_.hset[0][fid_offset + wid * (fset.size()+1)];
      //hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, wid);
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, nid);
    }

    int blockNum = col.getBlockNum(blockSize_);
    for(int blkid = 0; blkid < blockNum; blkid++){
        const DMatrixCompactColBlockDense block = col.getBlock(blkid, blockSize_);

        //one block
        #ifdef USE_UNROLL
        if (TStats::kSimpleStats != 0 && this->param_.cache_opt != 0) {
        #else
        if (0){
        #endif

          constexpr bst_uint kBuffer = 32;
          bst_uint align_length = block.size() / kBuffer * kBuffer;
          int buf_position[kBuffer];
          GradientPair buf_gpair[kBuffer];

          for (bst_uint j = 0; j < align_length; j += kBuffer) {
            
            #pragma ivdep
            for (bst_uint i = 0; i < kBuffer; ++i) {
              bst_uint ridx = block._index(j+i);
              buf_position[i]= this->DecodePosition(ridx);
              //const int nid = buf_position[i];
              //if (CHECKHALFCOND) {
                  buf_gpair[i] = gpair[ridx];
              //}
            }
            for (bst_uint i = 0; i < kBuffer; ++i) {
              const int nid = buf_position[i];
                if (CHECKHALFCOND) {
                  hbuilder[nid].AddWithIndex(block._binid(j+i), buf_gpair[i]);
                }
            }
          }
          for (bst_uint j = align_length; j < block.size(); ++j) {
            const bst_uint ridx = block._index(j);
            //const int nid = this->DecodePosition(ridx);
            const int nid = this->position_[ridx];

            if (CHECKHALFCOND) {
              hbuilder[nid].AddWithIndex(block._binid(j), gpair[ridx]);
            }
          }
        } else {
          //#pragma ivdep
          //#pragma omp simd
          for (bst_uint j = 0; j < block.size(); ++j) {
            const bst_uint ridx = block._index(j);
            //const int nid = this->DecodePosition(ridx);
            const int nid = this->position_[ridx];
            if (CHECKHALFCOND) {
              hbuilder[nid].AddWithIndex(block._binid(j), gpair, info, ridx);
            }
          }
        }
    } /*blk*/

#ifdef USE_HALFTRICK
    //get the right node
    const unsigned nid_start = this->qexpand_[0];
    if (nid_start == 0)
        return;

    CHECK_NE(nid_start % 2, 0);
    unsigned nid_parent = (nid_start+1)/2-1;
    for (size_t i = 0; i < this->qexpand_.size(); i+=2) {
      const unsigned nid = this->qexpand_[i];
      auto parent_hist = this->wspace_.hset[0].GetHistUnit(fid_offset, nid_parent + i/2);
      #pragma ivdep
      #pragma omp simd
      for(int j=0; j < hbuilder[nid].hist.size; j++){
        hbuilder[nid].hist.data[j].SetSubstract(parent_hist.data[j],hbuilder[nid+1].hist.data[j]);
      }

    }
#endif

  }

  //dup func
  void CorrectNonDefaultPositionByBatch2(
      DMatrixCompactBlockDense &batch, const std::vector<bst_uint> &sorted_split_set,
      const RegTree &tree) {
    for (size_t fid = 0; fid < batch.Size(); ++fid) {
      auto col = batch[fid];
      auto it = std::lower_bound(sorted_split_set.begin(), sorted_split_set.end(), fid);

      if (it != sorted_split_set.end() && *it == fid) {
        const auto ndata = static_cast<bst_omp_uint>(col.size());
        #pragma omp parallel for schedule(static)
        for (bst_omp_uint j = 0; j < ndata; ++j) {

          //const bst_uint ridx = col[j].index;
          //const bst_float fvalue = col[j].fvalue;
          //const bst_uint ridx = col[j]._index();
          //const bst_uint binid = col[j]._binid();
          const bst_uint ridx = col._index(j);
          const bst_uint binid = col._binid(j);



          const int nid = this->DecodePosition(ridx);
          CHECK(tree[nid].IsLeaf());
          int pid = tree[nid].Parent();

          // go back to parent, correct those who are not default
          if (!tree[nid].IsRoot() && tree[pid].SplitIndex() == fid) {
            if (binid <= tree[pid].SplitCond()) {
              this->SetEncodePosition(ridx, tree[pid].LeftChild());
            } else {
              this->SetEncodePosition(ridx, tree[pid].RightChild());
            }
          }
        }
      }
    }
  }

  bool UpdatePredictionCache(const DMatrix* p_fmat,
                             HostDeviceVector<bst_float>* p_out_preds) override {
    if ( this->param_.subsample < 1.0f) {
      return false;
    } else {
      std::vector<bst_float>& out_preds = p_out_preds->HostVector();

      // p_last_fmat_ is a valid pointer as long as UpdatePredictionCache() is called in
      // conjunction with Update().
      if (!p_last_tree_) {
        return false;
      }

      CHECK_GT(out_preds.size(), 0U);

      //get leaf_value for all nodes
      const auto nodes = p_last_tree_->GetNodes();
      std::vector<float> leaf_values;
      leaf_values.resize(nodes.size());

      for (int nid = 0; nid < nodes.size(); nid ++){
          bst_float leaf_value;
          int tnid = nid;
          // if a node is marked as deleted by the pruner, traverse upward to locate
          // a non-deleted leaf.
          if ((*p_last_tree_)[tnid].IsDeleted()) {
            while ((*p_last_tree_)[tnid].IsDeleted()) {
              tnid = (*p_last_tree_)[tnid].Parent();
            }
            CHECK((*p_last_tree_)[tnid].IsLeaf());
          }

          leaf_values[nid] = (*p_last_tree_)[tnid].LeafValue();
      }

      const auto nrows = static_cast<bst_omp_uint>(p_fmat->Info().num_row_);
      for(int ridx=0; ridx < nrows; ridx++){
        const int nid = this->DecodePosition(ridx);

        //update   
        out_preds[ridx] += leaf_values[nid];
      }

      LOG(CONSOLE) << "UpdatePredictionCache: nodes size=" << 
          nodes.size() << ",rowscnt=" << nrows;
    

      //printVec("updatech:", this->position_);
      return true;
    }
  }


  // code to create histogram
  void InitializeHist(const std::vector<GradientPair> &gpair,
                  DMatrix *p_fmat,
                  const std::vector<bst_uint> &fset,
                  const RegTree &tree) {

    const MetaInfo &info = p_fmat->Info();
    // fill in reverse map
    this->feat2workindex_.resize(tree.param.num_feature);
    this->work_set_ = fset;
    std::fill(this->feat2workindex_.begin(), this->feat2workindex_.end(), -1);
    for (size_t i = 0; i < fset.size(); ++i) {
      this->feat2workindex_[fset[i]] = static_cast<int>(i);
    }
    // start to work
    //this->wspace_.Init(this->param_, 1);
    //this->wspace_.Init(this->param_, 1, this->qexpand_.size());
    this->wspace_.Init(this->param_, 1, std::pow(2,this->param_.max_depth+1) /*256*/);
    // to gain speedup in recovery
    {
      this->thread_hist_.resize(omp_get_max_threads());

      // TWOPASS: use the real set + split set in the column iteration.
      this->SetDefaultPostion(p_fmat, tree);
      this->work_set_.insert(this->work_set_.end(), this->fsplit_set_.begin(),
                             this->fsplit_set_.end());
      std::sort(this->work_set_.begin(), this->work_set_.end());
      this->work_set_.resize(
          std::unique(this->work_set_.begin(), this->work_set_.end()) - this->work_set_.begin());

      /* OptApprox:: init bindid in pmat */
      if (!this->isInitializedHistIndex){
        InitHistIndex(p_fmat, fset, tree);
        this->isInitializedHistIndex = true;

        #ifdef   USE_COMPACT
        p_hmat->Init(*p_fmat->GetSortedColumnBatches().begin(), p_fmat->Info());
        //printdmat(*p_hmat);
        #endif  

        #ifndef USE_OMP_BUILDHIST
        //init the task graph
        //tbb::task_scheduler_init init;
        tg_root = TreeMaker::create(*p_hmat, blockSize_ );

        std::cout << "TaskTree Depth:" << TreeNode::getTreeDepth(tg_root) << ", blockSize:" << blockSize_ << ", param.block_size:" 
            << param_.block_size << "\n";
        #ifdef USE_DEBUG
        TreeNode::printTree(tg_root, 0);
        #endif

        //tbb::task_scheduler_init init(omp_get_max_threads());
        #endif


        //DEBUG
        //printdmat(*p_fmat->GetSortedColumnBatches().begin());
        //printcut(this->cut_);

        startVtune("vtune-flag.txt");
        LOG(INFO) << "End of initialization, start training";

        this->tminfo.trainstart_time = dmlc::GetTime();
      }
    }

  }
 

    /*
     * ftpruning test
     *
     */

 void UpdateHistColWithIndex_firstblock(const std::vector<GradientPair> &gpair,
                            const DMatrixCompactColBlockDense &col,
                            const MetaInfo &info,
                            const RegTree &tree,
                            const std::vector<bst_uint> &fset,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {
    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      const unsigned wid = this->node2workindex_[nid];
      hbuilder[nid].istart = 0;
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, nid);
    }

    int blockNum = col.getBlockNum(blockSize_);
    for(int blkid = 0; blkid < 1; blkid++){
        const DMatrixCompactColBlockDense block = col.getBlock(blkid, blockSize_);

        //#pragma ivdep
        //#pragma omp simd
        for (bst_uint j = 0; j < block.size(); ++j) {
          const bst_uint ridx = block._index(j);
          const int nid = this->position_[ridx];
          if (CHECKHALFCOND) {
            hbuilder[nid].AddWithIndex(block._binid(j), gpair, info, ridx);
          }
        }
    } /*blk*/

  }

 void UpdateHistColWithIndex_afterfirst(const std::vector<GradientPair> &gpair,
                            const DMatrixCompactColBlockDense &col,
                            const MetaInfo &info,
                            const RegTree &tree,
                            const std::vector<bst_uint> &fset,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {
    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      const unsigned wid = this->node2workindex_[nid];
      hbuilder[nid].istart = 0;
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, nid);
    }

    int blockNum = col.getBlockNum(blockSize_);
    for(int blkid = 1; blkid < blockNum; blkid++){
        const DMatrixCompactColBlockDense block = col.getBlock(blkid, blockSize_);

        //#pragma ivdep
        //#pragma omp simd
        for (bst_uint j = 0; j < block.size(); ++j) {
          const bst_uint ridx = block._index(j);
          const int nid = this->position_[ridx];
          if (CHECKHALFCOND) {
            hbuilder[nid].AddWithIndex(block._binid(j), gpair, info, ridx);
          }
        }
    } /*blk*/

  }

  /*
   * find topK features
   */

  void EnumerateTopK(const HistUnit &hist,
                             const TStats &node_sum,
                             bst_uint fid,
                             SplitEntry *best,
                             TStats *left_sum) {
    if (hist.size == 0) return;

    double root_gain = node_sum.CalcGain(param_);
    TStats s(param_), c(param_);
    for (int i = 0; i < hist.size; ++i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i, false)) {
            *left_sum = s;
          }
        }
      }
    }
    s.Clear();
    for (int i = hist.size - 1; i >= 0; --i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i-1, true)) {
            *left_sum = c;
          }
        }
      }
    }

    //debug
    //printSplit(*best, fid, 99);
  }


  //sort by index (feature id) according to the split_value
  std::vector<unsigned> sort_indexes(const std::vector<SplitEntry> &v) {
        // initialize original index locations
        std::vector<unsigned> idx(v.size());
        iota(idx.begin(), idx.end(), 0);

        // sort indexes based on comparing values in v
        std::sort(idx.begin(), idx.end(),
                [&v](unsigned i1, unsigned i2) {return v[i1].loss_chg > v[i2].loss_chg;});

        return idx;
    }

   void FindTopK_seltopk(const std::vector <bst_uint> &fset,
                std::vector<bst_uint>& workset) {
    
    const size_t num_feature = fset.size();
    auto nexpand = static_cast<bst_omp_uint>(qexpand_.size());

    //printInt("FindSplit::num_feature = ", num_feature);
    //printVec("FindSplit::fset=", fset);
    //
    // get the best split condition for each node
    //std::vector<SplitEntry> sol(qexpand_.size());
    std::vector<std::vector<SplitEntry>> sol(nexpand, std::vector<SplitEntry>(num_feature));
    //for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
    //    sol[wid].resize(num_feature);
    //}
    std::vector<TStats> left_sum(qexpand_.size());


    #pragma omp parallel for schedule(dynamic, 1)
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      CHECK_EQ(node2workindex_[nid], static_cast<int>(wid));

      //get best split in each feature
      //SplitEntry &best = sol[wid];
      TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, nid).data[0];
      for (size_t i = 0; i < fset.size(); ++i) {
        SplitEntry &best = sol[wid][i];
        EnumerateTopK(this->wspace_.hset[0].GetHistUnit(i, nid),
                       node_sum, fset[i], &best, &left_sum[wid]);
      }
    }

    //get topk
    std::vector<int> topk;
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {

        printmsg("FindTopK at node");

        auto idx = sort_indexes(sol[wid]);
        for(int i = 0; i < 10; i++){
            topk.push_back(idx[i]);

            //debug info
            printSplit(sol[wid][idx[i]], idx[i], wid);
        }

    }

    //auto last = std::unique(topk.begin(), topk.end());
    //topk.erase(last, topk.end());
    //workset.resize(topk.size());
    //std::copy(topk.begin(), topk.end(), workset.begin());
    std::unordered_set<unsigned> setk(topk.begin(), topk.end());
    workset.resize(setk.size());
    std::copy(setk.begin(), setk.end(), workset.begin());
  }


   void FindTopK_all(const std::vector <bst_uint> &fset,
                std::vector<std::vector<bst_uint>>& workset) {
    
    const size_t num_feature = fset.size();
    auto nexpand = static_cast<bst_omp_uint>(qexpand_.size());
    workset.resize(nexpand);
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
        workset[wid].clear();
        workset[wid].resize(num_feature);
    }
 
    //printInt("FindSplit::num_feature = ", num_feature);
    //printVec("FindSplit::fset=", fset);
    //
    // get the best split condition for each node
    //std::vector<SplitEntry> sol(qexpand_.size());
    std::vector<std::vector<SplitEntry>> sol(nexpand, std::vector<SplitEntry>(num_feature));
   std::vector<TStats> left_sum(qexpand_.size());


    #pragma omp parallel for schedule(dynamic, 1)
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      CHECK_EQ(node2workindex_[nid], static_cast<int>(wid));

      //get best split in each feature
      //SplitEntry &best = sol[wid];
      TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, nid).data[0];
      for (size_t i = 0; i < fset.size(); ++i) {
        SplitEntry &best = sol[wid][i];
        EnumerateTopK(this->wspace_.hset[0].GetHistUnit(i, nid),
                       node_sum, fset[i], &best, &left_sum[wid]);
      }
    }

    //save the sorted idx vectors
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
        auto idx = sort_indexes(sol[wid]);
        std::copy(idx.begin(), idx.end(), workset[wid].begin());
    }

  }

  /*
   * normal build hist
   *
   */

  void CreateHist(const std::vector<GradientPair> &gpair,
                  const std::vector<bst_uint> &fset,
                  const RegTree &tree) {
    const MetaInfo &info = p_hmat->Info();
    // fill in reverse map
    this->feat2workindex_.resize(tree.param.num_feature);
    this->work_set_ = fset;
    std::fill(this->feat2workindex_.begin(), this->feat2workindex_.end(), -1);
    for (size_t i = 0; i < fset.size(); ++i) {
      this->feat2workindex_[fset[i]] = static_cast<int>(i);
    }
    // start to work
    //this->wspace_.Init(this->param_, 1);
    //this->wspace_.Init(this->param_, 1, this->qexpand_.size());
    // to gain speedup in recovery
    {
      this->thread_hist_.resize(omp_get_max_threads());

      // TWOPASS: use the real set + split set in the column iteration.
      this->SetDefaultPostion(p_hmat, tree);
      this->work_set_.insert(this->work_set_.end(), this->fsplit_set_.begin(),
                             this->fsplit_set_.end());
      std::sort(this->work_set_.begin(), this->work_set_.end());
      this->work_set_.resize(
          std::unique(this->work_set_.begin(), this->work_set_.end()) - this->work_set_.begin());

      //for (const auto &batch : p_fmat->GetSortedColumnBatches()) 
      //only one page
      {
        // TWOPASS: use the real set + split set in the column iteration.
        this->CorrectNonDefaultPositionByBatch2(*p_hmat, this->fsplit_set_, tree);

        if (this->qexpand_.size() == 0){
            //last step to update position 
            return;
        }

        //auto batch = *p_fmat->GetSortedColumnBatches().begin();
        //this->CorrectNonDefaultPositionByBatchOrig(batch, this->fsplit_set_, tree);
        //this->CorrectNonDefaultPositionByBatch(batch, this->fsplit_set_, tree);

      // start enumeration
      double _tstart = dmlc::GetTime();
      #ifdef USE_OMP_BUILDHIST

        //1. work on the first block
        const auto nsize = static_cast<bst_omp_uint>(this->work_set_.size());
        #pragma omp parallel for schedule(dynamic, 1)
        for (bst_omp_uint i = 0; i < nsize; ++i) {
          int fid = this->work_set_[i];
          int offset = this->feat2workindex_[fid];
          if (offset >= 0) {
             this->UpdateHistColWithIndex_firstblock(gpair, (*p_hmat)[fid], info, tree,
                                fset, offset,
                                &this->thread_hist_[omp_get_thread_num()]);
          }
        }

        // update node statistics by this partial sum
        this->GetNodeStats(gpair, *p_hmat, tree,
                           &(this->thread_stats_), &(this->node_stats_));
        for (size_t i = 0; i < this->qexpand_.size(); ++i) {
          const int nid = this->qexpand_[i];
          const int wid = this->node2workindex_[nid];
          this->wspace_.hset[0].GetHistUnit(fset.size(),nid)
              .data[0] = this->node_stats_[nid];
        }
 


        //2. find the top K
        //FindTopK(fset, this->work_set_);
        //printVec("FindTopK::work_set=", this->work_set_);
        FindTopK_all(fset, best_ftidx_);

        //3. continue with the blocks remains
        #pragma omp parallel for schedule(dynamic, 1)
        for (bst_omp_uint i = 0; i < nsize; ++i) {
          int fid = this->work_set_[i];
          int offset = this->feat2workindex_[fid];
          if (offset >= 0) {
             this->UpdateHistColWithIndex_afterfirst(gpair, (*p_hmat)[fid], info, tree,
                                fset, offset,
                                &this->thread_hist_[omp_get_thread_num()]);
          }
        }

        #else
        /*
         * TBB scheduler 
         */
        {

          //this->gpair_ = gpair;
          this->num_nodes_ = tree.param.num_nodes;
          //this->thread_hist_.resize();

          //tbb::task_scheduler_init init(omp_get_max_threads());
          BuildHistTask& a = *new(tbb::task::allocate_root()) BuildHistTask(this->tg_root, this);
          tbb::task::spawn_root_and_wait(a);
        }

        #endif
        this->tminfo.buildhist_time += dmlc::GetTime() - _tstart;
      } // end of one-page




      // update node statistics.
      this->GetNodeStats(gpair, *p_hmat, tree,
                         &(this->thread_stats_), &(this->node_stats_));
      for (size_t i = 0; i < this->qexpand_.size(); ++i) {
        const int nid = this->qexpand_[i];
        const int wid = this->node2workindex_[nid];
        //this->wspace_.hset[0][fset.size() + wid * (fset.size()+1)]
        //    .data[0] = this->node_stats_[nid];
        //this->wspace_.hset[0].GetHistUnit(fset.size(),wid)
        //    .data[0] = this->node_stats_[nid];
        this->wspace_.hset[0].GetHistUnit(fset.size(),nid)
            .data[0] = this->node_stats_[nid];
        
      }
    }
    this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset[0].data),
                            this->wspace_.hset[0].data.size());

    //save the last tree point
    p_last_tree_ = &tree;
  }

  void ResetTree(RegTree& tree){

    const auto nodes = tree.GetNodes();
    for(int i=0; i < nodes.size(); i++){
        if (tree[i].IsLeaf() || tree[i].IsDeleted()){
            continue;
        }

        unsigned fid = tree[i].SplitIndex();
        auto splitCond = tree[i].SplitCond();
        int binid = static_cast<int>(splitCond);
        auto defaultLeft = tree[i].DefaultLeft();

        //turn splitCond from binid to fvalue
        //splitCond is binid now
        // the leftmost and rightmost bound should adjust

        float fvalue;
        //int cutSize = this->wspace_.rptr[fid + 1] - this->wspace_.rptr[fid];
        //if (binid == cutSize){
        //    //rightmost
        //    fvalue = this->wspace_.cut[this->wspace_.rptr[fid] + binid - 1];
        //}
        //else if (binid == -1 && defaultLeft){
        if (binid == -1 && defaultLeft){
            //leftmost
            fvalue = this->wspace_.min_val[fid];
        }
        else{
            fvalue = this->wspace_.cut[this->wspace_.rptr[fid] + binid];
        }
        tree[i].SetSplit(fid, fvalue, defaultLeft);
    }

    //update the position for update cache
    //this->CorrectNonDefaultPositionByBatch2(*p_hmat, this->fsplit_set_, tree);


  }


};


XGBOOST_REGISTER_TREE_UPDATER(HistMakerFTPrune, "grow_ftprune")
.describe("Tree constructor that uses approximate global of histogram construction.")
.set_body([]() {
    return new HistMakerFTPrune<GradStats>();
  });
}  // namespace tree
}  // namespace xgboost
