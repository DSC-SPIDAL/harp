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

Value SerialSumTree( TreeNode* root ) {
    double sum;
    
    int childNum = root->getChildNum();
    for(int i=0; i < childNum; i++){
        TreeNode* p = root->getChild(i);
    
        sum += SerialSumTree(p);
    }

    //children + me
    if (root->n_ >= 0){
        //add me
        for (int i=0; i<root->n_; i++){
            //only n_==1 support now
            for (int j=0; j<root->m_; j++){
                for(int k=0; k<300; k++){
                    //sum += root->data_[j];
                    sum += std::sqrt(root->data_[j] + k);
                }
            }
        }
    }
 
    return sum;
}
