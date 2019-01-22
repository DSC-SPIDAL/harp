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
#ifndef XGBOOST_COMMON_POS_SET_SINGLE_H_
#define XGBOOST_COMMON_POS_SET_SINGLE_H_

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
 * */


/*
 * This is a simple version, just to test the interface works
 */
enum class POSGroupType{
    NONE,
    LEFT,
    RIGHT,
    DUMMY
};
class POSSet{
    public:
    struct POSEntry{
        int _nodeid;
        int _rowid;

        POSEntry() = default;

        POSEntry(int nodeid, int rowid):
            _nodeid(nodeid), _rowid(rowid){
        }

        POSEntry(const POSEntry& pos){
            _nodeid = pos._nodeid;
            // clear the left, right status
            //_rowid = pos._rowid;
            _rowid = pos._rowid & 0x7fffffff;
        }

        //encoded data access interface
        inline void setDelete(){
            _nodeid = ~_nodeid;
        }
        inline bool isDelete(){
            return _nodeid < 0;
        }
        inline bool isLeft(){
            return _rowid < 0;
        }
        inline void setEncodePosition(int nid, bool left = false){
            //_nodeid = (_nodeid<0)? (~nid) : nid;
            _nodeid = nid;
            //CHECK_GE(nid, 0);

            if(left){
                _rowid |= 0x80000000;
            }
            else{
                _rowid &= 0x7fffffff;
            }
        }
        // get nodeid only
        inline int getEncodePosition() {
            return (_nodeid < 0)? (~_nodeid) : _nodeid;
        }
        // get nodeid and delete status 
        inline int getNodeId() {
            return _nodeid;
        }
        inline int getRowId(){
            return _rowid & 0x7fffffff;
        }
    };
    
    /*! \brief POSEntry set with the same set of node ids
     *  only for leaves
     */
    struct POSGroup{
        POSEntry* _start;
        int _len;
        POSGroupType _type;
        
        // todo tree structure info
        //int left;
        //int nodeid;

        // statistics of the row numbers of the left half nodes
        // use this infomation to apply split in place
#ifdef USE_ATMOIC_HAFLLEN
        std::atomic_int _leftlen;
        std::atomic_int _rightlen;
        std::atomic_int _deletelen;
#else
        int _leftlen;
        int _rightlen;
        int _deletelen;
#endif

#ifndef USE_ONENODEEACHGROUP
        // node id list for multiple node one group version
        std::vector<int> nodeids_;
        // get node id list
        inline std::vector<int>& getNodeIdList(){
            return nodeids_;
        }
#endif

        POSGroup(POSEntry* start, int len, POSGroupType type):
            _start(start),_len(len),_type(type){
        }

        //copy constructor as there are atomics
        POSGroup(const POSGroup& grp){
            _start = grp._start;
            _len = grp._len;
            _type = grp._type;
        }

        inline int size(){
            return _len;
        }
        inline bool isDummy(){
            return _type == POSGroupType::DUMMY;
        }
        inline bool isLeft(){
            return _type == POSGroupType::LEFT;
        }

       inline bool isRight(){
            return _type == POSGroupType::RIGHT;
        }

        //it's bad to expost internal data structures
        inline POSEntry& operator[](int i) {
            //no check the boundary here
            //CHECK_LT(i, _len);
            return _start[i];
        }

        //
        // update procedure, to collect upate statistics
        //  BeginUpdate(depth)
        //      call setDefaultPosition
        //      call CorrectNoneDefaultPosi
        //      ...
        //  EndUpdate()
        //
        inline void BeginUpdate(int depth){
            _leftlen = 0;
            _rightlen = 0;
            _deletelen = 0;
        }

        #ifdef USE_ATMOIC_HAFLLEN
        inline void setDelete(int i){
            _start[i].setDelete();
            _deletelen ++;
        }
        inline void setLeftPosition(int i, int nid){
            _start[i].setEncodePosition(nid, true);
            _leftlen ++;
        }
        //inline void setRightPosition(int i, int nid){
        inline void setRightPosition(int i, int nid){
            _start[i].setEncodePosition(nid, false);
            _rightlen ++;
        }

        inline void EndUpdate(int id){
            #ifdef USE_DEBUG
            LOG(CONSOLE) << "EndUpdate:[" << id << "]" << 
                ",leftlen=" << _leftlen <<
                ",rightlen=" << _rightlen <<
                ",delete=" << _deletelen <<
                ",len=" << _len;
            #endif
            //no update
            if((_deletelen == 0) && (_leftlen == 0) && (_rightlen ==0)){
                return;
            }

            // get the true len
            int remains = _len - _deletelen;
            if (remains == 0){
                //all deteled
                //CHECK_EQ(_leftlen, 0);
                //CHECK_EQ(_rightlen, 0);
            }
            else if (_leftlen > remains){
                _leftlen = remains - _rightlen;
            }
            else{
                _rightlen = remains - _leftlen;
            }
        }
        #else
        // simple scan to get the halflen
        inline void EndUpdate(int id){
            if (_type == POSGroupType::DUMMY){
                _deletelen = _len;
            }
            else{
                for (int i = 0 ; i < _len; i++){
                    if (_start[i].isDelete()){
                        _deletelen ++;
                    }
                    else if (_start[i].isLeft()){
                        _leftlen ++;
                    }
                    else{
                        _rightlen ++;
                    }
                }
            }
 
            #ifdef USE_DEBUG
            LOG(CONSOLE) << "EndUpdate:[" << id << "]" << 
                ",leftlen=" << _leftlen <<
                ",rightlen=" << _rightlen <<
                ",delete=" << _deletelen <<
                ",len=" << _len;
            #endif
        }
        #endif

        // general read access
        inline int getRowId(int i){
            return _start[i].getRowId();
        }
        inline bool isDelete(int i){
            return _start[i].isDelete();
        }
        inline bool isLeft(int i){
            return _start[i].isLeft();
        }
        inline int getEncodePosition(int i) {
            return _start[i].getEncodePosition();
        }
        inline int getNodeId(int i) {
            return _start[i].getNodeId();
        }
 
        //
        // apply split at the group level
        // call after EndUpdate() when _halflen is set correctly
        // all nodeid have been updated to new nids
        // split to two and save at the end of newgrp
        int ApplySplit(POSEntry* start, std::vector<POSGroup>& newgrp, int curgid){

            #ifdef USE_DEBUG
            LOG(CONSOLE) << "ApplySplit::[" << curgid << "],type=" << static_cast<int>(_type) << 
                ",leftlen=" << _leftlen <<
                ",rightlen=" << _rightlen << 
                ",dummylen=" << _deletelen;
            #endif

            // deleted group still should copy to new group
            if (_type == POSGroupType::DUMMY){
                POSGroup dummy(start, _len,POSGroupType::DUMMY);
                //copy data
                std::copy(_start, _start + _len, start);
                newgrp.push_back(dummy);
                return 0;
            }

            //write to new place
            POSGroup left(start, _leftlen, POSGroupType::LEFT);
            POSGroup right(start + _leftlen, _rightlen, POSGroupType::RIGHT);
            //write the deleted items for later upatepred
            POSGroup dummy(start + _leftlen + _rightlen, _deletelen,POSGroupType::DUMMY);

            //scan and write
            int l = 0, r = 0, d = 0;
            for (int i = 0 ; i < _len; i++){
                if (_start[i].isDelete()){
                    dummy[d++] = _start[i];
                }
                else if (_start[i].isLeft()){
                    left[l++] = _start[i];
                }
                else{
                    right[r++] = _start[i];
                }
            }

            //debug 
            CHECK_EQ(d+l+r, _len);

            #define USE_NOEMPTY_GROUP
            #ifdef USE_NOEMPTY_GROUP
            //
            // no push empty group verion
            //
            if (_leftlen > 0) newgrp.push_back(left);
            if (_rightlen > 0) newgrp.push_back(right);
            if (_deletelen > 0) newgrp.push_back(dummy);
            #else
            // 
            // always push left and right to make it symmetric
            //
            newgrp.push_back(left);
            newgrp.push_back(right);
            newgrp.push_back(dummy);
            #endif


            return _leftlen + _rightlen;
        }


    };
    /*! \brief POSGroup set 
     */
    //static allocation of memory
    std::vector<POSEntry> entry_[2];
    std::vector<POSGroup> grp_[2];
    int workid_;

    int rownum_;
    int base_rowid_;

    // thread local 
    std::vector<std::vector<POSGroup>> local_grp_;


    POSSet() = default;

    void Init(int rownumber, int threadnum, int start_rowid = 0){
        //clear first
        Clear();

        // thread init
        local_grp_.resize(threadnum);

        //CHECK_LE(rownumber, ROWID_MASK);

        rownum_ = rownumber;
        base_rowid_ = start_rowid;

        //init from nodeid=0
        workid_ = 0;
        entry_[0].resize(rownumber);
        entry_[1].resize(rownumber);
        for(int i = 0; i < rownumber; i++){
            entry_[0][i] = POSEntry(0,i);
        }
        grp_[0].push_back(POSGroup(dmlc::BeginPtr(entry_[0]), rownumber, POSGroupType::NONE));
    }

    void Clear(){
        base_rowid_ = 0;

        //entry_[0].clear();
        grp_[0].clear();
        //entry_[1].clear();
        grp_[1].clear();
    }

    // entry access
    inline int getEntrySize(){
        return rownum_;
    }
    inline POSEntry& getEntry(int i){
        return entry_[workid_][i];
    }

    // group access
    inline int getGroupCnt(){
        return grp_[workid_].size();
    }
    inline POSGroup& operator[](int i){
        return grp_[workid_][i];
    }


    inline void BeginUpdate(int depth){
        for (int i = 0; i < grp_[workid_].size() ; i++){
            grp_[workid_][i].BeginUpdate(depth);
        }
    }
    inline void EndUpdate(){
        #pragma omp parallel for schedule(static)
        for (int i = 0; i < grp_[workid_].size() ; i++){
            grp_[workid_][i].EndUpdate(i);
        }
    }

    //debug code
    unsigned long getNodeIdSum(){
        unsigned long sum = 0L;
        for (int i = 0 ; i < rownum_; i++){
            sum += entry_[workid_][i].getEncodePosition();
        }
        return sum;
    }

    //
    // apply split at the group level
    // call after EndUpdate() when _halflen is set correctly
    // all nodeid have been updated to new nids
    //
    void ApplySplit(){

        //create a new grp
        int nextid = (workid_ + 1)%2;
        //std::vector<POSGroup>& newgrp = grp_[nextid];
        //newgrp.clear();

        #ifdef USE_DEBUG
        unsigned long nodeid_sum_beforesplit = getNodeIdSum();
        #endif

        #pragma omp parallel for schedule(static)
        for (int i = 0; i < grp_[workid_].size() ; i++){
            int startpos = grp_[workid_][i]._start - dmlc::BeginPtr(entry_[workid_]);
            CHECK_LT(startpos, rownum_);

            //split and save result to newgrp
            grp_[workid_][i].ApplySplit(
                    dmlc::BeginPtr(entry_[nextid]) + startpos,
                    local_grp_[omp_get_thread_num()], i);
        }

        //collect from threads
        //change to newgrp
        std::vector<POSGroup>& newgrp = grp_[nextid];
        newgrp.clear();

        for(int i = 0; i < local_grp_.size(); i++){
            if (local_grp_[i].size() > 0){
                newgrp.insert(newgrp.end(), local_grp_[i].begin(), local_grp_[i].end());
                local_grp_[i].clear();
            }
        }

        workid_ = nextid;

        #ifdef USE_DEBUG
        unsigned long nodeid_sum_aftersplit = getNodeIdSum();
        LOG(CONSOLE) << "ApplySplit: nodeidSum = " << nodeid_sum_beforesplit << 
            ", after=" << nodeid_sum_aftersplit;
        #endif


    }



};




/*
        // too messy
        void ApplySplitInPlace(std::vector<POSGroup>& newgrp){


            POSGroup left(_start, _leftlen);
            POSGroup right(_start + _leftlen, _rightlen);

            // scan the two list, and resort inplace
            // some nodes are deleted
            int l, r = 0,0;
            int ls, rs = -1, -1;
            while( l < left._len && r < _len - left._len){
                //check delete nodes
                if (left[l].isDelete() || left[l].isLeft()){
                    if (left[l].isDelete() && ls == -1){
                        ls = l;
                    }
                    l++;
                    continue;
                }
                if (right[r].isDelete() || !right[r].isright()){
                    if (right[r].isDelete() && rs == -1){
                        rs = r;
                    }
                    r++;
                    continue;
                }

                //find exchaneable item here
                //check the save pos first
                if (ls == -1) ls = l;
                if (rs == -1) rs = r;

                //exchange them <l,s> but save to <ls, rs>
                POSEntry tmp = left[l];
                left[ls] = right[r];
                right[rs] = tmp;

                //move save pos forward
                if (ls == l ){
                    ls = -1;
                }
                else{
                    //move ls forward
                    while(ls < l && !left[ls].isDelete()){
                        ls ++;
                    }
                }
                if (rs == r ){
                    rs = -1;
                }
                else{
                    //move ls forward
                    while(rs < r && !right[rs].isDelete()){
                        rs ++;
                    }
                }

                //move item pos forward
                l++;
                r++;
            }
            //deal the remains
            // todo            
        }
*/

//#define NODEBLK_SPLITDEPTH  8
//#define NODEID_MASK (32 - NODEBLK_SPLITDEPTH)
//#define ROWID_MASK ((0xffffffff << NODEBLK_SPLITDEPTH) >> NODEBLK_SPLITDEPTH)
//class POSSetCompact{
//    public:
//    /*! \brief encoding <nodeid, rowid> into one integer
//     *  with NODEBLK_SPLITDEPTH = 8
//     *  nodeid 8 bits   ;256 in one group at most
//     *  rowid  24 bits  ;16M in one block at most
//     * */
//    struct POSEntry{
//        unsigned int _data;
//        POSEntry(unsigned int nodeid, unsigned int rowid){
//            _data = (nodeid << NODEID_MASK) | ( rowid & ROWID_MASK);
//        }
//    
//        inline unsigned int _nodeid() const{
//            return _data >> NODEID_MASK;
//        }
//    
//        inline unsigned int _rowid() const{
//            return _data & ROWID_MASK;
//        }
//
//
//    };
//    
//    /*! \brief POSEntry set with the same node group id
//     */
//    struct POSGroup{
//        POSEntry* _start;
//        int _len;
//        // statistics of the row numbers of the left half nodes
//        // use this infomation to apply split in place
//        int _halflen;
//
//        POSGroup(POSEntry* start, int len):_start(start),_len(len),_halflen(0){
//        }
//        
//        inline int size(){
//            return _len;
//        }
//
//        inline POSEntry& operator[](int i) {
//            //no check the boundary here
//            return _start[i];
//        }
//
//
//    };
//    /*! \brief POSGroup set 
//     */
//    std::vector<POSEntry> entry_;
//    std::vector<POSGroup> grp_;
//    int base_rowid_;
//    int split_depth_;
//
//    POSSet() = default;
//
//    void Init(int rownumber, int start_rowid = 0, int splitdepth = 8){
//        //clear first
//        Clear();
//
//        CHECK_LE(rownumber, ROWID_MASK);
//        CHECK_LE(splitdepth, NODEBLK_SPLITDEPTH);
//
//        base_rowid = start_rowid;
//        split_depth_ = splitdepth;
//
//        //init from nodeid=0
//        entry_.resize(rownumber);
//        for(int i = 0; i < rownumber; i++){
//            entry[i] = POSEntry(0,i);
//        }
//        grp_.push_back(POSGroup(dmlc::BeginPtr(entry_), rownumber));
//    }
//
//    void Clear(){
//        base_rowid = 0;
//        split_depth_ = 0;
//
//        entry_.clear();
//        grp_.clear();
//    }
//
//    unsigned int base_rowid_;
//    std::vector<POSEntry> data_;
//    std::vector<
//
//
//};


}  // namespace common
}  // namespace xgboost

#endif  // XGBOOST_COMMON_ROW_SET_H_
