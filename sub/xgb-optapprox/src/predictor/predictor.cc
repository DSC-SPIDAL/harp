/*!
 * Copyright by Contributors 2017
 */
#include <dmlc/registry.h>
#include <xgboost/predictor.h>

namespace dmlc {
DMLC_REGISTRY_ENABLE(::xgboost::PredictorReg);
}  // namespace dmlc
namespace xgboost {
void Predictor::Init(
    const std::vector<std::pair<std::string, std::string>>& cfg,
    const std::vector<std::shared_ptr<DMatrix>>& cache) {
  for (const std::shared_ptr<DMatrix>& d : cache) {
    cache_[d.get()].data = d;

    //addFeature:: init the predictions with 0
    const auto nrows = static_cast<bst_omp_uint>(d.get()->Info().num_row_);
    cache_[d.get()].predictions.Resize(nrows);

    //InitOutPredictions(d.get()->Info(), &(cache_[d.get()].predictions), model);
    const auto& base_margin = d.get()->Info().base_margin_.HostVector();
    std::vector<bst_float>& out_preds_h = cache_[d.get()].predictions.HostVector();
    if (base_margin.size() == nrows) {
      std::copy(base_margin.begin(), base_margin.end(), out_preds_h.begin());
    } else {
      std::fill(out_preds_h.begin(),out_preds_h.end(),0);
      LOG(CONSOLE) << "failed in fast Init of Predictor cache,rowxcol="
          << d.get()->Info().num_row_ << "x" 
          << d.get()->Info().num_col_ ;
    }
  }
}
Predictor* Predictor::Create(std::string name) {
  auto* e = ::dmlc::Registry<PredictorReg>::Get()->Find(name);
  if (e == nullptr) {
    LOG(FATAL) << "Unknown predictor type " << name;
  }
  return (e->body)();
}
}  // namespace xgboost

namespace xgboost {
namespace predictor {
// List of files that will be force linked in static links.
#ifdef XGBOOST_USE_CUDA
DMLC_REGISTRY_LINK_TAG(gpu_predictor);
#endif
DMLC_REGISTRY_LINK_TAG(cpu_predictor);
}  // namespace predictor
}  // namespace xgboost
