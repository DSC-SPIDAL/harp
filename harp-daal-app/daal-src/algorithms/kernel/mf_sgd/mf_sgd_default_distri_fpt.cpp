/* file: mf_sgd_dense_default_batch_fpt.cpp */
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
//  Implementation of mf_sgd algorithm and types methods.
//--
*/

#include "mf_sgd_default_distri.h"

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{

// template void* SOACopyBulkData<DAAL_FPTYPE>(void* arg);

namespace interface1
{

template void Input::convert_format_distri<DAAL_FPTYPE>(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<DAAL_FPTYPE>*>*> &map_train,
						                                const int64_t num_Train,
						                                mf_sgd::VPoint<DAAL_FPTYPE>* points_Train,  
						                                int64_t &row_num_w, 
						                                int64_t &col_num_h);


}// namespace interface1
}// namespace mf_sgd
}// namespace algorithms
}// namespace daal
