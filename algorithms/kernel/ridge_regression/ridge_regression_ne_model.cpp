/* file: ridge_regression_ne_model.cpp */
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
//  Implementation of the class defining the ridge regression model
//--
*/

#include "ridge_regression_ne_model_impl.h"
#include "serialization_utils.h"

namespace daal
{
namespace algorithms
{
namespace ridge_regression
{
using namespace daal::data_management;
using namespace daal::services;
__DAAL_REGISTER_SERIALIZATION_CLASS2(ModelNormEq, internal::ModelNormEqImpl, SERIALIZATION_RIDGE_REGRESSION_MODELNORMEQ_ID);

namespace internal
{
/**
 * Initializes the ridge regression model
 */
Status ModelNormEqInternal::initialize()
{
    Status s;
    DAAL_CHECK_STATUS(s, super::initialize());
    DAAL_CHECK_STATUS(s, this->setToZero(*_xtxTable));
    DAAL_CHECK_STATUS(s, this->setToZero(*_xtyTable));
    return s;
}

/**
 * Returns a Numeric table that contains partial sums X'*X
 * \return Numeric table that contains partial sums X'*X
 */
NumericTablePtr ModelNormEqInternal::getXTXTable() { return _xtxTable; }

/**
 * Returns a Numeric table that contains partial sums X'*Y
 * \return Numeric table that contains partial sums X'*Y
 */
NumericTablePtr ModelNormEqInternal::getXTYTable() { return _xtyTable; }

} // namespace internal
} // namespace ridge_regression
} // namespace algorithms
} // namespace daal
