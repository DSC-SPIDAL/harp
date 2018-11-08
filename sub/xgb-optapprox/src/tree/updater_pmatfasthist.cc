/*!
 * Copyright 2014 by Contributors
 * \file updater_HistMakerCompactFastHist.cc
 * \brief use histogram counting to construct a tree
 * \author Tianqi Chen
 */
#include <xgboost/base.h>
#include <xgboost/tree_updater.h>
#include <vector>
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


//#define _INIT_PER_TREE_ 1
namespace xgboost {
namespace tree {

using xgboost::common::HistCutMatrix;

DMLC_REGISTRY_FILE_TAG(updater_pmatfasthist);

template<typename TStats>
class HistMakerCompactFastHist: public BaseMaker {
 public:
  void Update(HostDeviceVector<GradientPair> *gpair,
              DMatrix *p_fmat,
              const std::vector<RegTree*> &trees) override {
    TStats::CheckInfo(p_fmat->Info());
    // rescale learning rate according to size of trees
    float lr = param_.learning_rate;
    param_.learning_rate = lr / trees.size();
    // build tree
    for (auto tree : trees) {
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
    size_t featnum;

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
                      &data[0] + rptr[fid] + nid*(fsetSize),
                      rptr[fid+1] - rptr[fid]);
    }


  };
  // thread workspace
  struct ThreadWSpace {
    /*! \brief actual unit pointer */
    std::vector<unsigned> rptr;
    /*! \brief cut field */
    std::vector<bst_float> cut;
    // per thread histset
    std::vector<HistSet> hset;
    // initialize the hist set
    inline void Init(const TrainParam &param, int nthread, int nodesize) {
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
        hset[tid].data.resize(cut.size() * nodesize, TStats(param));

        LOG(CONSOLE)<< "Init hset: rptrSize:" << rptr.size() <<
            ",cutSize:" <<  cut.size() <<",nodesize:" << nodesize <<
            ",fsetSize" << rptr.back();
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

    // aggregate all statistics to hset[0]
    inline void Aggregate() {
      bst_omp_uint nsize = static_cast<bst_omp_uint>(cut.size());
      #pragma omp parallel for schedule(static)
      for (bst_omp_uint i = 0; i < nsize; ++i) {
        for (size_t tid = 1; tid < hset.size(); ++tid) {
          hset[0].data[i].Add(hset[tid].data[i]);
        }
      }
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
  // workspace of thread
  ThreadWSpace wspace_;
  // reducer for histogram
  rabit::Reducer<TStats, TStats::Reduce> histred_;
  // set of working features
  std::vector<bst_uint> fwork_set_;
  std::vector<bst_uint> fsplit_set_;

  int treeid_{0};

  // update function implementation
  virtual void Update(const std::vector<GradientPair> &gpair,
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
    printVec("ResetPos::fwork_set=", fwork_set_);
    // reset and propose candidate split
    this->ResetPosAndPropose(gpair, p_fmat, fwork_set_, *p_tree);
    printtree(p_tree, "ResetPosAndPropose");


    for (int depth = 0; depth < param_.max_depth; ++depth) {

      // create histogram
      this->CreateHist(gpair, p_fmat, fwork_set_, *p_tree);

      //printVec("CreateHist::fwork_set=", fwork_set_);
      //printtree(p_tree, "After CreateHist");

#ifdef USE_DEBUG_SAVE
      this->wspace_.saveGHSum(treeid_, depth, this->qexpand_.size());
#endif

      // find split based on histogram statistics
      this->FindSplit(depth, gpair, p_fmat, fwork_set_, p_tree);

      printtree(p_tree, "FindSplit");

      // reset position after split
      this->ResetPositionAfterSplit(p_fmat, *p_tree);
      //printtree(p_tree, "ResetPositionAfterSPlit");


      this->UpdateQueueExpand(*p_tree);
      //printtree(p_tree, "UpdateQueueExpand");
      
      std::ostringstream stringStream;
      stringStream << "fsplit_set size:" << this->fsplit_set_.size();
      printmsg(stringStream.str());

      // if nothing left to be expand, break
      if (qexpand_.size() == 0) break;
    }


    for (size_t i = 0; i < qexpand_.size(); ++i) {
      const int nid = qexpand_[i];
      (*p_tree)[nid].SetLeaf(p_tree->Stat(nid).base_weight * param_.learning_rate);
    }

    /* optApprox */
    printtree(p_tree);

    //reset the binid to fvalue in this tree
    ResetTree(*p_tree);
    printtree(p_tree);
    
    treeid_ ++;
  }
  virtual void ResetTree(RegTree& tree){ }

  // this function does two jobs
  // (1) reset the position in array position, to be the latest leaf id
  // (2) propose a set of candidate cuts and set wspace.rptr wspace.cut correctly
  virtual void ResetPosAndPropose(const std::vector<GradientPair> &gpair,
                                  DMatrix *p_fmat,
                                  const std::vector <bst_uint> &fset,
                                  const RegTree &tree) = 0;
  // initialize the current working set of features in this round
  virtual void InitWorkSet(DMatrix *p_fmat,
                           const RegTree &tree,
                           std::vector<bst_uint> *p_fset) {
    p_fset->resize(tree.param.num_feature);
    for (size_t i = 0; i < p_fset->size(); ++i) {
      (*p_fset)[i] = static_cast<unsigned>(i);
    }
  }
  // reset position after split, this is not a must, depending on implementation
  virtual void ResetPositionAfterSplit(DMatrix *p_fmat,
                                       const RegTree &tree) {
  }
  virtual void CreateHist(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const std::vector <bst_uint> &fset,
                          const RegTree &tree)  = 0;

 private:
  inline void EnumerateSplit(const HistUnit &hist,
                             const TStats &node_sum,
                             bst_uint fid,
                             SplitEntry *best,
                             TStats *left_sum) {
    if (hist.size == 0) return;

    double root_gain = node_sum.CalcGain(param_);
    TStats s(param_), c(param_);
    for (bst_uint i = 0; i < hist.size; ++i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          //if (best->Update(static_cast<bst_float>(loss_chg), fid, hist.cut[i], false)) {
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i, false)) {
            *left_sum = s;
          }
        }
      }
    }
    s.Clear();
    for (bst_uint i = hist.size - 1; i != 0; --i) {
      s.Add(hist.data[i]);
      if (s.sum_hess >= param_.min_child_weight) {
        c.SetSubstract(node_sum, s);
        if (c.sum_hess >= param_.min_child_weight) {
          double loss_chg = s.CalcGain(param_) + c.CalcGain(param_) - root_gain;
          //if (best->Update(static_cast<bst_float>(loss_chg), fid, hist.cut[i-1], true)) {
          if (best->Update(static_cast<bst_float>(loss_chg), fid, i-1, true)) {
            *left_sum = c;
          }
        }
      }
    }

    //debug
    printSplit(*best);

   
  }
  inline void FindSplit(int depth,
                        const std::vector<GradientPair> &gpair,
                        DMatrix *p_fmat,
                        const std::vector <bst_uint> &fset,
                        RegTree *p_tree) {
    
    const size_t num_feature = fset.size();
    //printInt("FindSplit::num_feature = ", num_feature);
    printVec("FindSplit::fset=", fset);
    // get the best split condition for each node
    std::vector<SplitEntry> sol(qexpand_.size());
    std::vector<TStats> left_sum(qexpand_.size());
    auto nexpand = static_cast<bst_omp_uint>(qexpand_.size());
    #pragma omp parallel for schedule(dynamic, 1)
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      CHECK_EQ(node2workindex_[nid], static_cast<int>(wid));
      SplitEntry &best = sol[wid];
      //TStats &node_sum = wspace_.hset[0][num_feature + wid * (num_feature + 1)].data[0];
      TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, wid).data[0];
      for (size_t i = 0; i < fset.size(); ++i) {
        //int fid = this->fset[i];
        //int offset = this->feat2workindex_[fid];
        //EnumerateSplit(this->wspace_.hset[0][i + wid * (num_feature+1)],
        EnumerateSplit(this->wspace_.hset[0].GetHistUnit(i, wid),
                       node_sum, fset[i], &best, &left_sum[wid]);
        //EnumerateSplit(this->wspace_.hset[0][offset + wid * (num_feature+1)],
        //               node_sum, fid, &best, &left_sum[wid]);
      }
    }
    // get the best result, we can synchronize the solution
    for (bst_omp_uint wid = 0; wid < nexpand; ++wid) {
      const int nid = qexpand_[wid];
      const SplitEntry &best = sol[wid];
      //const TStats &node_sum = wspace_.hset[0][num_feature + wid * (num_feature + 1)].data[0];
      const TStats &node_sum = wspace_.hset[0].GetHistUnit(num_feature, wid).data[0];
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
      }
    }
  }

  inline void SetStats(RegTree *p_tree, int nid, const TStats &node_sum) {
    p_tree->Stat(nid).base_weight = static_cast<bst_float>(node_sum.CalcWeight(param_));
    p_tree->Stat(nid).sum_hess = static_cast<bst_float>(node_sum.sum_hess);
    node_sum.SetLeafVec(param_, p_tree->Leafvec(nid));
  }
};

template<typename TStats>
class CQHistMakerCompactFastHist: public HistMakerCompactFastHist<TStats> {
 public:
  CQHistMakerCompactFastHist()  = default;

 protected:
  struct HistEntry {
    typename HistMakerCompactFastHist<TStats>::HistUnit hist;
    unsigned istart;

    /* OptApprox:: init bindid in pmat */
    inline unsigned GetBinId(bst_float fv) {
      unsigned start = 0;
      while (start < hist.size && !(fv < hist.cut[start])) ++start;
      //CHECK_NE(start, hist.size);
      if(start == hist.size) start--;
      return start;
    }

#ifdef USE_BINID
    inline void AddWithIndex(unsigned binid,
                    GradientPair gstats) {
        //hist.data[binid].Add(gstats);
        hist.data[binid].Add(gstats);
    }
    inline void AddWithIndex(unsigned binid,
                    const std::vector<GradientPair> &gpair,
                    const MetaInfo &info,
                    const bst_uint ridx) {
      CHECK_NE(binid, hist.size);
      hist.data[binid].Add(gpair, info, ridx);
    }
#endif


    //inline void AddWithIndex(const float* binidptr,
    //                GradientPair gstats) {
    //    //hist.data[binid].Add(gstats);
    //  float* ptr = binidptr;
    //    unsigned int binid = *(reinterpret_cast<unsigned int*>(ptr));
    //    hist.data[binid].Add(gstats);
    //}
    //inline void AddWithIndex(const float* binidptr,
    //                const std::vector<GradientPair> &gpair,
    //                const MetaInfo &info,
    //                const bst_uint ridx) {
    //  float* ptr = binidptr;
    //  unsigned int binid = *(reinterpret_cast<unsigned int*>(ptr));
    //  CHECK_NE(binid, hist.size);
    //  hist.data[binid].Add(gpair, info, ridx);
    //}

#ifdef USE_BINIDUNION
    inline void AddWithIndex(float ptr,
                    GradientPair gstats) {
        //hist.data[binid].Add(gstats);
        unsigned int binid = static_cast<unsigned int>(ptr);
        hist.data[binid].Add(gstats);
    }
    inline void AddWithIndex(float ptr,
                    const std::vector<GradientPair> &gpair,
                    const MetaInfo &info,
                    const bst_uint ridx) {
      unsigned int binid = static_cast<unsigned int>(ptr);
      CHECK_NE(binid, hist.size);
      hist.data[binid].Add(gpair, info, ridx);
    }
#endif


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
  // sketch type used for this
  using WXQSketch = common::WXQuantileSketch<bst_float, bst_float>;
  // initialize the work set of tree
  void InitWorkSet(DMatrix *p_fmat,
                   const RegTree &tree,
                   std::vector<bst_uint> *p_fset) override {
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
  // code to create histogram
  void CreateHist(const std::vector<GradientPair> &gpair,
                  DMatrix *p_fmat,
                  const std::vector<bst_uint> &fset,
                  const RegTree &tree) override {
    const MetaInfo &info = p_fmat->Info();
    // fill in reverse map
    feat2workindex_.resize(tree.param.num_feature);
    std::fill(feat2workindex_.begin(), feat2workindex_.end(), -1);
    for (size_t i = 0; i < fset.size(); ++i) {
      feat2workindex_[fset[i]] = static_cast<int>(i);
    }
    // start to work
    this->wspace_.Init(this->param_, 1, this->qexpand_.size());
    // if it is C++11, use lazy evaluation for Allreduce,
    // to gain speedup in recovery
#if __cplusplus >= 201103L
    auto lazy_get_hist = [&]()
#endif
    {
      thread_hist_.resize(omp_get_max_threads());
      // start accumulating statistics
      for (const auto &batch : p_fmat->GetSortedColumnBatches()) {
        // start enumeration
        const auto nsize = static_cast<bst_omp_uint>(fset.size());
        #pragma omp parallel for schedule(dynamic, 1)
        for (bst_omp_uint i = 0; i < nsize; ++i) {
          int fid = fset[i];
          int offset = feat2workindex_[fid];
          if (offset >= 0) {
            this->UpdateHistCol(gpair, batch[fid], info, tree,
                                fset, offset,
                                &thread_hist_[omp_get_thread_num()]);
          }
        }
      }
      // update node statistics.
      this->GetNodeStats(gpair, *p_fmat, tree,
                         &thread_stats_, &node_stats_);
      for (size_t i = 0; i < this->qexpand_.size(); ++i) {
        const int nid = this->qexpand_[i];
        const int wid = this->node2workindex_[nid];
        //this->wspace_.hset[0][fset.size() + wid * (fset.size()+1)]
        this->wspace_.hset[0].GetHistUnit(fset.size(), wid)
            .data[0] = node_stats_[nid];
      }
    };
    // sync the histogram
    // if it is C++11, use lazy evaluation for Allreduce
#if __cplusplus >= 201103L
    this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset[0].data),
                            this->wspace_.hset[0].data.size(), lazy_get_hist);
#else
    this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset[0].data),
                            this->wspace_.hset[0].data.size());
#endif
  }
  void ResetPositionAfterSplit(DMatrix *p_fmat,
                               const RegTree &tree) override {
    this->GetSplitSet(this->qexpand_, tree, &this->fsplit_set_);
  }

  void ResetPosAndPropose(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const std::vector<bst_uint> &fset,
                          const RegTree &tree) override {
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
        cut_.Init(p_fmat,256); 
    //}

    //if (this->qexpand_.size() == 1) {

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
              if (a.size != 0) {
                bst_float cpt = a.cut[a.size - 1];
                // this must be bigger than last value in a scale
                bst_float last = cpt + fabs(cpt) + kRtEps;
                this->wspace_.cut.push_back(last);
              }
              this->wspace_.rptr.push_back(static_cast<unsigned>(this->wspace_.cut.size()));
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

  inline void InitHistCol(const SparsePage::Inst &col,
                            const RegTree &tree,
                            const std::vector<bst_uint> &fset,
                            bst_uint fid_offset,
                            std::vector<HistEntry> *p_temp) {

    if (col.size() == 0) return;
    // initialize sbuilder for use
    std::vector<HistEntry> &hbuilder = *p_temp;
    hbuilder.resize(tree.param.num_nodes);

    LOG(CONSOLE) << "InitHistCol: num_nodes=" << tree.param.num_nodes <<
            ", qexpand.size=" << this->qexpand_.size() ;

    for (size_t i = 0; i < this->qexpand_.size(); ++i) {
      const unsigned nid = this->qexpand_[i];
      const unsigned wid = this->node2workindex_[nid];
      hbuilder[nid].istart = 0;
      //hbuilder[nid].hist = this->wspace_.hset[0][fid_offset + wid * (fset.size()+1)];
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset,wid);
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
  inline void UpdateHistColWithIndex(const std::vector<GradientPair> &gpair,
                            const DMatrixCompact::Inst &col,
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
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, wid);
    }
    if (TStats::kSimpleStats != 0 && this->param_.cache_opt != 0) {
      constexpr bst_uint kBuffer = 32;
      bst_uint align_length = col.size() / kBuffer * kBuffer;
      int buf_position[kBuffer];
      GradientPair buf_gpair[kBuffer];
      for (bst_uint j = 0; j < align_length; j += kBuffer) {
        for (bst_uint i = 0; i < kBuffer; ++i) {
          bst_uint ridx = col[j + i]._index();
          buf_position[i] = this->position_[ridx];
          buf_gpair[i] = gpair[ridx];
        }
        for (bst_uint i = 0; i < kBuffer; ++i) {
          const int nid = buf_position[i];
          if (nid >= 0) {
            //hbuilder[nid].Add(col[j + i].fvalue, buf_gpair[i]);
//#ifdef USE_BINID
            hbuilder[nid].AddWithIndex(col[j + i]._binid(), buf_gpair[i]);
//#endif

            
#ifdef USE_BINIDUNION
            //hbuilder[nid].AddWithIndex(*(reinterpret_cast<bst_uint*>(&col[j + i].fvalue)), buf_gpair[i]);
            //hbuilder[nid].AddWithIndex(&(col[j + i].fvalue), buf_gpair[i]);
            //hbuilder[nid].AddWithIndex(col[j + i].fvalue, buf_gpair[i]);
#endif
          }
        }
      }
      for (bst_uint j = align_length; j < col.size(); ++j) {
        const bst_uint ridx = col[j]._index();
        const int nid = this->position_[ridx];
        if (nid >= 0) {
          //hbuilder[nid].Add(col[j].fvalue, gpair[ridx]);
 
#ifdef USE_BINID
          hbuilder[nid].AddWithIndex(col[j]._binid(), gpair[ridx]);
#endif
          
#ifdef USE_BINIDUNION
          //hbuilder[nid].AddWithIndex(*(reinterpret_cast<bst_uint*>(&col[j].fvalue)), gpair[ridx]);
          //hbuilder[nid].AddWithIndex(&(col[j].fvalue), gpair[ridx]);
          //hbuilder[nid].AddWithIndex(col[j].fvalue, gpair[ridx]);
#endif
        }
      }
    } else {
      for (const auto& c : col) {
        const bst_uint ridx = c._index();
        const int nid = this->position_[ridx];
        if (nid >= 0) {
          //hbuilder[nid].Add(c.fvalue, gpair, info, ridx);
#ifdef USE_BINID
          hbuilder[nid].AddWithIndex(c._binid(), gpair, info, ridx);
#endif
          
#ifdef USE_BINIDUNION
          //hbuilder[nid].AddWithIndex(*(reinterpret_cast<bst_uint*>(&c.fvalue)), gpair, info, ridx);
          //hbuilder[nid].AddWithIndex(&(c.fvalue), gpair, info, ridx);
          //hbuilder[nid].AddWithIndex(c.fvalue, gpair, info, ridx);
#endif
        }
      }
    }
  }
 
  /* ----------------------------------------
   * **/

  inline void UpdateHistCol(const std::vector<GradientPair> &gpair,
                            const SparsePage::Inst &col,
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
      hbuilder[nid].hist = this->wspace_.hset[0].GetHistUnit(fid_offset, wid);
    }
    if (TStats::kSimpleStats != 0 && this->param_.cache_opt != 0) {
      constexpr bst_uint kBuffer = 32;
      bst_uint align_length = col.size() / kBuffer * kBuffer;
      int buf_position[kBuffer];
      GradientPair buf_gpair[kBuffer];
      for (bst_uint j = 0; j < align_length; j += kBuffer) {
        for (bst_uint i = 0; i < kBuffer; ++i) {
          bst_uint ridx = col[j + i]._index();
          buf_position[i] = this->position_[ridx];
          buf_gpair[i] = gpair[ridx];
        }
        for (bst_uint i = 0; i < kBuffer; ++i) {
          const int nid = buf_position[i];
          if (nid >= 0) {
            //hbuilder[nid].Add(col[j + i].fvalue, buf_gpair[i]);
            hbuilder[nid].AddWithIndex(col[j + i]._binid(), buf_gpair[i]);
          }
        }
      }
      for (bst_uint j = align_length; j < col.size(); ++j) {
        const bst_uint ridx = col[j]._index();
        const int nid = this->position_[ridx];
        if (nid >= 0) {
          //hbuilder[nid].Add(col[j].fvalue, gpair[ridx]);
          hbuilder[nid].AddWithIndex(col[j]._binid(), gpair[ridx]);
        }
      }
    } else {
      for (const auto& c : col) {
        const bst_uint ridx = c._index();
        const int nid = this->position_[ridx];
        if (nid >= 0) {
          //hbuilder[nid].Add(c.fvalue, gpair, info, ridx);
          hbuilder[nid].AddWithIndex(c._binid(), gpair, info, ridx);
        }
      }
    }
  }
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
};

// global proposal
template<typename TStats>
class GlobalProposalHistMakerCompactFastHist: public CQHistMakerCompactFastHist<TStats> {

 public:
  GlobalProposalHistMakerCompactFastHist(){
      this->isInitializedHistIndex = false;
#ifdef USE_COMPACT
      p_hmat = new DMatrixCompact();
#endif
  }

  ~GlobalProposalHistMakerCompactFastHist(){
#ifdef USE_COMPACT
    delete p_hmat;
#endif
  }

 protected:
  void ResetPosAndPropose(const std::vector<GradientPair> &gpair,
                          DMatrix *p_fmat,
                          const std::vector<bst_uint> &fset,
                          const RegTree &tree) override {

      if(!this->isInitializedHistIndex){
        CHECK_EQ(this->qexpand_.size(), 1U);
        CQHistMakerCompactFastHist<TStats>::ResetPosAndPropose(gpair, p_fmat, fset, tree);
        LOG(CONSOLE) << "ResetPosAndPropose call in globalproposal";

        //this->wspace_.Init(this->param_, 1, 1);
        //CQHistMakerCompactFastHist<TStats>::InitHistIndex(p_fmat, fset, tree);
        //this->isInitializedHistIndex = true;

        //#ifdef   USE_COMPACT
        //p_hmat->Init(*p_fmat->GetSortedColumnBatches().begin());
        //printdmat(*p_hmat);
        //#endif  
        ////DEBUG
        //printdmat(*p_fmat->GetSortedColumnBatches().begin());
        //printcut(this->cut_);
      }
  }

  //dup func
  inline void CorrectNonDefaultPositionByBatch2(
      DMatrixCompact &batch, const std::vector<bst_uint> &sorted_split_set,
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
          const bst_uint ridx = col[j]._index();
          const bst_uint binid = col[j]._binid();


          const int nid = this->DecodePosition(ridx);
          CHECK(tree[nid].IsLeaf());
          int pid = tree[nid].Parent();

          // go back to parent, correct those who are not default
          if (!tree[nid].IsRoot() && tree[pid].SplitIndex() == fid) {
            if (binid < tree[pid].SplitCond()) {
              this->SetEncodePosition(ridx, tree[pid].LeftChild());
            } else {
              this->SetEncodePosition(ridx, tree[pid].RightChild());
            }
          }
        }
      }
    }
  }

  //inline void CorrectNonDefaultPositionByBatchOrig(
  //    const SparsePage &batch, const std::vector<bst_uint> &sorted_split_set,
  //    const RegTree &tree) {
  //  for (size_t fid = 0; fid < batch.Size(); ++fid) {
  //    auto col = batch[fid];
  //    auto it = std::lower_bound(sorted_split_set.begin(), sorted_split_set.end(), fid);

  //    if (it != sorted_split_set.end() && *it == fid) {
  //      const auto ndata = static_cast<bst_omp_uint>(col.size());
  //      #pragma omp parallel for schedule(static)
  //      for (bst_omp_uint j = 0; j < ndata; ++j) {
  //        const bst_uint ridx = col[j].index;
  //        const bst_float fvalue = col[j].fvalue;
  //        const int nid = this->DecodePosition(ridx);
  //        CHECK(tree[nid].IsLeaf());
  //        int pid = tree[nid].Parent();

  //        // go back to parent, correct those who are not default
  //        if (!tree[nid].IsRoot() && tree[pid].SplitIndex() == fid) {
  //          if (fvalue < tree[pid].SplitCond()) {
  //            this->SetEncodePosition(ridx, tree[pid].LeftChild());
  //          } else {
  //            this->SetEncodePosition(ridx, tree[pid].RightChild());
  //          }
  //        }
  //      }
  //    }
  //  }
  //}

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
          int tnid;
          // if a node is marked as deleted by the pruner, traverse upward to locate
          // a non-deleted leaf.
          if ((*p_last_tree_)[nid].IsDeleted()) {
            while ((*p_last_tree_)[nid].IsDeleted()) {
              tnid = (*p_last_tree_)[nid].Parent();
            }
            CHECK((*p_last_tree_)[tnid].IsLeaf());
          }
          else{
              tnid = nid;
          }
          leaf_values[nid] = (*p_last_tree_)[tnid].LeafValue();
      }

      const auto nrows = static_cast<bst_omp_uint>(p_fmat->Info().num_row_);
      for(int ridx=0; ridx < nrows; ridx++){
        const int nid = this->position_[ridx];
        //update   
        out_preds[ridx] += leaf_values[nid];
      }

      LOG(CONSOLE) << "UpdatePredictionCache: nodes size=" << 
          nodes.size() << ",rowscnt=" << nrows;

      return true;
    }
  }


  // code to create histogram
  void CreateHist(const std::vector<GradientPair> &gpair,
                  DMatrix *p_fmat,
                  const std::vector<bst_uint> &fset,
                  const RegTree &tree) override {
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
    this->wspace_.Init(this->param_, 1, this->qexpand_.size());
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
        CQHistMakerCompactFastHist<TStats>::InitHistIndex(p_fmat, fset, tree);
        this->isInitializedHistIndex = true;

#ifdef   USE_COMPACT
        p_hmat->Init(*p_fmat->GetSortedColumnBatches().begin());
        printdmat(*p_hmat);
#endif  
        //DEBUG
        printdmat(*p_fmat->GetSortedColumnBatches().begin());
        printcut(this->cut_);
      }

      // start accumulating statistics
      //for (const auto &batch : p_fmat->GetSortedColumnBatches()) 
      //only one page
      {
        // TWOPASS: use the real set + split set in the column iteration.
        this->CorrectNonDefaultPositionByBatch2(*p_hmat, this->fsplit_set_, tree);
        //auto batch = *p_fmat->GetSortedColumnBatches().begin();
        //this->CorrectNonDefaultPositionByBatchOrig(batch, this->fsplit_set_, tree);
        //this->CorrectNonDefaultPositionByBatch(batch, this->fsplit_set_, tree);

        // start enumeration
        const auto nsize = static_cast<bst_omp_uint>(this->work_set_.size());
        #pragma omp parallel for schedule(dynamic, 1)
        for (bst_omp_uint i = 0; i < nsize; ++i) {
          int fid = this->work_set_[i];
          int offset = this->feat2workindex_[fid];
          if (offset >= 0) {
#ifdef USE_COMPACT
             this->UpdateHistColWithIndex(gpair, (*p_hmat)[fid], info, tree,
                                fset, offset,
                                &this->thread_hist_[omp_get_thread_num()]);
#else

            this->UpdateHistCol(gpair, batch[fid], info, tree,
                                fset, offset,
                                &this->thread_hist_[omp_get_thread_num()]);
#endif
          }
        }
      }

      // update node statistics.
      this->GetNodeStats(gpair, *p_fmat, tree,
                         &(this->thread_stats_), &(this->node_stats_));
      for (size_t i = 0; i < this->qexpand_.size(); ++i) {
        const int nid = this->qexpand_[i];
        const int wid = this->node2workindex_[nid];
        //this->wspace_.hset[0][fset.size() + wid * (fset.size()+1)]
        //    .data[0] = this->node_stats_[nid];
        this->wspace_.hset[0].GetHistUnit(fset.size(),wid)
            .data[0] = this->node_stats_[nid];
      }
    }
    this->histred_.Allreduce(dmlc::BeginPtr(this->wspace_.hset[0].data),
                            this->wspace_.hset[0].data.size());

    //save the last tree point
    p_last_tree_ = &tree;
  }

  virtual void ResetTree(RegTree& tree) override{

    const auto nodes = tree.GetNodes();
    for(int i=0; i < nodes.size(); i++){
        if (tree[i].IsLeaf()){
            continue;
        }

        unsigned sindex = tree[i].SplitIndex();
        auto splitCond = tree[i].SplitCond();
        auto defaultLeft = tree[i].DefaultLeft();

        //turn splitCond from binid to fvalue
        //splitCond is binid now
        float fvalue = this->wspace_.cut[this->wspace_.rptr[sindex] + static_cast<int>(splitCond)];
        tree[i].SetSplit(sindex, fvalue, defaultLeft);
    }

  }



  // cached unit pointer
  std::vector<unsigned> cached_rptr_;
  // cached cut value.
  std::vector<bst_float> cached_cut_;
 // hist mat compact
#ifdef USE_COMPACT
  DMatrixCompact* p_hmat;
#endif
  //for predict cache
  RegTree* p_last_tree_;


};


XGBOOST_REGISTER_TREE_UPDATER(HistMakerCompactFastHist, "grow_pmatfasthist")
.describe("Tree constructor that uses approximate global of histogram construction.")
.set_body([]() {
    return new GlobalProposalHistMakerCompactFastHist<GradStats>();
  });
}  // namespace tree
}  // namespace xgboost
