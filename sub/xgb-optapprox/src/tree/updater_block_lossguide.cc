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
 * This is code in mixmode, which contains 3-phases, dp-mp-dp
 *
 */
#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
#include <atomic>
#include <thread>
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

template<typename TStats, typename TBlkAddr, template<typename> class TDMatrixCube, template<typename> class TDMatrixCubeBlock>
class HistMakerBlockLossguide: public BlockBaseMakerLossguide<TStats> {
 public:

 HistMakerBlockLossguide(){

      this->isInitializedHistIndex_ = false;
      p_blkmat = new TDMatrixCube<TBlkAddr>();
      p_hmat = new TDMatrixCube<unsigned char>();
      //p_hmat = new DMatrixCompactBlockDense();
      // for harpcom
      harpCom_ = nullptr;
      useModelRotation_ = false;
      usePipeline_ = false;
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

  /**
   * @brief Set the Harp Com object
   * 
   * @param harpCom 
   */
  void setHarpCom(harp::com::Communicator* harpCom) override {
    harpCom_ = harpCom;
  }

  void enableModelRotation() override { useModelRotation_ = true;}
  void enableRotationPipeline() override { usePipeline_ = true;}

 protected:

  harp::com::Communicator* harpCom_;
  bool useModelRotation_;
  bool usePipeline_;
  std::vector<bst_uint> localFeatSet_;

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
    // avoid object initialization and first touch in main thread
    // layout of model
    //      <nodeid, binid , fid>
    // store separate sum at the end
    TStats  *data{nullptr};
    unsigned long size{0};
    TStats  *nodesum{nullptr};

    // model replicas in data parallelism
    //row block replicas
    int rowblknum;
    TStats  *data_replica{nullptr};
    unsigned long size_replica{0};
    size_t node_block_size;

    //add fset size
    size_t fsetSize;
    size_t nodeSize;
    size_t featnum;
    BlockInfo* pblkInfo;

    // add for model rotation
    // record the first id of local blks
    size_t firstBlkID = 0;
    // record the number of local blks
    size_t localBlkNum = 0;

    // for pipeline in model rotation
    size_t firstBlkDComm = 0;
    size_t blkCommNum = 0;

    bool inPipeline = false;
    bool isDataSeperate = false;

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
    //
    // node summation store at the end
    //
    inline TStats& GetNodeSum(int nid){
        return nodesum[nid];
    }


    //#ifdef USE_ROW_MODELREPLICA
    //inline HistEntryCompact GetHistUnitByBlkidCompact(
    //        int blkid, int nid, int zid = 0) {
    //  return HistEntryCompact(
    //          &data[0] + (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize * rowblknum + nodeSize*zid + nid));
    //}
    //inline int GetHistUnitByBlkidSize(){
    //    return pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize();
    //}
    ////
    //// FindSplit will go through model by a feature column, not continuous
    ////
    //inline HistUnit GetHistUnitByFid(size_t fid, size_t nid) {
    //  int blkid = fid / pblkInfo->GetFeatureBlkSize();
    //  unsigned int blkoff = (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize*rowblknum + nid);
    //  return HistUnit(cut, /* not use*/
    //                  &data[0] + blkoff + fid % pblkInfo->GetFeatureBlkSize(),
    //                  rptr[fid+1] - rptr[fid],
    //                  pblkInfo->GetBinBlkSize(),
    //                  pblkInfo->GetFeatureBlkSize(),
    //                  pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize()*pblkInfo->GetFeatureBlkNum(featnum)*nodeSize*rowblknum);
    //}

 
    //#else


    inline HistEntryCompact GetHistUnitByBlkidCompact(
            int blkid, int nid, int zid = 0) {
      if (rowblknum == 1 || zid == 0){
        //go to the main model data
        return HistEntryCompact(
                &data[0] + (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize + nid));
      }
      else{
        //CHECK_LT(nid, node_block_size);
        //CHECK_LT(zid, rowblknum);

        //go to the replicas
        return HistEntryCompact(
                &data_replica[0] + 
                (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * node_block_size * rowblknum + node_block_size * zid + nid));

      }
    }

    //todo: add blkid to support blk with different size
    inline int GetHistUnitByBlkidSize(){
        return pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize();
    }


    // FindSplit will go through model by a feature column, 
    // not continuous in case of some block configures
    // always go to the main model
    inline HistUnit GetHistUnitByFid(size_t fid, size_t nid) {
      int blkid = fid / pblkInfo->GetFeatureBlkSize();
      unsigned int blkoff = (pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize())*(blkid * nodeSize + nid);
      return HistUnit(cut, /* not use*/
                      &data[0] + blkoff + fid % pblkInfo->GetFeatureBlkSize(),
                      rptr[fid+1] - rptr[fid],
                      pblkInfo->GetBinBlkSize(),
                      pblkInfo->GetFeatureBlkSize(),
                      pblkInfo->GetBinBlkSize()*pblkInfo->GetFeatureBlkSize()*pblkInfo->GetFeatureBlkNum(featnum)*nodeSize);
    }

    //#endif


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

    inline void update(const SplitInfo& other){
        CHECK_EQ(sol.size(), other.sol.size());

        const int num_node = sol.size();
        for (int wid = 0; wid < num_node; ++wid) {
            if (sol[wid].Update(other.sol[wid])){
                left_sum[wid] = other.left_sum[wid];
            }
        }
    }

    //debug
    void print(char* tag, std::vector<int>& nodeset){
        const int num_node = sol.size();
        for (int wid = 0; wid < num_node; ++wid) {
            std::cout << tag << " nid:" << nodeset[wid] << ",fid:" << sol[wid].SplitIndex() <<
                ",val:" << sol[wid].split_value << 
                ",loss:" << sol[wid].loss_chg << "\n";
        }
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

            // fix the split of nodes
            nodesetSmall.push_back( splitResult[i].left );
            nodesetLarge.push_back( splitResult[i].right );
            //if (splitResult[i].left_len < splitResult[i].right_len){
                //nodesetSmall.push_back( splitResult[i].left );
                //nodesetLarge.push_back( splitResult[i].right );
            //}
            //else{
                //nodesetSmall.push_back( splitResult[i].right );
                //nodesetLarge.push_back( splitResult[i].left );
            //}

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

    // added for model rotation, the local model block id
    std::vector<bst_uint> localMBlk;
    // model block id invariant in rotation
    std::vector<bst_uint> ownedMBlk; 

    // for model rotation pipeline
    std::vector<bst_uint> localComp; // the computation pipeline (half elements of localMBlk)
    std::vector<bst_uint> localComm; // the communication pipelien

    // initialize the hist set
    void Init(const TrainParam &param, int nodesize, BlockInfo& blkinfo, int rowblknum = 1, 
            harp::com::Communicator* harpCom = nullptr, std::vector<bst_uint>* localFSet = nullptr, 
            bool usePipeline = false) 
    {

      // cleanup statistics
      //for (int tid = 0; tid < nthread; ++tid) 
      {
        hset.rptr = dmlc::BeginPtr(rptr);
        hset.cut = dmlc::BeginPtr(cut);
        hset.fsetSize = rptr.back();
        hset.featnum = rptr.size() - 2;
        hset.nodeSize = nodesize;
        hset.rowblknum = 1;
        hset.node_block_size = 1;
        hset.pblkInfo = &blkinfo;

        //
        //  <binid, nid, fid> layout means hole in the plain
        //  simple solution to allocate full space
        //  other than resort to remove the holes
        //
        unsigned long cubesize = blkinfo.GetModelCubeSize(param.max_bin, hset.featnum, nodesize);

        // this function will be called for only two times and in intialization
        if (hset.data != nullptr){
            //second time call init will prepare the memory for UpdateHistBlock
            std::free(hset.data);

            //allocate replica memory
            if (param.data_parallelism != 0){
                //hset.rowblknum = blkinfo.GetRowBlkNum(dmat_info_.num_row_);
                hset.rowblknum = rowblknum;
                hset.node_block_size = param.node_block_size;

                hset.size_replica = blkinfo.GetModelCubeSize(
                        param.max_bin, hset.featnum, hset.rowblknum * param.node_block_size);

                hset.data_replica = static_cast<TStats*>(malloc(sizeof(TStats) * hset.size_replica));
            }
            else{
                hset.node_block_size = 1;
            }
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

        LOG(CONSOLE)<< "Init hset(memset): rptrSize:" << rptr.size() <<
            ",cutSize:" <<  cut.size() <<",nodesize:" << nodesize <<
            ",fsetSize:" << rptr.back() << ",max_depth:" << param.max_depth << 
            ",featnum:" << hset.featnum <<
            ",rowblknum:" << hset.rowblknum << 
            ",nodeblksize:" << hset.node_block_size << 
            ",replics:" << hset.size_replica << 
            ",cubesize:" << cubesize << ":" << 8*cubesize/(1024*1024*1024) << "GB";

        // for model rotation
        if (localFSet)
        {
            int binBlkNum = blkinfo.GetBinBlkNum(param.max_bin); 
            int featBlkNum = blkinfo.GetFeatureBlkNum(hset.featnum);
            int baseFNum = (featBlkNum + harpCom->getWorldSize() - 1)/harpCom->getWorldSize();
            int workerID = harpCom->getWorkerId();

            for(int fid = 0; fid < featBlkNum; fid++)
            {
                if ((fid / baseFNum) == workerID)
                {
                    // shall put all of the features in fid block into localFSet
                    if (fid != featBlkNum -1)
                    {
                        // feature start from fid*blkinfo.ft_blksize to (fid+1)*blkinfo.ft_blksize
                        for(int k=fid*blkinfo.ft_blksize; k<(fid+1)*blkinfo.ft_blksize; k++)
                        {
                            localFSet->push_back(k);
                        }

                    } else {
                        for(int k=fid*blkinfo.ft_blksize; k<hset.featnum; k++) {
                            localFSet->push_back(k);
                        }
                    }

                    for (int j = 0; j < binBlkNum; j++)
                    {
                        localMBlk.push_back(fid * binBlkNum + j);
                        ownedMBlk.push_back(fid * binBlkNum + j);
                    }
                }
            }

            // for pipeline
            if (!usePipeline) {

                //hset.data = new TStats[localMBlk.size()*hset.GetHistUnitByBlkidSize()*nodesize];
                //hset.size = localMBlk.size()*hset.GetHistUnitByBlkidSize()*nodesize;
                hset.localBlkNum = localMBlk.size();
                hset.firstBlkID = localMBlk[0];

            } else {

                // for pipeline version of rotation
                //hset.size = localMBlk.size()*hset.GetHistUnitByBlkidSize()*nodesize;
                //hset.data = nullptr; //defer the memory allocation
                //  hset.localBlkNum = localMBlk.size();
                //  hset.firstBlkID = localMBlk[0];
                // split localMBlk into two parts
                int pipeSize = (localMBlk.size()/2) > 0 ? (localMBlk.size()/2) : 1;
                for (size_t i = 0; i < localMBlk.size(); i++)
                {
                    if (i < pipeSize)
                    {
                        localComp.push_back(localMBlk[i]);
                    }
                    else
                    {
                        localComm.push_back(localMBlk[i]);
                    }
                }

                //hset.dataSlotComp = new TStats[localComp.size()*hset.GetHistUnitByBlkidSize()*nodesize];
                //hset.sizeComp = localComp.size()*hset.GetHistUnitByBlkidSize()*nodesize;
                //hset.dataSlotComm = new TStats[localComm.size()*hset.GetHistUnitByBlkidSize()*nodesize];
                //hset.sizeComm = localComm.size()*hset.GetHistUnitByBlkidSize()*nodesize;
                hset.localBlkNum = localComp.size();
                hset.firstBlkID = localComp[0];

                hset.blkCommNum = localComm.size();
                hset.firstBlkDComm = localComm[0];

            }
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
  using  BlockBaseMakerLossguide<TStats>::runopenmp;

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

  //mixmode, mutex for shared object
  spin_mutex mutex_qexpand_;

  /* ---------------------------------------------------
   * functions
   * ---------------------------------------------------
   */


  /*
   * update function implementation
   * the entrance of gbm execution
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
    // TODO:harp allreduce here
    InitializeHist(gpair, p_fmat, *p_tree);

    // init the posset before building the tree
    this->InitPosSet(gpair, *p_fmat, *p_tree, 
            max_leaves_,
            blkInfo_.GetRowBlkSize());
    // init the gh sum in the beginning
    this->InitNodeStats(gpair, 
            &(this->thread_stats_), 
            &(this->wspace_.hset.GetNodeSum(0)));

    // init the nodes in the tree
    p_tree->InitModelNodes(max_leaves_ * 2);

    // init the counters on num_leaves to track the progress
    // of tree growth
    std::atomic<int> num_leaves(0);
    std::atomic<int> timestamp(0);

    // init the stop condition of phase1
    int max_leaves_phase1 = max_leaves_;
    const int threadNum = omp_get_max_threads();
    // async_mixmode=2 will skip mixmode
    if (param_.async_mixmode != 2){
        //max_leaves_phase1 = threadNum;
        max_leaves_phase1 = threadNum * 2;
    }
    
    // init the number of nodes pop out from the priority queue
    int topK = param_.topk;
    if (param_.topk <= 0){
        if (param_.grow_policy != TrainParam::kLossGuide) {
            topK = max_leaves_;
        }
        else{
            topK = 1;
        }
    }


    // process the root node
    {
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
        this->StartOpenMP();
        build_nodeset.push_back(0);
        large_nodeset.clear();
        BuildHist(gpair, build_nodeset, large_nodeset, bwork_set_, *p_tree);

        printtree(p_tree, "After CreateHist");

        if (localFeatSet_.size() > 0)
        {
            FindSplit(gpair, localFeatSet_, build_nodeset, splitOutput);

        } else {

            FindSplit(gpair, work_set_, build_nodeset, splitOutput);
        }
 

        qexpand_->push(this->newEntry(0, 0 /* depth */, splitOutput.sol[0], 
                    splitOutput.left_sum[0], timestamp++));
        num_leaves++;
    }

    // go to the first phase, in mose cases for few nodes with openmp
    GrowTheTree(gpair, p_fmat, p_tree, param_,
            num_leaves, timestamp, max_leaves_phase1, topK);

    if (max_leaves_phase1 < max_leaves_){
        // mix mode, go on with the remain expansion in node parallelism
        LOG(CONSOLE) << "Enter node parallel mode: num_leaves=" << num_leaves 
            << ",group_parallel_cnt=" << param_.group_parallel_cnt <<
            ",async_mode=" << param_.async_mixmode <<
            ",topk=" << param_.topk;

        //if (param_.grow_policy == TrainParam::kLossGuide) {
        {
            const int nsize = bwork_set_.size();
            // block number in the row dimension
            const int zsize = p_blkmat->GetBlockZCol(0).GetBlockNum();
            // block number in the node dimension
            const int qsize = param_.topk/ param_.node_block_size + ((param_.topk % param_.node_block_size)?1:0);

            LOG(CONSOLE) << "BuildHist:: qsize=" << qsize << 
                ",nsize=" << nsize << ",zsize=" << zsize;
        }

        //
        // model parallelism in the middle phase
        //
        int save_dataparallelism = param_.data_parallelism;
        // stop dataparallelism
        param_.data_parallelism = 0;
        if (param_.async_mixmode == 1){
            this->StopOpenMP();
            UpdateWithNodeParallel(gpair, p_fmat, p_tree,
                    num_leaves, timestamp);
            this->StartOpenMP();
        }
        else{
            // stop dataparallelism only
            // set node_block_size = 1 in lossguide
            int save_nodeblksize = param_.node_block_size;
            if (param_.grow_policy == TrainParam::kLossGuide) {
            //    param_.node_block_size = 1;
            }
            GrowTheTree(gpair, p_fmat, p_tree, param_,
                    num_leaves, timestamp, 
                    max_leaves_ - threadNum,
                    topK /*topK*/
                    );
            //restore the nodebk setting
            param_.node_block_size = save_nodeblksize;
        }
        param_.data_parallelism = save_dataparallelism;

        // end part, go back to openmp
        GrowTheTree(gpair, p_fmat, p_tree, param_,
                num_leaves, timestamp, max_leaves_, topK);
    }

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

  /*
   * GrowTheTree: main tree building precedure
   * Input:
   *    num_leaves  ;   global atomic variable
   *    timestamp   ;   global atomic variable
   *    max_leaves  ;   stop condition
   *    topK        ;   pop count for each time
   *    thredid     ;   threadid assigned, -1 means openmp inside
   * Output:
   *    qexpand_    ;   pop and push back when tree grows
   * Notes:
   *    thread function, auto get references to this object
   *    pop nodes until the num_leaves reach the stop condition
   *    the ultimate stop condition is param.max_leaves
   *
   *
   */
  void GrowTheTree (const std::vector<GradientPair> &gpair,
                      DMatrix *p_fmat,
                      RegTree *p_tree,
                      TrainParam& param,
                      std::atomic<int>& num_leaves,
                      std::atomic<int>& timestamp,
                      int max_leaves,
                      int topK,
                      int threadid = -1
                      ) {
      #ifdef USE_DEBUG
      auto curThreadId = std::this_thread::get_id();
      LOG(CONSOLE) << "Inside Thread :: ID  = "<<std::this_thread::get_id() <<
          "cur_num_leaves=" << num_leaves <<
          "stop_leaves=" << max_leaves <<
          "max_leaves=" << param.max_leaves;
      #endif

      std::vector<int> build_nodeset;
      std::vector<int> large_nodeset;
      std::vector<int> split_nodeset;
      //depth
      std::vector<int> build_depth;
      std::vector<int> large_depth;
      std::vector<int> split_depth;

      SplitInfo splitOutput;
      SplitResult splitResult;

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

          mutex_qexpand_.lock();
          if (qexpand_->empty()) break;
          const auto candidate = qexpand_->top();
          const int nid = candidate.nid;
          qexpand_->pop();
          mutex_qexpand_.unlock();

          if (candidate.sol.loss_chg <= kRtEps){
              //
              // set to permanent leaf when loss_chg is too small
              //
              (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
          } else if(param_.max_depth > 0 && candidate.depth == param_.max_depth){
              //
              // when stop condition matches for depth aspect
              //
              (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
              //if (param_.grow_policy == TrainParam::kLossGuide) {
              //    //continue popout until queue empty
              //    (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
              //}
              //else{
              //    // depthwise mode, last level leaves should not 
              //    // go to the priority queue
              //    // should not be here
              //    CHECK_NE(1,1);
              //}
          //} else if(param_.max_leaves > 0 && num_leaves + popCnt == max_leaves){
          } else if(num_leaves + popCnt == max_leaves){
              //
              // when stop condition matches for num_leaves aspect
              //
              // only the final run will set leaf when stop condition matched
              if (max_leaves == max_leaves_){
                  //continue popout until queue empty
                  (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
              }
              else{
                  // jump out, stop pop out
                  // and go for work at once
                  break;
              }
          } else {
              //
              // add to split set, they are ready to go
              //
              split_nodeset.push_back(nid);
              split_depth.push_back(candidate.depth);
              splitOutput.append(candidate.sol, candidate.left_sum);

              popCnt ++;
          }
        } /* end of while pop out nodes */

        //quit if no need to split
        if (split_nodeset.size() == 0) continue;

        #ifdef USE_DEBUG
        std::ostringstream stringStream;
        stringStream << "ThreadId=" << curThreadId << ", splitNode=" << split_nodeset[0];
        printmsg(stringStream.str());
        #endif

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

        //const int workerNum = param_.num_worker;
        const int workerNum = 1;
        if (workerNum <= 1){
            // do buildhist  
            printVec("BuildHist on build_nodeset:", build_nodeset);
            printVec("BuildHist on large_nodeset:", large_nodeset);
            BuildHist(gpair, build_nodeset, large_nodeset, bwork_set_, *p_tree, threadid);
            printtree(p_tree, "After CreateHist");

            // 4. find split for these nodes
            if(large_nodeset.size() > 0){
                build_nodeset.insert(build_nodeset.end(), large_nodeset.begin(),
                        large_nodeset.end());
                build_depth.insert(build_depth.end(), large_depth.begin(),
                        large_depth.end());
            }

            if (localFeatSet_.size() > 0)
            {
                FindSplit(gpair, localFeatSet_, build_nodeset, splitOutput, threadid);

            } else {

                FindSplit(gpair, work_set_, build_nodeset, splitOutput, threadid);
            }

            //debug
            #ifdef USE_DEBUG
            splitOutput.print("SplitInfo", build_nodeset);
            #endif      
            
        }
        else{
            // ================= begin simulate model rotation ==========================

            std::vector<int> nullset;
            std::vector<bst_uint> bwork_set;
            std::vector<bst_uint> fwork_set;
            nullset.resize(0);


#ifdef TRUE_ROTATE
            BuildHist(gpair, build_nodeset, large_nodeset, bwork_set_, *p_tree, threadid);
#else
            const int bblk_chunksize = (bwork_set_.size() + workerNum -1)/workerNum;
            for ( int i = 0; i < workerNum; i++){
                //prepare the bwork_set
                const int bblk_size = (i == workerNum-1)? 
                        (bwork_set_.size() - i* bblk_chunksize): bblk_chunksize;
                bwork_set.resize(bblk_size);
                std::copy(bwork_set_.data() + i*bblk_size, 
                        bwork_set_.data() + i*bblk_size + bblk_size,
                        bwork_set.begin() );

                // do buildhist  
                printVec("BuildHist on build_nodeset:", build_nodeset);
                printVec("BuildHist on large_nodeset:", large_nodeset);

                //LOG(CONSOLE) << "BuildHist on worker: " << i <<
                //    ",bwork_set.size=" << bwork_set.size();
                printVec("bwork_set:" , bwork_set);


                #ifdef USE_DEBUG
                {
                    const int nsize = bwork_set_.size();
                    // block number in the row dimension
                    const int zsize = p_blkmat->GetBlockZCol(0).GetBlockNum();
                    // block number in the node dimension
                    const int qsize = build_nodeset.size()/ param_.node_block_size + ((build_nodeset.size() % param_.node_block_size)?1:0);
                    LOG(CONSOLE) << "BuildHist:: qsize=" << qsize << 
                        ",nsize=" << nsize << ",zsize=" << zsize;
                }
                #endif

                BuildHist(gpair, build_nodeset, nullset, bwork_set, *p_tree, threadid);
                BuildHist(gpair, nullset, large_nodeset, bwork_set, *p_tree, threadid);
                //BuildHist(gpair, build_nodeset, large_nodeset, bwork_set, *p_tree, threadid);
                printtree(p_tree, "After CreateHist");

            }
            //halftrick
            //BuildHist(gpair, nullset, large_nodeset, bwork_set_, *p_tree, threadid);

#endif
            // 4. find split for these nodes
            if(large_nodeset.size() > 0){
                build_nodeset.insert(build_nodeset.end(), large_nodeset.begin(),
                        large_nodeset.end());
                build_depth.insert(build_depth.end(), large_depth.begin(),
                        large_depth.end());
            }

            const int fblk_chunksize = (fwork_set_.size() + workerNum -1)/workerNum;
            for ( int i = 0; i < workerNum; i++){
                SplitInfo splitOutputPartial;
                const int fblk_size = (i == workerNum-1)? 
                        (work_set_.size() - i* fblk_chunksize): fblk_chunksize;
                fwork_set.resize(fblk_size);
                std::copy(fwork_set_.data() + i*fblk_size, 
                        fwork_set_.data() + i*fblk_size + fblk_size,
                        fwork_set.begin() );


                //LOG(CONSOLE) << "FindSplit on worker: " << i <<
                //    ",fwork_set.size=" << fwork_set.size();
                printVec("fwork_set:" , fwork_set);

                if (localFeatSet_.size() > 0)
                {
                    FindSplit(gpair, localFeatSet_, build_nodeset, splitOutputPartial, threadid);

                }else {

                    FindSplit(gpair, work_set_, build_nodeset, splitOutputPartial, threadid);

                }

                #ifdef USE_DEBUG
                splitOutputPartial.print("SplitInfoPartial", build_nodeset);
                #endif

                if (i == 0){
                    splitOutput = splitOutputPartial;
                }
                else{
                    splitOutput.update(splitOutputPartial);
                }

                //debug
                #ifdef USE_DEBUG
                splitOutput.print("SplitInfo", build_nodeset);
                #endif
            }
            // ================= end simulate model rotation ==========================
        }


        // 4. push result to queue
        for (int i = 0; i < build_nodeset.size(); i++){

          mutex_qexpand_.lock();
          qexpand_->push(this->newEntry(build_nodeset[i],
                      build_depth[i],
                      //p_tree->GetDepth(build_nodeset[i]),
                      splitOutput.sol[i],
                      splitOutput.left_sum[i], timestamp++));
          mutex_qexpand_.unlock();

          num_leaves++;
        }
        //remove those parents node splitted
        num_leaves -= large_nodeset.size();

        //check finish
        if (max_leaves != max_leaves_ && num_leaves >= max_leaves) {
          //in the middle, can jump out
          //but the last one, must wait until all items in the queue pop out
          break;
        }
      } /* end of while */
  };


  /*
   * Build Tree By node parallelism
   * each node runs in parallel to grow the tree
   * this generates a different tree 
   *
   */
  void UpdateWithNodeParallel(const std::vector<GradientPair> &gpair,
                      DMatrix *p_fmat,
                      RegTree *p_tree,
                      std::atomic<int>& num_leaves,
                      std::atomic<int>& timestamp
                      ) {

    CHECK_LT(num_leaves, max_leaves_);

    //
    // spawn threads
    //
    // group_parallel_cnt can be different with omp_max_threads()
    // in debug mode
    //
    int threadNum = param_.group_parallel_cnt;
    CHECK_GT(max_leaves_ - threadNum, 0);

    std::vector<std::thread> threads;
    for(int i = 0; i < threadNum; i++){
        const int threadid = i;
        //becareful & is dangerous
        threads.push_back(std::thread([&]{
                   GrowTheTree(gpair, p_fmat, p_tree, param_,
                       num_leaves, timestamp, 
                       max_leaves_ - threadNum,
                       1 /*topK*/,
                       threadid /*threadid*/
                       );}));
    }

    //wait for end
    for(int i = 0; i < threads.size() ; i++)
    {
        threads.at(i).join();
    }

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
                 SplitInfo& splitOutput,
                 int threadid = -1) {

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
    //TODO: need check runopenmp?
    if (threadNum > nodeset.size() && runopenmp){
        //call feature wised parallelism
        FindSplitByFeature(gpair, featureset, nodeset, splitOutput, threadid);
    }
    else{
        //call node wised parallelism
        FindSplitByNode(gpair, featureset, nodeset, splitOutput, threadid);
    }
    this->tminfo.aux_time[5] += dmlc::GetTime() - _tstartFindSplit;

    if (harpCom_ && harpCom_->getWorldSize() > 1 && useModelRotation_ ) 
    {
        // model rotation to allreduce the splitOuput
        // splitOutput->std::vector<SplitEntry> sol;
        // splitOUput->std::vector<TStats> left_sum;
        // serialize sol and left_sum
        // first allgather sol and left_sum, than reduce them according to SplitEntry loss_chg
        int splitElementSize = splitOutput.sol.size();

        //debug check the sol and left_sum value
        /* for(int k=0; k<splitElementSize;k++)
           {
           std::cout<<"Worker: "<< harpCom_->getWorkerId() << " Before Split Node "<<k<<" loss: "<<splitOutput.sol[k].loss_chg <<" sindex: "<<splitOutput.sol[k].sindex <<
           " split value: " << splitOutput.sol[k].split_value<< std::endl;
           } */

        harp::ds::Table<int>* syncSolTable = new harp::ds::Table<int>(harpCom_->getWorkerId());
        harp::ds::Table<double>* syncSumTable = new harp::ds::Table<double>(harpCom_->getWorkerId());

        int IntPerElem = sizeof(SplitEntry)/sizeof(int);
        int DoublePerElem = sizeof(GradStats)/sizeof(double);

        // from SplitEntry* to int*
        int* solBufLocal = reinterpret_cast<int*>(&(splitOutput.sol[0]));
        int solBufLocalSize = (IntPerElem)*splitElementSize; 
        harp::ds::Partition<int>* solPar = new harp::ds::Partition<int>(harpCom_->getWorkerId(), solBufLocal, solBufLocalSize);
        syncSolTable->addPartition(solPar);
        // do not free data on input table
        harpCom_->allGather<int>(syncSolTable, false);

        // now syncTable has new partition data and the original splitOUput.sol underlying data is lost
        double* sumBufLocal = reinterpret_cast<double*>(&(splitOutput.left_sum[0]));
        int sumBufLocalSize = (DoublePerElem)*splitElementSize;
        harp::ds::Partition<double>* sumPar = new harp::ds::Partition<double>(harpCom_->getWorkerId(), sumBufLocal, sumBufLocalSize);
        syncSumTable->addPartition(sumPar);
        harpCom_->allGather<double>(syncSumTable, false);

        // find the partition with max loss the table
        std::vector<int> selectParID;
        for(int k = 0; k<splitElementSize; k++)
        {
            bst_float lossChgBase = 0.0;
            unsigned sindexBase = 0;
            int parIDBase = harpCom_->getWorkerId();
            bool initBase = false; 

            int offsetBuf = (k*IntPerElem);
            for (const auto p : *syncSolTable->getPartitions())
            {
                bool toReplace = false;
                int* pBufPar = p.second->getData() + offsetBuf;
                bst_float lossChgCompare = (reinterpret_cast<bst_float*>(pBufPar))[0];
                unsigned sindexCompare = (reinterpret_cast<unsigned*>(pBufPar))[(sizeof(bst_float)/sizeof(int))]; 
                sindexCompare = sindexCompare & ((1U << 31) - 1U);
                if (!initBase)
                {
                    lossChgBase = lossChgCompare;
                    sindexBase = sindexCompare;
                    parIDBase = p.first;
                    initBase = true;
                }
                else {

                    if (sindexBase <= sindexCompare) {
                        toReplace = (lossChgCompare > lossChgBase);
                    } else {
                        toReplace = !(lossChgBase > lossChgCompare);
                    }

                    if (toReplace)
                    {
                        lossChgBase = lossChgCompare;
                        sindexBase = sindexCompare;
                        parIDBase = p.first;
                    }

                }

            }

            selectParID.push_back(parIDBase);

        }

        // deserialize and construct the new splitElementSize;
        splitOutput.sol.clear();
        splitOutput.left_sum.clear();
        for(int k = 0; k<splitElementSize; k++)
        {
            int* selectBuf = syncSolTable->getPartition(selectParID[k])->getData();
            SplitEntry selectEntry; 
            std::copy(selectBuf + k*IntPerElem, selectBuf + (k+1)*IntPerElem, reinterpret_cast<int*>(&selectEntry));
            splitOutput.sol.push_back(selectEntry);

            double* selectSumBuf = syncSumTable->getPartition(selectParID[k])->getData();
            GradStats selectSum;
            std::copy(selectSumBuf + k*DoublePerElem, selectSumBuf + (k+1)*DoublePerElem, reinterpret_cast<double*>(&selectSum));
            splitOutput.left_sum.push_back(selectSum);
        }

        harp::ds::util::deleteTable(syncSolTable, true);
        harp::ds::util::deleteTable(syncSumTable, true);

    }  

  }
    


  //
  // use the existing memory layout and parallelism as in BuildHistBlock
  //
  void FindSplitByFeature(const std::vector<GradientPair> &gpair,
                 std::vector<bst_uint> featureset,
                 std::vector<int> nodeset,
                 SplitInfo& splitOutput, 
                 int threadid = -1) {

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
    #pragma omp parallel for schedule(static) if(runopenmp)
    for (size_t i = 0; i < fset.size(); ++i) {
        int tid = (threadid == -1)?omp_get_thread_num():threadid;
        int fid = fset[i];
        
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
                 SplitInfo& splitOutput,
                 int threadid = -1) {

    const std::vector<bst_uint> &fset = featureset;

    const int num_feature = fset.size();
    const int num_node = nodeset.size();

    std::vector<SplitEntry>& sol = splitOutput.sol;
    std::vector<TStats>& left_sum = splitOutput.left_sum;
    #pragma omp parallel for schedule(dynamic, 1) if(runopenmp)
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
      #pragma omp parallel for schedule(static) if(runopenmp)
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
                //const auto binid = col._blkaddrByRowId(ridx, 0);
                const auto binid = col._binidByRowId(ridx, 0);

                //#ifndef  USE_NOEMPTYPLACEHOLDER
                if (binid == EMPTYBINID){
                    // missing value, go to default
                    grp.setDefaultPosition(j);
                } else if (binid <= tree[nid].SplitCond()) {
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

        if (harpCom_ && harpCom_->getWorldSize() > 1)
            feat_helper_.SyncInfo(harpCom_);
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
            cut_.Init(p_fmat,param_.max_bin, harpCom_ /*256*/);
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
              LOG(CONSOLE) << "Special Colulmn:" << fid << ",cpt=" << cpt;

              // TODO: deal with special columns correctly
              // need to change the logic of feat2workindex_
              // should add these special column to match the model with the input matrix(cube)
              //
              //this->wspace_.cut.push_back(cpt + fabs(cpt) + kRtEps);
              //this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
              //this->wspace_.min_val.push_back(cut_.min_val[fid]);
            }
 
          }

          // reserve last value for global statistics
          this->wspace_.cut.push_back(0.0f);
          this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
        }
        // TODO: deal with special columns correctly
        //CHECK_EQ(this->wspace_.rptr.size(), (fwork_set_.size() + 1)  + 1);
        CHECK_EQ(this->wspace_.rptr.size(), (work_set_.size() + 1)  + 1);


        //
        // 3. Init blkInfo
        //
        dmat_info_ = p_fmat->Info();
        auto& _info = dmat_info_;
        this->blkInfo_ = BlockInfo(param_.row_block_size, param_.ft_block_size, param_.bin_block_size);
        this->blkInfo_.init(_info.num_row_, _info.num_col_, param_.max_bin);
        
        LOG(CONSOLE) << "Init Param: node_block_size=" << param_.node_block_size <<
            ",topK=" << param_.topk <<
            ",group_parallel_cnt=" << param_.group_parallel_cnt;

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
            // InitHistIndexByCol removed !!
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
            LOG(CONSOLE) << "Load cube from metafile:" << param_.loadmeta;

            //load cube
            std::string fname;

            //load from meta-r#f#b#.blkmat
            std::ostringstream stringStream;
            stringStream << param_.loadmeta << 
                "-r" << blkInfo_.GetRowBlkSize() <<
                "-f" << blkInfo_.GetFeatureBlkSize() <<
                "-b" << blkInfo_.GetBinBlkSize() <<  ".blkmat";
            fname = stringStream.str();
            p_blkmat->load(fname, param_.max_bin, blkInfo_);

            fname = param_.loadmeta + ".hmat";
            BlockInfo hmat_blkInfo = BlockInfo(0, 1, 0);
            p_hmat->load(fname, param_.max_bin, hmat_blkInfo);
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

        // 7. Set the tree growth policy
        //
        LOG(INFO) << "re-order qexpand and block work set initialization";
        this->InitQExpand();
//
        // 7. Re-Init the model memory space, first touch
        //
        int rowblknum = blkInfo_.GetRowBlkNum(dmat_info_.num_row_);

        if (this->harpCom_ && this->harpCom_->getWorldSize() > 1 && useModelRotation_) 
        {
            this->wspace_.Init(this->param_, max_leaves_, this->blkInfo_, rowblknum, 
                    harpCom_, &localFeatSet_, usePipeline_);
        }else{
            this->wspace_.Init(this->param_, max_leaves_, this->blkInfo_, rowblknum);
        }
            
        //
        // 6. Init the block work set
        //  fwork_set_ --> the full features set
        //  work_set_  --> working feature set
        //  bwork_set_ --> the block set
    
        if (this->harpCom_ && this->harpCom_->getWorldSize() > 1 && this->useModelRotation_)
        {
            std::vector<bst_uint> &localWSet = this->wspace_.localMBlk; 
            // only use assigned partitions of model block ids
            this->bwork_base_blknum_ = localWSet.size();
            bwork_lock_ = new spin_mutex[this->bwork_base_blknum_];
            bwork_set_.clear();
            //bwork_set_ contains the block workset ids
            for (int i = 0; i < localWSet.size(); i++)
            {
                bwork_set_.push_back(localWSet[i]);
            }

            LOG(CONSOLE) << "Init bwork_set_: base_blknum=" << bwork_base_blknum_ << ":" << bwork_set_.size();
            printVec("bwork_set_:", bwork_set_);

        } else {

            this->bwork_base_blknum_ = p_blkmat->GetBaseBlockNum();
            bwork_lock_ = new spin_mutex[bwork_base_blknum_];
            bwork_set_.clear();
            //for(int i=0; i < p_blkmat->GetBlockNum(); i++){
            for(int i=0; i < p_blkmat->GetBaseBlockNum(); i++){
                bwork_set_.push_back(i);
            }

            LOG(CONSOLE) << "Init bwork_set_: base_blknum=" << bwork_base_blknum_ <<
                ":" << bwork_set_.size();
            printVec("bwork_set_:", bwork_set_);

        }

        //

        // save meta mat
        if (!param_.savemeta.empty()){

            LOG(CONSOLE) << "Save metafile:" << param_.savemeta;

            std::string fname;
            fname = param_.savemeta + ".meta";
            SaveMatrixMeta(cut_, dmat_info_, feat_helper_,
                    fname);

            std::ostringstream stringStream;
            stringStream << param_.savemeta << 
                "-r" << blkInfo_.GetRowBlkSize() <<
                "-f" << blkInfo_.GetFeatureBlkSize() <<
                "-b" << blkInfo_.GetBinBlkSize() <<  ".blkmat";
            fname = stringStream.str();
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
   *    nodeblkid   ;   node blk id (param_.node_block_size)
   *    build_nodeset  ;   node set to build
   * Dependency:
   *    posset_ ;   row set info
   *
   */
  void UpdateHistBlockWithReplica(const std::vector<GradientPair> &gpair,
                       const TDMatrixCubeBlock<TBlkAddr> &block,
                       const RegTree &tree,
                       bst_uint blkid_offset,
                       unsigned int zblkid,
                       int nodeblkid,
                       const std::vector<int>& build_nodeset,
                       std::vector<HistEntryCompact> *p_temp) {
    //check size
    if (block.size() == 0) return;

    // initialize sbuilder for use
    std::vector<HistEntryCompact> &hbuilder = *p_temp;
    //todo, 
    //  as in lossguide, build_nodeset.size() is small
    //  change vector to map will be better here
    hbuilder.resize(tree.num_nodes);

    int start_node_offset, end_node_offset;
    if (nodeblkid >= 0 ){
        start_node_offset = nodeblkid * param_.node_block_size;
        end_node_offset = std::min((int)build_nodeset.size(),
            start_node_offset + param_.node_block_size);
    }
    else{
        start_node_offset = 0;
        end_node_offset = build_nodeset.size();
    }

    // no lazy init neccessary for a small size nodeset
    for (int i = start_node_offset; i < end_node_offset; ++i) {
        int nid = build_nodeset[i];
        //lazy initialize
        if (zblkid == 0){
            //get model from main model
            int mid = node2workindex_[nid];
            hbuilder[nid] = this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid, zblkid);
        }
        else{
            //get from replicas
            int mid = i - start_node_offset;
            hbuilder[nid] = this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid, zblkid);

        }
        //init data
        hbuilder[nid].ClearData(this->wspace_.hset.GetHistUnitByBlkidSize());
    }

    //get lock
    //bwork_lock_[blkid_offset].lock();

    for (int i = start_node_offset; i < end_node_offset; ++i) {
        const int nid = build_nodeset[i];
        auto& grp = posset_.getGroup(nid, zblkid);

        //CHECK_NE(grp.isDummy(), true);
        if (grp.isDummy()) continue;

        for (int j = 0; j < grp.size(); ++j) {
            const int ridx = grp.getRowId(j);

            for (int k = 0; k < block.rowsizeByRowId(ridx); k++){

                if (block._binidByRowId(ridx,k) == EMPTYBINID) continue;

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
    //bwork_lock_[blkid_offset].unlock();

  }

  //
  // no rowblk model replicas
  //
  void UpdateHistBlock(const std::vector<GradientPair> &gpair,
                       const TDMatrixCubeBlock<TBlkAddr> &block,
                       const RegTree &tree,
                       bst_uint blkid_offset,
                       unsigned int zblkid,
                       int nodeblkid,
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

    #ifdef USE_MIXMODE
    // initialize sbuilder for use
    std::vector<HistEntryCompact> hbuilder;
    hbuilder.resize(tree.num_nodes);
    #else
    // initialize sbuilder for use
    std::vector<HistEntryCompact> &hbuilder = *p_temp;
    //todo, 
    //  as in lossguide, build_nodeset.size() is small
    //  change vector to map will be better here
    hbuilder.resize(tree.num_nodes);
    #endif




    int start_node_offset, end_node_offset;
    if (nodeblkid >= 0 ){
        start_node_offset = nodeblkid * param_.node_block_size;
        end_node_offset = std::min((int)build_nodeset.size(),
            start_node_offset + param_.node_block_size);
    }
    else{
        start_node_offset = 0;
        end_node_offset = build_nodeset.size();
    }


    // no lazy init neccessary for a small size nodeset
    for (int i = start_node_offset; i < end_node_offset; ++i) {
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
    #ifndef USE_MIXMODE
    bwork_lock_[blkid_offset].lock();
    #endif

    for (int i = start_node_offset; i < end_node_offset; ++i) {
        const int nid = build_nodeset[i];
        auto& grp = posset_.getGroup(nid, zblkid);

        //CHECK_NE(grp.isDummy(), true);
        if (grp.isDummy()) continue;

        for (int j = 0; j < grp.size(); ++j) {
            const int ridx = grp.getRowId(j);

            for (int k = 0; k < block.rowsizeByRowId(ridx); k++){

                if (block._binidByRowId(ridx,k) == EMPTYBINID) continue;

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
    #ifndef USE_MIXMODE
    bwork_lock_[blkid_offset].unlock();
    #endif

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
          #ifndef USE_MIXMODE
          if ((*p_last_tree_)[tnid].IsDummy()) {
              continue;
          }
          #endif
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
      LOG(CONSOLE) << "UpdateCache: num_leaves=" << num_node <<
                ",leaf_sum=" << leaf_val_sum << 
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
                  const RegTree &tree,
                  const int threadid = -1) {

      const MetaInfo &info = dmat_info_;

      //if (build_nodeset.size() == 0){
        //last step to update position 
      //  return;
      //}

      // start enumeration
      double _tstart = dmlc::GetTime();

      // for model rotation
      const std::vector<bst_uint>* pblkCompSet = nullptr;
      if (harpCom_ && harpCom_->getWorldSize() > 1 && usePipeline_) {
          pblkCompSet = &this->wspace_.localComp;
          this->wspace_.hset.inPipeline = true;
          this->wspace_.hset.isDataSeperate = false;
      }else{
          pblkCompSet = &blkset;
      }

      // block number on the base plain
      // const int nsize = p_blkmat->GetBaseBlockNum();
      const int nsize = pblkCompSet->size();
      // block number in the row dimension
      const int zsize = p_blkmat->GetBlockZCol(0).GetBlockNum();
      // block number in the node dimension
      const int qsize = build_nodeset.size()/ param_.node_block_size + ((build_nodeset.size() % param_.node_block_size)?1:0);


      #ifdef USE_DEBUG
      this->datasum_ = 0.;
      #endif

      // local computation
      LocalComputation(nsize, zsize, qsize, threadid, gpair, build_nodeset, pblkCompSet,tree);

      // implement the allreduce computation model 
      if (build_nodeset.size() > 0 && harpCom_ && harpCom_->getWorldSize() > 1 && (!useModelRotation_))
      {
          AllreduceModel(pblkCompSet, build_nodeset);
      }

      // implement the model rotation model 
      if (harpCom_ && harpCom_->getWorldSize() > 1 && useModelRotation_)
      {
          harp::ds::Table<double> *syncTable = new harp::ds::Table<double>(harpCom_->getWorkerId());
          harp::ds::Table<int> *syncTableMeta = new harp::ds::Table<int>(harpCom_->getWorkerId());

          RotateAllreduceNodeSum(syncTable);

          // (2) rotate hset.data
          if (!usePipeline_)
          {

              for(int rIter = 0; rIter<harpCom_->getWorldSize(); rIter++) 
              {
                  // first rotate but not copy data to hset, only update the localMBlk 
                  RotateComm(syncTable, syncTableMeta, build_nodeset);

                  // ---------------------------- start the local computation on the rotated data ----------------------------
                  if (rIter != harpCom_->getWorldSize() - 1)
                  { // do not update the last rotated block
                      LocalComputation(nsize, zsize, qsize, threadid, gpair, build_nodeset, &(this->wspace_.localMBlk),tree);
                  } 

                  // add the rotated data back to hset.data
                  RotateCommAppend(rIter, syncTable, build_nodeset);

              } //  End of rotate for loop  

          }

          delete syncTable;
          delete syncTableMeta;
      }

      //
      // build the other half
      //
      if (large_nodeset.size() != 0){
          // only happens when in halftrick
          #ifdef USE_HALFTRICK_EX
          double _tstart2 = dmlc::GetTime();
          #pragma omp parallel for schedule(static) if(runopenmp)
          for (int i = 0; i < nsize; ++i) {
            int offset = blkset[i];

            this->UpdateHalfTrick(offset, tree, large_nodeset);
          }

          //debug
          datasum_ += 10000;
          this->tminfo.aux_time[6] += dmlc::GetTime() - _tstart2;
          #endif
      }

      #ifdef USE_DEBUG
      LOG(CONSOLE) << "BuildHist:: datasum_=" << this->datasum_;
      #endif
      //if (param_.grow_policy != TrainParam::kLossGuide) {
      //LOG(CONSOLE) << "BuildHist:: qsize=" << qsize << 
      //    ",nsize=" << nsize << ",zsize=" << zsize << 
      //    ",nodeset_size=" << build_nodeset.size() <<
      //    ",largeset_size=" << large_nodeset.size();
      //}

      this->tminfo.buildhist_time += dmlc::GetTime() - _tstart;

      //this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset.data),
      //                        this->wspace_.hset.data.size());

  }

  /**
   * @brief rotate model without pipeline, communicate data
   * 
   */
  void RotateComm(harp::ds::Table<double>* syncTable, harp::ds::Table<int>* syncTableMeta, 
          const std::vector<int>& build_nodeset) 
  {
      
      harpCom_->barrier();
      // pack the local data
      double *rawData = reinterpret_cast<double *>(this->wspace_.hset.data);

      // for each block, only takes out nodes in build_nodeset 
      int rawDataPackSize = this->wspace_.localMBlk.size()*this->wspace_.hset.GetHistUnitByBlkidSize()*build_nodeset.size()*2; 
      // a dummy rotation if build_nodeset == 0
      if (rawDataPackSize == 0)
          rawDataPackSize = 1;

      double* rawDataPack = new double[rawDataPackSize];

      // pack copy the data
      int copyVolume = this->wspace_.hset.GetHistUnitByBlkidSize()*2;
      int packOffset = 0;

      if (build_nodeset.size() > 0)
      {
          for(int i=0; i<this->wspace_.localMBlk.size(); i++)
          {
              int hsetOffset = (this->wspace_.localMBlk[i])*this->wspace_.hset.GetHistUnitByBlkidSize()*this->wspace_.hset.nodeSize*2;
              for (int j = 0; j < build_nodeset.size(); ++j) 
              {
                  int nodesOffset = copyVolume*node2workindex_[build_nodeset[j]];
                  std::copy(rawData + hsetOffset + nodesOffset, rawData + hsetOffset + nodesOffset + copyVolume, rawDataPack + copyVolume*packOffset);
                  packOffset++;
              }
          }
      }else {
          // dummy rotation
          rawDataPack[0] = 0;
      }

      harp::ds::Partition<double> *dataPar = new harp::ds::Partition<double>(harpCom_->getWorkerId(), rawDataPack, rawDataPackSize);
      syncTable->addPartition(dataPar);
      // start rotate
      harpCom_->rotate<double>(syncTable);
      delete[] rawDataPack;

      // recover the hset.firstBlk, localblkNum, and wspace_.localMBlk 
      // rotate the wspace_.localMBlk vector
      int* localBlkMetaData = reinterpret_cast<int*>(&(this->wspace_.localMBlk)[0]);
      harp::ds::Partition<int> *localBlkMetaPar = new harp::ds::Partition<int>(harpCom_->getWorkerId(), localBlkMetaData, this->wspace_.localMBlk.size());
      syncTableMeta->addPartition(localBlkMetaPar);
      harpCom_->rotate<int>(syncTableMeta);

      // std::cout<<"Finish rotation of localMBlk"<<std::endl;

      // only one partition allowed in one rotation
      for (const auto p : *syncTableMeta->getPartitions()) {
          //debug
          // std::cout<<"Receiving Meta data from worker: "<<p.first<<std::endl;
          this->wspace_.localMBlk.clear();
          this->wspace_.localMBlk.resize(p.second->getSize());
          std::copy(p.second->getData(), p.second->getData()+p.second->getSize(), &(this->wspace_.localMBlk[0]));
      }

      this->wspace_.hset.firstBlkID = this->wspace_.localMBlk[0];
      this->wspace_.hset.localBlkNum = this->wspace_.localMBlk.size();

      // free the memory
      syncTableMeta->clear(true);

  }

  void RotateCommAppend(int rIter, harp::ds::Table<double>* syncTable, const std::vector<int>& build_nodeset) 
  {

      double *rawData = reinterpret_cast<double *>(this->wspace_.hset.data);

      int copyVolume = this->wspace_.hset.GetHistUnitByBlkidSize()*2;
      int packOffset = 0;
      // only one partiton allowed
      // deferred 
      for (const auto p : *syncTable->getPartitions()) {

          // create the new buffer
          // append the reduced data back to hset.data
          for(int i=0; i<this->wspace_.localMBlk.size(); i++)
          {
              int hsetOffset = (this->wspace_.localMBlk[i])*this->wspace_.hset.GetHistUnitByBlkidSize()*this->wspace_.hset.nodeSize*2;

              for (int j = 0; j < build_nodeset.size(); ++j) 
              {
                  int nodesOffset = copyVolume*node2workindex_[build_nodeset[j]];
                  double* psrc = p.second->getData() + packOffset*copyVolume; 
                  double* pdst = rawData + hsetOffset + nodesOffset; 
                  if (rIter != harpCom_->getWorldSize() - 1)
                  {
                      for(int k=0; k < copyVolume; k++)
                      {
                          pdst[k] += psrc[k];
                      }

                  }
                  else{
                      std::copy(psrc, psrc + copyVolume, pdst);
                  }

                  packOffset++;
              }

          }

      } 

      syncTable->clear(true);
  }

  /**
   * @brief allreduce the first element of hset.nodesum in rotation
   * 
   * @param syncTable 
   */
  void RotateAllreduceNodeSum(harp::ds::Table<double>* syncTable) {

        double *rawNodeSum = reinterpret_cast<double *>(this->wspace_.hset.nodesum);
        int rawNodeSumSize = 2;
        harp::ds::Partition<double> *nodeSumPar = new harp::ds::Partition<double>(0, rawNodeSum, rawNodeSumSize);
        syncTable->addPartition(nodeSumPar);
        harpCom_->allReduce<double>(syncTable, MPI_SUM, true);
        syncTable->removePartition(0, false);
  }

  void LocalComputation(const int nsize, const int zsize, const int qsize, const int threadid,
          const std::vector<GradientPair> &gpair, const std::vector<int>& build_nodeset, 
          const std::vector<bst_uint>* pblkCompSet,
          const RegTree &tree) 
  {
      // only works on the build_nodeset
      if(build_nodeset.size() > 0)
      {
          //
          // use data parallelism will bulid model on replicas
          // it's efficient for thin matrix
          //
          if (param_.data_parallelism != 0){
              //move node block parallelism out to 
              //control the replica memory foot print

              //int omp_loop_size = qsize * zsize * nsize;
              for(int nblkid = 0 ; nblkid < qsize; nblkid++){
                  // omp parallel on zsize*nsize
                  int omp_loop_size = zsize * nsize;
#pragma omp parallel for schedule(dynamic, 1) if(runopenmp)
                  for(int i = 0; i < omp_loop_size; i++){
                      // decode to get the block ids
                      // get node block id
                      //unsigned int nblkid = i / (zsize * nsize);

                      // blk id in the model cube
                      int blkid = i % (zsize * nsize);
                      // blk id on the base plain
                      int offset = (*pblkCompSet)[blkid % nsize];
                      // blk id on the row dimension
                      unsigned int zblkid = blkid / nsize;

                      // get dataBlock
                      auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                      int tid = (threadid == -1)?omp_get_thread_num():threadid;

                      // update model by this dataBlock and node_blkid
                      this->UpdateHistBlockWithReplica(gpair, block, tree,
                              offset, zblkid, nblkid,
                              build_nodeset,
                              &this->thread_histcompact_[tid]);

                  } // end of omp loop over zsize*nsize

                  //reduce the replicas
                  // nodesize * blksize
                  int start_node_offset, end_node_offset, gsize;
                  start_node_offset = nblkid * param_.node_block_size;
                  end_node_offset = std::min((int)build_nodeset.size(),
                          start_node_offset + param_.node_block_size);
                  gsize = end_node_offset - start_node_offset;

                  omp_loop_size = gsize * nsize;
#pragma omp parallel for schedule(static) if(runopenmp)
                  for(bst_omp_uint i = 0; i < omp_loop_size; ++i){
                      // get node blk offset
                      int nodeblk_offset = i / nsize;
                      // absolute blk id
                      int blkid_offset = (*pblkCompSet)[i % (nsize)];
                      int plainSize = this->wspace_.hset.GetHistUnitByBlkidSize();

                      int nid = build_nodeset[start_node_offset + nodeblk_offset];

                      // plain 0 in main model
                      int mid = node2workindex_[nid];
                      double* p_zplain0 = reinterpret_cast<double*>(
                              this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid, 0).data);

                      // other plains in replica
                      mid = nodeblk_offset;
                      for(int z = 1; z < zsize; z++){
                          //sum to rowblk 0
                          double* p_z = reinterpret_cast<double*>(
                                  this->wspace_.hset.GetHistUnitByBlkidCompact(blkid_offset, mid, z).data);

                          //#pragma omp simd
#pragma GCC ivdep
                          for(int j = 0; j < 2 * plainSize; j++){
                              p_zplain0[j] += p_z[j];
                          }

                      }
                  } // end of omp loop on gsize*nsize 

              } // end of loop over qsize
          } // end of non-data parallelsim
          else{

              // no data parallelism goes here
              // use spin lock instead of replicas
              // it's good for fat matrix

              //
              // use spinlock to schedule zbloks together
              //
              if (param_.use_spinlock == 1){
                  int omp_loop_size = qsize * zsize * nsize;
#pragma omp parallel for schedule(dynamic, 1) if(runopenmp)
                  for(int i = 0; i < omp_loop_size; i++){
                      // decode to get the block ids
                      // get node block id
                      unsigned int nblkid = i / (zsize * nsize);

                      // blk id in the model cube
                      int blkid = i % (zsize * nsize);
                      // blk id on the base plain
                      int offset = (*pblkCompSet)[blkid % nsize];
                      // blk id on the row dimension
                      unsigned int zblkid = blkid / nsize;

                      // get dataBlock
                      auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                      int tid = (threadid == -1)?omp_get_thread_num():threadid;
                      // update model by this dataBlock and node_blkid
                      this->UpdateHistBlock(gpair, block, tree,
                              offset, zblkid, nblkid,
                              build_nodeset,
                              &this->thread_histcompact_[tid]);
                  }
              }
              else{
                  //
                  // this sync for each zcol level
                  //

                  if (param_.async_mixmode == 1){
                      for (int z = 0; z < zsize; z++){
                          int omp_loop_size = qsize * nsize;
                          if (threadid == -1){
#pragma omp parallel for schedule(dynamic, 1) if(runopenmp)
                              for(int i = 0; i < omp_loop_size; i++){
                                  // decode to get the block ids
                                  // get node block id
                                  unsigned int nblkid = i / (nsize);

                                  // blk id in the model cube
                                  int blkid = i % (nsize);
                                  // blk id on the base plain
                                  int offset = (*pblkCompSet)[blkid % nsize];
                                  // blk id on the row dimension
                                  unsigned int zblkid = z;

                                  // get dataBlock
                                  auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                                  int tid = (threadid == -1)?omp_get_thread_num():threadid;
                                  // update model by this dataBlock and node_blkid
                                  this->UpdateHistBlock(gpair, block, tree,
                                          offset, zblkid, nblkid,
                                          build_nodeset,
                                          &this->thread_histcompact_[tid]);
                              }
                          }
                          else{
                              for(int i = 0; i < omp_loop_size; i++){
                                  // decode to get the block ids
                                  // get node block id
                                  unsigned int nblkid = i / (nsize);

                                  // blk id in the model cube
                                  int blkid = i % (nsize);
                                  // blk id on the base plain
                                  int offset = (*pblkCompSet)[blkid % nsize];
                                  // blk id on the row dimension
                                  unsigned int zblkid = z;

                                  // get dataBlock
                                  auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                                  int tid = (threadid == -1)?omp_get_thread_num():threadid;
                                  // update model by this dataBlock and node_blkid
                                  this->UpdateHistBlock(gpair, block, tree,
                                          offset, zblkid, nblkid,
                                          build_nodeset,
                                          &this->thread_histcompact_[tid]);
                              }

                          }

                      } /* for z */
                  } // for async mixmode 
                  else{

                      // no async mode
                      int omp_loop_size = qsize * nsize;
#pragma omp parallel for schedule(dynamic, 1) if(runopenmp)
                      for(int i = 0; i < omp_loop_size; i++){
                          // decode to get the block ids
                          // get node block id
                          unsigned int nblkid = i / (nsize);

                          // blk id in the model cube
                          int blkid = i % (nsize);
                          // blk id on the base plain
                          int offset = (*pblkCompSet)[blkid % nsize];

                          int tid = (threadid == -1)?omp_get_thread_num():threadid;


                          for (int z = 0; z < zsize; z++){
                              // blk id on the row dimension
                              unsigned int zblkid = z;
                              // get dataBlock
                              auto block = p_blkmat->GetBlockZCol(offset).GetBlock(zblkid);

                              // update model by this dataBlock and node_blkid
                              this->UpdateHistBlock(gpair, block, tree,
                                      offset, zblkid, nblkid,
                                      build_nodeset,
                                      &this->thread_histcompact_[tid]);
                          }
                      }

                      //debug
                      datasum_ += 1;

                  } // end for no async mode

              } // end of sync z

          } // end of data parallelsim


      } // end of build_nodeset

  }

  void AllreduceModel(const std::vector<bst_uint>* pblkCompSet, const std::vector<int>& build_nodeset) 
  {
          // use the allreduce computation model
          // check this snippet of codes
          harp::ds::Table<double> *syncTable = new harp::ds::Table<double>(harpCom_->getWorkerId());
          double *rawData = reinterpret_cast<double *>(this->wspace_.hset.data);
          double *rawNodeSum = reinterpret_cast<double *>(this->wspace_.hset.nodesum);

          // takes out the nodes in build_nodeset
          int rawDataPackSize = pblkCompSet->size()*this->wspace_.hset.GetHistUnitByBlkidSize()*build_nodeset.size()*2; 
          double* rawDataPack = new double[rawDataPackSize];
          // pack copy the data
          // retrieve all node values
          int copyVolume = this->wspace_.hset.GetHistUnitByBlkidSize()*2;
          int packOffset = 0;
          for(int i=0; i<pblkCompSet->size(); i++)
          {
            int hsetOffset = (*pblkCompSet)[i]*this->wspace_.hset.GetHistUnitByBlkidSize()*this->wspace_.hset.nodeSize*2;
            for (int j = 0; j < build_nodeset.size(); ++j) 
            {
                int nodesOffset = copyVolume*node2workindex_[build_nodeset[j]];
                std::copy(rawData + hsetOffset + nodesOffset, rawData + hsetOffset + nodesOffset + copyVolume, rawDataPack + copyVolume*packOffset);
                packOffset++;
            }
          }

          int rawNodeSumSize = 2;
          harp::ds::Partition<double> *dataPar = new harp::ds::Partition<double>(0, rawDataPack, rawDataPackSize);
          harp::ds::Partition<double> *nodeSumPar = new harp::ds::Partition<double>(1, rawNodeSum, rawNodeSumSize);

          syncTable->addPartition(dataPar);
          syncTable->addPartition(nodeSumPar);

          harpCom_->barrier();

          harpCom_->allReduce<double>(syncTable, MPI_SUM, true);

          harpCom_->barrier();
          syncTable->removePartition(0, false);
          syncTable->removePartition(1, false);

          // unpack the reduced data back to hset.data
          packOffset = 0;
          for(int i=0; i<pblkCompSet->size(); i++)
          {
            int hsetOffset = (*pblkCompSet)[i]*this->wspace_.hset.GetHistUnitByBlkidSize()*this->wspace_.hset.nodeSize*2;
            for (int j = 0; j < build_nodeset.size(); ++j) 
            {
                int nodesOffset = copyVolume*node2workindex_[build_nodeset[j]];
                std::copy(rawDataPack + copyVolume*packOffset, rawDataPack + copyVolume*(packOffset+1), rawData + hsetOffset + nodesOffset);
                packOffset++;
            }

          }

          delete[] rawDataPack;
          delete syncTable;

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

      int omp_loop_size = nodeset.size();
      #pragma omp parallel for schedule(static) if ((omp_loop_size >= 512)&&(runopenmp))
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


    #ifdef USE_MIXMODE
    tree.param.num_nodes = static_cast<int>(nodes.size());
    #endif

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
