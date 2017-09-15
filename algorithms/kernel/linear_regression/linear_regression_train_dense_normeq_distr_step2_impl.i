/* file: linear_regression_train_dense_normeq_distr_step2_impl.i */
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
//  Implementation of normel equations method for linear regression
//  coefficients calculation
//--
*/

#ifndef __LINEAR_REGRESSION_TRAIN_DENSE_NORMEQ_DISTR_STEP2_IMPL_I__
#define __LINEAR_REGRESSION_TRAIN_DENSE_NORMEQ_DISTR_STEP2_IMPL_I__

#include "linear_regression_train_kernel.h"
#include "linear_regression_train_dense_normeq_impl.i"
#include "threading.h"

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace training
{
namespace internal
{

template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::normEqDense, cpu>::compute(
    size_t n, daal::algorithms::Model **partialModels, daal::algorithms::Model *r, const daal::algorithms::Parameter *par)
{
    Status status;
    linear_regression::Model *rModel = static_cast<Model *>(r);

    DAAL_CHECK_STATUS(status, rModel->initialize());
    for (size_t i = 0; i < n; i++)
    {
        DAAL_CHECK_STATUS(status, merge(partialModels[i], r, par));
    }
    return status;
}

template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::normEqDense, cpu>::merge(
    daal::algorithms::Model *a, daal::algorithms::Model *r, const daal::algorithms::Parameter *par)
{
    ModelNormEq *aa = static_cast<ModelNormEq *>(a);
    ModelNormEq *rr = static_cast<ModelNormEq *>(r);

    const linear_regression::Parameter *parameter = static_cast<const linear_regression::Parameter *>(par);
    size_t nBetas = aa->getNumberOfBetas();
    if(!parameter->interceptFlag) { nBetas--; }

    const size_t nResponses = aa->getNumberOfResponses();

    DEFINE_TABLE_BLOCK_EX( ReadRows,  axtxBlock, aa->getXTXTable().get(), 0, nBetas     );
    DEFINE_TABLE_BLOCK_EX( ReadRows,  axtyBlock, aa->getXTYTable().get(), 0, nResponses );
    DEFINE_TABLE_BLOCK_EX( WriteRows, rxtxBlock, rr->getXTXTable().get(), 0, nBetas     );
    DEFINE_TABLE_BLOCK_EX( WriteRows, rxtyBlock, rr->getXTYTable().get(), 0, nResponses );

    algorithmFPType *axtx = const_cast<algorithmFPType *>(axtxBlock.get());
    algorithmFPType *axty = const_cast<algorithmFPType *>(axtyBlock.get());
    algorithmFPType *rxtx = rxtxBlock.get();
    algorithmFPType *rxty = rxtyBlock.get();

    mergePartialSums(nBetas, nResponses, axtx, axty, rxtx, rxty);

    return Status();
}

template <typename algorithmFPType, CpuType cpu>
void LinearRegressionTrainDistributedKernel<algorithmFPType, training::normEqDense, cpu>::mergePartialSums(
    DAAL_INT nBetas, DAAL_INT nResponses, algorithmFPType *axtx, algorithmFPType *axty, algorithmFPType *rxtx, algorithmFPType *rxty)
{
    size_t xtxSize = nBetas * nBetas;
    daal::threader_for( xtxSize, xtxSize, [ = ](size_t i)
    {
        rxtx[i] += axtx[i];
    } );

    size_t xtySize = nResponses * nBetas;
    daal::threader_for( xtySize, xtySize, [ = ](size_t i)
    {
        rxty[i] += axty[i];
    } );
}

template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::normEqDense, cpu>::finalizeCompute(
    linear_regression::Model *a, linear_regression::Model *r,
    const daal::algorithms::Parameter *par)
{
    return finalizeModelNormEq<algorithmFPType, cpu>(a, r);
}

} // namespace internal
} // training
} // linear_regression
} // algorithms
} // namespace daal

#endif
