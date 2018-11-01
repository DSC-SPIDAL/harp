/*!
 * Copyright 2015-2018 by Contributors
 * \file common.cc
 * \brief Enable all kinds of global variables in common.
 */
#include "debug.h"

namespace xgboost {

#ifdef USE_DEBUG
void printtree(RegTree* ptree, std::string header=""){

   if (header==""){ 
    std::cout << "RegTree===========================\n" ;
    }else{
    std::cout << "===" << header << "===\n" ;

   }

   std::cout << "Tree.param nodes=" << ptree->param.num_nodes <<
       ",num_roots=" << ptree->param.num_roots <<
       ",deleted=" << ptree->param.num_deleted << "\n";

   int nsize = ptree->GetNodes().size();
   for(int i=0; i< nsize; i++){
       auto node = ptree->GetNodes()[i];
       auto stat = ptree->Stat(i);

       unsigned split_index = node.SplitIndex();
       float split_value = node.SplitCond();
       if (node.IsLeaf()){
           std::cout << i << ":leaf";
       }
       else{
           std::cout << i << ":" << split_index << ":" << split_value;
       }

       std::cout << "<l" << stat.loss_chg << "h" << stat.sum_hess <<
           "w" << stat.base_weight << "c" << stat.leaf_child_cnt << ">\n";
   }
 
   //int id = 0;
   //for(auto node : ptree->GetNodes()){ 
   //    unsigned split_index = node.SplitIndex();
   //    float split_value = node.SplitCond();
   //    if (node.IsLeaf()){

   //    }
   //    else{
   //        std::cout << id << ":" << split_index << ":" << split_value << " ";
   //    }
   //}
   std::cout << "\n";
}

void printmsg(std::string msg){
    std::cout << "MSG:" << msg << "\n";
}

void printcut(HistCutMatrix& cut){
  std::cout << "GHistCutMAT======================================\n";
  int nfeature = cut.row_ptr.size() - 1;

  for (size_t fid = 0; fid < nfeature; ++fid) {
    auto a = cut[fid];
    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < a.size; ++j) {
        std::cout << j << ":" << a.cut[j] << " ";
     }
    std::cout << "\n";
  }
}

void printgmat(GHistIndexMatrix& gmat){
  std::cout << "GHistIndexMAT======================================\n";
  int nrows = gmat.row_ptr.size() - 1;

  for (size_t id = 0; id < nrows; ++id) {
    auto a = gmat[id];
    std::cout << "R:" << id << " "; 
    for (bst_omp_uint j = 0; j < a.size; ++j) {
        std::cout << j << ":" << a.index[j] << " ";
     }
    std::cout << "\n";
  }
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

void printtree(RegTree* ptree, std::string header=""){}
void printdmat(DMatrixCompact& dmat){}
void printdmat(const SparsePage& dmat){}

#endif

}  // namespace xgboost
