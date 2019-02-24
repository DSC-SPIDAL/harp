/*!
 * Copyright 2014 by Contributors
 * \file learner.cc
 * \brief Implementation of learning algorithm.
 * \author Tianqi Chen
 */
#include <dmlc/io.h>
#include <dmlc/timer.h>
#include <xgboost/learner.h>
#include <xgboost/logging.h>
#include <algorithm>
#include <iomanip>
#include <limits>
#include <sstream>
#include <string>
#include <utility>
#include <vector>
#include "./common/common.h"
#include "./common/host_device_vector.h"
#include "./common/io.h"
#include "./common/random.h"
#include "./common/debug.h"
#include "common/timer.h"
#include <fstream>

namespace {

const char* kMaxDeltaStepDefaultValue = "0.7";

}  // anonymous namespace

namespace xgboost {


// implementation of base learner.
bool Learner::AllowLazyCheckPoint() const {
  return gbm_->AllowLazyCheckPoint();
}

std::vector<std::string> Learner::DumpModel(const FeatureMap& fmap,
                                            bool with_stats,
                                            std::string format) const {
  return gbm_->DumpModel(fmap, with_stats, format);
}

/*! \brief training parameter for regression */
struct LearnerModelParam : public dmlc::Parameter<LearnerModelParam> {
  /* \brief global bias */
  bst_float base_score;
  /* \brief number of features  */
  unsigned num_feature;
  /* \brief number of classes, if it is multi-class classification  */
  int num_class;
  /*! \brief Model contain additional properties */
  int contain_extra_attrs;
  /*! \brief Model contain eval metrics */
  int contain_eval_metrics;
  /*! \brief reserved field */
  int reserved[29];
  /*! \brief constructor */
  LearnerModelParam() {
    std::memset(this, 0, sizeof(LearnerModelParam));
    base_score = 0.5f;
  }
  // declare parameters
  DMLC_DECLARE_PARAMETER(LearnerModelParam) {
    DMLC_DECLARE_FIELD(base_score)
        .set_default(0.5f)
        .describe("Global bias of the model.");
    DMLC_DECLARE_FIELD(num_feature)
        .set_default(0)
        .describe(
            "Number of features in training data,"
            " this parameter will be automatically detected by learner.");
    DMLC_DECLARE_FIELD(num_class).set_default(0).set_lower_bound(0).describe(
        "Number of class option for multi-class classifier. "
        " By default equals 0 and corresponds to binary classifier.");
  }
};

struct LearnerTrainParam : public dmlc::Parameter<LearnerTrainParam> {
  // stored random seed
  int seed;
  // whether seed the PRNG each iteration
  bool seed_per_iteration;
  // data split mode, can be row, col, or none.
  int dsplit;
  // tree construction method
  int tree_method;
  // internal test flag
  std::string test_flag;
  // number of threads to use if OpenMP is enabled
  // if equals 0, use system default
  int nthread;
  // flag to print out detailed breakdown of runtime
  int debug_verbose;
  // flag to disable default metric
  int disable_default_eval_metric;
  // declare parameters
  DMLC_DECLARE_PARAMETER(LearnerTrainParam) {
    DMLC_DECLARE_FIELD(seed).set_default(0).describe(
        "Random number seed during training.");
    DMLC_DECLARE_FIELD(seed_per_iteration)
        .set_default(false)
        .describe(
            "Seed PRNG determnisticly via iterator number, "
            "this option will be switched on automatically on distributed "
            "mode.");
    DMLC_DECLARE_FIELD(dsplit)
        .set_default(0)
        .add_enum("auto", 0)
        .add_enum("col", 1)
        .add_enum("row", 2)
        .describe("Data split mode for distributed training.");
    DMLC_DECLARE_FIELD(tree_method)
        .set_default(0)
        .add_enum("auto", 0)
        .add_enum("approx", 1)
        .add_enum("exact", 2)
        .add_enum("hist", 3)
        .add_enum("gpu_exact", 4)
        .add_enum("gpu_hist", 5)
        .add_enum("binidcache", 6)
        .add_enum("pmatcompact", 7)
        .add_enum("pmatfasthist", 8)
        .add_enum("fullcompact", 9)
        .add_enum("dmatdensevec", 10)
        .add_enum("pmathalftrick", 11)
        .add_enum("dmatdense", 12)
        .add_enum("densechainblock", 13)
        .add_enum("ftprune", 14)
        .add_enum("bnf00x", 15)
        .add_enum("lossguide", 16)
        .add_enum("block", 17)
        .add_enum("blockdenseseq", 18)
        .add_enum("blockdense", 19)
        .describe("Choice of tree construction method.");
    DMLC_DECLARE_FIELD(test_flag).set_default("").describe(
        "Internal test flag");
    DMLC_DECLARE_FIELD(nthread).set_default(0).describe(
        "Number of threads to use.");
    DMLC_DECLARE_FIELD(debug_verbose)
        .set_lower_bound(0)
        .set_default(0)
        .describe("flag to print out detailed breakdown of runtime");
    DMLC_DECLARE_FIELD(disable_default_eval_metric)
        .set_default(0)
        .describe("flag to disable default metric. Set to >0 to disable");
  }
};

DMLC_REGISTER_PARAMETER(LearnerModelParam);
DMLC_REGISTER_PARAMETER(LearnerTrainParam);

/*!
 * \brief learner that performs gradient boosting for a specific objective
 * function. It does training and prediction.
 */
class LearnerImpl : public Learner {
 public:
  explicit LearnerImpl(std::vector<std::shared_ptr<DMatrix> > cache)
      : cache_(std::move(cache)){
    // boosted tree
    name_obj_ = "reg:linear";
    name_gbm_ = "gbtree";
    harpCom_ = nullptr;
    
  }

void setHarpCom(harp::com::Communicator* harpCom) override {harpCom_ = harpCom;}

  static void AssertGPUSupport() {
#ifndef XGBOOST_USE_CUDA
    LOG(FATAL) << "XGBoost version not compiled with GPU support.";
#endif
  }

  // TODO: add the harpcom to updator
  void ConfigureUpdaters() {
    if (tparam_.tree_method == 0 || tparam_.tree_method == 1 ||
        tparam_.tree_method == 2) {
      if (cfg_.count("updater") == 0) {
        if (tparam_.dsplit == 1) {
          cfg_["updater"] = "distcol";
        } else if (tparam_.dsplit == 2) {
          cfg_["updater"] = "grow_histmaker,prune";
          //cfg_["updater"] = "grow_histmaker";
        }
      }
    } else if (tparam_.tree_method == 6) {
      cfg_["updater"] = "grow_binidcache,prune";
    } else if (tparam_.tree_method == 7) {
      cfg_["updater"] = "grow_pmatcompact,prune";
    } else if (tparam_.tree_method == 8) {
      cfg_["updater"] = "grow_pmatfasthist,prune";
      //cfg_["updater"] = "grow_pmatfasthist";
    } else if (tparam_.tree_method == 9) {
      cfg_["updater"] = "grow_fullcompact,prune";
    } else if (tparam_.tree_method == 10) {
      cfg_["updater"] = "grow_dmatdensevec,prune";
    } else if (tparam_.tree_method == 11) {
      cfg_["updater"] = "grow_pmathalftrick,prune";
    } else if (tparam_.tree_method == 12) {
      cfg_["updater"] = "grow_dmatdense,prune";
     } else if (tparam_.tree_method == 13) {
      cfg_["updater"] = "grow_densechainblock,prune";
     } else if (tparam_.tree_method == 14) {
      cfg_["updater"] = "grow_ftprune,prune";
     } else if (tparam_.tree_method == 15) {
      cfg_["updater"] = "grow_bnf00x,prune";
     } else if (tparam_.tree_method == 16) {
      cfg_["updater"] = "grow_block_lossguide,prune";
     } else if (tparam_.tree_method == 17) {
      cfg_["updater"] = "grow_block,prune";
     } else if (tparam_.tree_method == 18) {
      cfg_["updater"] = "grow_blockdense_seq,prune";
     } else if (tparam_.tree_method == 19) {
      cfg_["updater"] = "grow_blockdense,prune";

 } else if (tparam_.tree_method == 3) {

      /* histogram-based algorithm */
      LOG(CONSOLE) << "Tree method is selected to be \'hist\', which uses a "
                      "single updater "
                   << "grow_fast_histmaker.";
      cfg_["updater"] = "grow_fast_histmaker";
    } else if (tparam_.tree_method == 4) {
      this->AssertGPUSupport();
      if (cfg_.count("updater") == 0) {
        cfg_["updater"] = "grow_gpu,prune";
      }
      if (cfg_.count("predictor") == 0) {
        cfg_["predictor"] = "gpu_predictor";
      }
    } else if (tparam_.tree_method == 5) {
      this->AssertGPUSupport();
      if (cfg_.count("updater") == 0) {
        cfg_["updater"] = "grow_gpu_hist";
      }
      if (cfg_.count("predictor") == 0) {
        cfg_["predictor"] = "gpu_predictor";
      }
    }

    /* print the selected updater */
    LOG(CONSOLE) << "Treemethod=" << tparam_.tree_method <<
                " dsplit=" << tparam_.dsplit <<
                " updater=" << cfg_["updater"] << "\n";
  }

  // configure the updator but not yet create an updator
  void Configure(
      const std::vector<std::pair<std::string, std::string> >& args) override {
    // add to configurations
    tparam_.InitAllowUnknown(args);
    monitor_.Init("Learner", tparam_.debug_verbose);
    cfg_.clear();
    for (const auto& kv : args) {
      if (kv.first == "eval_metric") {
        // check duplication
        auto dup_check = [&kv](const std::unique_ptr<Metric>& m) {
          return m->Name() != kv.second;
        };
        if (std::all_of(metrics_.begin(), metrics_.end(), dup_check)) {
          metrics_.emplace_back(Metric::Create(kv.second));
          mparam_.contain_eval_metrics = 1;
        }
      } else {
        cfg_[kv.first] = kv.second;
      }
    }
    if (tparam_.nthread != 0) {
      omp_set_num_threads(tparam_.nthread);
    }

    // add additional parameters
    // These are cosntraints that need to be satisfied.
    if (tparam_.dsplit == 0 && rabit::IsDistributed()) {
      tparam_.dsplit = 2;
    }

    if (cfg_.count("num_class") != 0) {
      cfg_["num_output_group"] = cfg_["num_class"];
      if (atoi(cfg_["num_class"].c_str()) > 1 && cfg_.count("objective") == 0) {
        cfg_["objective"] = "multi:softmax";
      }
    }

    if (cfg_.count("max_delta_step") == 0 && cfg_.count("objective") != 0 &&
        cfg_["objective"] == "count:poisson") {
      cfg_["max_delta_step"] = kMaxDeltaStepDefaultValue;
    }

    // only configure not create the updator
    ConfigureUpdaters();

    if (cfg_.count("objective") == 0) {
      cfg_["objective"] = "reg:linear";
    }
    if (cfg_.count("booster") == 0) {
      cfg_["booster"] = "gbtree";
    }

    // initialize the model and params
    if (!this->ModelInitialized()) {
      mparam_.InitAllowUnknown(args);
      name_obj_ = cfg_["objective"];
      name_gbm_ = cfg_["booster"];
      // set seed only before the model is initialized
      common::GlobalRandom().seed(tparam_.seed);
    }

    // set number of features correctly.
    cfg_["num_feature"] = common::ToString(mparam_.num_feature);
    cfg_["num_class"] = common::ToString(mparam_.num_class);

    if (gbm_ != nullptr) {
      // configure the gbm 
      // focus on the gbm_ to be gbtree  
      gbm_->Configure(cfg_.begin(), cfg_.end());
      // assign the harp communicator
      gbm_->setHarpCom(harpCom_);
    }
    if (obj_ != nullptr) {
      obj_->Configure(cfg_.begin(), cfg_.end());
    }
  }

  void InitModel() override { this->LazyInitModel(); }

  void Load(dmlc::Stream* fi) override {
    // TODO(tqchen) mark deprecation of old format.
    common::PeekableInStream fp(fi);
    // backward compatible header check.
    std::string header;
    header.resize(4);
    if (fp.PeekRead(&header[0], 4) == 4) {
      CHECK_NE(header, "bs64")
          << "Base64 format is no longer supported in brick.";
      if (header == "binf") {
        CHECK_EQ(fp.Read(&header[0], 4), 4U);
      }
    }
    // use the peekable reader.
    fi = &fp;
    // read parameter
    CHECK_EQ(fi->Read(&mparam_, sizeof(mparam_)), sizeof(mparam_))
        << "BoostLearner: wrong model format";
    {
      // backward compatibility code for compatible with old model type
      // for new model, Read(&name_obj_) is suffice
      uint64_t len;
      CHECK_EQ(fi->Read(&len, sizeof(len)), sizeof(len));
      if (len >= std::numeric_limits<unsigned>::max()) {
        int gap;
        CHECK_EQ(fi->Read(&gap, sizeof(gap)), sizeof(gap))
            << "BoostLearner: wrong model format";
        len = len >> static_cast<uint64_t>(32UL);
      }
      if (len != 0) {
        name_obj_.resize(len);
        CHECK_EQ(fi->Read(&name_obj_[0], len), len)
            << "BoostLearner: wrong model format";
      }
    }
    CHECK(fi->Read(&name_gbm_)) << "BoostLearner: wrong model format";
    // duplicated code with LazyInitModel
    obj_.reset(ObjFunction::Create(name_obj_));
    gbm_.reset(GradientBooster::Create(name_gbm_, cache_, mparam_.base_score));
    gbm_->Load(fi);
    if (mparam_.contain_extra_attrs != 0) {
      std::vector<std::pair<std::string, std::string> > attr;
      fi->Read(&attr);
      attributes_ =
          std::map<std::string, std::string>(attr.begin(), attr.end());
    }
    if (name_obj_ == "count:poisson") {
      std::string max_delta_step;
      fi->Read(&max_delta_step);
      cfg_["max_delta_step"] = max_delta_step;
    }
    if (mparam_.contain_eval_metrics != 0) {
      std::vector<std::string> metr;
      fi->Read(&metr);
      for (auto name : metr) {
        metrics_.emplace_back(Metric::Create(name));
      }
    }
    cfg_["num_class"] = common::ToString(mparam_.num_class);
    cfg_["num_feature"] = common::ToString(mparam_.num_feature);
    obj_->Configure(cfg_.begin(), cfg_.end());
  }

  // rabit save model to rabit checkpoint
  void Save(dmlc::Stream* fo) const override {
    LearnerModelParam mparam = mparam_;  // make a copy to potentially modify
    std::vector<std::pair<std::string, std::string> > extra_attr;
      // extra attributed to be added just before saving

    if (name_obj_ == "count:poisson") {
      auto it = cfg_.find("max_delta_step");
      if (it != cfg_.end()) {
        // write `max_delta_step` parameter as extra attribute of booster
        mparam.contain_extra_attrs = 1;
        extra_attr.emplace_back("count_poisson_max_delta_step", it->second);
      }
    }
    fo->Write(&mparam, sizeof(LearnerModelParam));
    fo->Write(name_obj_);
    fo->Write(name_gbm_);
    gbm_->Save(fo);
    if (mparam.contain_extra_attrs != 0) {
      std::vector<std::pair<std::string, std::string> > attr(
          attributes_.begin(), attributes_.end());
      attr.insert(attr.end(), extra_attr.begin(), extra_attr.end());
      fo->Write(attr);
    }
    if (name_obj_ == "count:poisson") {
      auto it = cfg_.find("max_delta_step");
      if (it != cfg_.end()) {
        fo->Write(it->second);
      } else {
        // recover value of max_delta_step from extra attributes
        auto it2 = attributes_.find("count_poisson_max_delta_step");
        const std::string max_delta_step
          = (it2 != attributes_.end()) ? it2->second : kMaxDeltaStepDefaultValue;
        fo->Write(max_delta_step);
      }
    }
    if (mparam.contain_eval_metrics != 0) {
      std::vector<std::string> metr;
      for (auto& ev : metrics_) {
        metr.emplace_back(ev->Name());
      }
      fo->Write(metr);
    }
  }
  /**
   * @brief The major function for training one iteration
   * 
   * @param iter 
   * @param train 
   */
  void UpdateOneIter(int iter, DMatrix* train) override {
    monitor_.Start("UpdateOneIter");
    CHECK(ModelInitialized())
        << "Always call InitModel or LoadModel before update";
    if (tparam_.seed_per_iteration || rabit::IsDistributed()) {
      common::GlobalRandom().seed(tparam_.seed * kRandSeedMagic + iter);
    }
    this->LazyInitDMatrix(train);
    monitor_.Start("PredictRaw");
    this->PredictRaw(train, &preds_);
    monitor_.Stop("PredictRaw");

    //save preds
    save_preds(this->iterid_, this->tparam_.tree_method, preds_);


    monitor_.Start("GetGradient");
    obj_->GetGradient(preds_, train->Info(), iter, &gpair_);
    monitor_.Stop("GetGradient");
    
    
    save_grads(this->iterid_, this->tparam_.tree_method, gpair_);

    // the major step of training
    gbm_->DoBoost(train, &gpair_, obj_.get());
    monitor_.Stop("UpdateOneIter");

    this->iterid_++;
  }

  void BoostOneIter(int iter, DMatrix* train,
                    HostDeviceVector<GradientPair>* in_gpair) override {
    monitor_.Start("BoostOneIter");
    if (tparam_.seed_per_iteration || rabit::IsDistributed()) {
      common::GlobalRandom().seed(tparam_.seed * kRandSeedMagic + iter);
    }
    this->LazyInitDMatrix(train);
    gbm_->DoBoost(train, in_gpair);
    monitor_.Stop("BoostOneIter");
  }

  std::string EvalOneIter(int iter, const std::vector<DMatrix*>& data_sets,
                          const std::vector<std::string>& data_names) override {
    monitor_.Start("EvalOneIter");
    std::ostringstream os;
    os << '[' << iter << ']' << std::setiosflags(std::ios::fixed);
    if (metrics_.size() == 0 && tparam_.disable_default_eval_metric <= 0) {
      metrics_.emplace_back(Metric::Create(obj_->DefaultEvalMetric()));
    }
    for (size_t i = 0; i < data_sets.size(); ++i) {
      this->PredictRaw(data_sets[i], &preds_);
      obj_->EvalTransform(&preds_);
      for (auto& ev : metrics_) {
        os << '\t' << data_names[i] << '-' << ev->Name() << ':'
           << ev->Eval(preds_.ConstHostVector(), data_sets[i]->Info(),
                       tparam_.dsplit == 2);
      }
    }

    monitor_.Stop("EvalOneIter");
    return os.str();
  }

  void SetAttr(const std::string& key, const std::string& value) override {
    attributes_[key] = value;
    mparam_.contain_extra_attrs = 1;
  }

  bool GetAttr(const std::string& key, std::string* out) const override {
    auto it = attributes_.find(key);
    if (it == attributes_.end()) return false;
    *out = it->second;
    return true;
  }

  bool DelAttr(const std::string& key) override {
    auto it = attributes_.find(key);
    if (it == attributes_.end()) return false;
    attributes_.erase(it);
    return true;
  }

  std::vector<std::string> GetAttrNames() const override {
    std::vector<std::string> out;
    out.reserve(attributes_.size());
    for (auto& p : attributes_) {
      out.push_back(p.first);
    }
    return out;
  }

  std::pair<std::string, bst_float> Evaluate(DMatrix* data,
                                             std::string metric) {
    if (metric == "auto") metric = obj_->DefaultEvalMetric();
    std::unique_ptr<Metric> ev(Metric::Create(metric.c_str()));
    this->PredictRaw(data, &preds_);
    obj_->EvalTransform(&preds_);
    return std::make_pair(metric,
                          ev->Eval(preds_.ConstHostVector(), data->Info(),
                                   tparam_.dsplit == 2));
  }

  void Predict(DMatrix* data, bool output_margin,
               HostDeviceVector<bst_float>* out_preds, unsigned ntree_limit,
               bool pred_leaf, bool pred_contribs, bool approx_contribs,
               bool pred_interactions) const override {
    if (pred_contribs) {
      gbm_->PredictContribution(data, &out_preds->HostVector(), ntree_limit, approx_contribs);
    } else if (pred_interactions) {
      gbm_->PredictInteractionContributions(data, &out_preds->HostVector(), ntree_limit,
                                            approx_contribs);
    } else if (pred_leaf) {
      gbm_->PredictLeaf(data, &out_preds->HostVector(), ntree_limit);
    } else {
      this->PredictRaw(data, out_preds, ntree_limit);
      if (!output_margin) {
        obj_->PredTransform(out_preds);
      }
    }
  }

 protected:
  // check if p_train is ready to used by training.
  // if not, initialize the column access.
  inline void LazyInitDMatrix(DMatrix* p_train) {
    if (tparam_.tree_method == 3 || tparam_.tree_method == 4 ||
        tparam_.tree_method == 5 || name_gbm_ == "gblinear") {
      return;
    }

    if (!p_train->SingleColBlock() && cfg_.count("updater") == 0) {
      if (tparam_.tree_method == 2) {
        LOG(CONSOLE) << "tree method is set to be 'exact',"
                     << " but currently we are only able to proceed with "
                        "approximate algorithm";
      }
      cfg_["updater"] = "grow_histmaker,prune";
      if (gbm_ != nullptr) {
        gbm_->Configure(cfg_.begin(), cfg_.end());
        gbm_->setHarpCom(harpCom_);
      }
    }
  }

  // return whether model is already initialized.
  inline bool ModelInitialized() const { return gbm_ != nullptr; }

  inline void LazyInitModel() {
    if (this->ModelInitialized()) return;
    // estimate feature bound
    unsigned num_feature = 0;

    // obtain the maximal feature number from training data and evaluation data
    for (auto & matrix : cache_) {
      CHECK(matrix != nullptr);
      num_feature = std::max(num_feature,
                             static_cast<unsigned>(matrix->Info().num_col_));
    }

    // run allreduce on num_feature to find the maximum value
    rabit::Allreduce<rabit::op::Max>(&num_feature, 1);
    if (harpCom_ != nullptr && harpCom_->getWorldSize() > 1)
    {
      // int num_feature_all = 0;
      // int *num_feature_local = new int[1];
      // num_feature_local[0] = (int)num_feature;
      // harp::ds::Table<int> *fNumTable = new harp::ds::Table<int>(harpCom_->getWorkerId());
      // fNumTable->addPartition(new harp::ds::Partition<int>(0, num_feature_local, 1));
      // // doing allreduce
      // harpCom_->allReduce<int>(fNumTable, MPI_MAX);
      // num_feature_all = (*(fNumTable->getPartition(0)->getData()));
      int num_feature_global = (int)num_feature;
      harp::ds::Table<int> *fNumTable = new harp::ds::Table<int>(harpCom_->getWorkerId());
      fNumTable->addPartition(new harp::ds::Partition<int>(0, &num_feature_global, 1));
      harpCom_->allReduce<int>(fNumTable, MPI_MAX, true);

      // if (num_feature_all > mparam_.num_feature)
      if (num_feature_global > mparam_.num_feature)
      {
        // mparam_.num_feature = (unsigned)num_feature_all;
        mparam_.num_feature = (unsigned)num_feature_global;
      }

      fNumTable->removePartition(0, false);
      delete fNumTable;
    }
    else
    {
      // adjust the param of feature number
      if (num_feature > mparam_.num_feature)
      {
        mparam_.num_feature = num_feature;
      }
    }

    // setup
    cfg_["num_feature"] = common::ToString(mparam_.num_feature);

    CHECK(obj_ == nullptr && gbm_ == nullptr);

    obj_.reset(ObjFunction::Create(name_obj_));
    obj_->Configure(cfg_.begin(), cfg_.end());

    // reset the base score
    mparam_.base_score = obj_->ProbToMargin(mparam_.base_score);
    // reset and configure gbm 
    gbm_.reset(GradientBooster::Create(name_gbm_, cache_, mparam_.base_score));
    gbm_->Configure(cfg_.begin(), cfg_.end());
    // setup the harp comm
    gbm_->setHarpCom(harpCom_);

  }

  /*!
   * \brief get un-transformed prediction
   * \param data training data matrix
   * \param out_preds output vector that stores the prediction
   * \param ntree_limit limit number of trees used for boosted tree
   *   predictor, when it equals 0, this means we are using all the trees
   */
  inline void PredictRaw(DMatrix* data, HostDeviceVector<bst_float>* out_preds,
                         unsigned ntree_limit = 0) const {
    CHECK(gbm_ != nullptr)
        << "Predict must happen after Load or InitModel";
    gbm_->PredictBatch(data, out_preds, ntree_limit);
  }

  // model parameter
  LearnerModelParam mparam_;
  // training parameter
  LearnerTrainParam tparam_;
  // configurations
  std::map<std::string, std::string> cfg_;
  // attributes
  std::map<std::string, std::string> attributes_;
  // name of gbm
  std::string name_gbm_;
  // name of objective function
  std::string name_obj_;
  // temporal storages for prediction
  HostDeviceVector<bst_float> preds_;
  // gradient pairs
  HostDeviceVector<GradientPair> gpair_;

  // iterid
  int iterid_{0};

  // harp communicator
  harp::com::Communicator* harpCom_;

 private:
  /*! \brief random number transformation seed. */
  static const int kRandSeedMagic = 127;
  // internal cached dmatrix
  std::vector<std::shared_ptr<DMatrix> > cache_;

  common::Monitor monitor_;


};

Learner* Learner::Create(
    const std::vector<std::shared_ptr<DMatrix> >& cache_data) {
  return new LearnerImpl(cache_data);
}
}  // namespace xgboost
