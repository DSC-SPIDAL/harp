/* file: low_order_moments_online_impl.i */
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
//  Implementation of LowOrderMomentsOnlineKernel
//--
*/

#ifndef __LOW_ORDER_MOMENTS_ONLINE_IMPL_I__
#define __LOW_ORDER_MOMENTS_ONLINE_IMPL_I__

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
using namespace daal::services;
template<typename algorithmFPType, Method method, CpuType cpu>
services::Status LowOrderMomentsOnlineKernel<algorithmFPType, method, cpu>::compute(
            NumericTable *dataTable, PartialResult *partialResult,
            const Parameter *parameter, bool isOnline)
{
    if(method == defaultDense)
    {
        switch(parameter->estimatesToCompute)
        {
            case estimatesMinMax:
                return estimates_online_minmax::compute_estimates<algorithmFPType, method, cpu>( dataTable, partialResult, isOnline);
            case estimatesMeanVariance:
                return estimates_online_meanvariance::compute_estimates<algorithmFPType, method, cpu>( dataTable, partialResult, isOnline);
            default /* estimatesAll */:
                break;
            }
        return estimates_online_all::compute_estimates<algorithmFPType, method, cpu>(dataTable, partialResult, isOnline);
        }

    LowOrderMomentsOnlineTask<algorithmFPType, cpu> task(dataTable);
    Status s;
    DAAL_CHECK_STATUS(s, task.init(partialResult, isOnline));
        if (method == sumDense || method == sumCSR)
        {
        s = retrievePrecomputedStatsIfPossible<algorithmFPType, cpu>(task.nFeatures, task.nVectors,
            dataTable, task.resultArray[(int)partialSum], task.mean);
        if(!s)
            return s;
        }

    s = computeSumAndVariance<algorithmFPType, method, cpu>(task.nFeatures, task.nVectors, task.dataBlock,
        task.resultArray[(int)partialSum], task.prevSums, task.mean, task.raw2Mom, task.variance, isOnline);
    if(!s)
        return s;

        if (!isOnline)
        {
            initializeMinAndMax<algorithmFPType, cpu>(task.nFeatures, task.dataBlock,
                task.resultArray[(int)partialMinimum], task.resultArray[(int)partialMaximum]);
        }

        computeMinAndMax<algorithmFPType, cpu>(task.nFeatures, task.nVectors, task.dataBlock,
            task.resultArray[(int)partialMinimum], task.resultArray[(int)partialMaximum]);

        computeSumOfSquares<algorithmFPType, cpu>(task.nFeatures, task.nVectors, task.dataBlock,
            task.resultArray[(int)partialSumSquares], isOnline);

        computeSumOfSquaredDiffsFromMean<algorithmFPType, cpu>(task.nFeatures, task.nVectors,
            (size_t)(task.resultArray[(int)nObservations][0]),
            task.variance, task.resultArray[(int)partialSum], task.prevSums,
            task.resultArray[(int)partialSumSquaresCentered], isOnline);

        task.resultArray[(int)nObservations][0] += (algorithmFPType)(task.nVectors);

    return s;
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status LowOrderMomentsOnlineKernel<algorithmFPType, method, cpu>::finalizeCompute(
            NumericTable *nObservationsTable,
            NumericTable *sumTable, NumericTable *sumSqTable, NumericTable *sumSqCenTable,
            NumericTable *meanTable, NumericTable *raw2MomTable, NumericTable *varianceTable,
            NumericTable *stDevTable, NumericTable *variationTable,
            const Parameter *parameter)
{
    LowOrderMomentsFinalizeTask<algorithmFPType, cpu> task(
        nObservationsTable, sumTable, sumSqTable, sumSqCenTable, meanTable,
        raw2MomTable, varianceTable, stDevTable, variationTable);

    finalize<algorithmFPType, cpu>(task);
    return Status();
}

}
}
}
}

#endif