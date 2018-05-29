/* file: ridge_regression_ne_model.h */
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
//  Declaration of the ridge regression model class for the normal equations method
//--
*/

#ifndef __RIDGE_REGRESSION_NE_MODEL_H__
#define __RIDGE_REGRESSION_NE_MODEL_H__

#include "algorithms/ridge_regression/ridge_regression_model.h"

namespace daal
{
namespace algorithms
{
namespace ridge_regression
{
namespace interface1
{
/**
 * @ingroup ridge_regression
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__RIDGE_REGRESSION__MODELNORMEQ"></a>
 * \brief %Model trained with the ridge regression algorithm using the normal equations method
 *
 * \tparam modelFPType  Data type to store ridge regression model data, double or float
 *
 * \par References
 *      - Parameter class
 *      - \ref training::interface1::Batch "training::Batch" class
 *      - \ref training::interface1::Online "training::Online" class
 *      - \ref training::interface1::Distributed "training::Distributed" class
 *      - \ref prediction::interface1::Batch "prediction::Batch" class
 */
class DAAL_EXPORT ModelNormEq : public Model
{
public:
    DECLARE_MODEL(ModelNormEq, ridge_regression::Model);

    virtual ~ModelNormEq() { }

    /**
     * Returns a Numeric table that contains partial sums X'*X
     * \return Numeric table that contains partial sums X'*X
     */
    virtual data_management::NumericTablePtr getXTXTable() = 0;

    /**
     * Returns a Numeric table that contains partial sums X'*Y
     * \return Numeric table that contains partial sums X'*Y
     */
    virtual data_management::NumericTablePtr getXTYTable() = 0;
};
typedef services::SharedPtr<ModelNormEq> ModelNormEqPtr;
typedef services::SharedPtr<const ModelNormEq> ModelNormEqConstPtr;
/** @} */
} // namespace interface1
using interface1::ModelNormEq;
using interface1::ModelNormEqPtr;
using interface1::ModelNormEqConstPtr;

} // namespace ridge_regression
} // namespace algorithms
} // namespace daal

#endif
