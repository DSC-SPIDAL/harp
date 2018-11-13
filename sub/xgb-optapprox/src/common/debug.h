/*!
 * Copyright 2015-2018 by Contributors
 * \file DEBUG.h
 * \brief DEBUG utilities
 */
#ifndef XGBOOST_DEBUG_DEBUG_H_
#define XGBOOST_DEBUG_DEBUG_H_

#include <xgboost/base.h>
#include <xgboost/logging.h>

#include <exception>
#include <limits>
#include <type_traits>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
#include <thread>
#include <mutex>
#include <chrono>
#include <fstream>

#include <xgboost/tree_model.h>
#include "../data/compact_dmatrix.h"
#include "../common/hist_util.h"

namespace xgboost {

using xgboost::common::HistCutMatrix;
using xgboost::common::GHistIndexMatrix;
using xgboost::tree::SplitEntry;


void startVtune(std::string tagfilename, int waittime=10000);
void printVec(std::string msg, const std::vector<unsigned int>& vec);
void printVec(std::string msg, const std::vector<int>& vec);
void printInt(std::string msg, int val);
void printmsg(std::string msg);
void printtree(RegTree* ptree, std::string header="");
void printdmat(DMatrixCompact& dmat);
void printdmat(const SparsePage& dmat);
void printgmat(GHistIndexMatrix& gmat);
void printcut(HistCutMatrix& gmat);

//void printnodes(std::vector<NodeEntry>& nodes, std::string header="");
void printSplit(SplitEntry& split);

void save_preds(int iterid, int tree_method, HostDeviceVector<bst_float>& preds);
void save_grads(int iterid, int tree_method, HostDeviceVector<GradientPair>& gpair);

}  // namespace xgboost
#endif  // XGBOOST_DEBUG_DEBUG_H_
