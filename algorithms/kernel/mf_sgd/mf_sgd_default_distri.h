/* file: mf_sgd_dense_default_distri.h */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Implementation of mf_sgd algorithm and types methods in distributed mode.
//--
*/
#ifndef __MF_SGD_DEFAULT_DISTRI__
#define __MF_SGD_DEFAULT_DISTRI__

#include "mf_sgd_types.h"

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
namespace interface1
{

template <typename algorithmFPType>
void Input::convert_format_distri(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_train,
						          const int64_t num_Train,
						          mf_sgd::VPoint<algorithmFPType>* points_Train,  
						          int64_t &row_num_w, 
						          int64_t &col_num_h)
{/*{{{*/

    std::unordered_map<int64_t, int64_t> vMap_row_w;
    std::unordered_map<int64_t, int64_t> vMap_col_h;

    int64_t row_pos_itr = 0;
    int64_t col_pos_itr = 0;

    int64_t row_pos = 0;
    int64_t col_pos = 0;
    int64_t entry_itr = 0;

    int64_t row_id = 0;
    int64_t col_id = 0;
    algorithmFPType val = 0;

    typename std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*>::iterator it_map;
    typename std::vector<mf_sgd::VPoint<algorithmFPType>*>::iterator it_vec;

    /* iteration over train data map */
    for (it_map = map_train.begin(); it_map != map_train.end(); ++it_map) 
    {
        if (it_map->second->empty() == false)
        {
            for(it_vec = it_map->second->begin(); it_vec < it_map->second->end();++it_vec)
            {
                row_id = (*it_vec)->wPos;
                col_id = (*it_vec)->hPos;
                val = (*it_vec)->val;

                if (vMap_row_w.find(row_id) == vMap_row_w.end())
                {
                    /* not found row id */
                    vMap_row_w[row_id] = row_pos_itr;
                    row_pos = row_pos_itr;
                    row_pos_itr++;
                }
                else
                    row_pos = vMap_row_w[row_id];

                if (vMap_col_h.find(col_id) == vMap_col_h.end())
                {
                    /* not found col id */
                    vMap_col_h[col_id] = col_pos_itr;
                    col_pos = col_pos_itr;
                    col_pos_itr++;
                }
                else
                    col_pos = vMap_col_h[col_id];

                points_Train[entry_itr].wPos = row_pos;
                points_Train[entry_itr].hPos = col_pos;
                points_Train[entry_itr].val = val;
                entry_itr++;
            }
        }
    }

    row_num_w = row_pos_itr;
    col_num_h = col_pos_itr;

}/*}}}*/



}// namespace interface1
}// namespace mf_sgd
}// namespace algorithms
}// namespace daal

#endif
