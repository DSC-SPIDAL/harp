/* file: concat_layer_backward_batch_container.h */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
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
//  Implementation of concat layer container.
//--
*/

#ifndef __CONCAT_LAYER_BACKWARD_BATCH_CONTAINER_H__
#define __CONCAT_LAYER_BACKWARD_BATCH_CONTAINER_H__

#include "neural_networks/layers/concat/concat_layer.h"
#include "concat_layer_backward_kernel.h"
#include "service_numeric_table.h"

using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace concat
{
namespace backward
{
namespace interface1
{
template<typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::BatchContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::ConcatKernel, algorithmFPType, method);
}

template<typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::~BatchContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status BatchContainer<algorithmFPType, method, cpu>::compute()
{
    concat::backward::Input *input = static_cast<concat::backward::Input *>(_in);
    concat::backward::Result *result = static_cast<concat::backward::Result *>(_res);

    concat::Parameter *parameter = static_cast<concat::Parameter *>(_par);
    if (!parameter->propagateGradient) { return services::Status(); }

    daal::services::Environment::env &env = *_env;

    Tensor *inputTensor              = input->get(layers::backward::inputGradient).get();
    NumericTable *forwardOutputTable = input->get(layers::concat::auxInputDimensions).get();

    size_t nOutputs = forwardOutputTable->getNumberOfColumns();

    SmartPtr<cpu> resultBlock( nOutputs * sizeof(Tensor *));
    Tensor **resultTensors = (Tensor **)resultBlock.get();
    DAAL_CHECK_MALLOC(resultTensors);

    for(size_t i = 0; i < nOutputs; i++)
    {
        resultTensors[i] = result->get(layers::backward::resultLayerData, i).get();
    }

    __DAAL_CALL_KERNEL(env, internal::ConcatKernel, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), compute, inputTensor, forwardOutputTable, parameter,
                       resultTensors);
}
} // namespace interface1
} // namespace backward

} // namespace concat
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
