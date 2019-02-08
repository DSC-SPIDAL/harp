/*!
 * Copyright 2014 by Contributors
 * \file updater_HistMakerBlockLossguide.cc
 * \brief use histogram counting to construct a tree
 * \author Tianqi Chen
 *
 * 2018,2019
 * HARPDAAL-GBT optimize based on the approx and fast_hist codebase
 * 3-D cube of model <node_block_size, bin_block_size, ft_block_size)
 *
 */
#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
#include <atomic>
#include <algorithm>
#include <fstream>
#include <dmlc/timer.h>
#include "../data/compact_dmatrix.h"
#include "../common/sync.h"
#include "../common/quantile.h"
#include "../common/group_data.h"
#include "../common/debug.h"
#include "../common/hist_util.h"
#include "./fast_hist_param.h"
#include "../common/random.h"
#include "../common/bitmap.h"
#include "../common/sync.h"
#include "./updater_block_base_lossguide.h"


namespace xgboost {
namespace tree {

using xgboost::common::HistCutMatrix;

DMLC_REGISTRY_FILE_TAG(updater_block_lossguide);

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


//template<typename TStats, typename TDMatrixCube, typename TDMatrixCubeBlock>
template<typename TStats, typename TBlkAddr, template<typename> class TDMatrixCube, template<typename> class TDMatrixCubeBlock>
class HistMakerBlockLossguide: public BlockBaseMakerLossguide<TStats> {
 public:

 HistMakerBlockLossguide(){

      this->isInitializedHistIndex_ = false;
      p_blkmat = new TDMatrixCube<TBlkAddr>();
      p_hmat = new TDMatrixCube<unsigned char>();
      //p_hmat = new DMatrixCompactBlockDense();
  }

  ~HistMakerBlockLossguide(){
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
      //tminfo.posset_time -= tminfo.buildhist_time;
      return this->tminfo;
  }

  void Update(HostDeviceVector<GradientPair> *gpair,
              DMatrix *p_fmat,
              const std::vector<RegTree*> &trees) override {
    TStats::CheckInfo(p_fmat->Info());
    // rescale learning rate according to size of trees
    float lr = param_.learning_rate;
    param_.learning_rate = lr / trees.size();


    // build tree
    for (auto& tree : trees) {
      this->Update(gpair->ConstHostVector(), p_fmat, tree);
    }
    param_.learning_rate = lr;


  }

 protected:

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

    //lazy init
    inline bool isNull(){
        return data == nullptr;
    }
    inline void setNull(){
        data = nullptr;
    }

    // constructor
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

    // get i item from *data when not fid wised layout
    TStats& Get(int i) const{
        return data[(i/block_height)*cross_block_step + (i % block_height) * in_block_step];
    }

    void ClearData(){
        std::memset(data, 0., sizeof(GradStats) * size);
    }

  };

  struct HistEntry {
    typename HistMakerBlockLossguide<TStats,TBlkAddr, TDMatrixCube,TDMatrixCubeBlock>::HistUnit hist;
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

  };

  //
  // Compact structure for BuildHist
  //
  struct HistEntryCompact {
    TStats *data=nullptr;

    HistEntryCompact() = default;
    HistEntryCompact(TStats* _data):data(_data){}

    inline void AddWithIndex(unsigned binid,
                    GradientPair gstats) {
        #ifdef USE_BINID
        data[binid].Add(gstats);
        #endif
    }
    inline bool isNull(){
        return data == nullptr;
    }
    inline void setNull(){
        data = nullptr;
    }
    void ClearData(int size){
        if (data != nullptr){
            std::memset(data, 0., sizeof(GradStats) * size);
        }
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

    inline HistEntryCompact GetHistUnitByBlkidCompact(size_t blkid, size_t nid) {
      return HistEntryCompact(&data[0] + (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize + nid));
    }
    inline int GetHistUnitByBlkidSize(){
        return pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize();
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
#ifdef USE_VECTOR4MODEL
                      pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize()*pblkInfo->GetFeatureBlkNum(featnum+1)*nodeSize);
#else
                      pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize()*pblkInfo->GetFeatureBlkNum(featnum)*nodeSize);
#endif
    }



  };

   /*
   * for applysplit
   */
  struct SplitInfo{
    // best entry
    std::vector<SplitEntry> sol;
    std::vector<TStats> left_sum;

    inline void clear(){
        sol.clear();
        left_sum.clear();
    }

    inline void append(const SplitEntry& s, const TStats& l){
        sol.push_back(s);
        left_sum.push_back(l);
    }

  };

  /*
   * for output of applysplit
   * children nodes statistics
   *
   */
  struct SplitResult{
      struct SplitStat{
        int depth;
        int left;
        int left_len;
        int right;
        int right_len;

        SplitStat():depth(-1),left(-1),right(-1),left_len(0),right_len(0){}
        SplitStat(int d, int l, int ll, int r, int rl):
            depth(d),left(l),right(r),left_len(ll),right_len(rl){}
      };
    std::vector<SplitStat> splitResult;

    inline void resize(int newSize){
        splitResult.resize(newSize);
    }
    inline void clear(int i){
        splitResult[i] = SplitStat();
    }
    inline void set(int i, int depth, int left, int left_len, int right, int right_len){
        splitResult[i] = SplitStat(depth, left, left_len, right, right_len);
    }


    //
    // create the children node list for buildhist
    //
    void getResultNodeSet(std::vector<int>& nodesetSmall, 
            std::vector<int>& depthSmall, 
            std::vector<int>& nodesetLarge,
            std::vector<int>& depthLarge){
        //
        // filter out the unsplit nodes
        //
#ifdef USE_HALFTRICK_EX
        //
        // in HALFTRICK mode, split children into small and large ones
        //
        nodesetSmall.resize(0);
        nodesetLarge.resize(0);
        depthSmall.resize(0);
        depthLarge.resize(0);


        for(int i = 0; i < splitResult.size(); i++){
            if (splitResult[i].left < 0) continue;

            if (splitResult[i].left_len < splitResult[i].right_len){
                nodesetSmall.push_back( splitResult[i].left );
                nodesetLarge.push_back( splitResult[i].right );
            }
            else{
                nodesetSmall.push_back( splitResult[i].right );
                nodesetLarge.push_back( splitResult[i].left );
            }

            depthSmall.push_back( splitResult[i].depth + 1);
            depthLarge.push_back( splitResult[i].depth + 1);
        }
#else
        //
        // in normal mode, set large set empty
        //
        nodesetSmall.resize(0);
        nodesetLarge.resize(0);
        depthSmall.resize(0);
        depthLarge.resize(0);


        for(int i = 0; i < splitResult.size(); i++){
            if (splitResult[i].left < 0) continue;

            nodesetSmall.push_back( splitResult[i].left );
            nodesetSmall.push_back( splitResult[i].right );
            depthSmall.push_back( splitResult[i].depth + 1 );
            depthSmall.push_back( splitResult[i].depth + 1 );
        }

#endif


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
      //for (int tid = 0; tid < nthread; ++tid) 
      {
        hset.rptr = dmlc::BeginPtr(rptr);
        hset.cut = dmlc::BeginPtr(cut);
        hset.fsetSize = rptr.back();
        hset.featnum = rptr.size() - 2;
        hset.nodeSize = nodesize;

        hset.pblkInfo = &blkinfo;

        //
        //  <binid, nid, fid> layout means hole in the plain
        //  simple solution to allocate full space
        //  other than resort to remove the holes
        //
        unsigned long cubesize = blkinfo.GetModelCubeSize(param.max_bin, hset.featnum, nodesize);

        LOG(CONSOLE)<< "Init hset(memset): rptrSize:" << rptr.size() <<
            ",cutSize:" <<  cut.size() <<",nodesize:" << nodesize <<
            ",fsetSize:" << rptr.back() << ",max_depth:" << param.max_depth << 
            ",featnum:" << hset.featnum <<
            ",cubesize:" << cubesize << ":" << 8*cubesize/(1024*1024*1024) << "GB";

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

      }

   }

    void release(){
        if (hset.data != nullptr){
            std::free(hset.data);
        }
        if (hset.nodesum != nullptr){
            std::free(hset.nodesum);
        }
    }

    void ClearData(){
        // just clear the data
        // when GHSum=16GB, initialize take a long time in seconds
        // this function will not be called, now use thread local HistUnit.ClearData directly
        std::memset(hset.data, 0., sizeof(GradStats) * hset.size);
    }

    /*! \brief clear the workspace */
    inline void Clear() {
      cut.clear(); rptr.resize(1); rptr[0] = 0;
    }
    /*! \brief total size */
    inline size_t Size() const {
      return rptr.size() - 1;
    }

    //debug 
    #ifdef USE_DEBUG_SAVE
    void saveGHSum(int treeid, int depth, int nodecnt){
    }
    #endif


  };


 /* --------------------------------------------------
  * data members
  *                                                   
  */

  // workspace of thread
  ThreadWSpace wspace_;
  // reducer for histogram
  rabit::Reducer<TStats, TStats::Reduce> histred_;

  // temp space to map feature id to working index
  std::vector<int> feat2workindex_;
  // all features
  std::vector<bst_uint> fwork_set_;
 // set of index from fset that are current work set
  std::vector<bst_uint> work_set_;
  // no used feature set
  std::vector<bst_uint> dwork_set_;

  // workset of blocks
  std::vector<bst_uint> bwork_set_;
  unsigned bwork_base_blknum_;
  //row blk scheduler
  //std::vector<spin_mutex> bwork_lock_;
  spin_mutex* bwork_lock_{nullptr};

  // cached dmatrix where we initialized the feature on.
  const DMatrix* cache_dmatrix_{nullptr};
  // feature helper
  typename BlockBaseMakerLossguide<TStats>::FMetaHelper feat_helper_;
  
  // base template class members
  using  BlockBaseMakerLossguide<TStats>::param_;
  using  BlockBaseMakerLossguide<TStats>::node2workindex_;
  using  BlockBaseMakerLossguide<TStats>::posset_;
  using  BlockBaseMakerLossguide<TStats>::tminfo;
  using  BlockBaseMakerLossguide<TStats>::qexpand_;

  using BlockBaseMakerLossguide<TStats>::FMetaHelper;
  
  //
  // thread local data
  //
  // used to hold statistics
  std::vector<std::vector<TStats> > thread_stats_;
  // used to hold start pointer
  std::vector<std::vector<HistEntry> > thread_hist_;
  std::vector<std::vector<HistEntryCompact>> thread_histcompact_;
  //thread level findsplit
  std::vector<SplitInfo> thread_splitinfo_;

  // todo, remove this
  // node statistics
  std::vector<TStats> node_stats_;

  //HistCutMatrix
  HistCutMatrix cut_;
  // flag of initialization
  bool isInitializedHistIndex_;
 
  // block configure
  BlockInfo blkInfo_;
  int max_leaves_;

  // hist mat compact
  MetaInfo dmat_info_;
  TDMatrixCube<TBlkAddr>* p_blkmat;
  //todo, use dense cube for dense input in case of bin_block set
  TDMatrixCube<unsigned char>* p_hmat;
  //todo, replace pointers with object
  //TDMatrixCube<TBlkAddr> blkmat_;
  //TDMatrixCube<unsigned char> hmat_;
 
  //for predict cache
  const RegTree* p_last_tree_;
  int treeid_{0};
  
  //debug info
  double datasum_;

  /* ---------------------------------------------------
   * functions
   * ---------------------------------------------------
   */


  /*
   * update function implementation
   */
  void Update(const std::vector<GradientPair> &gpair,
                      DMatrix *p_fmat,
                      RegTree *p_tree) {
      
    double _tstartUpdate = dmlc::GetTime();
    printgh(gpair);

    // mark root node as fresh.
    CHECK_EQ(p_tree->param.num_roots, 1);
    for (int i = 0; i < p_tree->param.num_roots; ++i) {
      (*p_tree)[i].SetLeaf(0.0f, 0);
    }

    // Initialize the histogram and model related
    InitializeHist(gpair, p_fmat, *p_tree);

    // init the posset before building the tree
    this->InitPosSet(gpair, *p_fmat, *p_tree, 
            max_leaves_,
            blkInfo_.GetRowBlkSize());
    // init the gh sum in the beginning
    this->InitNodeStats(gpair, 
            &(this->thread_stats_), 
            &(this->wspace_.hset.GetNodeSum(0)));


    // start tree building
    int depth = 0;
    int num_leaves = 0;
    unsigned timestamp = 0;
    const int topK = param_.node_block_size;

    std::vector<int> build_nodeset;
    std::vector<int> large_nodeset;
    std::vector<int> split_nodeset;
    //depth
    std::vector<int> build_depth;
    std::vector<int> large_depth;
    std::vector<int> split_depth;

    SplitInfo splitOutput;
    SplitResult splitResult;

    // start from the root node
    build_nodeset.push_back(0);
    large_nodeset.clear();
    BuildHist(gpair, build_nodeset, large_nodeset, bwork_set_, *p_tree);

    printtree(p_tree, "After CreateHist");

    FindSplit(gpair, work_set_, build_nodeset, splitOutput);
 

    qexpand_->push(this->newEntry(0, 0 /* depth */, splitOutput.sol[0], 
                splitOutput.left_sum[0], timestamp++));
    num_leaves++;

    // expand
    while (!qexpand_->empty()) {

      // 1. pop top K candidates
      build_nodeset.clear();
      large_nodeset.clear();
      split_nodeset.clear();
      splitOutput.clear();
      build_depth.clear();
      large_depth.clear();
      split_depth.clear();
      

      int popCnt = 0;
      while (!qexpand_->empty() && popCnt < topK){

        const auto candidate = qexpand_->top();
        const int nid = candidate.nid;
        qexpand_->pop();

        if (candidate.sol.loss_chg <= kRtEps
            || (param_.max_depth > 0 && candidate.depth == param_.max_depth)
            || (param_.max_leaves > 0 && num_leaves + popCnt == param_.max_leaves) ) {

          //(*p_tree)[nid].SetLeaf(snode_[nid].weight * param_.learning_rate);
            (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
        } else {
            // add to split set
            split_nodeset.push_back(nid);
            split_depth.push_back(candidate.depth);
            splitOutput.append(candidate.sol, candidate.left_sum);

            popCnt ++;
        }
      }

      //quit if no need to split
      if (split_nodeset.size() == 0) continue;

      // 2. apply split on these nodes
      printVec("ApplySplit split_nodeset:", split_nodeset);

      ApplySplitOnTree(split_nodeset, splitOutput, p_tree);
      ApplySplitOnPos(split_nodeset, split_depth, splitResult, *p_tree);

      printPOSSetSingle(posset_);

      splitResult.getResultNodeSet(build_nodeset, build_depth,
              large_nodeset, large_depth);
      
      printVec("ApplySplitResult build_nodeset:", build_nodeset);
      //printVec("ApplySplitResult build_depth:", build_depth);


      // 3. build hist for the children nodes
      AllocateChildrenModel(build_nodeset, large_nodeset, *p_tree);
      UpdateChildrenModelSum(split_nodeset, splitOutput, *p_tree);

      // 3.5 do we need continue on the new nodes?
      if (param_.max_depth > 0){
          printVec("BeforeRemove build_nodeset:", build_nodeset);
          printVec("BeforeRemove build_depth:", build_depth);

          // remove the nodes with max_depth already
          RemoveNodesWithMaxDepth(build_nodeset, build_depth,
                  large_nodeset, large_depth,
                  param_.max_depth, p_tree);
          printVec("AfterRemove build_nodeset:", build_nodeset);
          printVec("AfterRemove build_depth:", build_depth);


      }

      if (build_nodeset.size() == 0 && large_nodeset.size() == 0) continue;

      // do buildhist  
      printVec("BuildHist on build_nodeset:", build_nodeset);
      printVec("BuildHist on large_nodeset:", large_nodeset);
      BuildHist(gpair, build_nodeset, large_nodeset, bwork_set_, *p_tree);

      printtree(p_tree, "After CreateHist");

      // 4. find split for these nodes
      if(large_nodeset.size() > 0){
          build_nodeset.insert(build_nodeset.end(), large_nodeset.begin(),
                  large_nodeset.end());
          build_depth.insert(build_depth.end(), large_depth.begin(),
                  large_depth.end());
      }
      FindSplit(gpair, work_set_, build_nodeset, splitOutput);
      for (int i = 0; i < build_nodeset.size(); i++){
        qexpand_->push(this->newEntry(build_nodeset[i],
                    build_depth[i],
                    //p_tree->GetDepth(build_nodeset[i]),
                    splitOutput.sol[i],
                    splitOutput.left_sum[i], timestamp++));
        num_leaves++;
      }
      //remove those parents node splitted
      num_leaves -= large_nodeset.size();

    } /* end of while */

    // set all the rest expanding nodes to leaf
    // This post condition is not needed in current code, but may be necessary
    // when there are stopping rule that leaves qexpand non-empty
    //while (!qexpand_->empty()) {
    //  const int nid = qexpand_->top().nid;
    //  qexpand_->pop();
    //  //(*p_tree)[nid].SetLeaf(snode_[nid].weight * param_.learning_rate);
    //  (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
    //}
    // remember auxiliary statistics in the tree node
    //for (int nid = 0; nid < p_tree->param.num_nodes; ++nid) {
    //  p_tree->Stat(nid).loss_chg = snode_[nid].best.loss_chg;
    //  p_tree->Stat(nid).base_weight = snode_[nid].weight;
    //  p_tree->Stat(nid).sum_hess = static_cast<float>(snode_[nid].stats.sum_hess);
    //  snode_[nid].stats.SetLeafVec(param_, p_tree->Leafvec(nid));
    //}

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

    /*
     * EnumerateSplit
     *  find the best split point for one feature
     *  search bi-direction to deal with missing values
     * Input:
     *  fid     ;   feature id
     *  node_sum;   total gh sum
     *  hist    ;   pointer to model ghSum for this feature
     *
     * Output:
     *  best    ; SplitEntry including loss_chg, feature and value
     *  left_sum; left side ghSum at the split point
     *
     */
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

  /*
   * FindSplit
   *    to find the best split point for each node
   * Input:
   *    featureset  ; the features to work on
   *                  in model rotation, it can be a subset of the whole features
   *    nodeset     ; the nodes to work on
   *                  in lossguide, it can be small
   *
   * Output:
   *    splitOutput ; best split info for each node, including best and left_sum
   *
   * Dependency:
   *    node2workindex_  ; global mapping from nodeid to physical position id of it's model
   *    feat2workindex_  ; global mapping from feature id to workable feauture position
   *    model   ;  workspace hset
   */
  void FindSplit(const std::vector<GradientPair> &gpair,
                 std::vector<bst_uint>& featureset,
                 std::vector<int>& nodeset,
                 SplitInfo& splitOutput) {

    double _tstartFindSplit = dmlc::GetTime();

    //init output
    const int num_node = nodeset.size();
    std::vector<SplitEntry>& sol = splitOutput.sol;
    std::vector<TStats>& left_sum = splitOutput.left_sum;
    sol.resize(num_node);
    std::fill(sol.begin(), sol.end(), SplitEntry());
    left_sum.resize(num_node);
    std::fill(left_sum.begin(), left_sum.end(), TStats());

    //adjust parallelism by the number of nodes
    const int threadNum = omp_get_max_threads();
    if (threadNum > nodeset.size()){
        //call feature wised parallelism
        FindSplitByFeature(gpair, featureset, nodeset, splitOutput);
    }
    else{
        //call node wised parallelism
        FindSplitByNode(gpair, featureset, nodeset, splitOutput);
    }
    this->tminfo.aux_time[5] += dmlc::GetTime() - _tstartFindSplit;

  }


  //
  // use the existing memory layout and parallelism as in BuildHistBlock
  //
  void FindSplitByFeature(const std::vector<GradientPair> &gpair,
                 std::vector<bst_uint> featureset,
                 std::vector<int> nodeset,
                 SplitInfo& splitOutput) {

    const std::vector<bst_uint> &fset = featureset;

    const int num_feature = fset.size();
    const int num_node = nodeset.size();

    //init, have to use thread local copies
    for (int i = 0; i< thread_splitinfo_.size(); i++){
        thread_splitinfo_[i].sol.clear();
        thread_splitinfo_[i].left_sum.clear();
    }

    //
    // todo
    //  it's okay to add node parallelism beside feature
    //
    #pragma omp parallel for schedule(static)
    for (size_t i = 0; i < fset.size(); ++i) {
        int tid = omp_get_thread_num();
        int fid = i;
        
        std::vector<SplitEntry>& sol = thread_splitinfo_[tid].sol;
        std::vector<TStats>& left_sum = thread_splitinfo_[tid].left_sum;

        //lazy init
        if (sol.size() == 0){
            // reset
            sol.resize(num_node);
            std::fill(sol.begin(), sol.end(), SplitEntry());
            left_sum.resize(num_node);
            std::fill(left_sum.begin(), left_sum.end(), TStats());
        }

        // for each node
        for (bst_omp_uint wid = 0; wid < num_node; ++wid) {
            int nid = nodeset[wid];
            SplitEntry &best = sol[wid];
            //adjust the physical location of this plain
            int mid = node2workindex_[nid];

            TStats &node_sum = wspace_.hset.GetNodeSum(mid);

            int fidoffset = this->feat2workindex_[fid];

            CHECK_GE(fidoffset, 0);
            EnumerateSplit(this->wspace_.hset.GetHistUnitByFid(fidoffset, mid),
                       node_sum, fid, &best, &left_sum[wid]);


            printSplit(best, fid, nid);
        }
        //printSplit(best, -1, nid);
    }

    // reduce from thread_local
    for (int i = 0; i < thread_splitinfo_.size(); ++i) {
        //skip empty thread info
        if (thread_splitinfo_[i].sol.size() == 0) continue;
        for (int wid = 0; wid < num_node; ++wid) {
            if (splitOutput.sol[wid].Update(thread_splitinfo_[i].sol[wid])){
                splitOutput.left_sum[wid] = thread_splitinfo_[i].left_sum[wid];
            }
        }
    }


  }

  //
  // using node parallelism
  //
  void FindSplitByNode(const std::vector<GradientPair> &gpair,
                 std::vector<bst_uint> featureset,
                 std::vector<int> nodeset,
                 SplitInfo& splitOutput) {

    const std::vector<bst_uint> &fset = featureset;

    const int num_feature = fset.size();
    const int num_node = nodeset.size();

    std::vector<SplitEntry>& sol = splitOutput.sol;
    std::vector<TStats>& left_sum = splitOutput.left_sum;
    #pragma omp parallel for schedule(dynamic, 1)
    for (int wid = 0; wid < num_node; ++wid) {
      int nid = nodeset[wid];
      SplitEntry &best = sol[wid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];

      TStats &node_sum = wspace_.hset.GetNodeSum(mid);

      for (size_t i = 0; i < fset.size(); ++i) {
        int fid = fset[i];
        int fidoffset = this->feat2workindex_[fid];

        CHECK_GE(fidoffset, 0);
        EnumerateSplit(this->wspace_.hset.GetHistUnitByFid(fidoffset, mid),
                       node_sum, fid, &best, &left_sum[wid]);


       //printSplit(best, fid, nid);
      }
      printSplit(best, -1, nid);
    }

  }

  /*
   * SetStats: Helper function in tree split
   *
   */
  void SetStats(RegTree *p_tree, int nid, const TStats &node_sum) {
    p_tree->Stat(nid).base_weight = static_cast<bst_float>(node_sum.CalcWeight(param_));
    p_tree->Stat(nid).sum_hess = static_cast<bst_float>(node_sum.sum_hess);
    node_sum.SetLeafVec(param_, p_tree->Leafvec(nid));
  }


  /*
   * ApplySplitOnTree
   *    split nodes and update the tree
   * Input:
   *    splitOutput ; split output from FindSplit
   *    nodeset     ; node set to work on
   *
   * Output:
   *    tree    ; children added to tree
   *
   * Dependency:
   *    node2workindex_ ;
   *    nodesum ; read nodesum through node2workindex_
   *
   */

  void ApplySplitOnTree(std::vector<int>& nodeset,
                 SplitInfo& splitOutput,
                 RegTree *p_tree) {

    const int num_node = nodeset.size();

    std::vector<SplitEntry>& sol = splitOutput.sol;
    std::vector<TStats>& left_sum = splitOutput.left_sum;
 
    // get the best result, we can synchronize the solution
    for (int wid = 0; wid < num_node; ++wid) {
      const int nid = nodeset[wid];

      const SplitEntry &best = sol[wid];

      //adjust the physical location of this plain
      int mid = node2workindex_[nid];
      const TStats &node_sum = wspace_.hset.GetNodeSum(mid);
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
        //bugfix: setleaf should be after addchilds
        (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
 
      }
    }
  }

  /*
   * ApplySplitOnPos
   *    update position by the split
   *
   * Input:
   *    nodeset ; node set to work on
   * Output:
   *    splitResult ; children statistics
   *
   * Dependency:
   *    posset_ ; the pos data to update
   * 
   */
  void ApplySplitOnPos(const std::vector<int>& nodeset,
          const std::vector<int>& depthset,
          SplitResult& splitResult,
          const RegTree &tree){

      double _tstart = dmlc::GetTime();

      const int num_node = nodeset.size();
      const int num_block = posset_.getBlockNum();

      // go split 
      const int omp_loop_size = num_block * num_node;
      #pragma omp parallel for schedule(static)
      for (int i = 0; i < omp_loop_size ; i++){

        const int blkid = i / num_node;
        const int qid = i % num_node;
        const int nid = nodeset[qid];
        //const int fid = splitOutput.sol[qid].SplitIndex();
        //CHECK_EQ(tree[nid].SplitIndex(),fid);
        const int fid = tree[nid].SplitIndex();
        
        auto& grp = posset_.getGroup(nid, blkid);

        //CHECK_NE(grp.isDelete(), true);
        if (grp.isDelete() || grp.isDummy()) continue;

        //1. set default direction for this node
        //double _tstartInit = dmlc::GetTime();
        if (tree[nid].IsLeaf()) {
          // mark finish when it is not a fresh leaf
          if (tree[nid].RightChild() == -1) {
            //position_[ridx] = ~nid;
            grp.setDelete();
          }

          // no split for this node
          continue;
        } 

        // push to default branch
        grp.BeginUpdate(tree[nid].LeftChild(),
                tree[nid].RightChild(),
                tree[nid].DefaultLeft());
        

        //2. correct non default posistion
        TDMatrixCube<unsigned char> &batch = *p_hmat;
        auto col = batch[fid].GetBlock(0);

        if (col.size() > 0){
            //check all rows in this grp
            for (int j = 0; j < grp.size(); ++j) {
                const int ridx = grp.getRowId(j);

                //access the data by ridx
                //
                // todo >>>>
                // check special value of binid for MISSING value
                // here, just use dense cube
                //
                if (col.rowsizeByRowId(ridx) == 0){
                    continue;
                }
                const auto binid = col._blkaddrByRowId(ridx, 0);

                if (binid <= tree[nid].SplitCond()) {
                  grp.setLeftPosition(j);
                } else {
                  grp.setRightPosition(j);
                }
            } /* end of group */
        }

        // end scan the rows, collect statistics of left and right length
        grp.EndUpdate();

        // split this grp
        auto start = posset_.getNextEntryStart(nid, blkid);
        auto& groupSet = posset_.getGroupSet(blkid);
        grp.ApplySplit(start, groupSet);

        //printPOSSet(posset_, gid);
 
      } /* end omp_loop */

      // build the split result
      //init the output
      splitResult.resize(num_node);

      for(int i = 0; i < num_node; i++){
        // clear 
        splitResult.clear(i);
        const int nid = nodeset[i];
        const int depth = depthset[i];

        int lid = tree[nid].LeftChild();
        if (lid >= 0) {
            // if child exist
            int leftid = -1, rightid = -1;
            int rid = tree[nid].RightChild();
            leftid = lid;
            rightid = rid;

            int left_len = 0, right_len = 0;
            for(int j = 0; j < num_block; j++){
                //for block j, node i
                auto& grp = posset_.getGroup(i, j);

                // get left and right
                left_len += grp.getLeftLen();
                right_len += grp.getRightLen();
            }
            splitResult.set(i, depth, leftid, left_len, rightid, right_len);
        }
      }


      this->tminfo.posset_time += dmlc::GetTime() - _tstart;
  }


  /*
   * Init Functions
   */
  // initialize the work set of tree
  void InitWorkSet(DMatrix *p_fmat,
                   const RegTree &tree,
                   std::vector<bst_uint> *p_fset,
                   bool initFtHelper){

    if (initFtHelper){
        if (p_fmat != cache_dmatrix_) {
          feat_helper_.InitByCol(p_fmat, tree);
          cache_dmatrix_ = p_fmat;
        }
        feat_helper_.SyncInfo();
    }


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

  /*
   * save/load for HistCutMatrix, MetaInfo and FMetaHelper
   *
   */
  void LoadMatrixMeta(HistCutMatrix& cut, 
          MetaInfo& info,
          typename BlockBaseMakerLossguide<TStats>::FMetaHelper& fthelper,
          std::string& cutFileName){
    dmlc::Stream * fs = dmlc::Stream::Create(cutFileName.c_str(), "r");
    int vecSize;
    //save meta
    info.LoadBinary(fs);
    
    //fthelper
    fs->Read(&vecSize, sizeof(vecSize));
    fthelper.fminmax_.resize(vecSize);
    fs->Read((fthelper.fminmax_.data()), sizeof(bst_float)*vecSize);

    //load HistCutMatrix
    fs->Read(&vecSize, sizeof(vecSize));
    cut.row_ptr.resize(vecSize);
    fs->Read((cut.row_ptr.data()), sizeof(uint32_t)*vecSize);

    fs->Read(&vecSize, sizeof(vecSize));
    cut.min_val.resize(vecSize);
    fs->Read((cut.min_val.data()), sizeof(bst_float)*vecSize);

    fs->Read(&vecSize, sizeof(vecSize));
    cut.cut.resize(vecSize);
    fs->Read((cut.cut.data()), sizeof(bst_float)*vecSize);



    delete fs;
 
  }

  void SaveMatrixMeta(HistCutMatrix& cut, 
          MetaInfo& info,
          typename BlockBaseMakerLossguide<TStats>::FMetaHelper& fthelper,
          std::string& cutFileName){
    dmlc::Stream * fs = dmlc::Stream::Create(cutFileName.c_str(), "w");
    int vecSize;
    //save meta
    info.SaveBinary(fs);
    //save fthelper
    vecSize = fthelper.fminmax_.size();
    fs->Write(&vecSize, sizeof(vecSize));
    fs->Write((fthelper.fminmax_.data()), sizeof(bst_float)*vecSize);

    //save zol vector
    vecSize = cut.row_ptr.size();
    fs->Write(&vecSize, sizeof(vecSize));
    fs->Write((cut.row_ptr.data()), sizeof(uint32_t)*vecSize);

    vecSize = cut.min_val.size();
    fs->Write(&vecSize, sizeof(vecSize));
    fs->Write((cut.min_val.data()), sizeof(bst_float)*vecSize);

    vecSize = cut.cut.size();
    fs->Write(&vecSize, sizeof(vecSize));
    fs->Write((cut.cut.data()), sizeof(bst_float)*vecSize);

    delete fs;
  }

  /*
   * initialize the proposal for only one time
   */
  void InitializeHist(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const RegTree &tree) {
    if (!isInitializedHistIndex_) {
        double _tstartInit = dmlc::GetTime();

        auto& fset = fwork_set_;
        if (!param_.loadmeta.empty()) {
            std::string fname;
            fname = param_.loadmeta + ".meta";
            // load saved binary 
            LoadMatrixMeta(cut_, dmat_info_, feat_helper_, fname);
            // 1. Initilize the feature set
            this->InitWorkSet(p_fmat, tree, &fset, false /*init from p_fmat*/);
        }
        else{
            // 1. Initilize the feature set
            this->InitWorkSet(p_fmat, tree, &fset, true /*init from p_fmat*/);

            // 2. Initilize the histgram
            cut_.Init(p_fmat,param_.max_bin /*256*/);
        }


        // now we get the final result of sketch, setup the cut
        // layout of wspace_.cut  (feature# +1) x (cut_points#)
        //    <cut_pt0, cut_pt1, ..., cut_ptM>
        //    cut_points# is variable length, therefore using .rptr
        //    the last row is the nodeSum
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
                 (work_set_.size() + 1)  + 1);


        //
        // 3. Init blkInfo
        //
        auto& _info = dmat_info_;
        this->blkInfo_ = BlockInfo(param_.row_block_size, param_.ft_block_size, param_.bin_block_size);
        this->blkInfo_.init(_info.num_row_, _info.num_col_, param_.max_bin);
        
        LOG(CONSOLE) << "Init Param: node_block_size=" << param_.node_block_size;

        //
        // 4. Add binid to dmatrix.
        // First time init model ghsum
        //
        max_leaves_ = 0;
        if (param_.grow_policy == TrainParam::kLossGuide) {
            if (param_.max_leaves > 0){
                max_leaves_ = param_.max_leaves;
            }
        }
        else{
            if (param_.max_depth > 0){
                max_leaves_ = std::pow(2,this->param_.max_depth);
            }
        }
        CHECK_NE(max_leaves_, 0);

        this->wspace_.Init(this->param_, max_leaves_, this->blkInfo_);
        // init the node2index map
        node2workindex_.Init(max_leaves_ * 2);

        unsigned int nthread = omp_get_max_threads();
        this->thread_hist_.resize(omp_get_max_threads());
        this->thread_histcompact_.resize(omp_get_max_threads());
        this->thread_splitinfo_.resize(omp_get_max_threads());
        for (unsigned int i=0; i< nthread; i++){
          //make memory access separate
          thread_hist_[i].resize(64);
          thread_histcompact_[i].resize(64);
        }


        if (param_.loadmeta.empty()) {
            // add binid to matrix
            //this->InitHistIndexByCol(p_fmat, fset, tree);
            this->InitHistIndexByRow(p_fmat, tree);

            //
            // 5. Build blkmat(block matrix) and hmat(column matrix)
            //
            dmat_info_ = p_fmat->Info();
            p_blkmat->Init(*p_fmat->GetRowBatches().begin(), p_fmat->Info(), param_.max_bin, blkInfo_);
            //if (blkInfo_.bin_block_size == 0 && blkInfo_.ft_block_size == 1){
            if (0){
                //reuse p_blkmat and set BLKADDR to byte for all matrix
                //p_hmat = p_blkmat;
            }
            else{
                BlockInfo hmat_blkInfo = BlockInfo(0, 1, 0);
                p_hmat->Init(*p_fmat->GetRowBatches().begin(), p_fmat->Info(), param_.max_bin, hmat_blkInfo);
            }
        }
        else{
            //load cube
            std::string fname;
            fname = param_.loadmeta + ".blkmat";
            p_blkmat->load(fname);
            fname = param_.loadmeta + ".hmat";
            p_hmat->load(fname);

            LOG(CONSOLE) << "Load cube from metafile:" << fname;
        }

        //DEBUG
        #ifdef USE_DEBUG
        //check the max value of binid
        printcut(this->cut_);

        //printmsg("SortedColumnBatch");
        //printdmat(*p_fmat->GetSortedColumnBatches().begin());

        printmsg("RowBatch");
        printdmat(*p_fmat->GetRowBatches().begin());
        #endif



        #ifdef USE_DEBUG
        printdmat(*p_hmat);
        printdmat(*p_blkmat);

        //
        // Cube Info
        //
        std::cout << "Cube Statistics:\n";
        for(int i=0; i < p_blkmat->GetBaseBlockNum(); i++){
            auto& zcol = p_blkmat->GetBlockZCol(i);
            if (zcol.getDataSize() <= 0) continue;

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

        //
        // 6. Init the block work set
        //  fwork_set_ --> the full features set
        //  work_set_  --> working feature set
        //  bwork_set_ --> the block set
        this->bwork_base_blknum_ = p_blkmat->GetBaseBlockNum();
        bwork_lock_ = new spin_mutex[bwork_base_blknum_];
        bwork_set_.clear();
        for(int i=0; i < p_blkmat->GetBlockNum(); i++){
            bwork_set_.push_back(i);
        }

        LOG(CONSOLE) << "Init bwork_set_: base_blknum=" << bwork_base_blknum_ <<
            ":" << bwork_set_.size();
        printVec("bwork_set_:", bwork_set_);

        //
        // 7. Set the tree growth policy
        //
        this->InitQExpand();


        //
        // 7. Re-Init the model memory space, first touch
        //
        this->wspace_.Init(this->param_, max_leaves_, this->blkInfo_);

        // save meta mat
        if (!param_.savemeta.empty()){

            LOG(CONSOLE) << "Save metafile:" << param_.savemeta;

            std::string fname;
            fname = param_.savemeta + ".meta";
            SaveMatrixMeta(cut_, dmat_info_, feat_helper_,
                    fname);

            fname = param_.savemeta + ".blkmat";
            p_blkmat->save(fname);
            fname = param_.savemeta + ".hmat";
            p_hmat->save(fname);
        }


        this->tminfo.aux_time[0] += dmlc::GetTime() - _tstartInit;
        //
        // 8.End of initialization, write flag file
        //
        startVtune("vtune-flag.txt");
        LOG(INFO) << "End of initialization, start training";

        this->tminfo.trainstart_time = dmlc::GetTime();

        isInitializedHistIndex_ = true;

    }// end if(isInitializedHistIndex_)
    else{

        node2workindex_.Init(max_leaves_ * 2);
        //
        // todo, remove this part, not necessary
        //
        //double _tstartInit = dmlc::GetTime();

        ////double _tstartInit = dmlc::GetTime();
        ////init for each tree
        //unsigned int nthread = omp_get_max_threads();
        //this->thread_hist_.resize(omp_get_max_threads());
        //this->thread_histcompact_.resize(omp_get_max_threads());
        //for (unsigned int i=0; i< nthread; i++){
        //  //make memory access separate
        //  thread_hist_[i].resize(64);
        //  thread_histcompact_[i].resize(64);
        //}

        //auto _info = p_fmat->Info();
        //this->blkInfo_.init(_info.num_row_, _info.num_col_, param_.max_bin);
        ////this->wspace_.Init(this->param_, 1, std::pow(2,this->param_.max_depth+1) /*256*/, blkInfo_);
        ////this->wspace_.ClearData();

        ////this->tminfo.aux_time[8] += dmlc::GetTime() - _tstartInit;
    }
  }

  /*
   * Init binid for the col-wise matrix
   */
  void InitHistCol(const SparsePage::Inst &col,
                            const RegTree &tree,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {

    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;

    //LOG(CONSOLE) << "InitHistCol: num_nodes=" << tree.param.num_nodes <<
    //        ", qexpand.size=" << this->qexpand_.size() ;

    // only for initialization
    // nid should be 0
    const unsigned nid = 0;
    hbuilder[nid].hist = this->wspace_.hset.InitGetHistUnitByFid(fid_offset,nid);
    for (auto& c : col) {
      const bst_uint ridx = c.index;
      // update binid in pmat
      unsigned binid = hbuilder[nid].GetBinId(c.fvalue);
      c.addBinid(binid);
    }
  } 


  void InitHistIndexByCol( DMatrix *p_fmat,
                      const std::vector<bst_uint> &fset,
                     const RegTree &tree){

      const auto nsize = static_cast<bst_omp_uint>(fset.size());
      std::cout  << "InitHistIndex : fset.size=" << nsize << "\n";

      // start accumulating statistics
      for (const auto &batch : p_fmat->GetSortedColumnBatches()) {
        // start enumeration
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
   * Init binid for the row-matrix
   */
  void InitHistRow(const SparsePage::Inst &row,
                            const RegTree &tree,
                            const bst_omp_uint rid,
                            std::vector<HistEntry> *p_temp) {

    if (row.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;

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
   * UpdateHlafTrick
   *    update ghsum by halftrick
   * Input:
   *    bulid_nodeset   ; the node to work on
   * Output:
   *    ghsum           ; ghsum model of these nodes
   *    node2workindex- ; replace parent model with one child
   *
   * Dependency:
   *    sibling ;   the ghsum of sibling nodes should be ready
   *    tree    ;   to get parent and sibling info 
   *    node2workindex_ ;   the mapping from nid to physical model plane id
   *
   */
  void UpdateHalfTrick(bst_uint blkid_offset,
                       const RegTree &tree,
                       const std::vector<int> build_nodeset){

    for (size_t i = 0; i < build_nodeset.size(); i++) {
      const int nid = build_nodeset[i];
      
      const int pid = tree[nid].Parent();
      if (pid < 0) continue;
      const int sibling = (tree[pid].LeftChild() == nid)? tree[pid].RightChild(): tree[pid].LeftChild();

      //
      //  two plain substraction
      //  start : hist.data
      //  size  : GradStat * hist.size
      //
      int mid_parent = node2workindex_[pid];
      int mid_sibling = node2workindex_[sibling];
      
      CHECK_NE(mid_parent, -1);
      CHECK_NE(mid_sibling, -1);

      double* p_left = reinterpret_cast<double*>(
              this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid_parent).data);

      double* p_right = reinterpret_cast<double*>(
              this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid_sibling).data);
      int plainSize = this->wspace_.hset.GetHistUnitByBlkidSize();
 
      //#pragma omp simd
      #pragma GCC ivdep
      for(int j=0; j < 2 * plainSize; j++){
          //reuse the parent storage for left nodes
          p_left[j] -= p_right[j];
      }

      // update index for left node
      // reuse the pranet mid
      //if (blkid_offset == 0){
      //    //only the first fid update the global index
      //    node2workindex_.reset(pid);
      //}
    }
  }


  /*
   * BuildHist for a single block 
   * Input:
   *    block   ;   a cube block of input data
   *    blkid_offset    ;   blkid in the base plain
   *    zblkid  ;   row blk id
   *    build_nodeset  ;   node set to build
   * Dependency:
   *    posset_ ;   row set info
   *
   */
  void UpdateHistBlock(const std::vector<GradientPair> &gpair,
                       const TDMatrixCubeBlock<TBlkAddr> &block,
                       const RegTree &tree,
                       bst_uint blkid_offset,
                       unsigned int zblkid,
                       const std::vector<int>& build_nodeset,
                       std::vector<HistEntryCompact> *p_temp) {
    //check size
    if (block.size() == 0) return;

    #ifdef USE_DEBUG
    //std::cout << "updateHistBlock: blkoffset=" << blkid_offset <<
    //    ", zblkid=" << zblkid << ",baserowid=" << block.base_rowid_ <<
    //    ", len=" << block.size() 
    //    << "\n";
    #endif

    // initialize sbuilder for use
    std::vector<HistEntryCompact> &hbuilder = *p_temp;
    //todo, 
    //  as in lossguide, build_nodeset.size() is small
    //  change vector to map will be better here
    hbuilder.resize(tree.param.num_nodes);

    // no lazy init neccessary for a small size nodeset
    for (int i = 0; i < build_nodeset.size(); ++i) {
        int nid = build_nodeset[i];
        //lazy initialize
        int mid = node2workindex_[nid];
        //hbuilder[nid].hist = this->wspace_.hset.GetHistUnitByBlkid(blkid_offset, mid);
        hbuilder[nid] = this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid);
        //init data for the first zblks
        if (zblkid == 0){
            hbuilder[nid].ClearData(this->wspace_.hset.GetHistUnitByBlkidSize());
        }
    }

    //get lock
    bwork_lock_[blkid_offset].lock();

    for (int i = 0; i < build_nodeset.size(); ++i) {
        const int nid = build_nodeset[i];
        auto& grp = posset_.getGroup(nid, zblkid);

        //CHECK_NE(grp.isDummy(), true);
        if (grp.isDummy()) continue;

        for (int j = 0; j < grp.size(); ++j) {
            const int ridx = grp.getRowId(j);

            for (int k = 0; k < block.rowsizeByRowId(ridx); k++){
                hbuilder[nid].AddWithIndex(block._blkaddrByRowId(ridx, k), gpair[ridx]);

                /*
                 * not much benefits from short->byte
                 */
                //unsigned short blkaddr = this->blkInfo_.GetBlkAddr(block._blkaddr(j, k), k);
                //unsigned short blkaddr = block._blkaddr(j, k)*2 + k;
                //hbuilder[nid].AddWithIndex(blkaddr, gpair[ridx]);

                //debug only
                #ifdef DEBUG
                this->datasum_ += block._blkaddrByRowId(ridx,k);
                #endif
            }
        }
    } /*loop build_nodeset*/

    //quit
    bwork_lock_[blkid_offset].unlock();

  }

  /*
   * UpdatePredictionCache
   *    update the prediction cache for the last tree
   *    for training data which are already assigned to the leaves
   * Input:
   * Output:
   *    p_out_preds ;   prediction vector
   *
   * Dependency:
   *    p_last_tree_   ;   the last tree learned and used to predict
   *    posset_ ;   the node assignment for all the rows
   *
   */
  bool UpdatePredictionCache(const DMatrix* p_fmat,
                             HostDeviceVector<bst_float>* p_out_preds) override {
    if ( this->param_.subsample < 1.0f) {
      return false;
    } 
    
    // check if it's validation
    std::vector<bst_float>& out_preds = p_out_preds->HostVector();
    if(out_preds.size() != posset_.getEntrySize()){
        return false;
    }
    
    {
      // p_last_fmat_ is a valid pointer as long as UpdatePredictionCache() is called in
      // conjunction with Update().
      if (!p_last_tree_) {
        return false;
      }

      double _tstart = dmlc::GetTime();
      CHECK_GT(out_preds.size(), 0U);

      //get leaf_value for all nodes
      const auto& nodes = p_last_tree_->GetNodes();
      std::vector<float> leaf_values;
      leaf_values.resize(nodes.size());
      std::vector<int> leaf_nodeset;

      for (int nid = 0; nid < nodes.size(); nid ++){
          bst_float leaf_value;
          int tnid = nid;

          //skip dummy nodes first
          if ((*p_last_tree_)[tnid].IsDummy()) {
              continue;
          }
          if (!(*p_last_tree_)[tnid].IsLeaf()) {
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

          // add to leaf_nodeset
          leaf_nodeset.push_back(nid);
      }

      //
      // because there are deleted nodes
      // todo, not a good idea to access internal entry directly
      //
      #ifdef USE_DEBUG
      double leaf_val_sum = 0.;
      long nid_sum = 0L;
      #endif

      const int num_block = posset_.getBlockNum();
      const int num_node = leaf_nodeset.size();

      const int omp_loop_size = num_block * num_node;
      #pragma omp parallel for schedule(static)
      for (int i = 0; i < omp_loop_size ; i++){
        const int blkid = i / num_node;
        const int qid = i % num_node;
        const int nid = leaf_nodeset[qid];

        auto& grp = posset_.getGroup(nid, blkid);

        // for pruned nodes, new leaves are in posset
        if(grp.isDummy()) continue;

        for (int k = 0; k < grp.size(); k++){
            const int ridx = grp.getRowId(k);
            out_preds[ridx] += leaf_values[nid];

            #ifdef USE_DEBUG
            CHECK((*p_last_tree_)[nid].IsLeaf()||(*p_last_tree_)[nid].IsDeleted());
            //std::cout << nid << ":" << ridx << " ";
            leaf_val_sum += leaf_values[nid];
            nid_sum += nid;
            #endif
        }
      }
      #ifdef USE_DEBUG
      LOG(CONSOLE) << "updatech leaf_sum=" << leaf_val_sum << 
                ",rowcnt=" << posset_.getEntrySize() <<
                ",nid_sum=" << nid_sum;
      #endif


      //LOG(CONSOLE) << "UpdatePredictionCache: nodes size=" << 
      //    nodes.size() << ",rowscnt=" << nrows;

      //this->tminfo.aux_time[5] += dmlc::GetTime() - _tstart;
      //printVec("updatech pos:", this->position_);
      printVec("updatech leaf:", leaf_values);
      printVec("updatech pred:", out_preds);
      return true;
    }
  }

  /*
   * BuildHist for a group of nodes
   * Input: 
   *    build_nodeset   ; a set of nodes to buildhist
   *    large_nodeset   ; in halftrick, the large portion of siblings
   *    blk_set ; a set of model blocks to buildhist
   * Output:
   *    ghSum   ; model ghSum for these nodes
   * Dependency:
   *    node2workindex_ ; isReady for nodes in build_nodeset and large_nodeset
   *    parent's ghSum  ; for halftrick at cost of large mem footprint
   *    p_blkmat    ;   input data in block wise cube format
   *    p_hmat      ;   input data in column wise cube format
   *
   *
   */
  void BuildHist( const std::vector<GradientPair> &gpair,
                  const std::vector<int>& build_nodeset,
                  const std::vector<int>& large_nodeset,
                  const std::vector<bst_uint> &blkset,
                  const RegTree &tree) {
      const MetaInfo &info = dmat_info_;

      if (build_nodeset.size() == 0){
        //last step to update position 
        return;
      }

      {
        // start enumeration
        double _tstart = dmlc::GetTime();

        // block number on the base plain
        const int nsize = p_blkmat->GetBaseBlockNum();
        // block number in the row dimension
        const int zsize = p_blkmat->GetBlockZCol(0).GetBlockNum();
        // node dimension blocks
        const int qsize = build_nodeset.size();
        //const int dsize = std::min(param_.group_parallel_cnt, qsize);
        const int dsize = 1;

        #ifdef USE_DEBUG
        this->datasum_ = 0.;
        #endif

        #ifdef USE_DYNAMIC_ROWBLOCK
        #pragma omp parallel for schedule(dynamic, 1)
        for(bst_omp_uint i = 0; i < dsize * nsize * zsize; ++i){

          // node block id
          unsigned int nblkid = i / (nsize *zsize);
          // absolute blk id
          int blkid = i % (nsize * zsize);
          // blk id on the base plain
          int offset = blkid % nsize;
          // blk id on the row dimension
          unsigned int zblkid = blkid / nsize;


          // get dataBlock
          auto& block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

          // update model by this dataBlock and node_blkid
          this->UpdateHistBlock(gpair, block, info, tree,
                offset, zblkid, nblkid,
                &this->thread_histcompact_[omp_get_thread_num()]);
                //&this->thread_hist_[0]);
        }
        #else

        //#define TEST_OMP_OVERHEAD
        #ifdef TEST_OMP_OVERHEAD
        for(int z = 0; z < zsize; z++){

            // test the overhead of omp scheduling
            for(int d = 0; d < dsize; d++){

                #pragma omp parallel for schedule(dynamic, 1)
                for(bst_omp_uint i = 0; i < nsize; ++i){

                  // node block id
                  //unsigned int nblkid = i / (nsize);
                  unsigned int nblkid = d;
                  // absolute blk id
                  int blkid = i % (nsize);
                  // blk id on the base plain
                  int offset = blkid % nsize;
                  // blk id on the row dimension
                  unsigned int zblkid = z;


                  // get dataBlock
                  auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                  // update model by this dataBlock and node_blkid
                  this->UpdateHistBlock(gpair, block, info, tree,
                        offset, zblkid, nblkid,
                        &this->thread_histcompact_[omp_get_thread_num()]);
                        //&this->thread_hist_[0]);
                }

            }
        }

        #else   // TEST_OMP_OVERHEAD

        for(int z = 0; z < zsize; z++){
            #pragma omp parallel for schedule(dynamic, 1)
            for(bst_omp_uint i = 0; i < dsize * nsize; ++i){

              // node block id
              unsigned int nblkid = i / (nsize);
              // absolute blk id
              int blkid = i % (nsize);
              // blk id on the base plain
              int offset = blkid % nsize;
              // blk id on the row dimension
              unsigned int zblkid = z;


              // get dataBlock
              auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

              // update model by this dataBlock and node_blkid
              this->UpdateHistBlock(gpair, block, tree,
                    offset, zblkid, 
                    build_nodeset,
                    &this->thread_histcompact_[omp_get_thread_num()]);
                    //&this->thread_hist_[0]);
            }

        }

        #endif  //TEST_OMP_OVERHEAD

        #endif  //USE_DYNAMIC_ROWBLOCK

        //
        // build the other half
        //
        if (large_nodeset.size() != 0){
            // only happens when in halftrick
            #ifdef USE_HALFTRICK_EX
            double _tstart2 = dmlc::GetTime();
            #pragma omp parallel for schedule(static)
            for (int i = 0; i < nsize; ++i) {
              int offset = i;

              this->UpdateHalfTrick(offset, tree, large_nodeset);
            }
            this->tminfo.aux_time[6] += dmlc::GetTime() - _tstart2;
            #endif
        }

        #ifdef USE_DEBUG
        LOG(CONSOLE) << "BuildHist:: datasum_=" << this->datasum_;
        #endif
        if (param_.grow_policy != TrainParam::kLossGuide) {
        LOG(CONSOLE) << "BuildHist:: dsize=" << dsize << 
            ",nsize=" << nsize << ",zsize=" << zsize << 
            ",nodeset_size=" << build_nodeset.size() <<
            ",largeset_size=" << large_nodeset.size();
        }

        this->tminfo.buildhist_time += dmlc::GetTime() - _tstart;
      } // end of one-page

     
    //this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset.data),
    //                        this->wspace_.hset.data.size());

  }

  /*
   * AllocateChildrenModel
   *    allocate model space to new nodes
   * Input:
   *    build_nodeset   ; the node set to work on
   *    large_nodeset   ;
   * Output:
   *    node2workindex_ ;   children nodes has been added to the index
   * Dependency:
   *    tree    ; ApplySplitOnTree has been done for these nodes
   *
   */
  void AllocateChildrenModel(
          std::vector<int>& build_nodeset,
          std::vector<int>& large_nodeset,
          const RegTree &tree) {

#ifdef USE_HALFTRICK_EX
    for (int i = 0; i < build_nodeset.size(); i++){
        node2workindex_.append(build_nodeset[i]);
    }
    for (int i = 0; i < large_nodeset.size(); i++){
        const int pid = tree[large_nodeset[i]].Parent();
        node2workindex_.replace(large_nodeset[i], pid);
    }
#else
    CHECK_EQ(large_nodeset.size(), 0);

    CHECK_EQ(build_nodeset.size()%2, 0);
    // left and right come in pair
    for (int i = 0; i < build_nodeset.size()/2; i++){
        node2workindex_.append(build_nodeset[i*2]);
        const int pid = tree[build_nodeset[i*2 + 1]].Parent();
        node2workindex_.replace(build_nodeset[i*2 + 1], pid);
    }
#endif

  }

  /*
   * RemoveNodesWithMaxDepth
   *    remove nodes which has the max depth already
   * Input:
   *    build_nodeset   ; the node set to work on
   *    large_nodeset   ;
   * Output:
   *    build_nodeset   ; max-depth nodes removed
   *    large_nodeset   ;
   *
   */
  void RemoveNodesWithMaxDepth(
          std::vector<int>& build_nodeset,
          std::vector<int>& build_depth,
          std::vector<int>& large_nodeset,
          std::vector<int>& large_depth,
          const int max_depth,
          RegTree *p_tree) {

    CHECK_EQ(build_nodeset.size(), build_depth.size());
    CHECK_EQ(large_nodeset.size(), large_depth.size());

    std::set<int> blacklist; 
    std::set<int> blacklistNid; 
    for (int i = 0; i < build_nodeset.size(); i++){
        if (build_depth[i] >= max_depth){
            const int nid = build_nodeset[i];
            blacklist.insert(i);
            blacklistNid.insert(nid);

            (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
        }
    }

    //debug
    #ifdef USE_DEBUG
    std::vector<int> blacklistVec(blacklist.begin(), blacklist.end()); 
    printVec("RemoveNodes blacklist:", blacklistVec);
    #endif

    if (blacklist.size() > 0){
        auto it = std::remove_if(
                    build_nodeset.begin(),
                    build_nodeset.end(),
                    [&blacklistNid](int nid){
                    return blacklistNid.find(nid) != blacklistNid.end();});
        build_nodeset.erase(it, build_nodeset.end());

        it = std::remove_if(
                    build_depth.begin(),
                    build_depth.end(),
                    [&blacklist](int i){
                    return blacklist.find(i) != blacklist.end();});
        build_depth.erase(it, build_depth.end());
    }

    // do for largeset
    blacklist.clear();
    blacklistNid.clear();
    for (int i = 0; i < large_nodeset.size(); i++){
        if (large_depth[i] >= max_depth){
            const int nid = large_nodeset[i];
            blacklist.insert(i);
            blacklistNid.insert(nid);
            (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
        }
    }

    if (blacklist.size() > 0){
        auto it = std::remove_if(
                    large_nodeset.begin(),
                    large_nodeset.end(),
                    [&blacklistNid](int nid){
                    return blacklistNid.find(nid) != blacklistNid.end();});
        large_nodeset.erase(it, large_nodeset.end());

        it = std::remove_if(
                    large_depth.begin(),
                    large_depth.end(),
                    [&blacklist](int i){
                    return blacklist.find(i) != blacklist.end();});
        large_depth.erase(it, large_depth.end());
    }

  }


  /*
   * UpdateNodeModelSum
   *    update the sum of gh for each node
   * Input:
   *    build_nodeset   ; the node set to work on
   *    splitOutput   ; best split info for each node
   * Output:
   *    nodesum ; the sum of gh for each node, global model
   * Dependency:
   *    tree    ; ApplySplitOnTree has been done for these nodes
   *    node2workindex_ ;   children nodes has been added to the index
   *
   */
  void UpdateChildrenModelSum(
          std::vector<int>& nodeset,
          SplitInfo& splitOutput,
          const RegTree &tree){

      // update node statistics.
      //double _tstartSum = dmlc::GetTime();
      #ifdef USE_DEBUG
      TStats modelSum = TStats{0.0,0.0};
      #endif

      for (int i = 0; i < nodeset.size(); ++i) {
        const int nid = nodeset[i];
        const int left = tree[nid].LeftChild();
        const int right = tree[nid].RightChild();

        // nid is already leaf node, skip it
        if (left < 0) continue;

        CHECK_GT(right, 0);

        //adjust the physical location of this plain
        int mid_this = node2workindex_[nid];
        int mid_left = node2workindex_[left];
        int mid_right = node2workindex_[right];

        CHECK_EQ(mid_this>=0 && mid_left>=0 && mid_right>=0, true);
        // read sum of nid first, it can be overwrite by children later
        TStats leftSum = splitOutput.left_sum[i];
        TStats rightSum;
        rightSum.SetSubstract(this->wspace_.hset.GetNodeSum(mid_this),
                leftSum);

        //check which is in the largest_nodeset and calc by halftrick
        this->wspace_.hset.GetNodeSum(mid_left) = leftSum;
        this->wspace_.hset.GetNodeSum(mid_right) = rightSum;

        #ifndef USE_HALFTRICK_EX
        // clear mid_this if no halftrick
        // otherwise, wait until halftrick done
        node2workindex_.reset(nid);
        #endif

        //debuf info
        #ifdef USE_DEBUG
        modelSum.Add(leftSum);
        modelSum.Add(rightSum);
        #endif
      }

      #ifdef USE_DEBUG
      LOG(CONSOLE) << "modelSum=" << modelSum.sum_grad << ":" << modelSum.sum_hess;
      #endif

      //this->tminfo.aux_time[6] += dmlc::GetTime() - _tstartSum;
  }

  /*
   * Reset the splitcont from binid back to fvalue
   *
   * Input:
   *    tree    ; the tree to update
   * Output:
   *    tree
   * Dependency:
   *    feat2workindex_ ; feature id mapping
   *    cut     ; the histogram
   */
  void ResetTree(RegTree& tree){

    double _tstart = dmlc::GetTime();

    const auto& nodes = tree.GetNodes();
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


    p_last_tree_ = &tree;

    //end ResetTree
    //this->tminfo.aux_time[4] += dmlc::GetTime() - _tstart;

  }


};


XGBOOST_REGISTER_TREE_UPDATER(HistMakerBlockLossguide, "grow_block_lossguide")
.describe("Tree constructor that uses approximate global of histogram construction.")
.set_body([]() {
    #ifdef USE_SPARSE_DMATRIX
    
    #ifdef USE_BLKADDR_BYTE
    return new HistMakerBlockLossguide<GradStats, unsigned char, DMatrixCube, DMatrixCubeBlock>();
    #else
    return new HistMakerBlockLossguide<GradStats, unsigned short, DMatrixCube, DMatrixCubeBlock>();
    #endif

    #else
    #ifdef USE_BLKADDR_BYTE
    return new HistMakerBlockLossguide<GradStats, unsigned char, DMatrixDenseCube, DMatrixDenseCubeBlock>();
    #else
    return new HistMakerBlockLossguide<GradStats, unsigned short, DMatrixDenseCube, DMatrixDenseCubeBlock>();
    #endif
    #endif
  });

}  // namespace tree
}  // namespace xgboost
