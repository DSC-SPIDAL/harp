/* file: linear_regression_train_dense_qr_distr_step2_impl.i */
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

#ifndef __LINEAR_REGRESSION_TRAIN_DENSE_QR_DISTR_STEP2_IMPL_I__
#define __LINEAR_REGRESSION_TRAIN_DENSE_QR_DISTR_STEP2_IMPL_I__

#include "service_memory.h"
#include "linear_regression_train_kernel.h"
#include "linear_regression_train_dense_qr_impl.i"

using namespace daal::services::internal;

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
Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::qrDense, cpu>::compute(
    size_t n,
    daal::algorithms::Model **partialModels,
    daal::algorithms::Model *r,
    const daal::algorithms::Parameter *par)
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
Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::qrDense, cpu>::merge(
    daal::algorithms::Model *a,
    daal::algorithms::Model *r,
    const daal::algorithms::Parameter *par)
{
    ModelQR *aa = static_cast<ModelQR *>(const_cast<daal::algorithms::Model *>(a));
    ModelQR *rr = static_cast<ModelQR *>(r);

    /* Retrieve data associated with input tables */
    DAAL_INT nBetas = (DAAL_INT)(aa->getNumberOfBetas());
    const linear_regression::Parameter *lrPar = static_cast<const linear_regression::Parameter *>(par);
    if (!lrPar->interceptFlag)
    {
        nBetas--;
    }

    DAAL_INT nResponses  = (DAAL_INT)(aa->getNumberOfResponses());
    DAAL_INT nBetas2 = 2 * nBetas;

    DEFINE_TABLE_BLOCK_EX( ReadRows,  r1Block,   aa->getRTable().get(),   0, nBetas     );
    DEFINE_TABLE_BLOCK_EX( ReadRows,  qty1Block, aa->getQTYTable().get(), 0, nResponses );
    DEFINE_TABLE_BLOCK_EX( WriteRows, r2Block,   rr->getRTable().get(),   0, nBetas     );
    DEFINE_TABLE_BLOCK_EX( WriteRows, qty2Block, rr->getQTYTable().get(), 0, nResponses );

    algorithmFPType *qrR1   = const_cast<algorithmFPType *>(r1Block.get());
    algorithmFPType *qrQTY1 = const_cast<algorithmFPType *>(qty1Block.get());
    algorithmFPType *qrR2   = r2Block.get();
    algorithmFPType *qrQTY2 = qty2Block.get();

    TArray<algorithmFPType, cpu> qrR12(nBetas * nBetas2);
    DAAL_CHECK_MALLOC(qrR12.get());

    TArray<algorithmFPType, cpu> qrQTY12(nResponses * nBetas2);
    DAAL_CHECK_MALLOC(qrQTY12.get());

    TArray<algorithmFPType, cpu> tau(nBetas);
    DAAL_CHECK_MALLOC(tau.get());

    DAAL_INT lwork;
    Status status = computeQRWorkSize<algorithmFPType, cpu>(&nBetas, &nBetas2, qrR12.get(), tau.get(), &lwork);
    DAAL_CHECK_STATUS_VAR(status);

    TArray<algorithmFPType, cpu> work(lwork); DAAL_CHECK_MALLOC(work.get());
    status |= mergeQR<algorithmFPType, cpu>(&nBetas, &nResponses, qrR1, qrQTY1, qrR2,
        qrQTY2, qrR12.get(), qrQTY12.get(), qrR2, qrQTY2, tau.get(), work.get(), &lwork);

    return status;
}

template <typename algorithmFPType, CpuType cpu>
services::Status LinearRegressionTrainDistributedKernel<algorithmFPType, training::qrDense, cpu>::finalizeCompute(
    linear_regression::Model *a,
    linear_regression::Model *r,
    const daal::algorithms::Parameter *par
)
{
    return finalizeModelQR<algorithmFPType, cpu>(a, r);
}

} // internal
} // training
} // linear_regression
} // algorithms
} // daal

#endif
