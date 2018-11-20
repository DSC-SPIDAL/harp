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

#ifndef TREE_MAKER_H_
#define TREE_MAKER_H_

#include "tbb/tick_count.h"
#include "tbb/task.h"

static double Pi = 3.14159265358979;

const bool tbbmalloc = true;
const bool stdmalloc = false;

template<bool use_tbbmalloc>
class TreeMaker {

public:
    static TreeNode* allocate_node() {
        return use_tbbmalloc? tbb::scalable_allocator<TreeNode>().allocate(1) : new TreeNode;
    }

 
    static TreeNode* create_and_time( DMatrix& dmat, int blocksize, bool silent=false ) {
        tbb::tick_count t0, t1;
 
        TreeNode* root = allocate_node();
        root->init(dmat, -1, -1, blocksize);

        int n = dmat.n_ ; 
        int blocknum = dmat.getBlockNum(blocksize);

        for(int i=0; i<n; i++){
            TreeNode* head = allocate_node();
            head->init(dmat, i, 0, blocksize);
            root->addChild(head);

            for(int blkid=1; blkid<blocknum; blkid++){
                TreeNode* n = allocate_node();
                n->init(dmat, i, blkid, blocksize);
                head->addChild(n);

                head = n;
            }
        }

        //create the task root node
        return root;
    }
};

#endif // TREE_MAKER_H_
