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

void DMatrixCompact::Init(const SparsePage& page){

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
        ",memory=" << this->data.size();

}

//}  // namespace data
}  // namespace xgboost
