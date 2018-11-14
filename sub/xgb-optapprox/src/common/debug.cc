/*!
 * Copyright 2015-2018 by Contributors
 * \file common.cc
 * \brief Enable all kinds of global variables in common.
 */
#include "debug.h"

namespace xgboost {

void startVtune(std::string tagfilename, int waittime /*10000*/){
    static bool isInit = false;

    if (!isInit){
        std::ofstream write;
        write.open(tagfilename);
        write << "okay" << std::endl;
        write.close();
        isInit = true;

#ifdef USE_VTUNE
        //sleep for 1 sec
        std::this_thread::sleep_for(std::chrono::milliseconds(waittime));
#endif

    }
}


#ifdef USE_DEBUG
//void printnodes(std::vector<NodeEntry>& nodes, std::string header=""){
//
//   if (header==""){ 
//    std::cout << "RegTree===========================\n" ;
//    }else{
//    std::cout << "===" << header << "===\n" ;
//
//   }
//
//   std::cout << "Tree.param nodes=" << nodes.size() << "\n";
//
//   int nsize = nodes.size();
//   for(int i=0; i< nsize; i++){
//       auto split = nodes[i].best;
//       auto stat = nodes[i].stats;
//
//       unsigned split_index = split.sindex_ & ((1U << 31) - 1U);
//       float split_value = split.split_value;
//       bool split_left = (split.(sindex_ >> 31) != 0);
//       //if (node.IsLeaf()){
//       //    std::cout << i << ":leaf";
//       //}
//       //else{
//           std::cout << i << ":" << split_index << ":" << split_value
//               << ":" << (split_left?1:0);
//       //}
//
//       std::cout << "<l" << split.loss_chg << "h" << stat.sum_hess <<
//           "w" << nodes[i].weight << ">\n";
//   }
// 
//   std::cout << "\n";
//}

void printtree(RegTree* ptree, std::string header /*""*/){

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
       bool split_left = node.DefaultLeft();
       if (node.IsLeaf()){
           std::cout << i << ":leaf";
       }
       else{
           std::cout << i << ":" << split_index << ":" << split_value
               << ":" << (split_left?1:0);
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

void printInt(std::string msg, int val){
    std::ostringstream stringStream;
    stringStream << msg << ":" << val;
    printmsg(stringStream.str());
}
void printVec(std::string msg, const std::vector<unsigned int>& vec){
    std::ostringstream stringStream;
    stringStream << msg ;
    for(int i=0; i< vec.size(); i++){
    stringStream << vec[i] << ",";
    }
    printmsg(stringStream.str());
}
void printVec(std::string msg, const std::vector<int>& vec){
    std::ostringstream stringStream;
    stringStream << msg ;
    for(int i=0; i< vec.size(); i++){
    stringStream << vec[i] << ",";
    }
    printmsg(stringStream.str());
}




void printcut(HistCutMatrix& cut){
  std::cout << "GHistCutMAT======================================\n";
  int nfeature = cut.row_ptr.size() - 1;

  nfeature = std::min(nfeature, 50);

  for (size_t fid = 0; fid < nfeature; ++fid) {
    auto a = cut[fid];
    std::cout << "F:" << fid << " "; 

    int asize = std::min(a.size, 50U);

    for (bst_omp_uint j = 0; j < a.size; ++j) {
        std::cout << j << ":" << a.cut[j] << " ";
     }
    std::cout << "\n";
  }
}

void printgmat(GHistIndexMatrix& gmat){
  std::cout << "GHistIndexMAT======================================\n";
  int nrows = gmat.row_ptr.size() - 1;

  nrows = std::min(nrows, 50);

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
  int nrows = dmat.Size();
  nrows = std::min(nrows, 50);

  for (size_t fid = 0; fid < nrows; ++fid) {
    auto col = dmat[fid];
    auto ndata = static_cast<bst_omp_uint>(col.size());

    ndata = std::min(ndata, 50U);

    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j]._index();
        const bst_uint binid = col[j]._binid();

        std::cout << ridx << ":" << binid << " ";
     }
    std::cout << "\n";
  }
}

void printdmat(const SparsePage& dmat){
  std::cout << "XDMAT======================================\n";

  unsigned int nrows = dmat.Size();
  nrows = std::min(nrows, 50U);

  for (size_t fid = 0; fid < nrows; ++fid) {
    auto col = dmat[fid];
    auto ndata = static_cast<bst_omp_uint>(col.size());
    
    ndata = std::min(ndata, 50U);

    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j].index;
        const bst_float fvalue = col[j].fvalue;

        std::cout << ridx << ":" << fvalue << " ";
     }
    std::cout << "\n";
  }
  
  for (size_t fid = 0; fid < nrows; ++fid) {
    auto col = dmat[fid];
    auto ndata = static_cast<bst_omp_uint>(col.size());

    ndata = std::min(ndata, 50U);

    std::cout << "F:" << fid << " "; 
    for (bst_omp_uint j = 0; j < ndata; ++j) {
        const bst_uint ridx = col[j].index;
        const bst_uint binid = col[j].binid;

        std::cout << ridx << ":" << binid << " ";
     }
    std::cout << "\n";
  }
}


void printSplit(SplitEntry& split){
    unsigned split_index = split.sindex & ((1U << 31) - 1U);
    bool split_left = ((split.sindex >> 31) != 0);

    static std::mutex m;

    m.lock();
    std::cout << "FindSplit best=i" << split_index << ",v" << 
        split.split_value << ",l" << split.loss_chg 
        << ":" << (split_left?1:0) << "\n";
    m.unlock();
}

#else
void printmsg(std::string msg){}
void printtree(RegTree* ptree, std::string header /*""*/){}
void printdmat(DMatrixCompact& dmat){}
void printdmat(const SparsePage& dmat){}
void printgmat(GHistIndexMatrix& gmat){}
void printcut(HistCutMatrix& gmat){}
void printSplit(SplitEntry& split){}
void printInt(std::string msg, int val){}
//void printnodes(std::vector<NodeEntry>& nodes, std::string header=""){}
void printVec(std::string msg, const std::vector<unsigned int>& vec){}
void printVec(std::string msg, const std::vector<int>& vec){}

#endif

#ifdef USE_DEBUG_SAVE

void save_preds(int iterid, int tree_method, HostDeviceVector<bst_float>& preds){
    std::ostringstream ss;
    ss << "preds_" << iterid << "_" << tree_method;

    std::ofstream write;
    write.open(ss.str());
    bst_float* data = preds.HostPointer();
    int nsize = preds.Size();
    for(int i=0; i< nsize; i++){
        write << data[i] << std::endl;
    }

    write.close();
}

void save_grads(int iterid, int tree_method, HostDeviceVector<GradientPair>& gpair){
    std::ostringstream ss;
    ss << "gpair_" << iterid << "_" << tree_method;

    std::ofstream write;
    write.open(ss.str());
    GradientPair* data = gpair.HostPointer();
    int nsize = gpair.Size();
    for(int i=0; i< nsize; i++){
        write << data[i].GetGrad() <<" " << data[i].GetHess() << std::endl;
    }

    write.close();
}

#else

void save_preds(int iterid, int tree_method, HostDeviceVector<bst_float>& preds){}
void save_grads(int iterid, int tree_method, HostDeviceVector<GradientPair>& gpair){}
#endif

}  // namespace xgboost
