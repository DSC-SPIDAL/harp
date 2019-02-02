/*!
 * Copyright 2017 by Contributors
 * \file row_set.h
 * \brief Quick Utility to compute subset of rows
 * \author Philip Cho, Tianqi Chen
 *
 * 2018,2019
 * HARPDAAL-GBT optimize based on the approx and fast_hist codebase
 * 3-D cube of model <node_block_size, bin_block_size, ft_block_size)
 
 */
#ifndef XGBOOST_COMMON_POS_SET_LOSSGUIDE_H_
#define XGBOOST_COMMON_POS_SET_LOSSGUIDE_H_

#include <xgboost/data.h>
#include <algorithm>
#include <vector>
#include <atomic>

namespace xgboost {
namespace tree {

/*! \brief collection of posset 
 *  posset is generalized rowset which can control the granularity 
 *  of the node group size.
 *  it maintains <nodeid, rowid> mapping in groups
 *  it split the existing posset to two parts when applySplit called
 *
 *  this is a specific version of pos_set that keep single nodeid as 
 *  a group, as the same as row_set
 *
 * */


class POSSetSingle{
    public:
    struct POSEntry{
        //
        // encode the first two bits to indicate the left/right 
        // assignement in split
        //
        int _rowid;

        POSEntry() = default;

        POSEntry(int rowid):
            _rowid(rowid){
        }

        POSEntry(const POSEntry& pos){
            // clear the left, right status
            // max 25 bits address = 32M
            _rowid = pos._rowid & 0x1fffffff;
        }

        //encoded data access interface
        inline bool isDelete(){
            return (_rowid & 0x80000000) != 0;
        }
        inline bool isLeft(){
            return (_rowid & 0x40000000) != 0;
        }
        inline bool isRight(){
            return (_rowid & 0x20000000) != 0;
        }

        inline void setEncodePosition(bool left = false){
            if(left){
                _rowid |= 0x40000000;
            }
            else{
                _rowid |= 0x20000000;
            }
        }
        inline void setDelete(){
            _rowid |= 0x80000000;
        }
        inline int getRowId(){
            return _rowid & 0x1fffffff;
        }
    };
    
    /*! \brief POSEntry set with the same set of node ids
     *  only for leaves
     *
     *  single nodeid in one group
     */
    struct POSGroup{
        POSEntry* _start;
        int _nodeid;
        int _len;
        // split 
        bool _defaultLeft;
        int _leftlen;
        int _rightlen;
        int _leftid;
        int _rightid;

        POSGroup(){
            _start = nullptr;
            _len = 0;
            _nodeid = -1;

            _defaultLeft = -1;
            _leftlen = 0;
            _rightlen = 0;
        }

        POSGroup(int nodeid, POSEntry* start, int len):
            _nodeid(nodeid),_start(start),_len(len){
            _defaultLeft = -1;
            _leftlen = 0;
            _rightlen = 0;
        }

        //copy constructor as there are atomics
        POSGroup(const POSGroup& grp){
            _start = grp._start;
            _len = grp._len;
            _nodeid = grp._nodeid;

            _defaultLeft = -1;
            _leftlen = 0;
            _rightlen = 0;
        }

        inline int size(){
            return _len;
        }

        //
        // update procedure, to collect upate statistics
        //  BeginUpdate(depth)
        //      call setDefaultPosition
        //      call CorrectNoneDefaultPosi
        //      ...
        //  EndUpdate()
        //
        inline void BeginUpdate(int leftId, int rightId,bool defaultLeft){
            _leftlen = 0;
            _rightlen = 0;
            _leftid = leftId;
            _rightid = rightId;
            _defaultLeft = defaultLeft;
        }
        inline void setLeftPosition(int i){
            _start[i].setEncodePosition(true);
            _leftlen ++;
        }
        inline void setRightPosition(int i){
            _start[i].setEncodePosition(false);
            _rightlen ++;
        }


        inline void EndUpdate(){
            //if (_nodeid < 0) return;
            if (_defaultLeft){
                // rightlen is accurate
                _leftlen = _len - _rightlen;
            }
            else{
                // leftlen is accurate
                _rightlen = _len - _leftlen;
            }
 
            #ifdef USE_DEBUG
            LOG(CONSOLE) << "EndUpdate:[" << id << "]" << 
                ",leftlen=" << _leftlen <<
                ",rightlen=" << _rightlen <<
                ",len=" << _len;
            #endif
        }

        //it's bad to expost internal data structures
        //inline POSEntry& operator[](int i) {
        //    //no check the boundary here
        //    //CHECK_LT(i, _len);
        //    return _start[i];
        //}
        inline bool isLeft(int i){
            return _start[i].isLeft();
        }
        inline bool isDelete(int i){
            return _start[i].isDelete();
        }
        inline bool setDelete(int i){
            _start[i].setDelete();
        }
        inline int getRowId(int i){
            return _start[i].getRowId();
        }

        // general read access
        inline void setDummy(){
            _start = nullptr;
        }
        // dummy is different to delete node
        inline bool isDummy(){
            return _start == nullptr;
        }
        inline bool isDelete(){
            return _nodeid < 0;
        }
        inline void setDelete(){
            //_nodeid = _nodeid | 0x80000000;
            _nodeid = ~_nodeid;
        }
        inline int getEncodePosition() {
            return (_nodeid < 0)? (~_nodeid) : _nodeid;
        }
        inline int getNodeId() {
            return _nodeid;
        }
        inline int getLeftLen(){
            return _leftlen;
        }
        inline int getRightLen(){
            return _rightlen;
        }
        inline POSEntry* getStartPtr(){
            return _start;
        }
 
        
        //
        // remove deleted rows
        //
        int Prune(POSEntry* start){

            #ifdef USE_DEBUG
            LOG(CONSOLE) << "Prune::[" << 
                getEncodePosition() << "],type=" << isDelete()?'D':'N' << ",defaultLeft=" << _defaultLeft <<
                ",len=" << _len;
            #endif

            int w = 0, d = 0;
            for (int i = 0 ; i < _len; i++){
                // check if deleted
                if (_start[i].isDelete()){
                    d++;
                }
                else{
                    start[w++] = _start[i];
                }
            }

            //debug 
            CHECK_EQ(w+d, _len);
            
            // update this row set
            //  replace the entry point
            //  erite to new place
            //POSGroup leftGrp(_nodeid, start, w);
            _len = w;
            if (w > 0){
                _start = start;
            }
            else{
                //empty row set
                setDummy();
                setDelete();
            }
        }

        //
        // apply split at the group level
        // call after EndUpdate() when _halflen is set correctly
        // all nodeid have been updated to new nids
        //
        int ApplySplit(POSEntry* start, std::vector<POSGroup>& group){

            #ifdef USE_DEBUG
            LOG(CONSOLE) << "ApplySplit::[" << 
                getEncodePosition() << "],type=" << isDelete()?'D':'N' << ",defaultLeft=" << _defaultLeft <<
                ",leftlen=" << _leftlen <<
                ",rightlen=" << _rightlen <<
                ",len=" << _len;
            #endif

            if (isDelete()) return 0;

            //scan and write
            POSEntry* left = start;
            POSEntry* right = start + _leftlen;
            int l = 0, r = 0;
            for (int i = 0 ; i < _len; i++){

                if (_defaultLeft){
                    //default goes to left
                    if (_start[i].isRight()){
                        right[r++] = _start[i];
                    }
                    else{
                        left[l++] = _start[i];
                    }
                }
                else{
                    //default goes to right
                    if (_start[i].isLeft()){
                        left[l++] = _start[i];
                    }
                    else{
                        right[r++] = _start[i];
                    }
                }
            }

            //debug 
            CHECK_EQ(l+r, _len);
            //write to new place
            POSGroup leftGrp(_leftid, start, _leftlen);
            POSGroup rightGrp(_rightid, start + _leftlen, _rightlen);


            //
            // no push empty group verion
            //
            if (_leftlen > 0) group[_leftid] = leftGrp;
            if (_rightlen > 0) group[_rightid] = rightGrp;

            return 1;
        }

    };

    private:
    /*! \brief POSGroup set 
     */
    // static allocation of memory
    std::vector<POSEntry> entry_[2];
    // flat group set
    std::vector<std::vector<POSGroup>> grp_;

    int nodenum_;
    int rownum_;
    int rowblknum_;
    int rowblksize_;

    public:
    POSSetSingle() = default;


    void Init(int rownumber, int nodenum, int row_block_size = 0){
        //Init entry first
        rownum_ = rownumber;
        //init from nodeid=0
        entry_[0].resize(rownumber);
        entry_[1].resize(rownumber);
        for(int i = 0; i < rownumber; i++){
            entry_[0][i] = POSEntry(i);
        }

        rowblknum_ = 1;
        if (row_block_size > 0){
            rowblknum_ = (rownumber + row_block_size -1 )/ row_block_size;
            rowblksize_ = row_block_size;
        }
        else{
            rowblknum_ = 1;
            rowblksize_ = rownum_;
        }

        // 
        // Simple implementation for direct index access
        // full groups for each block
        //
        grp_.resize(rowblknum_);

        for(int i; i < rowblknum_; i++){
            grp_[i].resize(nodenum);
        }
        const int omp_loop_size = rowblknum_ * nodenum;
        #pragma omp parallel for schedule(static) if (omp_loop_size >= 1024) 
        for(int i = 0; i < omp_loop_size; i++){
            const int blkid = i / nodenum;
            const int nodeid = i % nodenum;
            grp_[blkid][nodeid].setDummy();
        }

        //init one group for each block
        POSEntry* start = dmlc::BeginPtr(entry_[0]);
        for (int i = 0; i < rowblknum_; i++){
            int blocklen;
            if (i == rowblknum_ -1){
                blocklen = rownum_ % rowblksize_;
                if (blocklen==0) blocklen = rowblksize_;
            }
            else{
                blocklen = rowblksize_;
            }

            //
            // set the first group of node 0
            //
            grp_[i][0] = POSGroup(0, start + i*row_block_size, blocklen);

        }

        //debug 
        //printGroupBlocks();
    }

    void Clear(){
        //entry_[0].clear();
        //entry_[1].clear();

        //todo: reset all grp to dummy
        grp_.clear();
    }

    // general access
    inline int getBlockNum(){
        return grp_.size();
    }
    inline int getBlockBaseRowId(int blkid){
        return blkid * rowblksize_;
    }
    inline POSGroup& getGroup(int nodeid, int blkid){
        return grp_[blkid][nodeid];
    }
    inline std::vector<POSGroup>& getGroupSet(int blkid){
        return grp_[blkid];
    }
    inline int getEntrySize(){
        return rownum_;
    }

    // for split
    //
    // return the next entry pointer for this nodeid
    //
    POSEntry* getNextEntryStart(int nodeid, int blkid){
        auto grp = getGroup(nodeid, blkid);

        POSEntry* startEntry0 = dmlc::BeginPtr(entry_[0]);
        POSEntry* endEntry0 = startEntry0 + rownum_;
        
        int curid = 1;
        if (grp.getStartPtr() >= startEntry0 &&  
            grp.getStartPtr() < endEntry0){
            curid = 0;
        }

        int startPos = grp.getStartPtr() - dmlc::BeginPtr(entry_[curid]);

        curid = (curid + 1)%2;
        POSEntry* next = dmlc::BeginPtr(entry_[curid]) + startPos;
        return next;
    }


    //debug code
    unsigned long getNodeIdSum(){
        unsigned long sum = 0L;
        //for (int i = 0 ; i < rownum_; i++){
        //    sum += entry_[workid_][i].getEncodePosition();
        //}
        return sum;
    }

    void printGroupBlocks(){
        std::cout <<"POSSET::rowblk_offset=";
        //for(int i = 0; i < std::min(getBlockCnt(), 10); i++){
        //    int gid = getBlockStartGId(i);
        //    // gid, startPos
        //    std::cout << gid << "," <<
        //        grp_[workid_][gid]._start -dmlc::BeginPtr(entry_[workid_]) << " ";
        //}
        std::cout << "\n";


    }

};

}  // namespace common
}  // namespace xgboost

#endif  // XGBOOST_COMMON_ROW_SET_H_
