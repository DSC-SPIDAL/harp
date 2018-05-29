/* file: pca_dense_correlation_online_impl.i */
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
//  Functuons that are used in PCA algorithm
//--
*/

#ifndef __PCA_DENSE_CORRELATION_ONLINE_IMPL_I__
#define __PCA_DENSE_CORRELATION_ONLINE_IMPL_I__

#include "service_math.h"
#include "service_memory.h"
#include "service_numeric_table.h"
#include "pca_dense_correlation_online_kernel.h"

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace internal
{

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationKernel<online, algorithmFPType, cpu>::compute(
    const data_management::NumericTablePtr& pData,
    PartialResult<correlationDense> *partialResult,
    const OnlineParameter<algorithmFPType, correlationDense> *parameter)
{
    parameter->covariance->input.set(covariance::data, pData);
    parameter->covariance->parameter.outputMatrixType = covariance::correlationMatrix;

    services::Status s = parameter->covariance->computeNoThrow();
    if(s)
        s = copyCovarianceResultToPartialResult(parameter->covariance->getPartialResult().get(), partialResult);
    return s;
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationKernel<online, algorithmFPType, cpu>::finalize(
    PartialResult<correlationDense> *partialResult,
    const OnlineParameter<algorithmFPType, correlationDense> *parameter,
    data_management::NumericTable& eigenvectors,
    data_management::NumericTable& eigenvalues)
{
    services::Status s = parameter->covariance->finalizeCompute();
    if(!s)
        return s;

    data_management::NumericTablePtr correlation = parameter->covariance->getResult()->get(covariance::covariance);
    return this->computeCorrelationEigenvalues(*correlation, eigenvectors, eigenvalues);
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationKernel<online, algorithmFPType, cpu>::copyCovarianceResultToPartialResult(
    covariance::PartialResult *covariancePres,
    PartialResult<correlationDense> *partialResult)
{
    services::Status s = copyIfNeeded(covariancePres->get(covariance::sum).get(), partialResult->get(sumCorrelation).get());
    s |= copyIfNeeded(covariancePres->get(covariance::nObservations).get(), partialResult->get(nObservationsCorrelation).get());
    s |= copyIfNeeded(covariancePres->get(covariance::crossProduct).get(), partialResult->get(crossProductCorrelation).get());
    return s;
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationKernel<online, algorithmFPType, cpu>::copyIfNeeded(data_management::NumericTable *src, data_management::NumericTable *dst)
{
    if(src == dst)
        return services::Status();

    const size_t nRows = dst->getNumberOfRows();
    const size_t nCols = dst->getNumberOfColumns();

    ReadRows<algorithmFPType, cpu> srcBlock(*src, 0, nRows);
    DAAL_CHECK_BLOCK_STATUS(srcBlock);
    const algorithmFPType *srcArray = srcBlock.get();

    WriteOnlyRows<algorithmFPType, cpu> dstBlock(*dst, 0, nRows);
    DAAL_CHECK_BLOCK_STATUS(srcBlock);
    algorithmFPType *dstArray = dstBlock.get();

    const size_t nDataElements = nRows * nCols;
    daal::services::daal_memcpy_s(dstArray, nDataElements * sizeof(algorithmFPType), srcArray, nDataElements * sizeof(algorithmFPType));
    return services::Status();
}

} // namespace internal
} // namespace pca
} // namespace algorithms
} // namespace daal

#endif
