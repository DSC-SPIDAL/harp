/* file: pca_dense_correlation_online_container.h */
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
//  Implementation of PCA Correlation algorithm container.
//--
*/

#ifndef __PCA_DENSE_CORRELATION_ONLINE_CONTAINER_H__
#define __PCA_DENSE_CORRELATION_ONLINE_CONTAINER_H__

#include "kernel.h"
#include "pca_online.h"
#include "pca_dense_correlation_online_kernel.h"

namespace daal
{
namespace algorithms
{
namespace pca
{

template <typename algorithmFPType, CpuType cpu>
OnlineContainer<algorithmFPType, correlationDense, cpu>::OnlineContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::PCACorrelationKernel, online, algorithmFPType);
}

template <typename algorithmFPType, CpuType cpu>
OnlineContainer<algorithmFPType, correlationDense, cpu>::~OnlineContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template <typename algorithmFPType, CpuType cpu>
services::Status OnlineContainer<algorithmFPType, correlationDense, cpu>::compute()
{
    Input *input = static_cast<Input *>(_in);
    OnlineParameter<algorithmFPType, correlationDense> *parameter = static_cast<OnlineParameter<algorithmFPType, correlationDense> *>(_par);
    PartialResult<correlationDense> *partialResult = static_cast<PartialResult<correlationDense> *>(_pres);
    services::Environment::env &env = *_env;

    data_management::NumericTablePtr data = input->get(pca::data);

    __DAAL_CALL_KERNEL(env, internal::PCACorrelationKernel, __DAAL_KERNEL_ARGUMENTS(online, algorithmFPType),
                       compute, data, partialResult, parameter);
}

template <typename algorithmFPType, CpuType cpu>
services::Status OnlineContainer<algorithmFPType, correlationDense, cpu>::finalizeCompute()
{
    Input *input = static_cast<Input *>(_in);
    OnlineParameter<algorithmFPType, correlationDense> *parameter = static_cast<OnlineParameter<algorithmFPType, correlationDense> *>(_par);
    PartialResult<correlationDense> *partialResult = static_cast<PartialResult<correlationDense> *>(_pres);
    Result *result = static_cast<Result *>(_res);
    services::Environment::env &env = *_env;

    data_management::NumericTablePtr eigenvalues  = result->get(pca::eigenvalues);
    data_management::NumericTablePtr eigenvectors = result->get(pca::eigenvectors);

    __DAAL_CALL_KERNEL(env, internal::PCACorrelationKernel, __DAAL_KERNEL_ARGUMENTS(online, algorithmFPType),
                       finalize, partialResult, parameter, *eigenvectors, *eigenvalues);
}

}
}
} // namespace daal

#endif
