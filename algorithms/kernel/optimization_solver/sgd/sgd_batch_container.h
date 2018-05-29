/* file: sgd_batch_container.h */
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
//  Implementation of sgd calculation algorithm container.
//--
*/

#ifndef __SGD_BATCH_CONTAINER_H__
#define __SGD_BATCH_CONTAINER_H__

#include "sgd_batch.h"
#include "sgd_dense_default_kernel.h"
#include "sgd_dense_minibatch_kernel.h"
#include "sgd_dense_momentum_kernel.h"

namespace daal
{
namespace algorithms
{
namespace optimization_solver
{
namespace sgd
{
namespace interface1
{
template<typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::BatchContainer(daal::services::Environment::env *daalEnv)
{
    __DAAL_INITIALIZE_KERNELS(internal::SGDKernel, algorithmFPType, method);
}

template<typename algorithmFPType, Method method, CpuType cpu>
BatchContainer<algorithmFPType, method, cpu>::~BatchContainer()
{
    __DAAL_DEINITIALIZE_KERNELS();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status BatchContainer<algorithmFPType, method, cpu>::compute()
{
    iterative_solver::Input *input = static_cast<iterative_solver::Input *>(_in);
    iterative_solver::Result *result = static_cast<iterative_solver::Result *>(_res);
    Parameter<method> *parameter = static_cast<Parameter<method> *>(_par);

    daal::services::Environment::env &env = *_env;

    NumericTable *inputArgument = input->get(iterative_solver::inputArgument).get();
    NumericTable *minimum       = result->get(iterative_solver::minimum).get();
    NumericTable *nIterations   = result->get(iterative_solver::nIterations).get();
    OptionalArgument *optionalArgument = input->get(iterative_solver::optionalArgument).get();
    OptionalArgument *optionalResult = result->get(iterative_solver::optionalResult).get();

    NumericTable *learningRateSequence = parameter->learningRateSequence.get();
    NumericTable *batchIndices         = parameter->batchIndices.get();

    __DAAL_CALL_KERNEL(env, internal::SGDKernel, __DAAL_KERNEL_ARGUMENTS(algorithmFPType, method),
        compute, inputArgument, minimum, nIterations, parameter,
        learningRateSequence, batchIndices, optionalArgument, optionalResult, *parameter->engine);
}

} // namespace interface1

} // namespace sgd

} // namespace optimization_solver

} // namespace algorithms

} // namespace daal

#endif
