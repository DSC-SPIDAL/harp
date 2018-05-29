/* file: sgd_dense_minibatch_kernel.h */
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


#ifndef __SGD_DENSE_MINIBATCH_KERNEL_H__
#define __SGD_DENSE_MINIBATCH_KERNEL_H__

#include "sgd_batch.h"
#include "kernel.h"
#include "numeric_table.h"
#include "iterative_solver_kernel.h"
#include "sgd_dense_kernel.h"
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
enum IndicesStatus
{
    random = 0,     /*!< Indices of the terms are generated randomly */
    user   = 1,     /*!< Indices of the terms are provided by user */
    all    = 2      /*!< All objective function terms are used for computations */
};

template<typename algorithmFPType, CpuType cpu>
class SGDKernel<algorithmFPType, miniBatch, cpu> : public iterative_solver::internal::IterativeSolverKernel<algorithmFPType, cpu>
{
public:
    services::Status compute(NumericTable *inputArgument, NumericTable *minimum, NumericTable *nIterations,
                 Parameter<miniBatch> *parameter, NumericTable *learningRateSequence,
                 NumericTable *batchIndices, OptionalArgument *optionalArgument, OptionalArgument *optionalResult, engines::BatchBase &engine);
    using iterative_solver::internal::IterativeSolverKernel<algorithmFPType, cpu>::vectorNorm;
};

template<typename algorithmFPType, CpuType cpu>
struct SGDMiniBatchTask
{
    SGDMiniBatchTask(
        size_t nFeatures_,
        NumericTable *resultTable,
        NumericTable *startValueTable,
        NumericTable *nIterationsTable
    );

    SGDMiniBatchTask(
        size_t batchSize_,
        size_t nFeatures_,
        size_t maxIterations_,
        size_t nTerms_,
        NumericTable *resultTable,
        NumericTable *startValueTable,
        NumericTable *learningRateSequenceTable,
        NumericTable *conservativeSequenceTable,
        NumericTable *nIterationsTable,
        NumericTable *batchIndicesTable,
        NumericTable *pastWorkValueResultNT,
        NumericTable *lastIterationResultNT
    );

    virtual ~SGDMiniBatchTask();

    services::Status init(NumericTable *startValueTable);

    services::Status init(NumericTable *startValueTable,
        NumericTable *learningRateSequenceTable,
        NumericTable *conservativeSequenceTable,
        NumericTable *nIterationsTable,
        NumericTable *batchIndicesTable,
        OptionalArgument *optionalInput);

    services::Status setStartValue(NumericTable *startValueTable);

    void makeStep(const algorithmFPType *gradient,
        algorithmFPType learningRate,
        algorithmFPType consCoeff,
        size_t argumentSize);

    size_t batchSize;
    size_t argumentSize;
    size_t nIter;
    size_t nTerms;
    size_t startIteration;
    size_t nProceededIters;

    int             *nProceededIterations;
    const algorithmFPType *learningRateArray;
    const algorithmFPType *consCoeffsArray;
    size_t          learningRateLength;
    size_t          consCoeffsLength;
    TArray<algorithmFPType, cpu> prevWorkValue;
    IndicesStatus   indicesStatus;

    WriteRows<algorithmFPType, cpu> mtWorkValue;
    SharedPtr<daal::internal::HomogenNumericTableCPU<int, cpu>> ntBatchIndices;
    SharedPtr<daal::internal::HomogenNumericTableCPU<algorithmFPType, cpu>> ntWorkValue;
    ReadRows<algorithmFPType, cpu> mtLearningRate;
    ReadRows<algorithmFPType, cpu> mtConsCoeffs;
    WriteRows<int, cpu> mtNIterations;
    ReadRows<int, cpu> mtPredefinedBatchIndices;

    NumericTablePtr lastIterationResult;
    NumericTablePtr pastWorkValueResult;
};

} // namespace daal::internal

} // namespace sgd

} // namespace optimization_solver

} // namespace algorithms

} // namespace daal

#endif
