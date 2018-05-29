/* file: linear_model_train_normeq_finalize_impl.i */
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

#include "linear_model_train_normeq_kernel.h"
#include "service_lapack.h"

namespace daal
{
namespace algorithms
{
namespace linear_model
{
namespace normal_equations
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
Status FinalizeKernel<algorithmFPType, cpu>::compute(const NumericTable &xtxTable,
                                                     const NumericTable &xtyTable,
                                                     NumericTable &xtxFinalTable,
                                                     NumericTable &xtyFinalTable,
                                                     NumericTable &betaTable, bool interceptFlag,
                                                     const KernelHelperIface<algorithmFPType, cpu> &helper)
{
    const size_t nBetas    (betaTable.getNumberOfColumns());
    const size_t nResponses(betaTable.getNumberOfRows());
    const size_t nBetasIntercept = (interceptFlag ? nBetas : (nBetas - 1));

    const size_t xtxSizeInBytes(sizeof(algorithmFPType) * nBetasIntercept * nBetasIntercept);
    const size_t xtySizeInBytes(sizeof(algorithmFPType) * nBetasIntercept * nResponses);

    TArray<algorithmFPType, cpu> betaBufferArray;
    algorithmFPType *betaBuffer(nullptr);
    Status st;
    {
        ReadRowsType xtxBlock(const_cast<NumericTable &>(xtxTable), 0, nBetasIntercept);
        DAAL_CHECK_BLOCK_STATUS(xtxBlock);
        algorithmFPType *xtx = const_cast<algorithmFPType *>(xtxBlock.get());

        if (&xtxTable != &xtxFinalTable)
        {
            DAAL_CHECK_STATUS(st, copyDataToTable(xtx, xtxSizeInBytes, xtxFinalTable));
        }

        {
            ReadRowsType xtyBlock(const_cast<NumericTable &>(xtyTable), 0, nResponses);
            DAAL_CHECK_BLOCK_STATUS(xtyBlock);
            algorithmFPType *xty = const_cast<algorithmFPType *>(xtyBlock.get());

            if (&xtyTable != &xtyFinalTable)
            {
                DAAL_CHECK_STATUS(st, copyDataToTable(xty, xtySizeInBytes, xtyFinalTable));
            }

            betaBufferArray.reset(nResponses * nBetasIntercept);
            betaBuffer = betaBufferArray.get();
            DAAL_CHECK_MALLOC(betaBuffer);

            daal_memcpy_s(betaBuffer, xtySizeInBytes, xty, xtySizeInBytes);
        }
        {
            TArray<algorithmFPType, cpu> xtxCopyArray(nBetasIntercept * nBetasIntercept);
            algorithmFPType *xtxCopy = xtxCopyArray.get();
            DAAL_CHECK_MALLOC(xtxCopy);

            daal_memcpy_s(xtxCopy, xtxSizeInBytes, xtx, xtxSizeInBytes);

            DAAL_CHECK_STATUS(st, helper.computeBetasImpl(nBetasIntercept, xtx, xtxCopy, nResponses,
                                                          betaBuffer, interceptFlag));
        }
    }

    WriteOnlyRowsType betaBlock(betaTable, 0, nResponses);
    DAAL_CHECK_BLOCK_STATUS(betaBlock);
    algorithmFPType *beta = betaBlock.get();

    if (nBetasIntercept == nBetas)
    {
        for(size_t i = 0; i < nResponses; i++)
        {
          PRAGMA_IVDEP
          PRAGMA_VECTOR_ALWAYS
            for(size_t j = 1; j < nBetas; j++)
            {
                beta[i * nBetas + j] = betaBuffer[i * nBetas + j - 1];
            }
            beta[i * nBetas] = betaBuffer[i * nBetas + nBetas - 1];
        }
    }
    else
    {
        for(size_t i = 0; i < nResponses; i++)
        {
          PRAGMA_IVDEP
          PRAGMA_VECTOR_ALWAYS
            for(size_t j = 0; j < nBetas - 1; j++)
            {
                beta[i * nBetas + j + 1] = betaBuffer[i * nBetasIntercept + j];
            }
            beta[i * nBetas] = 0.0;
        }
    }
    return st;
}

template <typename algorithmFPType, CpuType cpu>
Status FinalizeKernel<algorithmFPType, cpu>::copyDataToTable(const algorithmFPType* data,
                                                             size_t dataSizeInBytes, NumericTable &table)
{
    WriteOnlyRowsType block(table, 0, table.getNumberOfRows());
    DAAL_CHECK_BLOCK_STATUS(block);
    algorithmFPType *dst = block.get();

    daal_memcpy_s(dst, dataSizeInBytes, data, dataSizeInBytes);
    return Status();
}

template <typename algorithmFPType, CpuType cpu>
Status FinalizeKernel<algorithmFPType, cpu>::solveSystem(DAAL_INT p, algorithmFPType *a,
                                                         DAAL_INT ny, algorithmFPType *b,
                                                         const ErrorID &internalError)
{
    char up = 'U';
    DAAL_INT info;

    /* Perform L*L' decomposition of X'*X */
    Lapack<algorithmFPType, cpu>::xpotrf(&up, &p, a, &p, &info);
    if (info < 0) { return Status(internalError);   }
    if (info > 0) { return Status(ErrorNormEqSystemSolutionFailed); }

    /* Solve L*L'*b=Y */
    Lapack<algorithmFPType, cpu>::xpotrs(&up, &p, &ny, a, &p, b, &p, &info);
    DAAL_CHECK(info == 0, internalError);
    return Status();
}

}
}
}
}
}
}
