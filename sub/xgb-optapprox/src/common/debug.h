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

#include <xgboost/tree_model.h>
#include "../data/compact_dmatrix.h"

namespace xgboost {
void printtree(RegTree* ptree, std::string header="");
void printdmat(DMatrixCompact& dmat);
void printdmat(const SparsePage& dmat);
}  // namespace xgboost
#endif  // XGBOOST_DEBUG_DEBUG_H_
