/* file: linear_model_train_qr_merge_impl.i */
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
//  Implementation of common base classes for normal equations model training.
//--
*/

#include "linear_model_train_qr_kernel.h"
#include "service_lapack.h"

namespace daal
{
namespace algorithms
{
namespace linear_model
{
namespace qr
{
namespace training
{
namespace internal
{
using namespace daal::services;
using namespace daal::data_management;
using namespace daal::internal;
using namespace daal::services::internal;

template <typename algorithmFPType, CpuType cpu>
Status MergeKernel<algorithmFPType, cpu>::compute(size_t n, NumericTable **partialr, NumericTable **partialqty,
                          NumericTable &rTable, NumericTable &qtyTable)
{
    size_t nBetas    (rTable.getNumberOfRows());
    size_t nBetas2   (2 * nBetas);
    size_t nResponses(qtyTable.getNumberOfRows());

    TArray<algorithmFPType, cpu> rMerge(nBetas * nBetas2);
    DAAL_CHECK_MALLOC(rMerge.get());

    TArray<algorithmFPType, cpu> qtyMerge(nResponses * nBetas2);
    DAAL_CHECK_MALLOC(qtyMerge.get());

    TArray<algorithmFPType, cpu> tau(nBetas);
    DAAL_CHECK_MALLOC(tau.get());

    WriteRowsType rFinalBlock(rTable, 0, nBetas);
    DAAL_CHECK_BLOCK_STATUS(rFinalBlock);
    algorithmFPType *rFinal = rFinalBlock.get();

    WriteRowsType qtyFinalBlock(qtyTable, 0, nResponses);
    DAAL_CHECK_BLOCK_STATUS(qtyFinalBlock);
    algorithmFPType *qtyFinal = qtyFinalBlock.get();

    ReadRowsType rBlock(partialr[0], 0, nBetas);
    DAAL_CHECK_BLOCK_STATUS(rBlock);
    const algorithmFPType *r = rBlock.get();

    ReadRowsType qtyBlock(partialqty[0], 0, nResponses);
    DAAL_CHECK_BLOCK_STATUS(qtyBlock);
    const algorithmFPType *qty = qtyBlock.get();

    const size_t rSizeInBytes   =     nBetas * nBetas * sizeof(algorithmFPType);
    const size_t qtySizeInBytes = nResponses * nBetas * sizeof(algorithmFPType);
    daal_memcpy_s(rFinal, rSizeInBytes, r, rSizeInBytes);
    daal_memcpy_s(qtyFinal, qtySizeInBytes, qty, qtySizeInBytes);

    DAAL_INT lwork;
    Status st = CommonKernel<algorithmFPType, cpu>::computeWorkSize(nBetas2, nBetas, nResponses, lwork);
    DAAL_CHECK_STATUS_VAR(st);

    TArray<algorithmFPType, cpu> work(lwork);
    DAAL_CHECK_MALLOC(work.get());

    for (size_t i = 1; i < n; i++)
    {
        rBlock.set(partialr[i], 0, nBetas);
        DAAL_CHECK_BLOCK_STATUS(rBlock);
        r = rBlock.get();

        qtyBlock.set(partialqty[i], 0, nResponses);
        DAAL_CHECK_BLOCK_STATUS(qtyBlock);
        qty = qtyBlock.get();

        st |= CommonKernel<algorithmFPType, cpu>::merge(nBetas, nResponses,
            r, qty, rFinal, qtyFinal, rMerge.get(), qtyMerge.get(), rFinal, qtyFinal, tau.get(), work.get(),
            lwork);
        DAAL_CHECK_STATUS_VAR(st);
    }
    return st;
}

}
}
}
}
}
}
