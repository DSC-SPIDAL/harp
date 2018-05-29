/* file: linear_regression_qr_model.cpp */
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
//  Implementation of the class defining the linear regression model
//--
*/

#include "linear_regression_qr_model_impl.h"
#include "serialization_utils.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
__DAAL_REGISTER_SERIALIZATION_CLASS2(ModelQR, internal::ModelQRImpl, SERIALIZATION_LINEAR_REGRESSION_MODELQR_ID);

namespace internal
{
/**
 * Initializes the linear regression model
 */
Status ModelQRInternal::initialize()
{
    Status s;
    DAAL_CHECK_STATUS(s, super::initialize());
    DAAL_CHECK_STATUS(s, this->setToZero(*_rTable));
    DAAL_CHECK_STATUS(s, this->setToZero(*_qtyTable));
    return s;
}

/**
 * Returns a Numeric table that contains the R factor of QR decomposition
 * \return Numeric table that contains the R factor of QR decomposition
 */
NumericTablePtr ModelQRInternal::getRTable() { return _rTable; }

/**
 * Returns a Numeric table that contains Q'*Y, where Q is the factor of QR decomposition for a data block,
 * Y is the respective block of the matrix of responses
 * \return Numeric table that contains partial sums Q'*Y
 */
NumericTablePtr ModelQRInternal::getQTYTable() { return _qtyTable; }

} // namespace internal
} // namespace linear_regression
} // namespace algorithms
} // namespace daal
