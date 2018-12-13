/*!
 * Copyright 2015-2018 by Contributors
 * \file DEBUG.h
 * \brief DEBUG utilities
 */
#ifndef XGBOOST_DEBUG_DEBUG_H_
#define XGBOOST_DEBUG_DEBUG_H_
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
#include <iostream>

namespace daal
{
namespace algorithms
{
namespace dtrees
{
namespace internal
{


void startVtune(std::string tagfilename, int waittime=10000);
void printVec(std::string msg, const std::vector<unsigned int>& vec);
void printVec(std::string msg, const std::vector<int>& vec);
void printInt(std::string msg, int val);
void printmsg(std::string msg);
}  // namespace xgboost
}
}
}

#endif  // XGBOOST_DEBUG_DEBUG_H_
