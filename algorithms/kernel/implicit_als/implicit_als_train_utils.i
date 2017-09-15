/* file: implicit_als_train_utils.i */
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

#ifndef __IMPLICIT_ALS_TRAIN_UTILS_I__
#define __IMPLICIT_ALS_TRAIN_UTILS_I__

#include "implicit_als_train_utils.h"
#include "service_memory.h"
#include "service_sort.h"
#include "service_numeric_table.h"

using namespace daal::internal;
using namespace daal::services;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace implicit_als
{
namespace training
{
namespace internal
{

template<typename algorithmFPType, CpuType cpu>
services::Status csr2csc(size_t nItems, size_t nUsers,
            const algorithmFPType *csrdata, const size_t *colIndices, const size_t *rowOffsets,
            algorithmFPType *cscdata, size_t *rowIndices, size_t *colOffsets)
{
    /* Convert CSR to COO */
    size_t dataSize = rowOffsets[nUsers] - rowOffsets[0];
    TArray<size_t, cpu> cooColIndicesPtr(dataSize);
    size_t *cooColIndices = cooColIndicesPtr.get();
    DAAL_CHECK_MALLOC(cooColIndices);

    daal_memcpy_s(cscdata, dataSize * sizeof(algorithmFPType), csrdata, dataSize * sizeof(algorithmFPType));
    daal_memcpy_s(cooColIndices, dataSize * sizeof(size_t), colIndices, dataSize * sizeof(size_t));

    /* Create array of row indices for COO data */
    for (size_t i = 1; i <= nUsers; i++)
    {
        size_t rowStart = rowOffsets[i-1] - 1;
        size_t rowEnd   = rowOffsets[i] - 1;
        for (size_t k = rowStart; k < rowEnd; k++)
        {
            rowIndices[k] = i;
        }
    }

    /* Sort arrays that represent data in COO format (values, column indices and row indices) over the column indices,
       and re-order arrays of values and row indices accordingly */
    daal::algorithms::internal::qSort<size_t, algorithmFPType, size_t, cpu>(dataSize, cooColIndices, cscdata, rowIndices);

    /* Create an array of columns offsets for the data in CSC format */
    size_t colOffset = 1;
    size_t colOffsetIndex = 0;
    for (; colOffsetIndex < cooColIndices[0]; colOffsetIndex++)
    {
        colOffsets[colOffsetIndex] = 1;
    }
    for (size_t i = 1; i < dataSize; i++)
    {
        if (cooColIndices[i] != cooColIndices[i - 1])
        {
            if (cooColIndices[i] == cooColIndices[i - 1] + 1)
            {
                colOffsets[colOffsetIndex++] = i + 1;
            }
            else
            {
                for (size_t k = cooColIndices[i - 1]; k < cooColIndices[i]; k++)
                {
                    colOffsets[colOffsetIndex++] = i + 1;
                }
            }
        }
    }
    for (size_t i = colOffsetIndex; i <= nItems; i++)
    {
        colOffsets[i] = rowOffsets[nUsers];
    }

    return Status();
}

}
}
}
}
}
#endif
