/* file: linear_regression_training_result.h */
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
//  Implementation of the linear regression algorithm interface
//--
*/

#ifndef __LINEAR_REGRESSION_TRAINING_RESULT_
#define __LINEAR_REGRESSION_TRAINING_RESULT_

#include "algorithms/linear_regression/linear_regression_training_types.h"
#include "linear_regression_ne_model_impl.h"
#include "linear_regression_qr_model_impl.h"

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace training
{
using namespace daal::services;

/**
 * Allocates memory to store the result of linear regression model-based training
 * \param[in] input Pointer to an object containing the input data
 * \param[in] method Computation method for the algorithm
 * \param[in] parameter %Parameter of linear regression model-based training
 */
template<typename algorithmFPType>
DAAL_EXPORT Status Result::allocate(const daal::algorithms::Input *input, const Parameter *parameter, const int method)
{
    const Input *in = static_cast<const Input *>(input);

    const algorithmFPType dummy = 1.0;
    if(method == qrDense)
    {
        set(model, linear_regression::ModelPtr(new linear_regression::internal::ModelQRImpl(in->getNumberOfFeatures(), in->getNumberOfDependentVariables(), *parameter, dummy)));
    }
    else if(method == normEqDense)
    {
        set(model, linear_regression::ModelPtr(new linear_regression::internal::ModelNormEqImpl(in->getNumberOfFeatures(), in->getNumberOfDependentVariables(), *parameter, dummy)));
    }

    return Status();
}

/**
 * Allocates memory to store the result of linear regression model-based training
 * \param[in] partialResult Pointer to an object containing the input data
 * \param[in] method        Computation method of the algorithm
 * \param[in] parameter     %Parameter of linear regression model-based training
 */
template<typename algorithmFPType>
DAAL_EXPORT Status Result::allocate(const daal::algorithms::PartialResult *partialResult, const Parameter *parameter, const int method)
{
    const PartialResult *partialRes = static_cast<const PartialResult *>(partialResult);

    const algorithmFPType dummy = 1.0;
    if(method == qrDense)
    {
        set(model, linear_regression::ModelPtr(new linear_regression::internal::ModelQRImpl(partialRes->getNumberOfFeatures(), partialRes->getNumberOfDependentVariables(), *parameter, dummy)));
    }
    else if(method == normEqDense)
    {
        set(model, linear_regression::ModelPtr(new linear_regression::internal::ModelNormEqImpl(partialRes->getNumberOfFeatures(), partialRes->getNumberOfDependentVariables(), *parameter, dummy)));
    }

    return Status();
}

} // namespace training
} // namespace linear_regression
} // namespace algorithms
} // namespace daal

#endif
