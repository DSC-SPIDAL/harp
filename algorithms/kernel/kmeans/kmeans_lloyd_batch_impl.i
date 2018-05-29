/* file: kmeans_lloyd_batch_impl.i */
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
//  Implementation of Lloyd method for K-means algorithm.
//--
*/

#include "algorithm.h"
#include "numeric_table.h"
#include "threading.h"
#include "daal_defines.h"
#include "service_memory.h"
#include "service_numeric_table.h"

#include "kmeans_lloyd_impl.i"

using namespace daal::internal;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace kmeans
{
namespace internal
{

#define __DAAL_FABS(a) (((a)>(algorithmFPType)0.0)?(a):(-(a)))

template <Method method, typename algorithmFPType, CpuType cpu>
Status KMeansBatchKernel<method, algorithmFPType, cpu>::compute(const NumericTable *const *a,
    const NumericTable *const *r, const Parameter *par)
{
    NumericTable *ntData     = const_cast<NumericTable *>( a[0] );
    const size_t nIter = par->maxIterations;
    const size_t p = ntData->getNumberOfColumns();
    const size_t n = ntData->getNumberOfRows();
    const size_t nClusters = par->nClusters;

    TArray<int, cpu> clusterS0(nClusters);
    TArray<algorithmFPType, cpu> clusterS1(nClusters*p);
    DAAL_CHECK(clusterS0.get() && clusterS1.get(), services::ErrorMemoryAllocationFailed);

    /* Categorial variables check and support: begin */
    int catFlag = 0;
    for(size_t i = 0; i < p; i++)
    {
        if (ntData->getFeatureType(i) == features::DAAL_CATEGORICAL)
        {
            catFlag = 1;
            break;
        }
    }
    TArray<algorithmFPType, cpu> catCoef(catFlag ? p : 0);
    if(catFlag)
    {
        DAAL_CHECK(catCoef.get(), services::ErrorMemoryAllocationFailed);
        for(size_t i = 0; i < p; i++)
        {
            if (ntData->getFeatureType(i) == features::DAAL_CATEGORICAL)
            {
                catCoef[i] = par->gamma;
            }
            else
            {
                catCoef[i] = (algorithmFPType)1.0;
            }
        }
    }

    ReadRows<algorithmFPType, cpu> mtInClusters(*const_cast<NumericTable*>(a[1]), 0, nClusters);
    DAAL_CHECK_BLOCK_STATUS(mtInClusters);
    WriteOnlyRows<algorithmFPType, cpu> mtClusters(*const_cast<NumericTable*>(r[0]), 0, nClusters);
    DAAL_CHECK_BLOCK_STATUS(mtClusters);

    algorithmFPType *inClusters = const_cast<algorithmFPType*>(mtInClusters.get());
    algorithmFPType *clusters = mtClusters.get();

    TArray<double, cpu> dS1(method == defaultDense ? p : 0);
    if (method == defaultDense)
    {
        DAAL_CHECK(dS1.get(), services::ErrorMemoryAllocationFailed);
    }

    TArray<algorithmFPType, cpu> cValues(nClusters);
    TArray<size_t, cpu> cIndices(nClusters);

    Status s;
    algorithmFPType oldTargetFunc(0.0);
    size_t kIter;
    for(kIter = 0; kIter < nIter; kIter++)
    {
        SharedPtr<task_t<algorithmFPType, cpu> > task = task_t<algorithmFPType, cpu>::create(p, nClusters, inClusters);
        DAAL_CHECK(task.get(), services::ErrorMemoryAllocationFailed);
        DAAL_ASSERT(task);

        s = task->template addNTToTaskThreaded<method>(ntData, catCoef.get());
        if(!s)
        {
            task->kmeansClearClusters(&oldTargetFunc);
            break;
        }

        task->template kmeansComputeCentroids<method>(clusterS0.get(), clusterS1.get(), dS1.get());

        size_t cNum;
        DAAL_CHECK_STATUS(s, task->kmeansComputeCentroidsCandidates(cValues.get(), cIndices.get(), cNum));
        size_t cPos = 0;

        algorithmFPType newCentersGoalFunc = (algorithmFPType)0.0;

        for (size_t i = 0; i < nClusters; i++)
        {
            if ( clusterS0[i] > 0 )
            {
                algorithmFPType coeff = 1.0 / clusterS0[i];

                for (size_t j = 0; j < p; j++)
                {
                    clusters[i * p + j] = clusterS1[i * p + j] * coeff;
                }
            }
            else
            {
                DAAL_CHECK(cPos < cNum, services::ErrorKMeansNumberOfClustersIsTooLarge);
                newCentersGoalFunc += cValues[cPos];
                ReadRows<algorithmFPType, cpu> mtRow(ntData, cIndices[cPos], 1);
                const algorithmFPType *row = mtRow.get();
                daal::services::daal_memcpy_s(&clusters[i * p], p * sizeof(algorithmFPType), row, p * sizeof(algorithmFPType));
                cPos++;
            }
        }

        if ( par->accuracyThreshold > (algorithmFPType)0.0 )
        {
            algorithmFPType newTargetFunc = (algorithmFPType)0.0;

            task->kmeansClearClusters(&newTargetFunc);

            newTargetFunc -= newCentersGoalFunc;

            if ( __DAAL_FABS(oldTargetFunc - newTargetFunc) < par->accuracyThreshold )
            {
                kIter++;
                break;
            }

            oldTargetFunc = newTargetFunc;
        }
        else
        {
            task->kmeansClearClusters(&oldTargetFunc);
            oldTargetFunc -= newCentersGoalFunc;
        }

        inClusters = clusters;
    }

    if( s.ok() && par->assignFlag )
    {
        if(!nIter)
            clusters = inClusters;

        SharedPtr<task_t<algorithmFPType, cpu> > task = task_t<algorithmFPType, cpu>::create(p, nClusters, inClusters);
        DAAL_CHECK(task.get(), services::ErrorMemoryAllocationFailed);
        DAAL_ASSERT(task);

        s = task->template getNTAssignmentsThreaded<method>(ntData, r[1], catCoef.get());
        task->kmeansClearClusters(0);
    }

    WriteOnlyRows<int, cpu> mtIterations(*const_cast<NumericTable *>(r[3]), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(mtIterations);
    *mtIterations.get() = kIter;

    WriteOnlyRows<algorithmFPType, cpu> mtTarget(*const_cast<NumericTable *>(r[2]), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(mtTarget);
    *mtTarget.get() = oldTargetFunc;
    return s;
}

} // namespace daal::algorithms::kmeans::internal
} // namespace daal::algorithms::kmeans
} // namespace daal::algorithms
} // namespace daal
