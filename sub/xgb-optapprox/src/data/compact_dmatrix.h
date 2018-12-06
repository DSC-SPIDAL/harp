/*!
 * Copyright 2015 by Contributors
 * \file simple_dmatrix.h
 * \brief In-memory version of DMatrix.
 * \author Tianqi Chen
 */
#ifndef XGBOOST_DATA_COMPACT_DMATRIX_H_
#define XGBOOST_DATA_COMPACT_DMATRIX_H_

#include <xgboost/base.h>
#include <xgboost/data.h>
#include <algorithm>
#include <cstring>
#include <vector>
#include "sparse_page_dmatrix.h"

namespace xgboost {
//namespace data {

#ifndef USE_COMPACT_BINID
class EntryCompact{
    public:
        unsigned int index;
        unsigned int binid;

        EntryCompact(unsigned int index, unsigned int binid):
            index(index),binid(binid){}

        inline unsigned int _index() const{
            return index;
        }

        inline unsigned int _binid() const{
            return binid;
        }
};

#else
class EntryCompact{
    public:
        unsigned int _data;
        EntryCompact(unsigned int index, unsigned int binid){
            _data = (binid << 24) | ( index & 0xffffff);
        }

        inline unsigned int _index() const{
            return _data & 0xffffff;
        }

        inline unsigned int _binid() const{
            return _data >> 24;
        }
};
#endif

class DMatrixCompact : public xgboost::data::SparsePageDMatrix {
 private:
     std::vector<EntryCompact> data;
     std::vector<size_t> offset;
     MetaInfo info_;

 public:
  explicit DMatrixCompact(){}

  //initialize
  void Init(const SparsePage& page, MetaInfo& info);

  using Inst = common::Span<EntryCompact const>;

  inline Inst operator[](size_t i) const {
    return {data.data() + offset[i],
            static_cast<Inst::index_type>(offset[i + 1] - offset[i])};
  }

  inline int Size(){
    return offset.size() - 1; 
  }

  MetaInfo& Info() override{
      return info_;
  }
  const MetaInfo& Info() const override{
      return info_;
  }
};

/*
 * Dense matrix stores only binid, which is one byte
 */
#ifdef USE_COMPACT_BINID
typedef unsigned char BinIDType;
#else
typedef unsigned int BinIDType;
#endif


class DMatrixCompactColDense {
    public:
        const BinIDType* data_;
        size_t len_;

    DMatrixCompactColDense(const BinIDType* data, size_t len):
        data_(data), len_(len){}

  inline unsigned int _binid(size_t i) const {
    return static_cast<unsigned int>(data_[i]);
  }
  inline unsigned int _index(size_t i) const {
    return i;
  }
  inline size_t size() const{
    return len_;
  }

};

class DMatrixCompactDense : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<BinIDType> data;
     std::vector<size_t> offset;
     MetaInfo info_;

 public:
  explicit DMatrixCompactDense(){}

  //initialize
  void Init(const SparsePage& page, MetaInfo& info);

  //using Inst = common::Span<EntryCompact const>;

  inline DMatrixCompactColDense operator[](size_t i) const {
    return {data.data() + offset[i],
            static_cast<size_t>(offset[i + 1] - offset[i])};
  }

  inline int Size(){
    return offset.size() - 1; 
  }

  MetaInfo& Info() override{
      return info_;
  }
  const MetaInfo& Info() const override{
      return info_;
  }
};

/*
 * Block Dense matrix stores only binid, which is one byte
 */
class DMatrixCompactColBlockDense {
    private:
        const BinIDType* data_;
        size_t len_;
        size_t base_rowid_;
        //int fid_;

    public:

    DMatrixCompactColBlockDense(const BinIDType* data, size_t len, size_t base):
        data_(data), len_(len), base_rowid_{base}{}

  //block interface
  inline size_t getBlockNum(size_t blockSize) const{
    return (blockSize<=0)? 1: len_ / blockSize + ((len_%blockSize)?1:0);
  }

  inline DMatrixCompactColBlockDense getBlock(size_t blockid, size_t blockSize) const {
    if ( blockSize <= 0 )
        return {data_, len_, 0};
    else
        return {data_ + static_cast<size_t>(blockid * blockSize),
            static_cast<size_t>( ((blockid+1)*blockSize > len_)? len_ - blockid*blockSize: blockSize),
            blockid*blockSize};
  }

  void setEmpty(){
      len_ = 0;
  }

  //elem interface
  inline unsigned int _binid(size_t i) const {
    return static_cast<unsigned int>(data_[i]);
  }
  inline unsigned int _index(size_t i) const {
    return base_rowid_ + i;
  }
  inline size_t size() const{
    return len_;
  }

};

class DMatrixCompactBlockDense : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<BinIDType> data;
     std::vector<size_t> offset;
     MetaInfo info_;

 public:
  explicit DMatrixCompactBlockDense(){}

  //initialize
  void Init(const SparsePage& page, MetaInfo& info);

  //using Inst = common::Span<EntryCompact const>;

  inline DMatrixCompactColBlockDense operator[](size_t i) const {
    return {data.data() + offset[i],
            static_cast<size_t>(offset[i + 1] - offset[i]), 0};
  }

  inline int Size(){
    return offset.size() - 1; 
  }

  MetaInfo& Info() override{
      return info_;
  }
  const MetaInfo& Info() const override{
      return info_;
  }
};

/*
 * General Block-based Matrix
 * 3-d cube with 
 *      base block are <binid, fid> 
 *      z column are rowid
 * It's a sparse structure
 *      rowid: <blockadd=binid:fid>
 */

typedef unsigned short BlkAddrType;

struct BlockInfo{
    // 3-D cube <rowid, fid, binid>
    unsigned int row_blksize;
    unsigned int ft_blksize;
    unsigned int bin_blksize;

    BlockInfo() = default;

    BlockInfo(unsigned int rowBlkSize, unsigned int ftBlkSize, 
            unsigned int binBlkSize):row_blksize(rowBlkSize),
            ft_blksize(ftBlkSize),bin_blksize(binBlkSize){}

    inline unsigned int GetRowBlkSize(){return row_blksize;}
    inline unsigned int GetFeatureBlkSize(){return ft_blksize;}
    inline unsigned int GetBinBlkSize(){return bin_blksize;}

    //init
    void init(int rownum, int ftnum, int binnum){
        //reset to maxvalue if size==0
        if (row_blksize <= 0){
            row_blksize = rownum;
        }
        if (ft_blksize <= 0){
            ft_blksize = ftnum;
        }
        if (bin_blksize <= 0){
            bin_blksize = binnum;
        }
    }
};

class DMatrixCubeBlock {
    //
    // Access by iterating on the blk row index
    // binid is now a general concept of 'blkaddr' inside one block on the binid-fid plain
    //
    public:
        const BlkAddrType* data_;
        const size_t* row_offset_;
        size_t len_;

        size_t base_rowid_;

    DMatrixCubeBlock(const BlkAddrType* data, const size_t* offset,
            size_t len, size_t base):
        data_(data), row_offset_(offset), len_(len), base_rowid_{base}{}

    //elem interface
    inline int rowsize(size_t i) const{
        return row_offset_[i+1] - row_offset_[i];
    }

    inline BlkAddrType _blkaddr(size_t i, size_t j) const {
      return static_cast<unsigned int>(data_[row_offset_[i] + j]);
    }

    // get rowid
    inline unsigned int _index(size_t i) const {
      return base_rowid_ + i;
    }
    inline size_t size() const{
      return len_;
    }

};


class DMatrixCubeZCol{
  private:
     int    blkid_;
     std::vector<BlkAddrType> data_;
     std::vector<size_t> row_offset_;
     std::vector<size_t> blk_offset_;

  public:

     DMatrixCubeZCol() = default;

    inline DMatrixCubeBlock GetBlock(size_t i) const {
        // blk_offset_ : idx in row_offset_
        // row_offset_ : addr in data_

        int rowidx = blk_offset_[i];

        //have to use the ptr from beginning
        //as row_offset_[] will pointer to this absolute address
        //return {data_.data() + row_offset_[rowidx],
        return {data_.data(),
            row_offset_.data() + rowidx,
            static_cast<size_t>(blk_offset_[i + 1] - blk_offset_[i]), 
            rowidx};
    }

    inline int GetBlockNum() const{
        return blk_offset_.size() - 1;
    }

    inline long getMemSize(){
        return sizeof(BlkAddrType)*data_.size() + row_offset_.size()*sizeof(size_t) +
            blk_offset_.size()*sizeof(size_t) + 4;
    }

    int init(int blkid){
        blkid_ = blkid;
        data_.clear();
        row_offset_.clear();
        blk_offset_.clear();
        //row_offset_.push_back(0);
        //blk_offset_.push_back(0);
    }

    inline void addrow(){
        row_offset_.push_back(data_.size());
    }

    inline void addblock(){
        blk_offset_.push_back(row_offset_.size());
    }

    inline void append(BlkAddrType blkaddr){
        data_.push_back(blkaddr);
    }

};

class DMatrixCube : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<DMatrixCubeZCol> data_;
     MetaInfo info_;

 public:
  explicit DMatrixCube(){}

  //interface for reading access in bulidhist
  inline const DMatrixCubeZCol& GetBlockZCol(size_t i) const {
    return data_[i];
  }

  inline int GetBlockNum() const{
    return data_.size();
  }
  //interface for compatible
  inline int Size() const{
    return data_.size();
  }
  inline const DMatrixCubeZCol& operator[](size_t i) const {
    return data_[i];
  }



  //interface for building the matrix
  void Init(const SparsePage& page, MetaInfo& info, int maxbins, BlockInfo& blkInfo);
  inline void addrow(){
      for(unsigned int i=0; i< data_.size(); i++){
          data_[i].addrow();
      }
  }

  inline void addblock(){
      for(unsigned int i=0; i< data_.size(); i++){
          data_[i].addblock();
      }
  }

  long getMemSize(){
      long memsize = 0; 
      for(unsigned int i=0; i< data_.size(); i++){
          memsize += data_[i].getMemSize();
      }
      return memsize;
  }

  //Info interface for compatibility
  MetaInfo& Info() override{
      return info_;
  }
  const MetaInfo& Info() const override{
      return info_;
  }
};



//}  // namespace data
}  // namespace xgboost
#endif  // XGBOOST_DATA_COMPACT_DMATRIX_H_
