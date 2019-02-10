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
#include <dmlc/base.h>
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
 * Dense BinId only Column-wised Matrix
 *  It's a sparse structure
 *      rowid: <binid>
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


  // random access interface
  inline unsigned int _binidByRowId(int rowid) const {
    return static_cast<unsigned int>(data_[rowid - base_rowid_]);
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
 * General BinId only Column-wised Matrix
 *  It's a sparse structure
 *      rowid: <binid>
*/
class DMatrixCompactColBlock {
  private:
      const EntryCompact* data_;
      size_t len_;
      size_t base_rowid_;
      //int fid_;

  public:

  DMatrixCompactColBlock(const EntryCompact* data, size_t len, size_t base):
      data_(data), len_(len), base_rowid_{base}{
  
      //todo: EntryCompact format, index has 24 bits address which is 16M
      //      for larger dataset, split into blocks and set base_rowid correspondingly
      CHECK_LT(len, 0x1000000);
      }

  /*
   * no support for block interface in sparse version, use cube instead
   */
  ////block interface
  inline size_t getBlockNum(size_t blockSize) const{return 0;}
  inline DMatrixCompactColBlock getBlock(size_t blockid, size_t blockSize) const {
        return {data_, len_, 0};
  }
  //inline size_t getBlockNum(size_t blockSize) const{
  //  return (blockSize<=0)? 1: len_ / blockSize + ((len_%blockSize)?1:0);
  //}

  //inline DMatrixCompactColBlock getBlock(size_t blockid, size_t blockSize) const {
  //  if ( blockSize <= 0 )
  //      return {data_, len_, 0};
  //  else
  //      return {data_ + static_cast<size_t>(blockid * blockSize),
  //          static_cast<size_t>( ((blockid+1)*blockSize > len_)? len_ - blockid*blockSize: blockSize),
  //          blockid*blockSize};
  //}

  void setEmpty(){
      len_ = 0;
  }

  //elem interface
  inline unsigned int _binid(size_t i) const {
    return static_cast<unsigned int>(data_[i]._binid());
  }
  inline unsigned int _index(size_t i) const {
    return base_rowid_ + data_[i]._index();
  }
  inline size_t size() const{
    return len_;
  }

};


class DMatrixCompactBlock : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<EntryCompact> data;
     std::vector<size_t> offset;
     MetaInfo info_;

 public:
  explicit DMatrixCompactBlock(){}

  //initialize
  void Init(const SparsePage& page, MetaInfo& info);

  //using Inst = common::Span<EntryCompact const>;

  inline DMatrixCompactColBlock operator[](size_t i) const {
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


typedef unsigned int PtrType;
//typedef unsigned char BlkAddrType;
//typedef unsigned short BlkAddrType;

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

    // feature blk number per row
    // ftnum = feature# + 1 (sum)
    inline unsigned int GetFeatureBlkNum(int ftnum){
        return ftnum / ft_blksize + ((ftnum % ft_blksize)?1:0);
    }
    inline unsigned int GetBinBlkNum(int binnum){
        return binnum / bin_blksize + ((binnum % bin_blksize)?1:0);
    }
    // the base plain size, the (bin, ft) plain
    inline unsigned int GetBaseBlkNum(int ftnum, int binnum){
        return GetBinBlkNum(binnum) * GetFeatureBlkNum(ftnum);
    }


    // cube size (maximum) 
    // not compact to make offset calculation easier
    inline unsigned long GetModelCubeSize(int binnum, int ftnum, int nodenum){
        return ((unsigned long)(nodenum)) * GetBinBlkNum(binnum) * bin_blksize *
               GetFeatureBlkNum(ftnum) * ft_blksize;
    }

    //calc blkadd
    inline unsigned short GetBlkAddr(int binid, int fid){
        return (binid % GetBinBlkSize()) * GetFeatureBlkSize() + fid % GetFeatureBlkSize();
    }

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

/*
 * dense cube : 
 *      when bin_block_num is smaller than 8, special case as 1
 *      no row_offset stored as in dense cube, rowsize is regular
 *      so as to blk_offset_
 */
template<typename BlkAddrType>
class DMatrixDenseCubeBlock {
    //
    // Access by iterating on the blk row index
    // binid is now a general concept of 'blkaddr' inside one block on the binid-fid plain
    //
    private:
    const BlkAddrType* data_;
    //const PtrType* row_offset_;
    const PtrType rowsize_;

    PtrType len_;
    PtrType base_rowid_;

    public:
    DMatrixDenseCubeBlock<BlkAddrType>(const BlkAddrType* data, PtrType rowsize,
            PtrType len, PtrType base):
        data_(data), rowsize_(rowsize), len_(len), base_rowid_{base}{}

    // sequential access by seq id
    inline BlkAddrType _blkaddr(int i, int j) const {
      return static_cast<BlkAddrType>(data_[rowsize_* (i + base_rowid_) + j]);
    }
    inline int rowsize(int i) const{
        //return row_offset_[i+1] - row_offset_[i];
        return rowsize_;
    }

    // get rowid
    inline unsigned int _index(int i) const {
      return base_rowid_ + i;
    }
    inline size_t size() const{
      return len_;
    }

    // random access by raw rowid
    inline BlkAddrType _blkaddrByRowId(int ridx, int j) const {
      return static_cast<BlkAddrType>(data_[rowsize_*ridx + j]);
    }
    inline int rowsizeByRowId(int i) const{
        return rowsize_;
    }
   
};


template<typename BlkAddrType>
class DMatrixDenseCubeZCol{
  private:
     int    blkid_;
     std::vector<BlkAddrType> data_;
     //std::vector<PtrType> row_offset_;
     std::vector<PtrType> blk_offset_;
     PtrType rowsize_;
     PtrType rowcnt_;
     //PtrType blksize_;

  public:

     DMatrixDenseCubeZCol() = default;

    inline DMatrixDenseCubeBlock<BlkAddrType> GetBlock(int i) const {
        // blk_offset_ : idx in row_offset_
        // row_offset_ : addr in data_
        //if (blk_offset_.size() > i){
        if (data_.size() > 0){

            PtrType rowidx = blk_offset_[i];

            //use the ptr from beginning
            return {data_.data(),
                rowsize_,
                static_cast<PtrType>(blk_offset_[i + 1] - blk_offset_[i]), 
                rowidx};
        }
        else{
            //return empty block
            return {nullptr,0,0,0};
        }
    }

    inline int GetBlockNum() const{
        return blk_offset_.size() - 1;
    }

    inline long getMemSize(){
        return sizeof(BlkAddrType)*data_.size() + 
            //row_offset_.size()*sizeof(PtrType) +
            2*sizeof(PtrType) +
            blk_offset_.size()*sizeof(PtrType) + 4;
    }

    int init(int blkid){
        blkid_ = blkid;
        data_.clear();
        //row_offset_.clear();
        blk_offset_.clear();
        //row_offset_.push_back(0);
        //blk_offset_.push_back(0);
        
        rowsize_ = 0;
        rowcnt_ = 0;
    }

    inline void addrow(){
        if (rowsize_ == 0){
            rowsize_ = data_.size();
        }

        //assert
        CHECK_EQ(rowcnt_ * rowsize_, data_.size());
        rowcnt_ ++;

        ////sort blkaddr
        //if (lastrow_offset_ > 0){
        //    auto start = data_.begin() + lastrow_offset_;
        //    auto end  = data_.begin() + data_.size();
        //    std::sort(start, end);
        //}
        //lastrow_offset_ = data_.size();
    }

    inline void addblock(){
        //blk_offset_.push_back(row_offset_.size());
        blk_offset_.push_back(rowcnt_);
    }

    inline void append(BlkAddrType blkaddr){
        data_.push_back(blkaddr);
    }

    //debug info
    inline int getRowSize(){return rowsize_;}
    inline int getDataSize(){return data_.size();}

    //save/load interface
    void save(dmlc::Stream* fs){
        fs->Write((&blkid_), sizeof(blkid_));
        fs->Write((&rowsize_), sizeof(rowsize_));
        fs->Write((&rowcnt_), sizeof(rowcnt_));
        //vectors
        int vecSize = data_.size();
        fs->Write((&vecSize), sizeof(vecSize));
        fs->Write((data_.data()), 
                sizeof(BlkAddrType)*vecSize);
        vecSize = blk_offset_.size();
        fs->Write((&vecSize), sizeof(vecSize));
        fs->Write((blk_offset_.data()), 
                sizeof(PtrType)*vecSize);
    }

    void load(dmlc::Stream* fs){
        fs->Read((&blkid_), sizeof(blkid_));
        fs->Read((&rowsize_), sizeof(rowsize_));
        fs->Read((&rowcnt_), sizeof(rowcnt_));
        //vectors
        int vecSize;
        fs->Read((&vecSize), sizeof(vecSize));
        data_.resize(vecSize);
        fs->Read((data_.data()), 
                sizeof(BlkAddrType)*vecSize);

        fs->Read((&vecSize), sizeof(vecSize));
        blk_offset_.resize(vecSize);
        fs->Read((blk_offset_.data()), 
                sizeof(PtrType)*vecSize);
    }

};

template<typename BlkAddrType>
class DMatrixDenseCube : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<DMatrixDenseCubeZCol<BlkAddrType>> data_;
     MetaInfo info_;
     BlockInfo blkInfo_;

 public:
  explicit DMatrixDenseCube(){}

  //interface for reading access in bulidhist
  inline DMatrixDenseCubeZCol<BlkAddrType>& GetBlockZCol(unsigned int i) {
    return data_[i];
  }

  inline int GetBaseBlockNum() const{
    return data_.size();
  }
  inline int GetBlockNum() const{
    return data_.size() * data_[0].GetBlockNum();
  }
  //interface for compatible
  inline int Size() const{
    return data_.size();
  }
  inline const DMatrixDenseCubeZCol<BlkAddrType>& operator[](unsigned int i) const {
    return data_[i];
  }



  //interface for building the matrix
  //void Init(const SparsePage& page, MetaInfo& info, int maxbins, BlockInfo& blkInfo);
    void Init(const SparsePage& page, MetaInfo& info, int num_maxbins, BlockInfo& blkInfo){
        //save the info
        //shallow copy only the num_
        info_.num_row_ = info.num_row_;
        info_.num_col_ = info.num_col_;
        info_.num_nonzero_ = info.num_nonzero_;
    
    #ifdef USE_VECTOR4MODEL
        blkInfo.init(info_.num_row_, info_.num_col_ + 1, num_maxbins);
    #else
        blkInfo.init(info_.num_row_, info_.num_col_, num_maxbins);
    #endif

        // todo, to check match in loading data from binary file
        //save it
        blkInfo_ = blkInfo;
    
        //init 
        data_.clear();
        int row_blknum = (info.num_row_ + blkInfo.GetRowBlkSize() - 1)/ blkInfo.GetRowBlkSize(); 
    #ifdef USE_VECTOR4MODEL
        int fid_blknum = (info.num_col_ + 1 + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    #else
        int fid_blknum = (info.num_col_ + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    #endif
        int binid_blknum = (num_maxbins + blkInfo.GetBinBlkSize() - 1)/ blkInfo.GetBinBlkSize(); 
        data_.resize(fid_blknum * binid_blknum);
        //todo: init blkid if necessary
        for(int i=0; i < fid_blknum * binid_blknum; i++){
            data_[i].init(i);
        }
    
        LOG(CONSOLE) << "BlockInfo: row_blksize=" << blkInfo.GetRowBlkSize() <<
            ",fid_blksize=" << blkInfo.GetFeatureBlkSize() <<
            ",binid_blksize=" << blkInfo.GetBinBlkSize();
    
        LOG(CONSOLE) << "DenseCubeInit:row_blknum=" << row_blknum <<
            ",fid_blknum=" << fid_blknum << ",binid_blknum=" << binid_blknum;
    
        // rowset
        // go through all rows
        int rownum = page.Size();
        for(int rowid = 0; rowid < rownum; rowid++){
    
            auto row = page[rowid];
            //split into blks to all DMatrixDenseCubeZCol items
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
                
                // for densecube, save only binid is okay, fid is the index
                //data_[blkid].append(ins.binid);
            }
    
       }
       //add sentinel at the end
       addblock();
       addrow();
    
    
       LOG(CONSOLE) << "DMatrixDenseCube::Init" <<
            ",BlkInfo=r" << blkInfo.GetRowBlkSize() << ",f" << blkInfo.GetFeatureBlkSize() << ",b" << blkInfo.GetBinBlkSize() <<
            ",memory=" << getMemSize()/(1024*1024) << "MB" <<
            ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << "x" << num_maxbins <<
            ",nonzero=" << info_.num_nonzero_;
    
    }


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

  //save/load interface
  void save(std::string& cubeFileName){
    dmlc::Stream * fs = dmlc::Stream::Create(cubeFileName.c_str(), "w");
    //save zol vector
    int vecSize = data_.size();
    fs->Write(&vecSize, sizeof(vecSize));
    for(int i = 0; i < vecSize; i++){
        //write each zcol
        data_[i].save(fs);
    }

    //save metainfo
    info_.SaveBinary(fs);
    delete fs;
  }

  void load(std::string& cubeFileName, int num_maxbins, BlockInfo& blkInfo){
    dmlc::Stream * fs = dmlc::Stream::Create(cubeFileName.c_str(), "r");
    //load zol vector
    int vecSize;
    fs->Read(&vecSize, sizeof(vecSize));
    data_.resize(vecSize);
    for(int i = 0; i < vecSize; i++){
        //read each zcol
        data_[i].load(fs);
    }

    //load metainfo
    info_.LoadBinary(fs);

    delete fs;

    blkInfo.init(info_.num_row_, info_.num_col_, num_maxbins);

    //output the init msg
    LOG(CONSOLE) << "BlockInfo: row_blksize=" << blkInfo.GetRowBlkSize() <<
        ",fid_blksize=" << blkInfo.GetFeatureBlkSize() <<
        ",binid_blksize=" << blkInfo.GetBinBlkSize();

    int row_blknum = (info_.num_row_ + blkInfo.GetRowBlkSize() - 1)/ blkInfo.GetRowBlkSize(); 
    int fid_blknum = (info_.num_col_ + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    int binid_blknum = (num_maxbins + blkInfo.GetBinBlkSize() - 1)/ blkInfo.GetBinBlkSize(); 
    
    LOG(CONSOLE) << "DenseCubeInit:row_blknum=" << row_blknum <<
        ",fid_blknum=" << fid_blknum << ",binid_blknum=" << binid_blknum;
 
    //verify
    CHECK_EQ(fid_blknum*binid_blknum, GetBaseBlockNum());
    CHECK_EQ(row_blknum, GetBlockZCol(0).GetBlockNum());


    LOG(CONSOLE) << "DMatrixDenseCube::Init" <<
         ",BlkInfo=r" << blkInfo.GetRowBlkSize() << ",f" << blkInfo.GetFeatureBlkSize() << ",b" << blkInfo.GetBinBlkSize() <<
         ",memory=" << getMemSize()/(1024*1024) << "MB" <<
         ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << "x" << num_maxbins <<
         ",nonzero=" << info_.num_nonzero_;
  }

};


/*
 * general cube supporint sparse dataset
 * todo:
 *      runlenght encoding of rowid
 *
 */

template<typename BlkAddrType>
class DMatrixCubeBlock {
    //
    // Access by iterating on the blk row index
    // binid is now a general concept of 'blkaddr' inside one block on the binid-fid plain
    //
  private:
    const BlkAddrType* data_;
    const PtrType* row_offset_;
    PtrType len_;
    PtrType base_rowid_;

  public:
    DMatrixCubeBlock<BlkAddrType>(const BlkAddrType* data, const PtrType* offset,
            PtrType len, PtrType base):
        data_(data), row_offset_(offset), len_(len), base_rowid_{base}{}

    // sequential access by seq id
    inline BlkAddrType _blkaddr(int i, int j) const {
      return static_cast<BlkAddrType>(data_[row_offset_[i + base_rowid_] + j]);
    }
    inline int rowsize(int i) const{
        return row_offset_[i + base_rowid_ +1] - row_offset_[i + base_rowid_];
    }

    // get rowid
    inline unsigned int _index(int i) const {
      return base_rowid_ + i;
    }
    inline size_t size() const{
      return len_;
    }

    // random access by raw rowid
    inline BlkAddrType _blkaddrByRowId(int ridx, int j) const {
      return static_cast<BlkAddrType>(data_[row_offset_[ridx] + j]);
    }
    inline int rowsizeByRowId(int ridx) const{
        return row_offset_[ridx+1] - row_offset_[ridx];
    }


};


template<typename BlkAddrType>
class DMatrixCubeZCol{
  private:
     int    blkid_;
     std::vector<BlkAddrType> data_;
     std::vector<PtrType> row_offset_;
     std::vector<PtrType> blk_offset_;

  public:

     DMatrixCubeZCol() = default;

    inline DMatrixCubeBlock<BlkAddrType> GetBlock(int i) const {
        // blk_offset_ : idx in row_offset_
        // row_offset_ : addr in data_

        if (data_.size() > 0){
            PtrType rowidx = blk_offset_[i];

            //have to use the ptr from beginning
            //as row_offset_[] will pointer to this absolute address
            return {data_.data(),
                row_offset_.data(),
                static_cast<PtrType>(blk_offset_[i + 1] - blk_offset_[i]), 
                rowidx};
        }
        else{
            //return empty block
            return {nullptr,0,0,0};
        }
        
    }

    inline int GetBlockNum() const{
        return blk_offset_.size() - 1;
    }

    inline long getMemSize(){
        return sizeof(BlkAddrType)*data_.size() + row_offset_.size()*sizeof(PtrType) +
            blk_offset_.size()*sizeof(PtrType) + 4;
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
        //sort blkaddr
        if (row_offset_.size() > 0){
            auto start = data_.begin() + row_offset_.back();
            auto end  = data_.begin() + data_.size();
            std::sort(start, end);
        }
        row_offset_.push_back(data_.size());
    }

    inline void addblock(){
        blk_offset_.push_back(row_offset_.size());
    }

    inline void append(BlkAddrType blkaddr){
        data_.push_back(blkaddr);
    }

    //debug info
    inline int getRowSize(){return -1;}
    inline int getDataSize(){return data_.size();}

    //save/load interface
    void save(dmlc::Stream* fs){
        fs->Write((&blkid_), sizeof(blkid_));
        //vectors
        int vecSize = data_.size();
        fs->Write((&vecSize), sizeof(vecSize));
        fs->Write((data_.data()), 
                sizeof(BlkAddrType)*vecSize);

        vecSize = row_offset_.size();
        fs->Write((&vecSize), sizeof(vecSize));
        fs->Write((row_offset_.data()), 
                sizeof(PtrType)*vecSize);

        vecSize = blk_offset_.size();
        fs->Write((&vecSize), sizeof(vecSize));
        fs->Write((blk_offset_.data()), 
                sizeof(PtrType)*vecSize);
        
    }

    void load(dmlc::Stream* fs){
        fs->Read((&blkid_), sizeof(blkid_));
        //vectors
        int vecSize;
        fs->Read((&vecSize), sizeof(vecSize));
        data_.resize(vecSize);
        fs->Read((data_.data()), 
                sizeof(BlkAddrType)*vecSize);

        fs->Read((&vecSize), sizeof(vecSize));
        row_offset_.resize(vecSize);
        fs->Read((row_offset_.data()), 
                sizeof(PtrType)*vecSize);

        fs->Read((&vecSize), sizeof(vecSize));
        blk_offset_.resize(vecSize);
        fs->Read((blk_offset_.data()), 
                sizeof(PtrType)*vecSize);
        
    }



};

template<typename BlkAddrType>
class DMatrixCube : public xgboost::data::SparsePageDMatrix {
 
 private:
     std::vector<DMatrixCubeZCol<BlkAddrType>> data_;
     MetaInfo info_;

 public:
  explicit DMatrixCube(){}

  //interface for reading access in bulidhist
  inline DMatrixCubeZCol<BlkAddrType>& GetBlockZCol(unsigned int i){
    return data_[i];
  }

  inline int GetBaseBlockNum() const{
    return data_.size();
  }
  inline int GetBlockNum() const{
    return data_.size() * data_[0].GetBlockNum();
  }
  //interface for compatible
  inline int Size() const{
    return data_.size();
  }
  inline const DMatrixCubeZCol<BlkAddrType>& operator[](unsigned int i) const {
    return data_[i];
  }



  //interface for building the matrix
  //void Init(const SparsePage& page, MetaInfo& info, int maxbins, BlockInfo& blkInfo);
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

    void Init(const SparsePage& page, MetaInfo& info, int num_maxbins, BlockInfo& blkInfo){
        //save the info
        //shallow copy only the num_
        info_.num_row_ = info.num_row_;
        info_.num_col_ = info.num_col_;
        info_.num_nonzero_ = info.num_nonzero_;
    
    #ifdef USE_VECTOR4MODEL
        blkInfo.init(info.num_row_, info_.num_col_ + 1, num_maxbins);
    #else
        blkInfo.init(info.num_row_, info_.num_col_, num_maxbins);
    #endif
    
    
        //init 
        data_.clear();
        int row_blknum = (info.num_row_ + blkInfo.GetRowBlkSize() - 1)/ blkInfo.GetRowBlkSize(); 
     #ifdef USE_VECTOR4MODEL
        int fid_blknum = (info.num_col_ + 1 + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    #else
        int fid_blknum = (info.num_col_ + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    #endif
       
        int binid_blknum = (num_maxbins + blkInfo.GetBinBlkSize() - 1)/ blkInfo.GetBinBlkSize(); 
        data_.resize(fid_blknum * binid_blknum);
        //todo: init blkid if necessary
        for(int i=0; i < fid_blknum * binid_blknum; i++){
            data_[i].init(i);
        }
    
        LOG(CONSOLE) << "BlockInfo: row_blksize=" << blkInfo.GetRowBlkSize() <<
            ",fid_blksize=" << blkInfo.GetFeatureBlkSize() <<
            ",binid_blksize=" << blkInfo.GetBinBlkSize();
    
        LOG(CONSOLE) << "CubeInit:row_blknum=" << row_blknum <<
            ",fid_blknum=" << fid_blknum << ",binid_blknum=" << binid_blknum;
    
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
    
  //save/load interface
  void save(std::string& cubeFileName){
    dmlc::Stream * fs = dmlc::Stream::Create(cubeFileName.c_str(), "w");
    //save zol vector
    int vecSize = data_.size();
    fs->Write(&vecSize, sizeof(vecSize));
    for(int i = 0; i < vecSize; i++){
        //write each zcol
        data_[i].save(fs);
    }

    //save metainfo
    info_.SaveBinary(fs);
    delete fs;
  }

  void load(std::string& cubeFileName, int num_maxbins, BlockInfo& blkInfo){
    dmlc::Stream * fs = dmlc::Stream::Create(cubeFileName.c_str(), "r");
    //load zol vector
    int vecSize;
    fs->Read(&vecSize, sizeof(vecSize));
    data_.resize(vecSize);
    for(int i = 0; i < vecSize; i++){
        //read each zcol
        data_[i].load(fs);
    }

    //load metainfo
    info_.LoadBinary(fs);

    delete fs;

    blkInfo.init(info_.num_row_, info_.num_col_, num_maxbins);

    //output the init msg
    LOG(CONSOLE) << "BlockInfo: row_blksize=" << blkInfo.GetRowBlkSize() <<
        ",fid_blksize=" << blkInfo.GetFeatureBlkSize() <<
        ",binid_blksize=" << blkInfo.GetBinBlkSize();

    int row_blknum = (info_.num_row_ + blkInfo.GetRowBlkSize() - 1)/ blkInfo.GetRowBlkSize(); 
    int fid_blknum = (info_.num_col_ + blkInfo.GetFeatureBlkSize() - 1)/ blkInfo.GetFeatureBlkSize(); 
    int binid_blknum = (num_maxbins + blkInfo.GetBinBlkSize() - 1)/ blkInfo.GetBinBlkSize(); 
    
    LOG(CONSOLE) << "CubeInit:row_blknum=" << row_blknum <<
        ",fid_blknum=" << fid_blknum << ",binid_blknum=" << binid_blknum;
 
    //verify
    CHECK_EQ(fid_blknum*binid_blknum, GetBaseBlockNum());
    CHECK_EQ(row_blknum, GetBlockZCol(0).GetBlockNum());


    LOG(CONSOLE) << "DMatrixCube::Init" <<
         ",BlkInfo=r" << blkInfo.GetRowBlkSize() << ",f" << blkInfo.GetFeatureBlkSize() << ",b" << blkInfo.GetBinBlkSize() <<
         ",memory=" << getMemSize()/(1024*1024) << "MB" <<
         ",rowxcol=" << info_.num_row_ << "x" << info_.num_col_ << "x" << num_maxbins <<
         ",nonzero=" << info_.num_nonzero_;
  }



};



//}  // namespace data
}  // namespace xgboost
#endif  // XGBOOST_DATA_COMPACT_DMATRIX_H_
