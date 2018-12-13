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

#include "common.h"
#include "tbb/task.h"
#include <numeric>
#include <cmath>

class SimpleSumTask: public tbb::task {
    Value* sum;
    TreeNode* root;
    bool is_continuation;
public:
    SimpleSumTask( TreeNode* root_, Value* sum_ ) : root(root_), sum(sum_), is_continuation(false){}
    task* execute() /*override*/ {

        tbb::task* next = NULL;
        if( !is_continuation ) {
            if (root->n_ < 0){
                //root

                int childNum = root->getChildNum();
                std::vector<double> lsum;
                lsum.resize(childNum);
                int count = 1; 
                tbb::task_list list;
 
                for(int i=0; i < childNum; i++){
                    TreeNode* p = root->getChild(i);
                    ++count;
                    list.push_back( *new( allocate_child() ) SimpleSumTask(p,&lsum[i]) );
                }

                // Argument to set_ref_count is one more than size of the list,
                // because spawn_and_wait_for_all expects an augmented ref_count.
                set_ref_count(count);
                std::cout << "Start spawn_all :" << childNum << "\n";
                spawn_and_wait_for_all(list);

                std::cout << "End spawn_all :" << childNum << "\n";



                for(int i=0; i < childNum; i++){
                    *sum += lsum[i];
                }
                //std::cout << "root:" << root->off_ << ",sum:" << *sum << "\n";
                //*sum = std::accumulate(lsum.begin(), lsum.end(), 0);
                //std::cout << "root:" << root->off_ << ",sum:" << *sum << "\n";
            }
            else{
                
                //std::cout << "StartNode off:" << root->off_ << "\n";

                //add me
                double lsum = 0.;
                for (int i=0; i<root->n_; i++){
                    //only n_==1 support now
                    for (int j=0; j<root->m_; j++){
                        for(int k=0; k<300; k++){
                            lsum += std::sqrt(root->data_[j] + k);
                        }
                    }
                }

                //std::cout << "off:" << root->off_ << ",sum:" << lsum << "\n";

                *sum += lsum;

                //std::cout << "off:" << root->off_ << ",sum:" << *sum << "\n";
                //go to next child
                if (root->getChildNum() == 1 ){
                    TreeNode* p = root->getChild(0);
                    auto* pchild = new( allocate_child() ) SimpleSumTask(p,sum);
 
                    recycle_as_continuation();
                    is_continuation = true;
                    set_ref_count(1);
                    //spawn(*pchild);
                    next = pchild;
                    //return this;
                }
                
                //std::cout << "EndNode off:" << root->off_ << "\n";
            }
        }

        return next;
    }
};

Value SimpleParallelSumTree( TreeNode* root ) {
    Value sum;
    SimpleSumTask& a = *new(tbb::task::allocate_root()) SimpleSumTask(root,&sum);
    tbb::task::spawn_root_and_wait(a);
    return sum;
}

