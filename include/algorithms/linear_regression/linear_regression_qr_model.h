/* file: linear_regression_qr_model.h */
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
//  Declaration of the linear regression model class for the QR decomposition-based method
//--
*/

#ifndef __LINREG_QR_MODEL_H__
#define __LINREG_QR_MODEL_H__

#include "algorithms/linear_regression/linear_regression_model.h"

namespace daal
{
namespace algorithms
{
namespace linear_regression
{

namespace interface1
{
/**
 * @ingroup linear_regression
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__LINEAR_REGRESSION__MODELQR"></a>
 * \brief %Model trained with the linear regression algorithm using the QR decomposition-based method
 *
 * \par References
 *      - Parameter class
 *      - Prediction class
 *      - \ref training::interface1::Batch "training::Batch" class
 *      - \ref training::interface1::Online "training::Online" class
 *      - \ref training::interface1::Distributed "training::Distributed" class
 *      - \ref prediction::interface1::Batch "prediction::Batch" class
 */
class DAAL_EXPORT ModelQR : public Model
{
public:
    DECLARE_MODEL(ModelQR, linear_regression::Model);

    virtual ~ModelQR() { }

    /**
     * Returns a Numeric table that contains the R factor of QR decomposition
     * \return Numeric table that contains the R factor of QR decomposition
     */
    virtual data_management::NumericTablePtr getRTable() = 0;

    /**
     * Returns a Numeric table that contains Q'*Y, where Q is the factor of QR decomposition for a data block,
     * Y is the respective block of the matrix of responses
     * \return Numeric table that contains partial sums Q'*Y
     */
    virtual data_management::NumericTablePtr getQTYTable() = 0;
};
typedef services::SharedPtr<ModelQR> ModelQRPtr;
typedef services::SharedPtr<const ModelQR> ModelQRConstPtr;
/** @} */
} // namespace interface1
using interface1::ModelQR;
using interface1::ModelQRPtr;
using interface1::ModelQRConstPtr;

}
}
} // namespace daal
#endif
