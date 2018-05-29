/* file: neural_networks_training_distr_container.h */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Implementation of neural_networks calculation algorithm container.
//--
*/

#ifndef __NEURAL_NETWORKS_TRAINING_DISTR_CONTAINER_H__
#define __NEURAL_NETWORKS_TRAINING_DISTR_CONTAINER_H__

#include "neural_networks/neural_networks_training_distributed.h"
#include "neural_networks_types.h"
#include "neural_networks_training_types.h"
#include "neural_networks_training_feedforward_kernel.h"
#include "kernel.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace training
{
namespace interface1
{
/* TrainingKernelDistributed */
template<typename algorithmFPType, Method method, CpuType cpu>
DistributedContainer<step1Local, algorithmFPType, method, cpu>::DistributedContainer(daal::services::interface1::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::TrainingKernelDistributed, algorithmFPType, method);
}

template<typename algorithmFPType, Method method, CpuType cpu>
DistributedContainer<step1Local, algorithmFPType, method, cpu>::~DistributedContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step1Local, algorithmFPType, method, cpu>::compute()
{
    DistributedInput<step1Local> *input = static_cast<DistributedInput<step1Local> *>(_in);
    PartialResult *partialResult = static_cast<PartialResult *>(_pres);
    Parameter *parameter = static_cast<Parameter *>(_par);
    daal::services::Environment::env &env = *_env;

    Tensor* data = input->get(training::data).get();
    Model* nnModel = input->get(training::inputModel).get();
    KeyValueDataCollectionPtr groundTruthCollectionPtr = input->get(training::groundTruthCollection);

    __DAAL_CALL_KERNEL(env, internal::TrainingKernelDistributed, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), compute,
                       data, nnModel, groundTruthCollectionPtr, partialResult, parameter);
}

template <typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step1Local, algorithmFPType, method, cpu>::finalizeCompute()
{
    return services::Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step1Local, algorithmFPType, method, cpu>::setupCompute()
{
    DistributedInput<step1Local> *input = static_cast<DistributedInput<step1Local> *>(_in);
    Parameter *parameter = static_cast<Parameter *>(_par);
    daal::services::Environment::env &env = *_env;

    Tensor* data = input->get(training::data).get();
    Model* nnModel = input->get(training::inputModel).get();
    KeyValueDataCollectionPtr groundTruthCollectionPtr = input->get(training::groundTruthCollection);

    __DAAL_CALL_KERNEL(env, internal::TrainingKernelDistributed, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), initialize,
                       data, nnModel, groundTruthCollectionPtr, parameter);
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step1Local, algorithmFPType, method, cpu>::resetCompute()
{
    daal::services::Environment::env &env = *_env;
    __DAAL_CALL_KERNEL(env, internal::TrainingKernelDistributed, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), reset);
}

/* TrainingKernelDistributedStep2 */
template<typename algorithmFPType, Method method, CpuType cpu>
DistributedContainer<step2Master, algorithmFPType, method, cpu>::DistributedContainer(daal::services::interface1::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::TrainingKernelDistributedStep2, algorithmFPType, method);
}

template<typename algorithmFPType, Method method, CpuType cpu>
DistributedContainer<step2Master, algorithmFPType, method, cpu>::~DistributedContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step2Master, algorithmFPType, method, cpu>::compute()
{
    DistributedInput<step2Master> *input = static_cast<DistributedInput<step2Master> *>(_in);
    DistributedPartialResult *partialResult = static_cast<DistributedPartialResult *>(_pres);
    Parameter *parameter = static_cast<Parameter *>(_par);
    daal::services::Environment::env &env = *_env;

    KeyValueDataCollection* collection = input->get(training::partialResults).get();
    Model* nnModel = partialResult->get(resultFromMaster)->get(training::model).get();

    __DAAL_CALL_KERNEL(env, internal::TrainingKernelDistributedStep2, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), compute,
                       collection, parameter, nnModel);
}

template <typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step2Master, algorithmFPType, method, cpu>::finalizeCompute()
{
    return services::Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step2Master, algorithmFPType, method, cpu>::setupCompute()
{
    return services::Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status DistributedContainer<step2Master, algorithmFPType, method, cpu>::resetCompute()
{
    return services::Status();
}

} // namespace interface1
} // namespace training
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
