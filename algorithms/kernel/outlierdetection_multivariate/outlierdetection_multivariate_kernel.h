/* file: outlierdetection_multivariate_kernel.h */
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
//  Declaration of template structs for multivariate outlier detection
//--
*/

#ifndef __OUTLIERDETECTION_MULTIVARIATE_KERNEL_H__
#define __OUTLIERDETECTION_MULTIVARIATE_KERNEL_H__

#include "outlier_detection_multivariate.h"
#include "kernel.h"
#include "service_numeric_table.h"
#include "service_math.h"

using namespace daal::internal;
using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace multivariate_outlier_detection
{
namespace internal
{

template <typename algorithmFPType, Method method, CpuType cpu>
struct OutlierDetectionKernel : public Kernel
{
    static const size_t blockSize = 1000;

    /** \brief Calculate Mahalanobis distance for a block of observations */
    inline void mahalanobisDistance(const size_t nFeatures,
                                    const size_t nVectors,
                                    const algorithmFPType *data,
                                    const algorithmFPType *location,
                                    const algorithmFPType *invScatter,
                                    algorithmFPType *distance,
                                    algorithmFPType *buffer);

    inline void defaultInitialization(algorithmFPType *location,
                                      algorithmFPType *scatter,
                                      algorithmFPType *threshold,
                                      const size_t nFeatures);

    /** \brief Detect outliers in the data from input micro-table
               and store resulting weights into output micro-table */
    inline Status computeInternal(const size_t nFeatures,
                                  const size_t nVectors,
                                  NumericTable &dataTable,
                                  NumericTable &resultTable,
                                  const algorithmFPType *location,
                                  const algorithmFPType *scatter,
                                  const algorithmFPType threshold,
                                  algorithmFPType *buffer);

    Status compute(NumericTable &dataTable,
                   NumericTable *locationTable,
                   NumericTable *scatterTable,
                   NumericTable *thresholdTable,
                   NumericTable &resultTable);
};

/**
 * Added to support deprecated baconDense value
 */
template <typename algorithmFPType, CpuType cpu>
struct OutlierDetectionKernel<algorithmFPType, baconDense, cpu> : public Kernel
{
    Status compute(NumericTable &dataTable,
                   NumericTable *locationTable,
                   NumericTable *scatterTable,
                   NumericTable *thresholdTable,
                   NumericTable &resultTable) {return services::Status();}
};

} // namespace internal

} // namespace multivariate_outlier_detection

} // namespace algorithms

} // namespace daal

#endif
