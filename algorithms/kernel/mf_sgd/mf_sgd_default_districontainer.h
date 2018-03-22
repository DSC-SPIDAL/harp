/* file: mf_sgd_dense_districontainer.h */
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
//  Implementation of mf_sgd calculation algorithm container.
//--
*/
#include <cstdlib> 
#include <cstring>
#include <ctime> 
#include <iostream>
#include <cstdio>
#include <math.h>       
#include <random>
#include <vector>
#include <assert.h>
#include "numeric_table.h"
#include "data_collection.h"
#include "service_rng.h"
#include "services/daal_memory.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"


#include <omp.h>
#include "mf_sgd_types.h"
#include "mf_sgd_distri.h"
#include "mf_sgd_default_kernel.h"

using namespace tbb;

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
    

typedef tbb::concurrent_hash_map<int, int> ConcurrentModelMap;
typedef tbb::concurrent_hash_map<int, std::vector<int> > ConcurrentDataMap;

/**
 *  @brief Initialize list of mf_sgd with implementations for supported architectures
 */
template<ComputeStep step, typename interm, Method method, CpuType cpu>
DistriContainer<step, interm, method, cpu>::DistriContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::MF_SGDDistriKernel, interm, method);
}

template<ComputeStep step, typename interm, Method method, CpuType cpu>
DistriContainer<step, interm, method, cpu>::~DistriContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template<ComputeStep step, typename interm, Method method, CpuType cpu>
daal::services::interface1::Status DistriContainer<step, interm, method, cpu>::compute()
{
    services::Status status;
    // prepare the computation
    Input *input = static_cast<Input *>(_in);
    DistributedPartialResult *result = static_cast<DistributedPartialResult *>(_pres);
    Parameter *par = static_cast<Parameter*>(_par);

    //get the feature dimension
    size_t dim_r = par->_Dim_r;
    //get the num of threads
    int thread_num = par->_thread_num;

    // retrieve the training and test datasets 
    // NumericTable *a0 = static_cast<NumericTable *>(input->get(wPos).get());
    // NumericTable *a1 = static_cast<NumericTable *>(input->get(hPos).get());
    // NumericTable *a2 = static_cast<NumericTable *>(input->get(val).get());
    KeyValueDataCollection* train_a = static_cast<KeyValueDataCollection *>(input->get(dataTrain).get());
    NumericTable *a0 = static_cast<NumericTable *>((*train_a)[0].get());
    NumericTable *a1 = static_cast<NumericTable *>((*train_a)[1].get());
    NumericTable *a2 = static_cast<NumericTable *>((*train_a)[2].get());

    KeyValueDataCollection* test_a = static_cast<KeyValueDataCollection *>(input->get(dataTest).get());
    NumericTable *a3 = static_cast<NumericTable *>((*test_a)[0].get());
    NumericTable *a4 = static_cast<NumericTable *>((*test_a)[1].get());
    NumericTable *a5 = static_cast<NumericTable *>((*test_a)[2].get());

    assert(a0 != NULL);
    assert(a1 != NULL);
    assert(a2 != NULL);
    assert(a3 != NULL);
    assert(a4 != NULL);
    assert(a5 != NULL);

    NumericTable **WPos = &a0;
    NumericTable **HPos = &a1;
    NumericTable **Val = &a2;

    NumericTable **WPosTest = &a3;
    NumericTable **HPosTest = &a4;
    NumericTable **ValTest = &a5;

    NumericTable *r[4];
    //r[0] stores ids of W matrix
    r[0] = static_cast<NumericTable *>(result->get(presWMat).get());
    //r[1] stores values of H matrix
    r[1] = static_cast<NumericTable *>(result->get(presHMat).get());

    //if wMat is not initialized, generate it only once
    if (par->_wMat_map == NULL && par->_wMatFinished == 0)
          internal::wMat_generate_distri<interm, cpu>(r, par,result, dim_r, thread_num);

    //r[2] stores the values of W matrix
    r[2] = static_cast<NumericTable *>(result->get(presWData).get());

    //if training dataset hashmap is not initialized, generate it only once
    if (par->_train_map == NULL && par->_trainMapFinished == 0)
          internal::train_generate_distri<interm, cpu>(r, a0, a1, par, dim_r, thread_num);

    //if test dataset hashmap is not initialized, generate it only once
    if (par->_test_map == NULL && par->_testMapFinished == 0 )
          internal::test_generate_distri<interm, cpu>(r, a3, a4, par, dim_r, thread_num);

    // clear wMap
    if (par->_wMat_map != NULL)
    {
        delete par->_wMat_map;
        par->_wMat_map = NULL;
    }

    //------------------------------- build up the hMat matrix -------------------------------

    //store the col_ids of this iteration
    int* col_ids = NULL;
    //native memory space to hold H matrix values
    interm** hMat_native_mem = NULL;

    //containers for copying h matrix data between java and c++ in parallel
    BlockDescriptor<interm>** hMat_blk_array = NULL;
    internal::SOADataCopy<interm>** copylist = NULL;

    //generate h matrix on native side in parallel
    // internal::hMat_generate<interm, cpu>(r, par, dim_r, thread_num, col_ids, hMat_native_mem, hMat_blk_array, copylist);
    
    //r[3] is used in test dataset to hold rmse values
    if ((static_cast<Parameter*>(_par))->_isTrain)
        r[3] = NULL;
    else
        r[3] = static_cast<NumericTable *>(result->get(presRMSE).get());

    daal::services::Environment::env &env = *_env;

    __DAAL_CALL_KERNEL_STATUS(env, internal::MF_SGDDistriKernel, __DAAL_KERNEL_ARGUMENTS(interm, method), compute, WPos, HPos, Val, WPosTest, HPosTest, ValTest, r, par, col_ids, hMat_native_mem)

    //release h matrix from native side back to Java side after updating values
    // internal::hMat_release<interm, cpu>(r, par, dim_r, thread_num, hMat_blk_array, copylist);

    //clean up the memory space per iteration
    if (col_ids != NULL)
        free(col_ids);

    if (par->_hMat_map != NULL)
    {
        delete par->_hMat_map;
        par->_hMat_map = NULL;
    }
    
    if (hMat_blk_array != NULL)
        delete[] hMat_blk_array;

    if (hMat_native_mem != NULL)
        delete[] hMat_native_mem;

    return status;
}


template<ComputeStep step, typename interm, Method method, CpuType cpu>
daal::services::interface1::Status DistriContainer<step, interm, method, cpu>::finalizeCompute() {

    services::Status status;
    return status;
}

}
}
} // namespace daal
