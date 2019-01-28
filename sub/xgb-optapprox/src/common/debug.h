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
#include "../common/pos_set.h"
#include "../tree/param.h"


namespace xgboost {

using xgboost::common::HistCutMatrix;
using xgboost::common::GHistIndexMatrix;
using xgboost::tree::SplitEntry;
using xgboost::tree::POSSet;


void startVtune(std::string tagfilename, int waittime=10000);
void printVec(std::string msg, const std::vector<unsigned int>& vec);
void printVec(std::string msg, const std::vector<int>& vec);
void printVec(std::string msg, const std::vector<float>& vec);
void printInt(std::string msg, int val);
void printmsg(std::string msg);
void printtree(RegTree* ptree, std::string header="");
//void printdmat(DMatrixCube& dmat);
//void printdmat(DMatrixDenseCube& dmat);
void printdmat(DMatrixCompactDense& dmat);
void printdmat(DMatrixCompactBlockDense& dmat);
void printdmat(DMatrixCompactBlock& dmat);
void printdmat(DMatrixCompact& dmat);
void printdmat(const SparsePage& dmat);
void printgmat(GHistIndexMatrix& gmat);
void printcut(HistCutMatrix& gmat);

void printPOSSet(POSSet& pos, int gid=0);

void printgh(const std::vector<GradientPair> &gpair);

//void printnodes(std::vector<NodeEntry>& nodes, std::string header="");
void printSplit(SplitEntry& split, int fid=-1, int nid=-1);

void save_preds(int iterid, int tree_method, HostDeviceVector<bst_float>& preds);
void save_grads(int iterid, int tree_method, HostDeviceVector<GradientPair>& gpair);

/*
 * template functions
 */
template<typename T>
void printdmat(T& dmat){
  std::cout << "HMAT(DMatrixCube)======================================\n";
  int nblks = dmat.Size();
  nblks = std::min(nblks, 5);

  for (size_t blkid = 0; blkid < nblks; ++blkid) {

    int znum = dmat[blkid].GetBlockNum();
    int nzblks = std::min(znum, 2);

    std::cout << "BLK:" << blkid << "\n";
    for(int z = 0; z < nzblks; z++){
        auto blk = dmat[blkid].GetBlock(z);
        auto ndata = static_cast<bst_omp_uint>(blk.size());

        std::cout << "    zblk:" << z << ", len=" << ndata << "\n";

        int tn = 0;
        for (size_t j = 0; tn < 10 && j < ndata; ++j) {
            const bst_uint ridx = blk._index(j);
            if (blk.rowsize(j) > 0 ){
                std::cout << "r:" << ridx << "<";
                for(size_t k = 0; k < std::min(blk.rowsize(j),10); k++){
                    const bst_uint blkaddr = blk._blkaddr(j,k);
                    std::cout << ridx << ":" << blkaddr << " ";
                }
                std::cout << "> ";

                tn++;
            }
        }
        std::cout << "\n";
    }
  }
}








}  // namespace xgboost
#endif  // XGBOOST_DEBUG_DEBUG_H_
