/*
    Copyright (c) 2005-2018 Intel Corporation

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.




*/
#ifndef COMMON_H
#define COMMON_H 1

#include <iostream>
#include <vector>
#include <cmath>

typedef double Value;

struct DMatrix {
    int n_;
    int m_;
    float* data_;

    void init(int n, int m){
        //load the problem, X=NxM
        n_ = n; m_ = m;
        data_ = new float[n*m];
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                data_[i*m + j] = j;
                //data_[i*m + j] = 1;
            }
        }
    }

    int getBlockNum(int blockSize){
        return m_ / blockSize + ((m_%blockSize)?1:0);
    }

};

struct TreeNode {
    //block info
    int n_;
    int m_;
    float* data_;
    int base_;

    //task graph
    std::vector<TreeNode*> children_;

    //todo remove
    double sum;
    size_t off_;

    void init(DMatrix& dmat, int n, int blockid, int blockSize){
        
        children_.clear();

        if ( blockid >= 0){
            //build a block
            n_ = 1;
            m_ =  ((blockid+1)*blockSize > dmat.m_)? dmat.m_ - blockid*blockSize: blockSize;

            data_ = dmat.data_ + n*dmat.m_ + blockid*blockSize;
            off_ = n*dmat.m_ + blockid*blockSize;
            base_ = blockid*blockSize;
        }
        else{
            //root only
            n_ = m_ = -1;
            data_ =dmat.data_;
        }
    }

    void addChild(TreeNode* p){
        children_.push_back(p);
    }
    TreeNode* getChild(int id){
        return children_[id];
    }
    int getChildNum(){
        return children_.size();
    }

    static void printTree(TreeNode* root, int level){
        //print self
        std::cout << "level[" << level << "] n:" << 
            root->n_ << ",m:" << root->m_ << ",b:" << root->base_ 
            << ", off=" << root->off_ << "\n";

        //print children
        int childNum = root->getChildNum();
        level++;
        for(int i=0; i < childNum; i++){
            TreeNode* p = root->getChild(i);
            printTree(p, level);
        }
    }

};

Value SerialSumTree( TreeNode* root );
Value SimpleParallelSumTree( TreeNode* root );
Value OptimizedParallelSumTree( TreeNode* root );

#endif
