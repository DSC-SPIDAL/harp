/* file: outlierdetection_univariate_kernel.h */
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
//  Declaration of template structs for Outliers Detection.
//--
*/

#ifndef __UNIVAR_OUTLIERDETECTION_KERNEL_H__
#define __UNIVAR_OUTLIERDETECTION_KERNEL_H__

#include "outlier_detection_univariate.h"
#include "kernel.h"
#include "numeric_table.h"
#include "service_math.h"
#include "service_memory.h"
#include "service_numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace univariate_outlier_detection
{
namespace internal
{

using namespace daal::internal;
using namespace daal::internal;
using namespace daal::services;

template <typename algorithmFPType, Method method, CpuType cpu>
struct OutlierDetectionKernel : public Kernel
{
    static const size_t blockSize = 1000;

    /** \brief Detect outliers in the data from input table
               and store resulting weights into output table */
    inline static Status computeInternal(size_t nFeatures, size_t nVectors,
                                       NumericTable &dataTable,
                                       NumericTable &resultTable,
                                       const algorithmFPType *location, const algorithmFPType *scatter, algorithmFPType *invScatter,
                                       const algorithmFPType *threshold)
    {
        ReadRows<algorithmFPType, cpu> dataBlock(dataTable);
        WriteOnlyRows<algorithmFPType, cpu> resultBlock(resultTable);

        const algorithmFPType zero(0.0);
        const algorithmFPType one(1.0);

        PRAGMA_IVDEP
        PRAGMA_VECTOR_ALWAYS
        for (size_t j = 0; j < nFeatures; j++)
        {
            invScatter[j] = one;
            if (scatter[j] != zero)
            {
                invScatter[j] = one / scatter[j];
            }
        }

        size_t nBlocks = nVectors / blockSize;
        if (nBlocks * blockSize < nVectors)
        {
            nBlocks++;
        }

        /* Process input data table in blocks */
        for (size_t iBlock = 0; iBlock < nBlocks; iBlock++)
        {
            size_t startRow = iBlock * blockSize;
            size_t nRowsInBlock = blockSize;
            if (startRow + nRowsInBlock > nVectors)
            {
                nRowsInBlock = nVectors - startRow;
            }

            const algorithmFPType *data = dataBlock.next(startRow, nRowsInBlock);
            algorithmFPType *weight = resultBlock.next(startRow, nRowsInBlock);
            DAAL_CHECK_BLOCK_STATUS(dataBlock);
            DAAL_CHECK_BLOCK_STATUS(resultBlock);

            const algorithmFPType *dataPtr = data;
            algorithmFPType *weightPtr = weight;
            algorithmFPType diff;
            for (size_t i = 0;  i < nRowsInBlock; i++, dataPtr += nFeatures, weightPtr += nFeatures)
            {
                PRAGMA_IVDEP
                PRAGMA_VECTOR_ALWAYS
                for (size_t j = 0; j < nFeatures; j++)
                {
                    weightPtr[j] = one;
                    diff = daal::internal::Math<algorithmFPType, cpu>::sFabs(dataPtr[j] - location[j]);
                    if (scatter[j] != zero)
                    {
                        /* Here if scatter is greater than zero */
                        if (diff * invScatter[j] > threshold[j]) { weightPtr[j] = zero; }
                    }
                    else
                    {
                        /* Here if scatter is equal to zero */
                        if (diff > zero) { weightPtr[j] = zero; }
                    }
                }
            }
        }
        return Status();
    }

    /** \brief Detect outliers in the data from input numeric table
               and store resulting weights into output numeric table */
    Status compute(NumericTable &dataTable, NumericTable &resultTable,
                   NumericTable *locationTable,
                   NumericTable *scatterTable,
                   NumericTable *thresholdTable);

    void defaultInitialization(algorithmFPType *location,
                               algorithmFPType *scatter,
                               algorithmFPType *threshold,
                               const size_t nFeatures);
};

} // namespace internal

} // namespace univariate_outlier_detection

} // namespace algorithms

} // namespace daal

#endif
