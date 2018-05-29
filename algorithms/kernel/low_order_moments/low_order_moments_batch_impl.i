/* file: low_order_moments_batch_impl.i */
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
//  Implementation of LowOrderMomentsBatchKernel
//--
*/

#ifndef __LOW_ORDER_MOMENTS_BATCH_IMPL_I__
#define __LOW_ORDER_MOMENTS_BATCH_IMPL_I__

#include "low_order_moments_kernel.h"
#include "low_order_moments_impl.i"

namespace daal
{
namespace algorithms
{
namespace low_order_moments
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status LowOrderMomentsBatchKernel<algorithmFPType, method, cpu>::compute( NumericTable *dataTable,
                                                                                    Result *result,
                                                                                    const Parameter *parameter )
{
    if( method == defaultDense)
    {
        switch(parameter->estimatesToCompute)
        {
        case estimatesMinMax: return estimates_batch_minmax::compute_estimates<algorithmFPType,cpu>(dataTable, result);
        case estimatesMeanVariance: return estimates_batch_meanvariance::compute_estimates<algorithmFPType,cpu>(dataTable, result);
            default /* estimatesAll */:
                break;
            }
        return estimates_batch_all::compute_estimates<algorithmFPType, cpu>(dataTable, result);
        }

        bool isOnline = false;
        LowOrderMomentsBatchTask<algorithmFPType, cpu> task(dataTable, result);
        if (method == sumDense || method == sumCSR)
        {
        Status s = retrievePrecomputedStatsIfPossible<algorithmFPType, cpu>(task.nFeatures,
                                                                      task.nVectors,
                                                                      dataTable,
                                                                      task.resultArray[(int)sum],
                                                                            task.resultArray[(int)mean]);
        if(!s)
            return s;
        }

    Status s = computeSum_Mean_SecondOrderRawMoment_Variance_Variation<algorithmFPType, method, cpu>( task.nFeatures,
                                                                                               task.nVectors,
                                                                                               task.dataBlock,
                                                                                               task.resultArray[(int)sum],
                                                                                               task.resultArray[(int)mean],
                                                                                               task.resultArray[(int)secondOrderRawMoment],
                                                                                               task.resultArray[(int)variance],
                                                                                                    task.resultArray[(int)variation]);
    if(!s)
        return s;

        /* Compute standard deviation */
        daal::internal::Math<algorithmFPType,cpu>::vSqrt( task.nFeatures,
                                                          task.resultArray[(int)variance],
                                                          task.resultArray[(int)standardDeviation] );

        initializeMinAndMax<algorithmFPType, cpu>( task.nFeatures,
                                                   task.dataBlock,
                                                   task.resultArray[(int)minimum],
                                                   task.resultArray[(int)maximum] );

        computeMinAndMax<algorithmFPType, cpu>( task.nFeatures,
                                                task.nVectors,
                                                task.dataBlock,
                                                task.resultArray[(int)minimum],
                                                task.resultArray[(int)maximum] );

        computeSumOfSquares<algorithmFPType, cpu>( task.nFeatures,
                                                   task.nVectors,
                                                   task.dataBlock,
                                                   task.resultArray[(int)sumSquares],
                                                   isOnline );

        computeSumOfSquaredDiffsFromMean<algorithmFPType, cpu>( task.nFeatures,
                                                                task.nVectors,
                                                                0,
                                                                task.resultArray[(int)variance],
                                                                task.resultArray[(int)sum],
                                                                task.resultArray[(int)sum],
                                                                task.resultArray[(int)sumSquaresCentered],
                                                                isOnline );
    return s;
}

}
}
}
}

#endif
