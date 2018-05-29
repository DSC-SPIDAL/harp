/* file: sgd_dense_momentum_kernel.h */
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

//++
//  Declaration of template function that calculate sgd.
//--


#ifndef __SGD_DENSE_MOMENTUM_KERNEL_H__
#define __SGD_DENSE_MOMENTUM_KERNEL_H__

#include "sgd_batch.h"
#include "kernel.h"
#include "numeric_table.h"
#include "iterative_solver_kernel.h"
#include "sgd_dense_kernel.h"
#include "sgd_dense_minibatch_kernel.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"
#include "service_math.h"
#include "service_utils.h"

using namespace daal::data_management;
using namespace daal::internal;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace optimization_solver
{
namespace sgd
{
namespace internal
{

/**
* Statuses of the indices of objective function terms that are used for gradient
*/

template<typename algorithmFPType, CpuType cpu>
class SGDKernel<algorithmFPType, momentum, cpu> : public iterative_solver::internal::IterativeSolverKernel<algorithmFPType, cpu>
{
public:
    services::Status compute(NumericTable *inputArgument, NumericTable *minimum, NumericTable *nIterations,
                 Parameter<momentum> *parameter, NumericTable *learningRateSequence,
                 NumericTable *batchIndices, OptionalArgument *optionalArgument, OptionalArgument *optionalResult, engines::BatchBase &engine);
    using iterative_solver::internal::IterativeSolverKernel<algorithmFPType, cpu>::vectorNorm;
};

template<typename algorithmFPType, CpuType cpu>
struct SGDmomentumTask
{
    SGDmomentumTask(
        size_t batchSize_,
        size_t nTerms_,
        NumericTable *resultTable,
        NumericTable *batchIndicesTable,
        NumericTable *pastUpdateResult,
        NumericTable *lastIterationResultNT,
        Parameter<momentum> *parameter);

    virtual ~SGDmomentumTask();

    Status init(NumericTable *batchIndicesTable, NumericTable *resultTable, Parameter<momentum> *parameter,
        NumericTable *pastUpdateInput,
        NumericTable *lastIterationInput);

    Status setStartValue(NumericTable *inputArgument, NumericTable *minimum);

    Status makeStep(NumericTable *gradient,
                  NumericTable *minimum,
                  NumericTable *pastUpdate,
                  const algorithmFPType learningRate,
                  const algorithmFPType momentum);

    size_t batchSize;
    size_t nTerms;
    size_t startIteration;
    size_t nProceededIters;

    IndicesStatus   indicesStatus;

    SharedPtr<daal::internal::HomogenNumericTableCPU<int, cpu>> ntBatchIndices;
    NumericTablePtr minimimWrapper;
    NumericTablePtr pastUpdate;
    NumericTablePtr lastIterationResult;
};

} // namespace daal::internal

} // namespace sgd

} // namespace optimization_solver

} // namespace algorithms

} // namespace daal

#endif
