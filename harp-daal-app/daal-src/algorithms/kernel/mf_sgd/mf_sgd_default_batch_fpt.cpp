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

#include "mf_sgd_default_batch.h"

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
namespace interface1
{

template DAAL_EXPORT void Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT void Result::free_mem<DAAL_FPTYPE>(size_t r, size_t w, size_t h);

template DAAL_EXPORT void Result::allocateImpl<DAAL_FPTYPE>(size_t r, size_t w, size_t h );
template DAAL_EXPORT void Result::freeImpl<DAAL_FPTYPE>(size_t r, size_t w, size_t h );

template DAAL_EXPORT void Result::allocateImpl_cache_aligned<DAAL_FPTYPE>(size_t r, size_t w, size_t h );
template DAAL_EXPORT void Result::freeImpl_cache_aligned<DAAL_FPTYPE>(size_t r, size_t w, size_t h );

template DAAL_EXPORT void Result::allocateImpl_hbw_mem<DAAL_FPTYPE>(size_t r, size_t w, size_t h );
template DAAL_EXPORT void Result::freeImpl_hbw_mem<DAAL_FPTYPE>(size_t r, size_t w, size_t h );

template void Input::generate_points<DAAL_FPTYPE>(const int64_t num_Train,
						    const int64_t num_Test, 
						    const int64_t row_num_w, 
						    const int64_t col_num_h,
						    mf_sgd::VPoint<DAAL_FPTYPE>* points_Train,
						    mf_sgd::VPoint<DAAL_FPTYPE>* points_Test);

template int64_t Input::loadData<DAAL_FPTYPE>(const std::string filename, std::vector<int64_t>* lineContainer, 
				                 std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<DAAL_FPTYPE>*>*> &map);

template void Input::convert_format<DAAL_FPTYPE>(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<DAAL_FPTYPE>*>*> &map_train,
						            std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<DAAL_FPTYPE>*>*> &map_test,
						            const int64_t num_Train,
					                const int64_t num_Test,
						            mf_sgd::VPoint<DAAL_FPTYPE>* points_Train,  
						            mf_sgd::VPoint<DAAL_FPTYPE>* points_Test, 
						            int64_t &row_num_w, 
						            int64_t &col_num_h,
                                    size_t &absent_test_num);

template void Input::freeData<DAAL_FPTYPE>(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<DAAL_FPTYPE>*>*> &map);

}// namespace interface1
}// namespace mf_sgd
}// namespace algorithms
}// namespace daal
