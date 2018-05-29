/* file: low_order_moments_distributed_impl.i */
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
//  Implementation of LowOrderMomentsDistributedKernel
//--
*/

#ifndef __LOW_ORDER_MOMENTS_DISTRIBUTED_IMPL_I__
#define __LOW_ORDER_MOMENTS_DISTRIBUTED_IMPL_I__

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
services::Status LowOrderMomentsDistributedKernel<algorithmFPType, method, cpu>::compute(
            data_management::DataCollection *partialResultsCollection,
            PartialResult *partialResult, const Parameter *parameter)
{
    TArray<int, cpu> partialNObservations(partialResultsCollection->size());
    if (!partialNObservations.get())
        return Status(services::ErrorMemoryAllocationFailed);

    mergeNObservations<algorithmFPType, cpu>(partialResultsCollection, partialResult, partialNObservations.get());
    mergeMinAndMax<algorithmFPType, cpu>(partialResultsCollection, partialResult);
    mergeSums<algorithmFPType, cpu>(partialResultsCollection, partialResult, partialNObservations.get());
    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status LowOrderMomentsDistributedKernel<algorithmFPType, method, cpu>::finalizeCompute(
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
