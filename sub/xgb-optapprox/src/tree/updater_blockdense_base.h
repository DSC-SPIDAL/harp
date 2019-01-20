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
#include <algorithm>
#include <string>
#include <limits>
#include <utility>
#include "./param.h"
#include "../common/sync.h"
#include "../common/io.h"
#include "../common/random.h"
#include "../common/quantile.h"
#include "../common/pos_set.h"

namespace xgboost {
namespace tree {
/*!
 * \brief base tree maker class that defines common operation
 *  needed in tree making
 */
class BlockBaseMaker: public TreeUpdater {
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
  // ------static helper functions ------
  // helper function to get to next level of the tree
  /*! \brief this is  helper function for row based data*/
  inline static int NextLevel(const SparsePage::Inst &inst, const RegTree &tree, int nid) {
    const RegTree::Node &n = tree[nid];
    bst_uint findex = n.SplitIndex();
    for (const auto& ins : inst) {
      if (findex == ins.index) {
        if (ins.fvalue < n.SplitCond()) {
          return n.LeftChild();
        } else {
          return n.RightChild();
        }
      }
    }
    return n.DefaultChild();
  }
  //  ------class member helpers---------
  /*! \brief initialize temp data structure */
  void InitData(const std::vector<GradientPair> &gpair,
                       const DMatrix &fmat,
                       const RegTree &tree) {
    CHECK_EQ(tree.param.num_nodes, tree.param.num_roots)
        << "TreeMaker: can only grow new tree";
    const std::vector<unsigned> &root_index =  fmat.Info().root_index_;
    {
      // setup position
      //position_.resize(gpair.size());
      //if (root_index.size() == 0) {
      //  std::fill(position_.begin(), position_.end(), 0);
      //} else {
      //  for (size_t i = 0; i < position_.size(); ++i) {
      //    position_[i] = root_index[i];
      //    CHECK_LT(root_index[i], (unsigned)tree.param.num_roots)
      //        << "root index exceed setting";
      //  }
      //}
      posset_.Init(gpair.size(),omp_get_max_threads());

      // mark delete for the deleted datas
      //for (size_t i = 0; i < position_.size(); ++i) {
      //  if (gpair[i].GetHess() < 0.0f) position_[i] = ~position_[i];
      //}
      for (size_t i = 0; i < posset_.getEntrySize(); ++i) {
        if (gpair[i].GetHess() < 0.0f) 
            posset_.getEntry(i).setDelete();
      }

      // mark subsample
      if (param_.subsample < 1.0f) {
        std::bernoulli_distribution coin_flip(param_.subsample);
        auto& rnd = common::GlobalRandom();
        //for (size_t i = 0; i < position_.size(); ++i) {
        //  if (gpair[i].GetHess() < 0.0f) continue;
        //  if (!coin_flip(rnd)) position_[i] = ~position_[i];
        //}
        for (size_t i = 0; i < posset_.getEntrySize(); ++i) {
          if (gpair[i].GetHess() < 0.0f) continue;
          if (!coin_flip(rnd)) posset_.getEntry(i).setDelete();
        }
        
      }
    }
    {
      // expand query
      qexpand_.reserve(256); qexpand_.clear();
      for (int i = 0; i < tree.param.num_roots; ++i) {
        qexpand_.push_back(i);
      }
      this->UpdateNode2WorkIndex(tree);
    }
  }
  /*! \brief update queue expand add in new leaves */
  void UpdateQueueExpand(const RegTree &tree) {
    std::vector<int> newnodes;
    for (int nid : qexpand_) {
      //if (!tree[nid].IsLeaf()) {
      if (!tree[nid].IsLeaf() && !tree[nid].IsDeleted() && !tree[nid].IsDummy()) {
        newnodes.push_back(tree[nid].LeftChild());
        newnodes.push_back(tree[nid].RightChild());
      }
    }
    // use new nodes for qexpand
    qexpand_ = newnodes;
    this->UpdateNode2WorkIndex(tree);
  }
  // return decoded position
  //inline int DecodePosition(bst_uint ridx) const {
  //  const int pid = position_[ridx];
  //  return pid < 0 ? ~pid : pid;
  //}
  //// encode the encoded position value for ridx
  //inline void SetEncodePosition(bst_uint ridx, int nid) {
  //  if (position_[ridx] < 0) {
  //    position_[ridx] = ~nid;
  //  } else {
  //    position_[ridx] = nid;
  //  }
  //}
  /*!
   * \brief helper function to set the non-leaf positions to default direction.
   *  This function can be applied multiple times and will get the same result.
   * \param p_fmat feature matrix needed for tree construction
   * \param tree the regression tree structure
   */
  void SetDefaultPostion(DMatrix *p_fmat,
                                const RegTree &tree) {
    // set default direct nodes to default
    // for leaf nodes that are not fresh, mark then to ~nid,
    // so that they are ignored in future statistics collection
    const auto ndata = static_cast<bst_omp_uint>(p_fmat->Info().num_row_);

    //#pragma omp parallel for schedule(static)
    //for (bst_omp_uint ridx = 0; ridx < ndata; ++ridx) {
    //  const int nid = this->DecodePosition(ridx);
    //  if (tree[nid].IsLeaf()) {
    //    // mark finish when it is not a fresh leaf
    //    if (tree[nid].RightChild() == -1) {
    //      position_[ridx] = ~nid;
    //    }
    //  } else {
    //    // push to default branch
    //    if (tree[nid].DefaultLeft()) {
    //      this->SetEncodePosition(ridx, tree[nid].LeftChild());
    //    } else {
    //      this->SetEncodePosition(ridx, tree[nid].RightChild());
    //    }
    //  }
    //}
    //
    //for (int i = 0; i < posset_.getGroupCnt(); i++){
    //    #pragma omp parallel for schedule(static)
    //    for (int j = 0; j < posset_[i].size(); j++){
    //      //check delete first

    //      if (posset_[i].isDelete(j)) continue;


    //      //const int ridx = posset_[i].getRowId(j);
    //      const int nid = posset_[i].getEncodePosition(j);
    //      if (tree[nid].IsLeaf()) {
    //        // mark finish when it is not a fresh leaf
    //        if (tree[nid].RightChild() == -1) {
    //          //position_[ridx] = ~nid;
    //          posset_[i].setDelete(j);
    //        }
    //      } else {
    //        // push to default branch
    //        if (tree[nid].DefaultLeft()) {
    //          //this->SetEncodePosition(ridx, tree[nid].LeftChild());
    //          posset_[i].setLeftPosition(j, tree[nid].LeftChild());
    //        } else {
    //          //this->SetEncodePosition(ridx, tree[nid].RightChild());
    //          posset_[i].setRightPosition(j, tree[nid].RightChild());
    //        }
    //      }
    //    }
    //}
    #pragma omp parallel for schedule(static)
    for (size_t i = 0; i < posset_.getEntrySize(); ++i) {
        auto &entry = posset_.getEntry(i);

        if(entry.isDelete()) continue;
        const int ridx = entry.getRowId();
        const int nid = entry.getEncodePosition();

        if (tree[nid].IsLeaf()) {
          // mark finish when it is not a fresh leaf
          if (tree[nid].RightChild() == -1) {
            //position_[ridx] = ~nid;
            entry.setDelete();
          }
        } else {
          // push to default branch
          if (tree[nid].DefaultLeft()) {
            //this->SetEncodePosition(ridx, tree[nid].LeftChild());
            entry.setEncodePosition(tree[nid].LeftChild(), true);
          } else {
            //this->SetEncodePosition(ridx, tree[nid].RightChild());
            entry.setEncodePosition(tree[nid].RightChild(), false);
          }
        }
    }
 
  }
  /*!
   * \brief this is helper function uses column based data structure,
   * \param nodes the set of nodes that contains the split to be used
   * \param tree the regression tree structure
   * \param out_split_set The split index set
   */
  void GetSplitSet(const std::vector<int> &nodes,
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
  /*! \brief helper function to get statistics from a tree */
  template<typename TStats>
  void GetNodeStats(const std::vector<GradientPair> &gpair,
                           const DMatrix &fmat,
                           const RegTree &tree,
                           std::vector< std::vector<TStats> > *p_thread_temp,
                           std::vector<TStats> *p_node_stats) {
    std::vector< std::vector<TStats> > &thread_temp = *p_thread_temp;
    const MetaInfo &info = fmat.Info();
    thread_temp.resize(omp_get_max_threads());
    p_node_stats->resize(tree.param.num_nodes);
    #pragma omp parallel
    {
      const int tid = omp_get_thread_num();
      thread_temp[tid].resize(tree.param.num_nodes, TStats(param_));
      for (unsigned int nid : qexpand_) {
        thread_temp[tid][nid].Clear();
      }
    }
    
    // setup position
    //const auto ndata = static_cast<bst_omp_uint>(fmat.Info().num_row_);
    //#pragma omp parallel for schedule(static)
    //for (bst_omp_uint ridx = 0; ridx < ndata; ++ridx) {
    //  const int nid = position_[ridx];
    //  const int tid = omp_get_thread_num();
    //  if (nid >= 0) {
    //    thread_temp[tid][nid].Add(gpair, info, ridx);
    //  }
    //}
    //for (int i = 0; i < posset_.getGroupCnt(); i++){
    //    #pragma omp parallel for schedule(static)
    //    for (int j = 0; j < posset_[i].size(); j++){
    //      if (posset_[i].isDelete(j)) continue;  
    //      const int ridx = posset_[i].getRowId(j);
    //      const int nid = posset_[i].getEncodePosition(j);
    //      const int tid = omp_get_thread_num();
    //      thread_temp[tid][nid].Add(gpair, info, ridx);
    //    }
    //}

    #pragma omp parallel for schedule(static)
    for (size_t i = 0; i < posset_.getEntrySize(); ++i) {
        auto &entry = posset_.getEntry(i);
        if(entry.isDelete()) continue;
        const int ridx = entry.getRowId();
        const int nid = entry.getEncodePosition();
        const int tid = omp_get_thread_num();
        thread_temp[tid][nid].Add(gpair, info, ridx);
    }

    // sum the per thread statistics together
    for (int nid : qexpand_) {
      TStats &s = (*p_node_stats)[nid];
      s.Clear();
      for (size_t tid = 0; tid < thread_temp.size(); ++tid) {
        s.Add(thread_temp[tid][nid]);
      }
    }
  }
  /*! \brief training parameter of tree grower */
  TrainParam param_;
  /*! \brief queue of nodes to be expanded */
  std::vector<int> qexpand_;
  /*!
   * \brief map active node to is working index offset in qexpand,
   *   can be -1, which means the node is node actively expanding
   */
  std::vector<int> node2workindex_;
  /*!
   * \brief position of each instance in the tree
   *   can be negative, which means this position is no longer expanding
   *   see also Decode/EncodePosition
   */
  //std::vector<int> position_;
  POSSet posset_;

  TimeInfo tminfo;

 private:
  inline void UpdateNode2WorkIndex(const RegTree &tree) {
    // update the node2workindex
    int oldsize = node2workindex_.size();
    node2workindex_.resize(tree.param.num_nodes);

    // clean the new indexes
#ifndef USE_HALFTRICK
    // clear all
    std::fill(node2workindex_.begin(), node2workindex_.end(), -1);
#else
    if (oldsize < node2workindex_.size()){
        //clear only the new ones, reserve the old ones
        std::fill(node2workindex_.begin() + oldsize, node2workindex_.end(), -1);
    }
    else{
        std::fill(node2workindex_.begin(), node2workindex_.end(), -1);
    }
#endif

    for (int i = 0; i < qexpand_.size(); ++i) {
#ifndef USE_HALFTRICK
      //nohalftrick always use compact storage mode
      node2workindex_[qexpand_[i]] = static_cast<int>(i);
#else
      //halftrick will have a full binary tree
      #ifdef ALLOCATE_ALLNODES
      node2workindex_[qexpand_[i]] = qexpand_[i];
      #else
      int nid = qexpand_[i];
      if ((nid&1) == 0){
        //write right nodes only
        //interleave mode, (3,5 | 4,6) -> (7,9,11,13| 8,10,12,14)
        int num_leaves = (tree.param.num_nodes +1 ) / 2;
        node2workindex_[qexpand_[i]] = qexpand_[i]/2 + (qexpand_[i]%2)*num_leaves/2;
      }

      #endif  
#endif
    }
  }
};
}  // namespace tree
}  // namespace xgboost
#endif  // XGBOOST_TREE_UPDATER_BASEMAKER_INL_H_
