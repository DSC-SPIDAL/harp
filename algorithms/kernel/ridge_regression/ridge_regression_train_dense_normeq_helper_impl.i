/* file: ridge_regression_train_dense_normeq_helper_impl.i */
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
//  Implementation of auxiliary functions for ridge regression Normal Equations (normEqDense) method.
//--
*/

#ifndef __RIDGE_REGRESSION_TRAIN_DENSE_NORMEQ_HELPER_IMPL_I__
#define __RIDGE_REGRESSION_TRAIN_DENSE_NORMEQ_HELPER_IMPL_I__

#include "ridge_regression_train_kernel.h"

namespace daal
{
namespace algorithms
{
namespace ridge_regression
{
namespace training
{
namespace internal
{
using namespace daal::algorithms::linear_model::normal_equations::training::internal;

template <typename algorithmFPType, CpuType cpu>
Status KernelHelper<algorithmFPType, cpu>::computeBetasImpl(DAAL_INT p, const algorithmFPType *a,
                                                            algorithmFPType *aCopy, DAAL_INT ny,
                                                            algorithmFPType *b, bool interceptFlag) const
{
    size_t nRidge = _ridge.getNumberOfRows();
    ReadRows<algorithmFPType, cpu> ridgeBlock(const_cast<NumericTable &>(_ridge), 0, nRidge);
    const algorithmFPType *ridge = ridgeBlock.get();

    const DAAL_INT pToFix = (interceptFlag ? p - 1 : p);

    Status st;
    if (nRidge == 1)
    {
        for(DAAL_INT i = 0, idx = 0; i < pToFix; i++, idx += (p + 1))
        {
            aCopy[idx] += *ridge;
        }

        st |= FinalizeKernel<algorithmFPType, cpu>::solveSystem(p, aCopy, ny, b,
            ErrorRidgeRegressionInternal);
        DAAL_CHECK_STATUS_VAR(st);
    }
    else
    {
        algorithmFPType * bPtr = b;
        const size_t aSizeInBytes = p * p * sizeof(algorithmFPType);
        for (DAAL_INT j = 0; j < ny; j++, bPtr += (pToFix + 1))
        {
            daal::services::daal_memcpy_s(aCopy, aSizeInBytes, a, aSizeInBytes);
            for(DAAL_INT i = 0, idx = 0; i < pToFix; i++, idx += (p + 1))
            {
                aCopy[idx] += ridge[j];
            }

            DAAL_INT one(1);

            st |= FinalizeKernel<algorithmFPType, cpu>::solveSystem(p, aCopy, one, b,
                ErrorRidgeRegressionInternal);
            DAAL_CHECK_STATUS_VAR(st);
        }
    }
    return st;
}

} // namespace internal
} // namespace training
} // namespace ridge_regression
} // namespace algorithms
} // namespace daal

#endif
