/* file: covariance_distributed_impl.i */
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
//  Covariance matrix computation algorithm implementation in distributed mode
//--
*/

#ifndef __COVARIANCE_DISTRIBUTED_IMPL_I__
#define __COVARIANCE_DISTRIBUTED_IMPL_I__

#include "covariance_kernel.h"
#include "covariance_impl.i"

namespace daal
{
namespace algorithms
{
namespace covariance
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status CovarianceDistributedKernel<algorithmFPType, method, cpu>::compute(
    DataCollection *partialResultsCollection,
    NumericTable *nObservationsTable,
    NumericTable *crossProductTable,
    NumericTable *sumTable,
    const Parameter *parameter)
{
    const size_t collectionSize = partialResultsCollection->size();
    const size_t nFeatures      = crossProductTable->getNumberOfColumns();

    DEFINE_TABLE_BLOCK( WriteOnlyRows, sumBlock,           sumTable           );
    DEFINE_TABLE_BLOCK( WriteOnlyRows, crossProductBlock,  crossProductTable  );
    DEFINE_TABLE_BLOCK( WriteOnlyRows, nObservationsBlock, nObservationsTable );

    algorithmFPType *sums          = sumBlock.get();
    algorithmFPType *crossProduct  = crossProductBlock.get();
    algorithmFPType *nObservations = nObservationsBlock.get();

    algorithmFPType zero = 0.0;
    daal::services::internal::service_memset<algorithmFPType, cpu>(crossProduct, zero, nFeatures * nFeatures);
    daal::services::internal::service_memset<algorithmFPType, cpu>(sums, zero, nFeatures);
    *nObservations = zero;

    for (size_t i = 0; i < collectionSize; i++)
    {
        PartialResult *patrialResult = static_cast<PartialResult*>((*partialResultsCollection)[i].get());
        NumericTable  *partialSumsTable          = patrialResult->get(covariance::sum).get();
        NumericTable  *partialCrossProductTable  = patrialResult->get(covariance::crossProduct).get();
        NumericTable  *partialNObservationsTable = patrialResult->get(covariance::nObservations).get();

        DEFINE_TABLE_BLOCK( ReadRows, partialSumsBlock,          partialSumsTable          );
        DEFINE_TABLE_BLOCK( ReadRows, partialCrossProductBlock,  partialCrossProductTable  );
        DEFINE_TABLE_BLOCK( ReadRows, partialNObservationsBlock, partialNObservationsTable );

        mergeCrossProductAndSums<algorithmFPType, cpu>(nFeatures, partialCrossProductBlock.get(),
            partialSumsBlock.get(), partialNObservationsBlock.get(), crossProduct, sums, nObservations);
    }

    return services::Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status CovarianceDistributedKernel<algorithmFPType, method, cpu>::finalizeCompute(
    NumericTable *nObservationsTable,
    NumericTable *crossProductTable,
    NumericTable *sumTable,
    NumericTable *covTable,
    NumericTable *meanTable,
    const Parameter *parameter)
{
    return finalizeCovariance<algorithmFPType, cpu>(nObservationsTable,
        crossProductTable, sumTable, covTable, meanTable, parameter);
}

} // internal
} // covariance
} // algorithms
} // daal

#endif
