/* file: pca_explained_variance_default_batch_impl.i */
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
//  Declaration of template class that computes linear regression quality metrics.
//--
*/

#ifndef __PCA_EXPLAINED_VARIANCE_DEFAULT_BATCH_IMPL_I__
#define __PCA_EXPLAINED_VARIANCE_DEFAULT_BATCH_IMPL_I__

#include "service_memory.h"
#include "service_math.h"
#include "service_lapack.h"
#include "service_numeric_table.h"
#include "service_data_utils.h"
#include "threading.h"
#include "service_error_handling.h"

using namespace daal::internal;
using namespace daal::services;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace quality_metric
{
namespace explained_variance
{
namespace internal
{

template<Method method, typename algorithmFPType, CpuType cpu>
Status ExplainedVarianceKernel<method, algorithmFPType, cpu>::compute
        (const NumericTable& eigenvalues,
         NumericTable& explainedVariances,
         NumericTable& explainedVariancesRatios,
         NumericTable& noiseVariance)
{
    ReadRows<algorithmFPType, cpu, NumericTable> rowsEigenvalues(const_cast<NumericTable&>(eigenvalues), 0, 1);
    const algorithmFPType* pEigenvalues = rowsEigenvalues.get();

    DEFINE_TABLE_BLOCK(WriteOnlyRows, rowsExplainedVariances, &explainedVariances);
    DEFINE_TABLE_BLOCK(WriteOnlyRows, rowsExplainedVariancesRatios, &explainedVariancesRatios);
    DEFINE_TABLE_BLOCK(WriteOnlyRows, rowsNoiseVariance, &noiseVariance);

    algorithmFPType* pExplainedVariances = rowsExplainedVariances.get();
    algorithmFPType* pExplainedVariancesRatios = rowsExplainedVariancesRatios.get();
    algorithmFPType* pNoiseVariance = rowsNoiseVariance.get();

    size_t nFeatures = eigenvalues.getNumberOfColumns();
    size_t nComponents = explainedVariances.getNumberOfColumns();

    algorithmFPType sum = 0;
    algorithmFPType noiseSum = 0;

    for (size_t id = 0; id < nFeatures; ++id)
    {
        sum += pEigenvalues[id];
        if(id >= nComponents)
            noiseSum += pEigenvalues[id];
    }

    PRAGMA_IVDEP
    PRAGMA_VECTOR_ALWAYS
    for (size_t id = 0; id < nComponents; ++id)
    {
        pExplainedVariances[id] = pEigenvalues[id];
        pExplainedVariancesRatios[id] = pEigenvalues[id] / sum;
    }

    int delta = nFeatures - nComponents;
    pNoiseVariance[0] = (delta > 0) ? noiseSum / delta : 0;

    return Status();
}

}
}
}
}
}
}

#endif
