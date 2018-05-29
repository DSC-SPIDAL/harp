/* file: linear_regression_model.cpp */
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

#include "algorithms/linear_regression/linear_regression_model.h"
#include "algorithms/linear_regression/linear_regression_ne_model.h"
#include "algorithms/linear_regression/linear_regression_qr_model.h"
#include "algorithms/linear_regression/linear_regression_training_types.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace linear_regression
{

Status checkModel(
    linear_regression::Model* model, const daal::algorithms::Parameter &par, size_t nBeta, size_t nResponses, int method)
{
    services::Status s;
    DAAL_CHECK_STATUS(s, linear_model::checkModel(model, par, nBeta, nResponses));

    size_t dimWithoutBeta = (model->getInterceptFlag() ? nBeta : nBeta - 1);

    if(method == linear_regression::training::normEqDense)
    {
        linear_regression::ModelNormEq* modelNormEq = dynamic_cast<linear_regression::ModelNormEq*>(model);
        DAAL_CHECK(modelNormEq, ErrorIncorrectTypeOfModel);

        DAAL_CHECK_STATUS(s, checkNumericTable(modelNormEq->getXTXTable().get(), XTXTableStr(), 0, 0, dimWithoutBeta, dimWithoutBeta));
        DAAL_CHECK_STATUS(s, checkNumericTable(modelNormEq->getXTYTable().get(), XTYTableStr(), 0, 0, dimWithoutBeta, nResponses));
    }
    else if(method == linear_regression::training::qrDense)
    {
        linear_regression::ModelQR* modelQR = dynamic_cast<linear_regression::ModelQR*>(model);
        DAAL_CHECK(modelQR, ErrorIncorrectTypeOfModel);

        DAAL_CHECK_STATUS(s, checkNumericTable(modelQR->getRTable().get(), RTableStr(), 0, 0, dimWithoutBeta, dimWithoutBeta));
        DAAL_CHECK_STATUS(s, checkNumericTable(modelQR->getQTYTable().get(), QTYTableStr(), 0, 0, dimWithoutBeta, nResponses));
    }

    return s;
}

} // namespace linear_regression
} // namespace algorithms
} // namespace daal
