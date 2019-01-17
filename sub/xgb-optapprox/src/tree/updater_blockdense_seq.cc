/*!
 * Copyright 2014 by Contributors
 * \file updater_HistMakerBlockDenseSeq.cc
 * \brief use histogram counting to construct a tree
 * \author Tianqi Chen
 *
 * 2018,2019
 * HARPDAAL-GBT optimize based on the approx and fast_hist codebase
 * node_block_size = 0, use strict sequential access to inputData and POS/GH array
 *
 */
#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
#include <atomic>
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
//#include "tbb/tick_count.h"
//#include "tbb/task.h"
//#include "tbb/scalable_allocator.h"
//#include "tbb/task_scheduler_init.h"
#endif

//#define _INIT_PER_TREE_ 1
namespace xgboost {
namespace tree {

using xgboost::common::HistCutMatrix;

DMLC_REGISTRY_FILE_TAG(updater_blockdense_seq);

class spin_mutex {
    volatile std::atomic_flag flag = ATOMIC_FLAG_INIT;
    unsigned long dirtyCnt_ = 0;
    public:
    spin_mutex() = default;
    spin_mutex(const spin_mutex&) = delete;
    spin_mutex& operator= (const spin_mutex&) = delete;
    void lock() {
        while(flag.test_and_set(std::memory_order_acquire)){
            __asm__("pause");
            dirtyCnt_ ++;
            ;
        }
    }
    void unlock() {
        flag.clear(std::memory_order_release);
    }

    unsigned long getCnt(){return dirtyCnt_;}
    void clear(){
        dirtyCnt_ = 0;
    }
};


template<typename TStats>
class HistMakerBlockDenseSeq: public BaseMaker {
 public:

 HistMakerBlockDenseSeq(){

      this->isInitializedHistIndex = false;
      p_blkmat = new DMatrixDenseCube();
      p_hmat = new DMatrixCompactBlockDense();
  }

  ~HistMakerBlockDenseSeq(){
    /*
     * free memory
     */
    delete p_hmat;
    delete p_blkmat;
    if(bwork_lock_) delete bwork_lock_;

    /*
     * model memory
     */
    wspace_.release();

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
    //// init param for scheduling
    //this->gpair_ = gpair;
    #endif
    blkInfo_ = BlockInfo(param_.row_block_size, param_.ft_block_size, param_.bin_block_size);

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
#endif


    /*! \brief a single histogram */
  struct HistUnit {
    /*! \brief cutting point of histogram, contains maximum point */
    const bst_float *cut;
    /*! \brief content of statistics data */
    TStats *data;
    /*! \brief size of histogram */
    unsigned size;

    /* plain size*/
    //unsigned step_size;
    unsigned block_height;  // boundary of crossing a block
    unsigned in_block_step;    // in block step = FT_BLOCK_SIZE
    unsigned cross_block_step;    // cross block step = FT_BLOCK_SIZE*NODESIZE

    // default constructor
    HistUnit() = default;

    // constructor
    //HistUnit(const bst_float *cut, TStats *data, unsigned size, unsigned stepsize = 1)
    //    : cut(cut), data(data), size(size), step_size(stepsize) {}

    HistUnit(const bst_float *cut, TStats *data, unsigned size, 
            unsigned height = 1, unsigned instep = 1, unsigned crossstep = 1)
        : cut(cut), data(data), size(size), 
            block_height(height), in_block_step(instep), cross_block_step(crossstep) {}

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

    //
    // get i item from *data when not fid wised layout
    //
    // fir for (0,0,1)
    //TStats& Get(int i) const{
    //    return data[i*step_size];
    //}

    TStats& Get(int i) const{
        return data[(i/block_height)*cross_block_step + (i % block_height) * in_block_step];
    }

    void ClearData(){
        std::memset(data, 0., sizeof(GradStats) * size);
    }

  };

  /*
   * Core Data Structure for Model GHSum
   *
   */
  struct HistSet {
    /*! \brief the index pointer of each histunit */
    const unsigned *rptr;
    /*! \brief cutting points in each histunit */
    const bst_float *cut;

    /*
     * GHSum is the model, preallocated
     *
     */
#ifdef USE_VECTOR4MODEL
    // store sum for each node as the last feature vector, 
    // and only use the first element
    std::vector<TStats> data;
#else
    // avoid object initialization and first touch in main thread
    // layout of model
    //      <nodeid, binid , fid>
    // store separate sum at the end
    TStats  *data{nullptr};
    unsigned long size{0};
    TStats  *nodesum{nullptr};

#endif
    //add fset size
    size_t fsetSize;
    size_t nodeSize;
    size_t featnum;
    BlockInfo* pblkInfo;

    /*
     * GHSum is the model, preallocated
     * Layout <fid, nid, binid>
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
    inline HistUnit InitGetHistUnitByFid(size_t fid, size_t nid) {
      return HistUnit(cut + rptr[fid],
                      //&data[0] + rptr[fid] + nid*(fsetSize),
                      &data[0] + rptr[fid]*nodeSize + nid*(rptr[fid+1]-rptr[fid]),
                      rptr[fid+1] - rptr[fid]);
    }

    /*
     * get by blkid, layout as <blkid, nid, blockaddr>
     */

    // /* simple version as blkinfo=(0,0,1)
    // *
    // */
    //inline HistUnit GetHistUnitByBinid(size_t binid, size_t nid) {
    //  return HistUnit(cut, /* not use*/
    //                  &data[0] + (featnum+1)*(binid * nodeSize + nid),
    //                  featnum);
    //}

    //inline HistUnit GetHistUnitByFid(size_t fid, size_t nid) {
    //  return HistUnit(cut, /* not use*/
    //                  &data[0] + nid*(featnum+1) + fid,
    //                  rptr[fid+1] - rptr[fid],
    //                  (featnum+1)*nodeSize);
    //}

    // /* only for (0,0,n)
    // *
    // */
    //inline HistUnit GetHistUnitByBlkid(size_t blkid, size_t nid) {
    //  return HistUnit(cut, /* not use*/
    //                  &data[0] + (pblkInfo->GetBinBlkSize()*(featnum+1))*(blkid * nodeSize + nid),
    //                  pblkInfo->GetBinBlkSize()*(featnum+1));
    //}

    //
    //inline HistUnit GetHistUnitByFid(size_t fid, size_t nid) {
    //  return HistUnit(cut, /* not use*/
    //                  &data[0] + nid*pblkInfo->GetBinBlkSize()*(featnum+1) + fid,
    //                  rptr[fid+1] - rptr[fid],
    //                  pblkInfo->GetBinBlkSize(),
    //                  featnum,
    //                  pblkInfo->GetBinBlkSize()*(featnum+1)*nodeSize);
    //}



    /*
     * general version of blkinfo: (row,ft,bin_blk_size)
     */
    //
    // model organized by block, memory are continuous for each block
    // used in BuildHist
    //
    inline HistUnit GetHistUnitByBlkid(size_t blkid, size_t nid) {
      return HistUnit(cut, /* not use*/
                      &data[0] + (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize + nid),
                      pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize());
    }

    //
    // node summation store at the end
    //
    inline TStats& GetNodeSum(int nid){
        return nodesum[nid];
    }

    //
    // FindSplit will go through model by a feature column, not continuous
    //
    inline HistUnit GetHistUnitByFid(size_t fid, size_t nid) {
      int blkid = fid / pblkInfo->GetFeatureBlkSize();
      unsigned int blkoff = (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize + nid);
      return HistUnit(cut, /* not use*/
                      &data[0] + blkoff + fid % pblkInfo->GetFeatureBlkSize(),
                      rptr[fid+1] - rptr[fid],
                      pblkInfo->GetBinBlkSize(),
                      pblkInfo->GetFeatureBlkSize(),
                      pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize()*pblkInfo->GetFeatureBlkNum(featnum+1)*nodeSize);
    }



  };
  // thread workspace
  struct ThreadWSpace {
    /*! \brief actual unit pointer */
    std::vector<unsigned> rptr;
    /*! \brief cut field */
    std::vector<bst_float> cut;
    std::vector<bst_float> min_val;
    // the model
    HistSet hset;

    // initialize the hist set
    void Init(const TrainParam &param, int nodesize, BlockInfo& blkinfo) {

      // cleanup statistics
      //for (int tid = 0; tid < nthread; ++tid) {
      {
#ifdef USE_VECTOR4MODEL
        for (size_t i = 0; i < hset.data.size(); ++i) {
          hset.data[i].Clear();
        }
#endif
        hset.rptr = dmlc::BeginPtr(rptr);
        hset.cut = dmlc::BeginPtr(cut);
        hset.fsetSize = rptr.back();
        hset.featnum = rptr.size() - 2;
        hset.nodeSize = nodesize;

        hset.pblkInfo = &blkinfo;

        /*
         * <binid, nid, fid> layout means hole in the plain
         * simple solution to allocate full space
         * other than resort to remove the holes
         */
#ifdef USE_VECTOR4MODEL
        unsigned long cubesize = blkinfo.GetModelCubeSize(param.max_bin, hset.featnum+1, nodesize);
#else
        unsigned long cubesize = blkinfo.GetModelCubeSize(param.max_bin, hset.featnum, nodesize);
#endif

        LOG(CONSOLE)<< "Init hset(memset): rptrSize:" << rptr.size() <<
            ",cutSize:" <<  cut.size() <<",nodesize:" << nodesize <<
            ",fsetSize:" << rptr.back() << ",max_depth:" << param.max_depth << 
            ",featnum:" << hset.featnum <<
            ",cubesize:" << cubesize << ":" << 8*cubesize/(1024*1024*1024) << "GB";

#ifdef USE_VECTOR4MODEL
        hset.data.resize(cubesize, TStats(param));
#else
        // this function will be called for only two times and in intialization
        if (hset.data != nullptr){
            //second time call init will prepare the memory for UpdateHistBlock
            std::free(hset.data);
        }
        // add sum at the end
        hset.data = static_cast<TStats*>(malloc(sizeof(TStats) * cubesize));
        hset.size = cubesize;
        if (hset.nodesum == nullptr){
            hset.nodesum = static_cast<TStats*>(malloc(sizeof(TStats) * nodesize));
        }
        if (hset.data == nullptr){
            LOG(CONSOLE) << "FATAL ERROR, quit";
            std::exit(-1);
        }
#endif

      }

   }

    void release(){
#ifndef USE_VECTOR4MODEL
        if (hset.data != nullptr){
            std::free(hset.data);
        }
        if (hset.nodesum != nullptr){
            std::free(hset.nodesum);
        }
#endif
    }

    void ClearData(){
        // just clear the data
        // when GHSum=16GB, initialize take a long time in seconds
        // this function will not be called, now use thread local HistUnit.ClearData directly
#ifdef USE_VECTOR4MODEL
        //std::fill(hset.data.begin(), hset.data.end(), GradStats(0.,0.));
        std::memset(hset.data.data(), 0., sizeof(GradStats) * hset.data.size());
#else
        std::memset(hset.data, 0., sizeof(GradStats) * hset.size);
#endif
    }

    void saveGHSum(int treeid, int depth, int nodecnt){
      std::ostringstream ss;
      ss << "ghsum_" << treeid << "_" << depth;

      std::ofstream write;
      write.open(ss.str());
 
      int pagesize = hset.data.size() / nodecnt;

      LOG(CONSOLE) << "saveGHSUM(" << treeid << "," << depth << "," 
          << nodecnt << ") pagesize=" << pagesize <<
          "fset=" << hset.featnum;

      for(int i=0; i< nodecnt; i++){
          write << "NODE:" << i << "\n";

          for (int fid = 0; fid < hset.featnum; fid++){
            write << "\tF:" << fid << "\t";

            int sumlen = hset.rptr[fid+1] - hset.rptr[fid]; 
            for (int j=0; j < sumlen; j++){ 
              auto offset = j + i*pagesize + hset.rptr[fid];
              write << hset.data[offset].sum_grad <<"," << hset.data[offset].sum_hess << " ";
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
    typename HistMakerBlockDenseSeq<TStats>::HistUnit hist;
    //unsigned istart;

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
    //inline void Add(bst_float fv,
    //                const std::vector<GradientPair> &gpair,
    //                const MetaInfo &info,
    //                const bst_uint ridx) {
    //  while (istart < hist.size && !(fv < hist.cut[istart])) ++istart;
    //  CHECK_NE(istart, hist.size);
    //  hist.data[istart].Add(gpair, info, ridx);
    //}
    ///*!
    // * \brief add a histogram to data,
    // * do linear scan, start from istart
    // */
    //inline void Add(bst_float fv,
    //                GradientPair gstats) {
    //  if (fv < hist.cut[istart]) {
    //    hist.data[istart].Add(gstats);
    //  } else {
    //    while (istart < hist.size && !(fv < hist.cut[istart])) ++istart;
    //    if (istart != hist.size) {
    //      hist.data[istart].Add(gstats);
    //    } else {
    //      LOG(INFO) << "fv=" << fv << ", hist.size=" << hist.size;
    //      for (size_t i = 0; i < hist.size; ++i) {
    //        LOG(INFO) << "hist[" << i << "]=" << hist.cut[i];
    //      }
    //      LOG(FATAL) << "fv=" << fv << ", hist.last=" << hist.cut[hist.size - 1];
    //    }
    //  }
    //}
  };
 

#ifndef USE_OMP_BUILDHIST
#endif

 /* --------------------------------------------------
  * data members
  */ 

  // workspace of thread
  ThreadWSpace wspace_;
  // reducer for histogram
  rabit::Reducer<TStats, TStats::Reduce> histred_;
  //
  std::vector<bst_uint> fwork_set_;
  // temp space to map feature id to working index
  std::vector<int> feat2workindex_;
  // set of index from fset that are current work set
  std::vector<bst_uint> work_set_;
  //no used feature set
  std::vector<bst_uint> dwork_set_;

  //binid as workset
  std::vector<bst_uint> bwork_set_;
  unsigned bwork_base_blknum_;
  //row blk scheduler
  //std::vector<spin_mutex> bwork_lock_;
  spin_mutex* bwork_lock_{nullptr};
  double datasum_;

  std::vector<bst_uint> fsplit_set_;

  // cached dmatrix where we initialized the feature on.
  const DMatrix* cache_dmatrix_{nullptr};
  // feature helper
  BaseMaker::FMetaHelper feat_helper_;
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
  BlockInfo blkInfo_;
  //node block, =d8 by default
  // !!!
  // unsigned check_mask_ = 0x80ffff00;
  // change the mask which is hard coded in CHECKHALFCOND
  // !!!
  int node_block_size{256};


  #ifndef USE_OMP_BUILDHIST
  ////task graph
  //TreeNode* tg_root;
  ////std::vector<GradientPair>& gpair_;
  //HostDeviceVector<GradientPair> *gpair_{nullptr};

  //int num_nodes_;
  #endif

 // hist mat compact
  DMatrixDenseCube* p_blkmat;
  DMatrixCompactBlockDense* p_hmat;

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
      
    double _tstartUpdate = dmlc::GetTime();
    printgh(gpair);

    double _tstartInitData = dmlc::GetTime();
    this->InitData(gpair, *p_fmat, *p_tree);
    this->tminfo.aux_time[5] += dmlc::GetTime() - _tstartInitData;
    //this->InitWorkSet(p_fmat, *p_tree, &fwork_set_);
    // mark root node as fresh.
    for (int i = 0; i < p_tree->param.num_roots; ++i) {
      (*p_tree)[i].SetLeaf(0.0f, 0);
    }

    /*
     * Initialize the histogram and DMatrixCompact
     */
    //printVec("ResetPos::fwork_set=", fwork_set_);
    // reset and propose candidate split
    //this->ResetPosAndPropose(gpair, p_fmat, fwork_set_, *p_tree);
    //printtree(p_tree, "ResetPosAndPropose");
    // initialize the histogram only
    InitializeHist(gpair, p_fmat, *p_tree);

    for (int depth = 0; depth < param_.max_depth; ++depth) {


      printVec("qexpand:", this->qexpand_);
      printVec("node2workindex:", this->node2workindex_);
      // create histogram
      double _tstart = dmlc::GetTime();
      this->CreateHist(depth, gpair, bwork_set_, *p_tree);
      this->tminfo.posset_time += dmlc::GetTime() - _tstart;

      printVec("position:", this->position_);
      printtree(p_tree, "After CreateHist");

      #ifdef USE_DEBUG_SAVE
      //this->wspace_.saveGHSum(treeid_, depth, this->qexpand_.size());
      #endif


      // find split based on histogram statistics
      this->FindSplit(depth, gpair, p_tree);
      //printtree(p_tree, "FindSplit");

      // reset position after split
      this->ResetPositionAfterSplit(NULL, *p_tree);
      //printtree(p_tree, "ResetPositionAfterSPlit");


      this->UpdateQueueExpand(*p_tree);
      //this->tminfo.aux_time[6] += dmlc::GetTime() - _tstart;
      printtree(p_tree, "UpdateQueueExpand");

      // if nothing left to be expand, break
      if (qexpand_.size() == 0) break;
    }

    if(this->fsplit_set_.size() > 0){
        //update the position for update cache
        //printVec("before updatepos:", this->position_);
        double _tstart = dmlc::GetTime();
        this->CreateHist(param_.max_depth, gpair, bwork_set_, *p_tree);
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

    /*
     * debug info
     */
    #ifdef USE_DEBUG
    std::cout << "SpinLock dirtyCnt:";
    for(int i=0; i< bwork_base_blknum_; i++){
        std::cout << bwork_lock_[i].getCnt() << ",";
        bwork_lock_[i].clear();
    }
    std::cout << "\n";
    #endif

    this->tminfo.aux_time[7] += dmlc::GetTime() - _tstartUpdate;
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
      //s.Add(hist.data[i]);
      s.Add(hist.Get(i));
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          //default goes to right
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i, false)) {
            *left_sum = s;
          }
        }
      }
    }
    s.Clear();
    for (int i = hist.size - 1; i >= 0; --i) {
      //s.Add(hist.data[i]);
      s.Add(hist.Get(i));
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          //default goes to left
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i-1, true)) {
            *left_sum = c;
          }
        }
      }
    }
  }


  void FindSplit(int depth,
                 const std::vector<GradientPair> &gpair,
                 RegTree *p_tree) {

    double _tstartFindSplit = dmlc::GetTime();

//#ifndef ALLOCATE_ALLNODES
//    //get first nid of this level
//    int firstnid = std::pow(2, depth) -1;
//#else 
//    int firstnid = 0;
//#endif

    //always work on work_set_ instead of fwork_set_
    const std::vector <bst_uint> &fset = work_set_;

    const size_t num_feature = fset.size();
    //printInt("FindSplit::num_feature = ", num_feature);
    //printVec("FindSplit::fset=", fset);
    // get the best split condition for each node
    std::vector<SplitEntry> sol(qexpand_.size());
    std::vector<TStats> left_sum(qexpand_.size());
    auto nexpand = static_cast<bst_omp_uint>(qexpand_.size());
    #pragma omp parallel for schedule(dynamic, 1)
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      int nid = qexpand_[wid];
      SplitEntry &best = sol[wid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];

#ifdef USE_VECTOR4MODEL
      TStats &node_sum = wspace_.hset.GetHistUnitByFid(num_feature, mid).data[0];
#else
      TStats &node_sum = wspace_.hset.GetNodeSum(mid);
#endif

      for (size_t i = 0; i < fset.size(); ++i) {
        int fid = fset[i];
        int fidoffset = this->feat2workindex_[fid];

        CHECK_GE(fidoffset, 0);
        EnumerateSplit(this->wspace_.hset.GetHistUnitByFid(fidoffset, mid),
                       node_sum, fid, &best, &left_sum[wid]);


        printSplit(best, fid, nid);
      }
    }
    // get the best result, we can synchronize the solution
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];

      const SplitEntry &best = sol[wid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];
#ifdef USE_VECTOR4MODEL
      const TStats &node_sum = wspace_.hset.GetHistUnitByFid(num_feature, mid).data[0];
#else
      const TStats &node_sum = wspace_.hset.GetNodeSum(mid);
#endif

      //
      //raw nid to access the tree
      //
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
        #ifdef USE_HALFTRICK
        //add empty childs anyway to keep the node id as the same as the full
        //binary tree
        //they are not fresh leaf, set parent = -1 to indicate in the prune process
        p_tree->AddDummyChilds(nid);
        //p_tree->AddChilds(nid);
        //(*p_tree)[(*p_tree)[nid].LeftChild()].SetLeaf(0.0f);
        //(*p_tree)[(*p_tree)[nid].RightChild()].SetLeaf(0.0f);
        //(*p_tree)[(*p_tree)[nid].LeftChild()].SetParent(-1);
        //(*p_tree)[(*p_tree)[nid].RightChild()].SetParent(-1);
        #endif
        //bugfix: setleaf should be after addchilds
        (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
 
      }
    }
    
    //end findSplit
    this->tminfo.aux_time[3] += dmlc::GetTime() - _tstartFindSplit;

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

    // init for all features
    auto fset = *p_fset;
    work_set_.clear();
    dwork_set_.clear();

    feat2workindex_.resize(tree.param.num_feature);
    std::fill(feat2workindex_.begin(), feat2workindex_.end(), -1);

    for (auto fidx : fset) {
      // Type: 0 empty, 1 binary, 2 real
      //if (feat_helper_.Type(fidx) == 2) {
      if (feat_helper_.Type(fidx) > 0) {
        feat2workindex_[fidx] = static_cast<int>(work_set_.size());
        work_set_.push_back(fidx);
      } else {
        feat2workindex_[fidx] = -2;
        dwork_set_.push_back(fidx);
      }
    }

    LOG(CONSOLE) << "Init workset:";
    printVec("full set:", fset);
    printVec("delete work_set_:", dwork_set_);
    printVec("work_set_:", work_set_);
    printVec("feat2workindex_:", feat2workindex_);
  }
  /*!
   * \brief this is helper function uses column based data structure,
   * \param nodes the set of nodes that contains the split to be used
   * \param tree the regression tree structure
   * \param out_split_set The split index set
   */
  inline void GetSplitSet(const std::vector<int> &nodes,
                          const RegTree &tree,
                          std::vector<unsigned>* out_split_set) {
    std::vector<unsigned>& fsplits = *out_split_set;
    fsplits.clear();
    // step 1, classify the non-default data into right places
    for (int nid : nodes) {
      if (!tree[nid].IsLeaf()) {
        fsplits.push_back(tree[nid].SplitIndex());
      }
    }
    std::sort(fsplits.begin(), fsplits.end());
    fsplits.resize(std::unique(fsplits.begin(), fsplits.end()) - fsplits.begin());
  }
 
  void ResetPositionAfterSplit(DMatrix *p_fmat,
                               const RegTree &tree){
    this->GetSplitSet(this->qexpand_, tree, &this->fsplit_set_);
  }



  /*
   * initialize the proposal for only one time
   */
  void InitializeHist(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const RegTree &tree) {
    if (!isInitializedHistIndex && this->qexpand_.size() == 1) {
        
        double _tstartInit = dmlc::GetTime();

        auto& fset = fwork_set_;

        this->InitWorkSet(p_fmat, tree, &fset);

        const MetaInfo &info = p_fmat->Info();

        /* Initilize the histgram
         */
        cut_.Init(p_fmat,param_.max_bin /*256*/);

        /*
         * Debug on cut, higgs feature fid=8
         * 0.0
         * 1.0865380764007568
         * 2.1730761528015137
         *
         */
        #ifdef DEBUG
        {
            auto a = cut_[8];
            std::cout << "higgs[8] cut size:" << a.size << "=" ;
            for (size_t i = 0; i < a.size; ++i) {
                std::cout << a.cut[i] << ",";
            }
            std::cout << "min_val=" << cut_.min_val[8] << "\n";

        }
        #endif

        /*
        // now we get the final result of sketch, setup the cut
        // layout of wspace_.cut  (feature# +1) x (cut_points#)
        //    <cut_pt0, cut_pt1, ..., cut_ptM>
        //    cut_points# is variable length, therefore using .rptr
        //    the last row is the nodeSum
        */
        this->wspace_.cut.clear();
        this->wspace_.rptr.clear();
        this->wspace_.rptr.push_back(0);
        {
          for (unsigned int fid : fset) {
            int offset = feat2workindex_[fid];
            if (offset >= 0) {
              auto a = cut_[fid];

              for (size_t i = 0; i < a.size; ++i) {
                this->wspace_.cut.push_back(a.cut[i]);
              }
              // skip this part, which is already inside cut_
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
              LOG(CONSOLE) << "Special Colulum:" << fid << ",cpt=" << cpt;

              //this->wspace_.cut.push_back(cpt + fabs(cpt) + kRtEps);
              //this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
              //this->wspace_.min_val.push_back(cut_.min_val[fid]);
            }
 
          }

          // reserve last value for global statistics
          this->wspace_.cut.push_back(0.0f);
          this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
        }
        CHECK_EQ(this->wspace_.rptr.size(),
                 //(fset.size() + 1) * this->qexpand_.size() + 1);
                 (work_set_.size() + 1) * this->qexpand_.size() + 1);


        /*
        * OptApprox:: init bindid in p_fmat
        */
        auto _info = p_fmat->Info();
#ifdef USE_VECTOR4MODEL
        this->blkInfo_.init(_info.num_row_, _info.num_col_+1, param_.max_bin);
#else
        this->blkInfo_.init(_info.num_row_, _info.num_col_, param_.max_bin);
#endif

#ifdef ALLOCATE_ALLNODES
        this->wspace_.Init(this->param_, std::pow(2,this->param_.max_depth+1), this->blkInfo_);
#else
        this->wspace_.Init(this->param_, std::pow(2,this->param_.max_depth), this->blkInfo_);
#endif

        this->SetDefaultPostion(p_fmat, tree);

        unsigned int nthread = omp_get_max_threads();
        this->thread_hist_.resize(omp_get_max_threads());
        for (unsigned int i=0; i< nthread; i++){
          //make memory access separate
          thread_hist_[i].resize(64);
        }

        this->InitHistIndex(p_fmat, fset, tree);

        this->InitHistIndexByRow(p_fmat, tree);

        //DEBUG
        #ifdef USE_DEBUG
        printcut(this->cut_);

        printmsg("SortedColumnBatch");
        printdmat(*p_fmat->GetSortedColumnBatches().begin());

        printmsg("RowBatch");
        printdmat(*p_fmat->GetRowBatches().begin());
        #endif

        this->isInitializedHistIndex = true;
        /*
         * build blkmat(block matrix) and hmat(column matrix)
         */
        p_blkmat->Init(*p_fmat->GetRowBatches().begin(), p_fmat->Info(), param_.max_bin, blkInfo_);
        p_hmat->Init(*p_fmat->GetSortedColumnBatches().begin(), p_fmat->Info());

        #ifdef USE_DEBUG
        printdmat(*p_hmat);
        printdmat(*p_blkmat);

        /*
         * Cube Info
         */
        std::cout << "Cube Statistics:\n";
        for(int i=0; i < p_blkmat->GetBaseBlockNum(); i++){
            auto zcol = p_blkmat->GetBlockZCol(i);
            std::cout << "blk " << i <<":" << "rowsize:" <<
                zcol.getRowSize() << ", datasize:" << zcol.getDataSize() << ":"; 
            unsigned len = 0;
            for(int j=0; j < zcol.GetBlockNum(); j++){
                len += zcol.GetBlock(j).size();
                std::cout << zcol.GetBlock(j).size() << ",";
            }
            std::cout <<" total=" << len << "\n";
        }
        #endif

        // the map used by scheduler
        //this->bwork_base_blknum_ = this->blkInfo_.GetBaseBlkNum(_info.num_col_+1, param_.max_bin);
        this->bwork_base_blknum_ = p_blkmat->GetBaseBlockNum();
        bwork_lock_ = new spin_mutex[bwork_base_blknum_];
        //for(int i=0; i < p_blkmat->GetBaseBlockNum(); i++){
        //    bwork_lock_[i].unlock();
        //}
 
        //fwork_set_ --> features set
        //work_set_ --> blkset, (binset) in (0,0,1)
        bwork_set_.clear();
        for(int i=0; i < p_blkmat->GetBlockNum(); i++){
            bwork_set_.push_back(i);
        }

        LOG(CONSOLE) << "Init bwork_set_: base_blknum=" << bwork_base_blknum_ <<
            ":" << bwork_set_.size();
        printVec("bwork_set_:", bwork_set_);

        #ifndef USE_OMP_BUILDHIST
        ////init the task graph
        ////tbb::task_scheduler_init init;
        //tg_root = TreeMaker::create(*p_hmat, blockSize_ );

        //std::cout << "TaskTree Depth:" << TreeNode::getTreeDepth(tg_root) << ", blockSize:" << blockSize_ << ", param.block_size:" 
        //    << param_.block_size << "\n";
        //#ifdef USE_DEBUG
        //TreeNode::printTree(tg_root, 0);
        //#endif

        ////tbb::task_scheduler_init init(omp_get_max_threads());
        #endif
        
        //re-init the model memory space, first touch
#ifdef ALLOCATE_ALLNODES
        this->wspace_.Init(this->param_, std::pow(2,this->param_.max_depth+1), this->blkInfo_);
#else
        this->wspace_.Init(this->param_, std::pow(2,this->param_.max_depth), this->blkInfo_);
#endif


        this->tminfo.aux_time[0] += dmlc::GetTime() - _tstartInit;
        /*
         * end of initialization, write flag file
         */
        startVtune("vtune-flag.txt");
        LOG(INFO) << "End of initialization, start training";

        this->tminfo.trainstart_time = dmlc::GetTime();


    }// end if(isInitializedHistIndex)
    else{
        double _tstartInit = dmlc::GetTime();

        //double _tstartInit = dmlc::GetTime();
        //init for each tree
        unsigned int nthread = omp_get_max_threads();
        this->thread_hist_.resize(omp_get_max_threads());
        for (unsigned int i=0; i< nthread; i++){
          //make memory access separate
          thread_hist_[i].resize(64);
        }

        auto _info = p_fmat->Info();
#ifdef USE_VECTOR4MODEL
        this->blkInfo_.init(_info.num_row_, _info.num_col_+1, param_.max_bin);
#else
        this->blkInfo_.init(_info.num_row_, _info.num_col_, param_.max_bin);
#endif
        //this->wspace_.Init(this->param_, 1, std::pow(2,this->param_.max_depth+1) /*256*/, blkInfo_);
        //this->wspace_.ClearData();

        this->tminfo.aux_time[8] += dmlc::GetTime() - _tstartInit;
    }
  }

  /*
   * Init binid col-wise
   */
  void InitHistCol(const SparsePage::Inst &col,
                            const RegTree &tree,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {

    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    //hbuilder.resize(tree.param.num_nodes);

    //LOG(CONSOLE) << "InitHistCol: num_nodes=" << tree.param.num_nodes <<
    //        ", qexpand.size=" << this->qexpand_.size() ;

    //for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      //const unsigned nid = this->qexpand_[i];
      const unsigned nid = 0;
      hbuilder[nid].hist = this->wspace_.hset.InitGetHistUnitByFid(fid_offset,nid);
    //}
    for (auto& c : col) {
      const bst_uint ridx = c.index;
      const int nid = this->position_[ridx];
      if (nid >= 0) {
          CHECK_EQ(nid, 0);
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

      //thread_hist_.resize(omp_get_max_threads());

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
                                offset,
                                &thread_hist_[omp_get_thread_num()]);
          }
        }
      }
  }

  /*
   * Init the row-matrix
   *
   */
  void InitHistRow(const SparsePage::Inst &row,
                            const RegTree &tree,
                            const bst_omp_uint rid,
                            std::vector<HistEntry> *p_temp) {

    if (row.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    //hbuilder.resize(tree.param.num_nodes);

    //LOG(CONSOLE) << "InitHistCol: num_nodes=" << tree.param.num_nodes <<
    //        ", qexpand.size=" << this->qexpand_.size() ;

    for (auto& c : row) {

      const bst_uint fid_offset = feat2workindex_[c.index];
      const bst_uint nid = 0;

      hbuilder[nid].hist = this->wspace_.hset.InitGetHistUnitByFid(fid_offset,nid);
      unsigned binid = hbuilder[nid].GetBinId(c.fvalue);
      //mapping index(raw fid) to working fid
      c.addBinid(binid, fid_offset);
    }
  } 


  void InitHistIndexByRow( DMatrix *p_fmat,
                     const RegTree &tree){

      //unsigned int nthread = omp_get_max_threads();
      //thread_hist_.resize(omp_get_max_threads());
      //for (unsigned int i=0; i< nthread; i++){
      //    //make memory access separate
      //    thread_hist_[i].resize(64);
      //}

      // start accumulating statistics
      unsigned long total=0;
      for (const auto &batch : p_fmat->GetRowBatches()) {
        const auto nsize = static_cast<bst_omp_uint>(batch.Size());
        total += nsize;

        //#pragma omp parallel for schedule(dynamic, 1)
        #pragma omp parallel for schedule(guided)
        for (bst_omp_uint rid = 0; rid < nsize; ++rid) {
            this->InitHistRow(batch[rid], tree, rid,
                                &thread_hist_[omp_get_thread_num()]);
        }
      }

      LOG(CONSOLE)<< "Init HistIndexByRow: nsize:" << total;
  }





/*
 * halftrick 
 *
 */
  #ifdef USE_HALFTRICK
  //#define CHECKHALFCOND (nid>=0 && (nid&1)==0)
  #define CHECKHALFCOND ((nid & 0x80000001) ==0)
  //#define CHECKHALFCOND ((nid & 0x80ffff01) == nblkid)
  #else
  #define CHECKHALFCOND (nid>=0)
  // check the node blkid
  //#define CHECKHALFCOND ((nid & 0x80ffff00) == nblkid)
  #endif

  //half trick
  void UpdateHalfTrick(bst_uint blkid_offset,
                       const RegTree &tree,
                       std::vector<HistEntry> *p_temp) {

    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];

      hbuilder[nid].hist = this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, nid);
    }


    if(0){
        /*
         * this code depend on the full entries in 
         * qexpand, which fails when there are deleted 
         * nodes in deep tree
         */
        //get the right node
        const unsigned nid_start = this->qexpand_[0];

        CHECK_NE(nid_start % 2, 0);
        unsigned nid_parent = (nid_start+1)/2-1;
        for (size_t i = 0; i < this->qexpand_.size(); i+=2) {
          const unsigned nid = this->qexpand_[i];
          auto parent_hist = this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, nid_parent + i/2);
          #pragma ivdep
          #pragma omp simd
          for(int j=0; j < hbuilder[nid].hist.size; j++){
            hbuilder[nid].hist.Get(j).SetSubstract(
                    parent_hist.Get(j),
                    hbuilder[nid+1].hist.Get(j));
          }

        }
    }

    /*
     * more general version
     *
     */
    {
       for (size_t i = 0; i < this->qexpand_.size(); i++) {
         const unsigned nid = this->qexpand_[i];
         
         // skip right nodes which are already updated in UpdateHistBlock
         //if(CHECKHALFCOND) continue;
         // as nodes in qexpand should always > 0
         // we can simply check even and odd
         if ((nid & 1)==0) continue;

         //left child not calculated yet
         auto parent_hist = this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, nid/2);
         #ifdef NOUSE_DIRECTPTR
         #pragma ivdep
         #pragma omp simd
         for(int j=0; j < hbuilder[nid].hist.size; j++){
             hbuilder[nid].hist.Get(j).SetSubstract(
                   parent_hist.Get(j),
                   hbuilder[nid+1].hist.Get(j));
         }

         #else
         /*
          * two plain substraction
          * start : hist.data
          * size  : GradStat * hist.size
          */
          double* p_parent = reinterpret_cast<double*>(parent_hist.data);
          double* p_left = reinterpret_cast<double*>(hbuilder[nid].hist.data);
          double* p_right = reinterpret_cast<double*>(hbuilder[nid+1].hist.data);
          #pragma ivdep
          #pragma omp simd
          for(int j=0; j < 2 * hbuilder[nid].hist.size; j++){
              p_left[j] = p_parent[j] - p_right[j];
          }

          #endif

        }
    }
    //end 
  }

  void UpdateHalfTrickCompact(bst_uint blkid_offset,
                       const RegTree &tree,
                       std::vector<HistEntry> *p_temp) {

    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); i++) {
      const unsigned nid = this->qexpand_[i];
      
      // calc the left for all existing right nodes which are already updated in UpdateHistBlock
      //if(CHECKHALFCOND) continue;
      // as nodes in qexpand should always > 0
      // we can simply check even and odd
      if ((nid & 1)==1){
        /*
         * two plain substraction
         * start : hist.data
         * size  : GradStat * hist.size
         */
        int mid_parent = node2workindex_[nid/2];
        int mid_right = node2workindex_[nid+1];
        
        CHECK_NE(mid_parent, -1);
        CHECK_NE(mid_right, -1);

        double* p_left = reinterpret_cast<double*>(
                this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, mid_parent).data);

        double* p_right = reinterpret_cast<double*>(
                this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, mid_right).data);

        #pragma ivdep
        #pragma omp simd
        for(int j=0; j < 2 * hbuilder[nid].hist.size; j++){
            //halftrick will have a full binary tree
            //reuse the parent storage for left nodes
            p_left[j] -= p_right[j];
        }

        // update index for left node
        // reuse the pranet mid
        node2workindex_[nid] = mid_parent;
      }

    }
  }


  //
  //single block 
  //
  void UpdateHistBlock(const int depth,
                       const std::vector<GradientPair> &gpair,
                       const DMatrixDenseCubeBlock &block,
                       const MetaInfo &info,
                       const RegTree &tree,
                       bst_uint blkid_offset,
                       unsigned int zblkid,
                       unsigned int nblkid,
                       std::vector<HistEntry> *p_temp) {
    //check size
    if (block.size() == 0) return;

    #ifdef USE_DEBUG
    //std::cout << "updateHistBlock: blkoffset=" << blkid_offset <<
    //    ", zblkid=" << zblkid << ",baserowid=" << block.base_rowid_ <<
    //    ", len=" << block.size() 
    //    << "\n";
    #endif

    //get lock
    bwork_lock_[blkid_offset].lock();

//#ifndef ALLOCATE_ALLNODES
//    //get first nid of this level
//    int firstnid = std::pow(2, depth) -1;
//#else 
//    int firstnid = 0;
//#endif

    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);
    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      unsigned nid = this->qexpand_[i];
      //const unsigned wid = this->node2workindex_[nid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];

      hbuilder[nid].hist = this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, mid);

      //init data for the first zblks
      if (zblkid == 0){
        if (CHECKHALFCOND) {
            //only clear the data for 'right' nodes in USE_HALFTRICK mode
            hbuilder[nid].hist.ClearData();
        }
      }
    }

    //int blockNum = zcol.GetBlockNum();
    //for(int blkid = 0; blkid < blockNum; blkid++){
    //    const auto block = zcol.GetBlock(blkid);
    {
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
                  for (int k = 0; k < block.rowsize(j+i); k++){
                    hbuilder[nid].AddWithIndex(block._blkaddr(j+i, k), buf_gpair[i]);
                  }

                }
            }
          }
          for (bst_uint j = align_length; j < block.size(); ++j) {
            const bst_uint ridx = block._index(j);
            //const int nid = this->DecodePosition(ridx);
            const int nid = this->position_[ridx];

            if (CHECKHALFCOND) {
                  for (int k = 0; k < block.rowsize(j); k++){
                      hbuilder[nid].AddWithIndex(block._blkaddr(j, k), gpair[ridx]);
                  }
            }
          }
        } else {
          //#pragma ivdep
          //#pragma omp simd
          for (bst_uint j = 0; j < block.size(); ++j) {
            const bst_uint ridx = block._index(j);
            //const int nid = this->DecodePosition(ridx);
            int nid = this->position_[ridx];
            if (CHECKHALFCOND) {

              for (int k = 0; k < block.rowsize(j); k++){

                hbuilder[nid].AddWithIndex(block._blkaddr(j, k), gpair[ridx]);

                /*
                 * not much benefits from short->byte
                 */
                //unsigned short blkaddr = this->blkInfo_.GetBlkAddr(block._blkaddr(j, k), k);
                //unsigned short blkaddr = block._blkaddr(j, k)*2 + k;
                //hbuilder[nid].AddWithIndex(blkaddr, gpair[ridx]);

                //debug only
                #ifdef DEBUG
                this->datasum_ += block._blkaddr(j,k);
                #endif
              }
            }
          }
        }
    } /*blk*/

    //quit
    bwork_lock_[blkid_offset].unlock();

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

          const bst_uint ridx = col._index(j);
          const bst_uint binid = col._binid(j);

          //const int nid = this->DecodePosition(ridx);
          
          //check if it's a non-fresh leaf node
          const int nid = position_[ridx];
          if (nid < 0){
              continue;
          }

          CHECK(tree[nid].IsLeaf());

          //#ifdef USE_HALFTRICK
          ////// if parent is leaf and not fresh, then this node is a dummy node, skip it
          //if (tree[nid].IsDummy()){
          //  continue;
          //}
          //#endif

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

      double _tstart = dmlc::GetTime();
      CHECK_GT(out_preds.size(), 0U);

      //get leaf_value for all nodes
      const auto nodes = p_last_tree_->GetNodes();
      std::vector<float> leaf_values;
      leaf_values.resize(nodes.size());

      for (int nid = 0; nid < nodes.size(); nid ++){
          bst_float leaf_value;
          int tnid = nid;

          //skip dummy nodes first
          if ((*p_last_tree_)[tnid].IsDummy()) {
              continue;
          }

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
        //const int nid = this->position_[ridx];
        //if (nid>=0){
            //update   
            out_preds[ridx] += leaf_values[nid];
        //}
      }

      //LOG(CONSOLE) << "UpdatePredictionCache: nodes size=" << 
      //    nodes.size() << ",rowscnt=" << nrows;

      this->tminfo.aux_time[2] += dmlc::GetTime() - _tstart;
      printVec("updatech pos:", this->position_);
      printVec("updatech leaf:", leaf_values);
      printVec("updatech pred:", out_preds);
      return true;
    }
  }


  void CreateHist(const int depth,
                  const std::vector<GradientPair> &gpair,
                  const std::vector<bst_uint> &blkset,
                  const RegTree &tree) {
    const MetaInfo &info = p_hmat->Info();

//#ifndef ALLOCATE_ALLNODES
//    //get first nid of this level
//    int firstnid = std::pow(2, depth) -1;
//#else 
//    int firstnid = 0;
//#endif



    // start to work
    {
      //this->thread_hist_.resize(omp_get_max_threads());

      //init the position_
      this->SetDefaultPostion(p_hmat, tree);
      this->CorrectNonDefaultPositionByBatch2(*p_hmat, this->fsplit_set_, tree);

      if (this->qexpand_.size() == 0){
        //last step to update position 
        return;
      }


      {
        // start enumeration
        double _tstart = dmlc::GetTime();

        #ifdef USE_OMP_BUILDHIST
        // block number on the base plain
        const auto nsize = p_blkmat->GetBaseBlockNum();
        // block number in the row dimension
        const auto zsize = p_blkmat->GetBlockZCol(0).GetBlockNum();

        // node dimension blocks
        //const int num_leaves = std::pow(2, depth);
        //const int dsize = (num_leaves + node_block_size -1)/ node_block_size;
        const int dsize = 1;

        #ifdef USE_DEBUG
        this->datasum_ = 0.;
        #endif

        #pragma omp parallel for schedule(dynamic, 1)
        for(bst_omp_uint i = 0; i < dsize * nsize * zsize; ++i){

          // node block id
          unsigned int nblkid = (i / (nsize *zsize)) << 8;
          // absolute blk id
          int blkid = i % (nsize * zsize);
          // blk id on the base plain
          int offset = blkid % nsize;
          // blk id on the row dimension
          unsigned int zblkid = blkid / nsize;


          // get dataBlock
          auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

          // update model by this dataBlock and node_blkid
          this->UpdateHistBlock(depth, gpair, block, info, tree,
                offset, zblkid, nblkid,
                &this->thread_hist_[omp_get_thread_num()]);
                //&this->thread_hist_[0]);
        }

        //build the other half
        #ifdef USE_HALFTRICK
        if (this->qexpand_[0] != 0){

            double _tstart2 = dmlc::GetTime();
            //#pragma omp parallel for schedule(dynamic, 1)
            #pragma omp parallel for schedule(static)
            for (bst_omp_uint i = 0; i < nsize; ++i) {
              int offset = i;

              #ifdef ALLOCATE_ALLNODES
              this->UpdateHalfTrick(offset, tree,
                    &this->thread_hist_[omp_get_thread_num()]);
                    //&this->thread_hist_[0]);
              #else
              this->UpdateHalfTrickCompact(offset, tree,
                    &this->thread_hist_[omp_get_thread_num()]);
              #endif
 
            }
            this->tminfo.aux_time[1] += dmlc::GetTime() - _tstart2;
        }
        #endif

        #ifdef USE_DEBUG
        std::cout << "BuildHist:: datasum_=" << this->datasum_;
        #endif

        #else
        /*
         * TBB scheduler 
         */
        #endif
        this->tminfo.buildhist_time += dmlc::GetTime() - _tstart;
      } // end of one-page

      // update node statistics.
      double _tstartSum = dmlc::GetTime();
      this->GetNodeStats(gpair, *p_hmat, tree,
                         &(this->thread_stats_), &(this->node_stats_));
      for (size_t i = 0; i < this->qexpand_.size(); ++i) {
        const int nid = this->qexpand_[i];
        //const int wid = this->node2workindex_[nid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];
#ifdef USE_VECTOR4MODEL
        this->wspace_.hset.GetHistUnitByFid(work_set_.size(),mid)
            .data[0] = this->node_stats_[nid];
#else
        this->wspace_.hset.GetNodeSum(mid)
                     = this->node_stats_[nid];
#endif

      }
      this->tminfo.aux_time[6] += dmlc::GetTime() - _tstartSum;
    }
    //this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset.data),
    //                        this->wspace_.hset.data.size());

    //save the last tree point
    //p_last_tree_ = &tree;
  }

  /*
   * Reset the splitcont to fvalue
   */
  void ResetTree(RegTree& tree){

    double _tstart = dmlc::GetTime();

    const auto nodes = tree.GetNodes();
    for(int i=0; i < nodes.size(); i++){
        if (tree[i].IsLeaf() || tree[i].IsDeleted() || tree[i].IsDummy()){
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

        int offset = feat2workindex_[fid];
        CHECK_GE(offset,0);
        if (binid == -1 && defaultLeft){
            //leftmost
            fvalue = this->wspace_.min_val[offset];
        }
        else{
            fvalue = this->wspace_.cut[this->wspace_.rptr[offset] + binid];
        }
        tree[i].SetSplit(fid, fvalue, defaultLeft);
    }

    //update the position for update cache
    //this->CorrectNonDefaultPositionByBatch2(*p_hmat, this->fsplit_set_, tree);

    p_last_tree_ = &tree;

    //end ResetTree
    this->tminfo.aux_time[4] += dmlc::GetTime() - _tstart;

  }


};


XGBOOST_REGISTER_TREE_UPDATER(HistMakerBlockDenseSeq, "grow_blockdense_seq")
.describe("Tree constructor that uses approximate global of histogram construction.")
.set_body([]() {
    return new HistMakerBlockDenseSeq<GradStats>();
  });
}  // namespace tree
}  // namespace xgboost
