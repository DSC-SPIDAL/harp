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
            #ifdef USE_BINID
            this->data.push_back(EntryCompact(c.index, c.binid));
            #else
            this->data.push_back(EntryCompact(c.index, c.fvalue));
            #endif
        }
    }
    //end 
    this->offset.push_back(this->data.size());

    LOG(CONSOLE) << "DMatrixCompact::Init size=" << _size <<
        ",memory=" << this->data.size()*sizeof(EntryCompact)/(1024*1024) << "MB" <<
        ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << ",nonzero=" << info_.num_nonzero_;

}

void DMatrixCompactDense::Init(const SparsePage& page, MetaInfo& info){

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
        auto col = page[i];
        int startpos = this->data.size();
        for (int j=0; j < col.size(); j++){
            this->data.push_back(0);
        }
        for (int j=0; j < col.size(); j++){
            #ifdef USE_BINID
            this->data[startpos + col[j].index] = static_cast<BinIDType>(col[j].binid);
            #else
            this->data[startpos + col[j].index] = static_cast<BinIDType>(col[j].fvalue);
            #endif
 
        }
    }
    //end 
    this->offset.push_back(this->data.size());

    LOG(CONSOLE) << "DMatrixCompact::Init size=" << _size <<
        ",memory=" << this->data.size()*sizeof(BinIDType)/(1024*1024) << "MB" <<
        ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << ",nonzero=" << info_.num_nonzero_;

}

void DMatrixCompactBlockDense::Init(const SparsePage& page, MetaInfo& info){
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
        auto col = page[i];
        int startpos = this->data.size();
        for (int j=0; j < col.size(); j++){
            this->data.push_back(0);
        }
        for (int j=0; j < col.size(); j++){
            #ifdef USE_BINID
            this->data[startpos + col[j].index] = static_cast<BinIDType>(col[j].binid);
            #else
            this->data[startpos + col[j].index] = static_cast<BinIDType>(col[j].fvalue);
            #endif
        }
    }
    //end 
    this->offset.push_back(this->data.size());

    LOG(CONSOLE) << "DMatrixCompact::Init size=" << _size <<
        ",memory=" << this->data.size()*sizeof(BinIDType)/(1024*1024) << "MB" <<
        ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << ",nonzero=" << info_.num_nonzero_;

}

/*
 *
 *
 */
void DMatrixCube::Init(const SparsePage& page, MetaInfo& info, int num_maxbins, BlockInfo& blkInfo){
    //save the info
    //shallow copy only the num_
    info_.num_row_ = info.num_row_;
    info_.num_col_ = info.num_col_;
    info_.num_nonzero_ = info.num_nonzero_;

    blkInfo.init(info.num_row_, info_.num_col_, num_maxbins);

    //init 
    data_.clear();
    int row_blknum = (info.num_col_ + blkInfo.GetRowBlkSize() - 1)/ blkInfo.GetRowBlkSize(); 
    int fid_blknum = (info.num_col_ + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    int binid_blknum = (num_maxbins + blkInfo.GetBinBlkSize() - 1)/ blkInfo.GetBinBlkSize(); 
    data_.resize(fid_blknum * binid_blknum);
    //todo: init blkid if necessary
    for(int i=0; i < fid_blknum * binid_blknum; i++){
        data_[i].init(i);
    }

    LOG(CONSOLE) << "CubeInit:row_blknum=" << row_blknum <<
        "fid_blknum=" << fid_blknum << "binid_blknum=" << binid_blknum;

    // rowset
    // go through all rows
    int rownum = page.Size();
    for(int rowid = 0; rowid < rownum; rowid++){

        auto row = page[rowid];
        //split into blks to all DMatrixCubeZCol items
        int rowblkid = rowid / blkInfo.GetRowBlkSize();

        //
        //one step for all ZCols
        //add block before row
        //
        if ((rowid % blkInfo.GetRowBlkSize()) == 0){
            addblock();
        }
        addrow();
 
        //add data
        for (auto& ins: row){
            //for all <features,binid> items in this row
            int blkid = (ins.binid / blkInfo.GetBinBlkSize()) * fid_blknum + ins.index / blkInfo.GetFeatureBlkSize(); 
            BlkAddrType blkaddr = (ins.binid % blkInfo.GetBinBlkSize()) * blkInfo.GetFeatureBlkSize() + ins.index % blkInfo.GetFeatureBlkSize();

            CHECK_LT(blkid, data_.size());
            data_[blkid].append(blkaddr);

        }

   }
   //add sentinel at the end
   addblock();
   addrow();


   LOG(CONSOLE) << "DMatrixCube::Init" <<
        ",BlkInfo=r" << blkInfo.GetRowBlkSize() << ",f" << blkInfo.GetFeatureBlkSize() << ",b" << blkInfo.GetBinBlkSize() <<
        ",memory=" << getMemSize()/(1024*1024) << "MB" <<
        ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << "x" << num_maxbins <<
        ",nonzero=" << info_.num_nonzero_;

}



//}  // namespace data
}  // namespace xgboost
