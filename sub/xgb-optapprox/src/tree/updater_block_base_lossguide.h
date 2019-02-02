/*!
 * Copyright 2014 by Contributors
 * \file updater_basemaker-inl.h
 * \brief implement a common tree constructor
 * \author Tianqi Chen
 *
 * 2018,2019
 * HARPDAAL-GBT optimize based on the approx and fast_hist codebase
 * Code based on updater_basemaker-inl.h
 *
 *
 */
#ifndef XGBOOST_TREE_UPDATER_BASEMAKER_INL_H_
#define XGBOOST_TREE_UPDATER_BASEMAKER_INL_H_

#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
#include <queue>
#include <algorithm>
#include <string>
#include <limits>
#include <utility>
#include "./param.h"
#include "../common/sync.h"
#include "../common/io.h"
#include "../common/random.h"
#include "../common/quantile.h"
#include "../common/pos_set_lossguide.h"

namespace xgboost {
namespace tree {
/*!
 * \brief base tree maker class that defines common operation
 *  needed in tree making
 */
template<typename TStats>
class BlockBaseMakerLossguide: public TreeUpdater {
 public:
  void Init(const std::vector<std::pair<std::string, std::string> >& args) override {
    param_.InitAllowUnknown(args);
  }

  TimeInfo getTimeInfo() override{
      return tminfo;
  }

 protected:
  // helper to collect and query feature meta information
  struct FMetaHelper {
   public:
    /*! \brief find type of each feature, use column format */
    inline void InitByCol(DMatrix* p_fmat,
                          const RegTree& tree) {
      fminmax_.resize(tree.param.num_feature * 2);
      std::fill(fminmax_.begin(), fminmax_.end(),
                -std::numeric_limits<bst_float>::max());
      // start accumulating statistics
      for (const auto &batch : p_fmat->GetSortedColumnBatches()) {
        for (bst_uint fid = 0; fid < batch.Size(); ++fid) {
          auto c = batch[fid];
          if (c.size() != 0) {
            fminmax_[fid * 2 + 0] =
                std::max(-c[0].fvalue, fminmax_[fid * 2 + 0]);
            fminmax_[fid * 2 + 1] =
                std::max(c[c.size() - 1].fvalue, fminmax_[fid * 2 + 1]);
          }
        }
      }
    }
    /*! \brief synchronize the information */
    inline void SyncInfo() {
      rabit::Allreduce<rabit::op::Max>(dmlc::BeginPtr(fminmax_), fminmax_.size());
    }
    // get feature type, 0:empty 1:binary 2:real
    inline int Type(bst_uint fid) const {
      CHECK_LT(fid * 2 + 1, fminmax_.size())
          << "FeatHelper fid exceed query bound ";
      bst_float a = fminmax_[fid * 2];
      bst_float b = fminmax_[fid * 2 + 1];
      if (a == -std::numeric_limits<bst_float>::max()) return 0;
      if (-a == b) {
        return 1;
      } else {
        return 2;
      }
    }
    inline bst_float MaxValue(bst_uint fid) const {
      return fminmax_[fid *2 + 1];
    }
    inline void SampleCol(float p, std::vector<bst_uint> *p_findex) const {
      std::vector<bst_uint> &findex = *p_findex;
      findex.clear();
      for (size_t i = 0; i < fminmax_.size(); i += 2) {
        const auto fid = static_cast<bst_uint>(i / 2);
        if (this->Type(fid) != 0) findex.push_back(fid);
      }
      auto n = static_cast<unsigned>(p * findex.size());
      std::shuffle(findex.begin(), findex.end(), common::GlobalRandom());
      findex.resize(n);
      // sync the findex if it is subsample
      std::string s_cache;
      common::MemoryBufferStream fc(&s_cache);
      dmlc::Stream& fs = fc;
      if (rabit::GetRank() == 0) {
        fs.Write(findex);
      }
      rabit::Broadcast(&s_cache, 0);
      fs.Read(&findex);
    }

   private:
    std::vector<bst_float> fminmax_;
  };

 
  //
  // tree growing policies 
  //
  struct ExpandEntry {
    int nid;
    int depth;
    SplitEntry sol;
    TStats left_sum;
    unsigned timestamp;

    ExpandEntry(int nid, int depth, SplitEntry sol, TStats left_sum, unsigned tstmp)
      : nid(nid), depth(depth), sol(sol), left_sum(left_sum),timestamp(tstmp) {}
  };
  inline static bool DepthWise(ExpandEntry lhs, ExpandEntry rhs) {
    if (lhs.depth == rhs.depth) {
      return lhs.timestamp > rhs.timestamp;  // favor small timestamp
    } else {
      return lhs.depth > rhs.depth;  // favor small depth
    }
  }
  inline static bool LossGuide(ExpandEntry lhs, ExpandEntry rhs) {
    if (lhs.sol.loss_chg == rhs.sol.loss_chg) {
      return lhs.timestamp > rhs.timestamp;  // favor small timestamp
    } else {
      return lhs.sol.loss_chg < rhs.sol.loss_chg;  // favor large loss_chg
    }
  }
  inline ExpandEntry newEntry(int nid, int depth, SplitEntry sol, TStats left_sum, unsigned tstmp){
    return ExpandEntry(nid, depth, sol, left_sum, tstmp);
  }
    


  //
  // reset the qexpand according to the tree growth policy
  //
  void InitQExpand(){
        if (param_.grow_policy == TrainParam::kLossGuide) {
          this->qexpand_.reset(new ExpandQueue(LossGuide));
        } else {
          this->qexpand_.reset(new ExpandQueue(DepthWise));
        }

        SplitEntry sol;
        TStats left;
        auto entry = ExpandEntry(0,0,sol, left,0);
        qexpand_->push(entry);
  }

  //
  // map active node to is working index offset in qexpand,
  //   can be -1, which means the node is node actively expanding
  // activate node are leaves
  //
  struct Node2Index{
    // map nid to mid, the physical plane id
    std::vector<int> index_;
    int num_leaves;

    inline void Init(int node_num){
        index_.resize(node_num);
        std::fill(index_.begin(), index_.end(), -1);
        //set for root node
        num_leaves = 1;
        index_[0] = 0;
    }

    // direct access (no modify)
    inline int operator[](int i){
        return index_[i];
    }
    
    inline bool isReady(int nid){
        return index_[nid] >= 0;
    }

    // add a new node and allocate model to it
    inline void append(int nid){
        index_[nid] = num_leaves;
        num_leaves ++;
    }
    // replace parent place for this new node
    inline void replace(int nid, int pid){
        index_[nid] = index_[pid];
    }
    // reset when the index is invalid
    inline void reset(int nid){
        index_[nid] = -1;
    }

    inline void clear(){
        std::fill(index_.begin(), index_.end(), -1);
        num_leaves = 0;
    }

  };
 


  //  ------class member helpers---------

  /*
   * Initialize PosSet
   *    mark rows deleted in the beginning
   *
   * */
  void InitPosSet(const std::vector<GradientPair> &gpair,
                       const DMatrix &fmat,
                       const RegTree &tree,
                       const int rowblksize) {
    CHECK_EQ(tree.param.num_nodes, tree.param.num_roots)
        << "TreeMaker: can only grow new tree";
    //const std::vector<unsigned> &root_index =  fmat.Info().root_index_;
    {
      // setup position
      // todo: or max_leaves
      posset_.Init(gpair.size(), std::pow(2, param_.max_depth), rowblksize);

      //
      // todo: parallelism on all entry
      //
      // mark delete for the deleted data
      const int num_block = posset_.getBlockNum();

      #pragma omp parallel for schedule(static)
      for (size_t i = 0; i < num_block; ++i) {
        //only node 0
        auto grp = posset_.getGroup(0, i);
        for (int k = 0; k < grp.size(); k++){
            const int ridx = grp.getRowId(k);
            if (gpair[ridx].GetHess() < 0.0f){ 
                grp.setDelete(k);
            }
        }

        // remove deleted rows
        auto start = posset_.getNextEntryStart(0, i);
        grp.Prune(start);

      }

      // mark subsample
      if (param_.subsample < 1.0f) {
        std::bernoulli_distribution coin_flip(param_.subsample);
        auto& rnd = common::GlobalRandom();

        #pragma omp parallel for schedule(static)
        for (size_t i = 0; i < num_block; ++i) {
          //only node 0
          auto grp = posset_.getGroup(0, i);
          for (int k = 0; k < grp.size(); k++){
              const int ridx = grp.getRowId(k);
              if (gpair[ridx].GetHess() < 0.0f) continue;
              if (!coin_flip(rnd)) grp.setDelete(k);
          }

          // remove deleted rows
          auto start = posset_.getNextEntryStart(0, i);
          grp.Prune(start);

        }

      }
    }
  }
  
  /*
   * Init the sum in the begining
   */
  void InitNodeStats(const std::vector<GradientPair> &gpair,
                           std::vector< std::vector<TStats> > *p_thread_temp,
                           TStats *p_node_stats) {
    const int nid = 0;

    std::vector< std::vector<TStats> > &thread_temp = *p_thread_temp;
    //const MetaInfo &info = fmat.Info();
    thread_temp.resize(omp_get_max_threads());
    #pragma omp parallel
    {
      const int tid = omp_get_thread_num();
      thread_temp[tid].resize(1, TStats(param_));
      thread_temp[tid][nid].Clear();
    }
    
    // get sum of valid ghpair
    const int num_block = posset_.getBlockNum();
    #pragma omp parallel for schedule(static)
    for (size_t i = 0; i < num_block; ++i) {
      //only node 0
      auto grp = posset_.getGroup(0, i);
      
      if (grp.isDummy() || grp.isDelete()) continue;

      for (int k = 0; k < grp.size(); k++){
        const int ridx = grp.getRowId(k);
        const int tid = omp_get_thread_num();
        //thread_temp[tid][nid].Add(gpair, info, ridx);
        thread_temp[tid][nid].Add(gpair[ridx]);
      }
    }

    // sum the per thread statistics together
    TStats &s = *p_node_stats;
    s.Clear();
    for (size_t tid = 0; tid < thread_temp.size(); ++tid) {
      s.Add(thread_temp[tid][nid]);
    }
  }
  
  /*! \brief training parameter of tree grower */
  TrainParam param_;
  /*! \brief queue of nodes to be expanded */
  using ExpandQueue =
        std::priority_queue<ExpandEntry, std::vector<ExpandEntry>,
                            std::function<bool(ExpandEntry, ExpandEntry)>>;
  std::unique_ptr<ExpandQueue> qexpand_;

  /*!
   * \brief map active node to is working index offset in qexpand,
   *   can be -1, which means the node is node actively expanding
   */
  Node2Index node2workindex_;
  /*!
   * \brief position of each instance in the tree
   */
  POSSetSingle posset_;

  TimeInfo tminfo;

 private:


};


}  // namespace tree
}  // namespace xgboost
#endif  // XGBOOST_TREE_UPDATER_BASEMAKER_INL_H_
