/* file: subgraph_dense_districontainer.h */
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
//  Implementation of subgraph calculation algorithm container.
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
#include "service_rng.h"
#include "services/daal_memory.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"


#include <omp.h>
#include "subgraph_types.h"
#include "subgraph_distri.h"
#include "subgraph_default_kernel.h"

using namespace tbb;

namespace daal
{
namespace algorithms
{
namespace subgraph
{
    
/**
 *  @brief Initialize list of subgraph with implementations for supported architectures
 */
template<ComputeStep step, typename interm, Method method, CpuType cpu>
DistriContainer<step, interm, method, cpu>::DistriContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::subgraphDistriKernel, interm, method);
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
    Input *input = static_cast<Input *>(_in);
    DistributedPartialResult *result = static_cast<DistributedPartialResult *>(_pres);
    Parameter *par = static_cast<Parameter*>(_par);
    daal::services::Environment::env &env = *_env;

    int compute_stage = par->_stage;
    int sub_itr = par->_sub_itr;
    std::printf("Sub itr: %d, Compute stage: %d\n", sub_itr, compute_stage);
    std::fflush;

    //kernel for stage 0 bottom
    __DAAL_CALL_KERNEL_STATUS(env, internal::subgraphDistriKernel, __DAAL_KERNEL_ARGUMENTS(interm, method), compute, par, input)

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
