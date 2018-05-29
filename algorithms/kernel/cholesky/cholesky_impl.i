/* file: cholesky_impl.i */
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
//  Implementation of cholesky algorithm
//--
*/

#include "service_numeric_table.h"
#include "service_lapack.h"

using namespace daal::internal;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace cholesky
{
namespace internal
{

template <typename algorithmFPType, CpuType cpu>
bool isFull(NumericTableIface::StorageLayout rLayout);

/**
 *  \brief Kernel for Cholesky calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
Status CholeskyKernel<algorithmFPType, method, cpu>::compute(NumericTable *aTable, NumericTable *r, const daal::algorithms::Parameter *par)
{
    const DAAL_INT dim = (DAAL_INT)(aTable->getNumberOfColumns());   /* Dimension of input feature vectors */

    const NumericTableIface::StorageLayout iLayout = aTable->getDataLayout();
    const NumericTableIface::StorageLayout rLayout = r->getDataLayout();

    WriteOnlyRows<algorithmFPType, cpu> rowsR;
    WriteOnlyPacked<algorithmFPType, cpu> packedR;

    algorithmFPType *L = nullptr;
    if(isFull<algorithmFPType, cpu>(rLayout))
    {
        rowsR.set(*r, 0, dim);
        DAAL_CHECK_BLOCK_STATUS(rowsR);
        L = rowsR.get();
    }
    else
    {
        packedR.set(r);
        DAAL_CHECK_BLOCK_STATUS(packedR);
        L = packedR.get();
    }

    Status s;
    if(isFull<algorithmFPType, cpu>(iLayout))
    {
        ReadRows<algorithmFPType, cpu> rowsA(*aTable, 0, dim);
        DAAL_CHECK_BLOCK_STATUS(rowsA);
        s = copyMatrix(iLayout, rowsA.get(), rLayout, L, dim);
    }
    else
    {
        ReadPacked<algorithmFPType, cpu> packedA(*aTable);
        DAAL_CHECK_BLOCK_STATUS(packedA);
        s = copyMatrix(iLayout, packedA.get(), rLayout, L, dim);
    }
    return s.ok() ? performCholesky(rLayout, L, dim) : s;
}

template <typename algorithmFPType, Method method, CpuType cpu>
Status CholeskyKernel<algorithmFPType, method, cpu>::copyMatrix(NumericTableIface::StorageLayout iLayout,
    const algorithmFPType *pA, NumericTableIface::StorageLayout rLayout, algorithmFPType *pL, DAAL_INT dim) const
{
    if(isFull<algorithmFPType, cpu>(rLayout))
    {
        if(!copyToFullMatrix(iLayout, pA, pL, dim))
            return Status(ErrorIncorrectTypeOfInputNumericTable);

    }
    else
    {
        if(!copyToLowerTrianglePacked(iLayout, pA, pL, dim))
            return Status(ErrorIncorrectTypeOfOutputNumericTable);

    }
    return Status();
}

template <typename algorithmFPType, Method method, CpuType cpu>
Status CholeskyKernel<algorithmFPType, method, cpu>::performCholesky(NumericTableIface::StorageLayout rLayout,
                                                                   algorithmFPType *pL, DAAL_INT dim)
{
    DAAL_INT info;
    char uplo = 'U';

    if (isFull<algorithmFPType, cpu>(rLayout))
    {
        Lapack<algorithmFPType, cpu>::xpotrf(&uplo, &dim, pL, &dim, &info);
    }
    else if (rLayout == NumericTableIface::lowerPackedTriangularMatrix)
    {
        Lapack<algorithmFPType, cpu>::xpptrf(&uplo, &dim, pL, &info);
    }
    else
    {
        return Status(ErrorIncorrectTypeOfOutputNumericTable);
    }

    if(info > 0)
        return Status(Error::create(services::ErrorInputMatrixHasNonPositiveMinor, services::Minor, (int)info));

    return info < 0 ? Status(services::ErrorCholeskyInternal) : Status();
}

template <typename algorithmFPType, CpuType cpu>
bool isFull(NumericTableIface::StorageLayout layout)
{
    int layoutInt = (int) layout;
    if (packed_mask & layoutInt && NumericTableIface::csrArray != layoutInt)
    {
        return false;
    }
    return true;
}

template <typename algorithmFPType, Method method, CpuType cpu>
bool CholeskyKernel<algorithmFPType, method, cpu>::copyToFullMatrix(NumericTableIface::StorageLayout iLayout,
    const algorithmFPType *pA, algorithmFPType *pL, DAAL_INT dim) const
{
    if (isFull<algorithmFPType, cpu>(iLayout))
    {
        for (DAAL_INT i = 0; i < dim; i++)
        {
            for (DAAL_INT j = 0; j <= i; j++)
            {
                pL[i * dim + j] = pA[i * dim + j];
            }
            for (DAAL_INT j = (i + 1); j < dim; j++)
            {
                pL[i * dim + j] = 0;
            }
        }
    }
    else if (iLayout == NumericTableIface::lowerPackedSymmetricMatrix)
    {
        DAAL_INT ind = 0;
        for (DAAL_INT i = 0; i < dim; i++)
        {
            for (DAAL_INT j = 0; j <= i; j++)
            {
                pL[i * dim + j] = pA[ind++];
            }
            for (DAAL_INT j = (i + 1); j < dim; j++)
            {
                pL[i * dim + j] = 0;
            }
        }
    }
    else if (iLayout == NumericTableIface::upperPackedSymmetricMatrix)
    {
        DAAL_INT ind = 0;
        for (DAAL_INT j = 0; j < dim; j++)
        {
            for (DAAL_INT i = 0; i < j ; i++)
            {
                pL[i * dim + j] = 0;
            }
            for (DAAL_INT i = j; i < dim; i++)
            {
                pL[i * dim + j] = pA[ind++];
            }
        }
    }
    else
    {
        return false;
    }
    return true;
}

template <typename algorithmFPType, Method method, CpuType cpu>
bool CholeskyKernel<algorithmFPType, method, cpu>::copyToLowerTrianglePacked(NumericTableIface::StorageLayout iLayout,
    const algorithmFPType *pA, algorithmFPType *pL, DAAL_INT dim) const
{
    if (isFull<algorithmFPType, cpu>(iLayout))
    {
        DAAL_INT ind = 0;
        for (DAAL_INT i = 0; i < dim; i++)
        {
            for (DAAL_INT j = 0; j <= i; j++)
            {
                pL[ind++] = pA[i * dim + j];
            }
        }
    }
    else if (iLayout == NumericTableIface::lowerPackedSymmetricMatrix)
    {
        for (DAAL_INT i = 0; i < dim * (dim + 1) / 2; i++)
        {
            pL[i] = pA[i];
        }
    }
    else if (iLayout == NumericTableIface::upperPackedSymmetricMatrix)
    {
        DAAL_INT ind = 0;
        for(DAAL_INT j = 0; j < dim; j++)
        {
            for(DAAL_INT i = 0; i <= j ; i++)
            {
                pL[ind++] = pA[(dim * i - i * (i - 1) / 2 - i) + j];
            }
        }
    }
    else
    {
        return false;
    }
    return true;
}

} // namespace daal::internal
} // namespace daal::cholesky
}
} // namespace daal
