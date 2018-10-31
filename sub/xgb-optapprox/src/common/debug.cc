/*!
 * Copyright 2015-2018 by Contributors
 * \file common.cc
 * \brief Enable all kinds of global variables in common.
 */
#include "debug.h"

namespace xgboost {

#if defined(USE_DEBUG)
void printtree(RegTree* ptree, std::string header=""){

   if (header==""){ 
    std::cout << "RegTree===========================\n" ;
    }else{
    std::cout << "===" << header << "===\n" ;

   }
   int id = 0;
   for(auto node : ptree->GetNodes()){ 
       unsigned split_index = node.SplitIndex();
       float split_value = node.SplitCond();

       std::cout << id << ":" << split_index << ":" << split_value << " ";
   }
   std::cout << "\n";
}


void printdmat(DMatrixCompact& dmat){
  std::cout << "HMAT======================================\n";
  for (size_t fid = 0; fid < dmat.Size(); ++fid) {
    auto col = dmat[fid];
    const auto ndata = static_cast<bst_omp_uint>(col.size());
    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j].index();
        const bst_uint binid = col[j].binid();

        std::cout << ridx << ":" << binid << " ";
     }
    std::cout << "\n";
  }
}

void printdmat(const SparsePage& dmat){
  std::cout << "DMAT======================================\n";
  for (size_t fid = 0; fid < dmat.Size(); ++fid) {
    auto col = dmat[fid];
    const auto ndata = static_cast<bst_omp_uint>(col.size());
    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j].index;
        const bst_float fvalue = col[j].fvalue;

        std::cout << ridx << ":" << fvalue << " ";
     }
    std::cout << "\n";
  }
  
  for (size_t fid = 0; fid < dmat.Size(); ++fid) {
    auto col = dmat[fid];
    const auto ndata = static_cast<bst_omp_uint>(col.size());
    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j].index;
        const bst_uint binid = col[j].binid;

        std::cout << ridx << ":" << binid << " ";
     }
    std::cout << "\n";
  }
}
#else

void printtree(RegTree* ptree){}
void printdmat(DMatrixCompact& dmat){}
void printdmat(const SparsePage& dmat){}

#endif

}  // namespace xgboost
