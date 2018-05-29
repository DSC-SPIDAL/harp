/* file: adaboost_predict_impl.i */
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
//  Implementation of Fast method for Ada Boost prediction algorithm.
//--
*/

#ifndef __ADABOOST_PREDICT_IMPL_I__
#define __ADABOOST_PREDICT_IMPL_I__

#include "service_numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace adaboost
{
namespace prediction
{
namespace internal
{
using namespace daal::internal;

template <Method method, typename algorithmFPType, CpuType cpu>
services::Status AdaBoostPredictKernel<method, algorithmFPType, cpu>::compute(const NumericTablePtr& xTable,
    const Model *m, const NumericTablePtr& rTable, const Parameter *par)
{
    const size_t nVectors = xTable->getNumberOfRows();

    Model *boostModel = const_cast<Model *>(m);
    const size_t nWeakLearners = boostModel->getNumberOfWeakLearners();
    services::Status s;
    WriteOnlyColumns<algorithmFPType, cpu> mtR(*rTable, 0, 0, nVectors);
    DAAL_CHECK_BLOCK_STATUS(mtR);
    algorithmFPType *r = mtR.get();
    DAAL_ASSERT(r);

    {
        ReadColumns<algorithmFPType, cpu> mtAlpha(*boostModel->getAlpha(), 0, 0, nWeakLearners);
        DAAL_CHECK_BLOCK_STATUS(mtAlpha);
        DAAL_ASSERT(mtAlpha.get());
        DAAL_CHECK_STATUS(s, this->compute(xTable, m, nWeakLearners, mtAlpha.get(), r, par));
    }

    const algorithmFPType zero = (algorithmFPType)0.0;
    const algorithmFPType one = (algorithmFPType)1.0;
    for(size_t j = 0; j < nVectors; j++)
    {
        r[j] = ((r[j] >= zero) ? one : -one);
    }
    return s;
}

} // namespace daal::algorithms::adaboost::prediction::internal
}
}
}
} // namespace daal

#endif
