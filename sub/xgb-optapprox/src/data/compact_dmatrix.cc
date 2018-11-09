/*!
 * Copyright 2014 by Contributors
 * \file simple_dmatrix.cc
 * \brief the input data structure for gradient boosting
 * \author Tianqi Chen
 */
#include "./compact_dmatrix.h"
#include <xgboost/data.h>
#include "../common/random.h"

namespace xgboost {
//namespace data {

void DMatrixCompact::Init(const SparsePage& page, MetaInfo& info){

    //save the info
    //shallow copy only the num_
    info_.num_row_ = info.num_row_;
    info_.num_col_ = info.num_col_;
    info_.num_nonzero_ = info.num_nonzero_;

    //clear
    this->data.clear();
    this->offset.clear();

    //go through all columns
    int _size = page.Size();
    for(int i=0; i < _size; i++){
        //SparsePage::Inst col = page[i];
        this->offset.push_back(this->data.size());
        for (auto& c : page[i]){
            this->data.push_back(EntryCompact(c.index, c.binid));
        }
    }
    //end 
    this->offset.push_back(this->data.size());

    LOG(CONSOLE) << "DMatrixCompact::Init size=" << _size <<
        ",memory=" << this->data.size()*sizeof(EntryCompact)/(1024*1024) << "MB" <<
        ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << ",nonzero=" << info_.num_nonzero_;

}

//}  // namespace data
}  // namespace xgboost
