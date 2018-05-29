/* file: stump_train_aux.i */
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
//  Implementation of Fast method for Decision Stump algorithm.
//--
*/

#ifndef __STUMP_TRAIN_AUX_I__
#define __STUMP_TRAIN_AUX_I__

#include "daal_defines.h"
#include "service_memory.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace stump
{
namespace training
{
namespace internal
{
using namespace daal::internal;

/**
 *  \brief Perform stump regression for data set X on responses Y with weights W
 */
template <Method method, typename algorithmFPtype, CpuType cpu>
services::Status StumpTrainKernel<method, algorithmFPtype, cpu>::compute(size_t n, const NumericTable *const *a, stump::Model *r,
                                                             const Parameter *par)
{
    const NumericTable *xTable = a[0];
    const NumericTable *yTable = a[1];
    const NumericTable *wTable = (n >= 3 ? a[2] : 0);

    const size_t nFeatures = xTable->getNumberOfColumns();
    const size_t nVectors  = xTable->getNumberOfRows();
    r->setNFeatures(nFeatures);

    services::Status s;
    ReadColumns<algorithmFPtype, cpu> wBlock(const_cast<NumericTable *>(wTable), 0, 0, nVectors);
    TArray<algorithmFPtype, cpu> wArray(wTable ? 0 : nVectors);
    if(wTable)
    {
        /* Here if array of weight is provided */
        DAAL_CHECK_STATUS(s, wBlock.status());
    }
    else
    {
        DAAL_CHECK(wArray.get(), services::ErrorMemoryAllocationFailed);
        /* Here if array of weight is not provided */
        const algorithmFPtype weight = 1.0 / algorithmFPtype(nVectors);
        algorithmFPtype *w = wArray.get();
        for(size_t i = 0; i < nVectors; i++)
            w[i] = weight;
        }

    algorithmFPtype splitPoint;
    algorithmFPtype leftValue;
    algorithmFPtype rightValue;
    size_t splitFeature;
    {
        ReadColumns<algorithmFPtype, cpu> y(const_cast<NumericTable *>(yTable), 0, 0, nVectors);
        DAAL_CHECK_STATUS(s, y.status());
        doStumpRegression(nVectors, nFeatures, xTable, (wTable ? wBlock.get() : wArray.get()), y.get(),
            splitFeature, splitPoint, leftValue, rightValue);
    }

    r->setSplitFeature(splitFeature);
    r->setSplitValue<algorithmFPtype>(splitPoint);
    r->setLeftSubsetAverage<algorithmFPtype>(leftValue);
    r->setRightSubsetAverage<algorithmFPtype>(rightValue);

    return s;
}

} // namespace daal::algorithms::stump::training::internal
}
}
}
} // namespace daal

#endif
