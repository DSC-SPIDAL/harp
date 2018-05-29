/* file: gbt_regression_train_container.h */
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
//  Implementation of gradient boosted trees container.
//--
*/

#ifndef __GBT_REGRESSION_TRAIN_CONTAINER_H__
#define __GBT_REGRESSION_TRAIN_CONTAINER_H__

#include "kernel.h"
#include "gbt_regression_training_types.h"
#include "gbt_regression_training_batch.h"
#include "gbt_regression_train_kernel.h"
#include "gbt_regression_model_impl.h"
#include "service_algo_utils.h"

namespace daal
{
namespace algorithms
{
namespace gbt
{
namespace regression
{
namespace training
{

/**
 *  \brief Initialize list of gradient boosted trees
 *  kernels with implementations for supported architectures
 */
template <typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::BatchContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::RegressionTrainBatchKernel, algorithmFPType, method);
}

template <typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::~BatchContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

/**
 *  \brief Choose appropriate kernel to calculate gradient boosted trees model.
 *
 *  \param env[in]  Environment
 *  \param a[in]    Array of numeric tables contating input data
 *  \param r[out]   Resulting model
 *  \param par[in]  Decision forest algorithm parameters
 */
template <typename algorithmFPType, Method method, CpuType cpu>
services::Status BatchContainer<algorithmFPType, method, cpu>::compute()
{
    Input *input = static_cast<Input *>(_in);
    Result *result = static_cast<Result *>(_res);

    const NumericTable *x = input->get(data).get();
    const NumericTable *y = input->get(dependentVariable).get();

    gbt::regression::Model *m = result->get(model).get();

    const Parameter *par = static_cast<gbt::regression::training::Parameter*>(_par);
    daal::services::Environment::env &env = *_env;
    daal::algorithms::engines::internal::BatchBaseImpl* engine = dynamic_cast<daal::algorithms::engines::internal::BatchBaseImpl*>(par->engine.get());

    __DAAL_CALL_KERNEL(env, internal::RegressionTrainBatchKernel,
        __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method), compute, daal::services::internal::hostApp(*input), x, y, *m, *result, *par, *engine);
}

template <typename algorithmFPType, Method method, CpuType cpu>
services::Status BatchContainer<algorithmFPType, method, cpu>::setupCompute()
{
    Result *result = static_cast<Result *>(_res);
    gbt::regression::Model *m = result->get(model).get();
    gbt::regression::internal::ModelImpl* pImpl = dynamic_cast<gbt::regression::internal::ModelImpl*>(m);
    DAAL_ASSERT(pImpl);
    pImpl->clear();
    return services::Status();
}

}
}
}
}
}
#endif
