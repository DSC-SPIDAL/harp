/* file: outlierdetection_univariate_dense_default_impl.i */
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
//  Implementation of univariate outlier detection
//--
*/

#ifndef __UNIVAR_OUTLIERDETECTION_DENSE_DEFAULT_IMPL_I__
#define __UNIVAR_OUTLIERDETECTION_DENSE_DEFAULT_IMPL_I__

namespace daal
{
namespace algorithms
{
namespace univariate_outlier_detection
{
namespace internal
{

template <typename algorithmFPType, Method method, CpuType cpu>
Status OutlierDetectionKernel<algorithmFPType, method, cpu>::
compute(NumericTable &dataTable, NumericTable &resultTable,
        NumericTable *locationTable,
        NumericTable *scatterTable,
        NumericTable *thresholdTable)
{
    /* Create micro-tables for input data and output results */
    size_t nFeatures = dataTable.getNumberOfColumns();
    size_t nVectors = resultTable.getNumberOfRows();

    TArray<algorithmFPType, cpu> locationPtr, scatterPtr, thresholdPtr;
    ReadRows<algorithmFPType, cpu> locationBlock(locationTable), scatterBlock(scatterTable), thresholdBlock(thresholdTable);

    algorithmFPType* locationArray  = (locationTable) ?  const_cast<algorithmFPType *>(locationBlock.next(0, 1)):  locationPtr.reset(nFeatures);
    algorithmFPType* scatterArray   = (scatterTable) ?   const_cast<algorithmFPType *>(scatterBlock.next(0, 1)):   scatterPtr.reset(nFeatures);
    algorithmFPType* thresholdArray = (thresholdTable) ? const_cast<algorithmFPType *>(thresholdBlock.next(0, 1)): thresholdPtr.reset(nFeatures);

    DAAL_CHECK(locationArray && scatterArray && thresholdArray, ErrorMemoryAllocationFailed)

    if(!locationTable || !scatterTable || !thresholdTable)
    {
        defaultInitialization(locationArray, scatterArray, thresholdArray, nFeatures);
    }

    /* Allocate memory for storing intermediate results */
    TArray<algorithmFPType, cpu> invScatterPtr(nFeatures);
    DAAL_CHECK(invScatterPtr.get(), ErrorMemoryAllocationFailed)

    /* Calculate results */
    return computeInternal(nFeatures, nVectors, dataTable, resultTable,
                           locationArray,
                           scatterArray,
                           invScatterPtr.get(),
                           thresholdArray);
}

template <typename algorithmFPType, Method method, CpuType cpu>
void OutlierDetectionKernel<algorithmFPType, method, cpu>::defaultInitialization(
    algorithmFPType *locationArray,
    algorithmFPType *scatterArray,
    algorithmFPType *thresholdArray,
    const size_t nFeatures)
{
    for(size_t i = 0; i < nFeatures; i++)
    {
        locationArray[i]  = 0.0;
        scatterArray[i]   = 1.0;
        thresholdArray[i] = 3.0;
    }
}

} // namespace internal

} // namespace univariate_outlier_detection

} // namespace algorithms

} // namespace daal

#endif
