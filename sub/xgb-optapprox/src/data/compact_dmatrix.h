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
class DMatrixCompactColDense {
    public:
        const unsigned char* data_;
        size_t len_;

    DMatrixCompactColDense(const unsigned char* data, size_t len):
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
     std::vector<unsigned char> data;
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
    public:
        const unsigned char* data_;
        size_t len_;
        size_t base_rowid_;

    DMatrixCompactColBlockDense(const unsigned char* data, size_t len, size_t base):
        data_(data), len_(len), base_rowid_{base}{}

  //block interface
  inline size_t getBlockNum(size_t blockSize) const{
    return len_ / blockSize + ((len_%blockSize)?1:0);
  }

  inline DMatrixCompactColBlockDense getBlock(size_t blockid, size_t blockSize) const {
    return {data_ + static_cast<size_t>(blockid * blockSize),
            static_cast<size_t>( ((blockid+1)*blockSize > len_)? len_ - blockid*blockSize: blockSize),
            blockid*blockSize};
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
     std::vector<unsigned char> data;
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


//}  // namespace data
}  // namespace xgboost
#endif  // XGBOOST_DATA_COMPACT_DMATRIX_H_
