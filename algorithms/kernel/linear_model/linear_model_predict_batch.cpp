/* file: linear_model_predict_batch.cpp */
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
//  Implementation of the regression algorithm classes.
//--
*/

#include "algorithms/linear_model/linear_model_predict_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace linear_model
{
namespace prediction
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_LM_PREDICTION_RESULT_ID);

Input::Input(size_t nElements) : regression::prediction::Input(nElements)
{}
Input::Input(const Input &other) : regression::prediction::Input(other)
{}

NumericTablePtr Input::get(NumericTableInputId id) const
{
    return regression::prediction::Input::get(regression::prediction::NumericTableInputId(id));
}

linear_model::ModelPtr Input::get(ModelInputId id) const
{
    return linear_model::Model::cast(regression::prediction::Input::get(regression::prediction::ModelInputId(id)));
}

void Input::set(NumericTableInputId id, const NumericTablePtr &value)
{
    regression::prediction::Input::set(regression::prediction::NumericTableInputId(id), value);
}

void Input::set(ModelInputId id, const linear_model::ModelPtr &value)
{
    regression::prediction::Input::set(regression::prediction::ModelInputId(id), value);
}

Status Input::check(const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, regression::prediction::Input::check(parameter, method));

    size_t nBeta = get(data)->getNumberOfColumns() + 1;
    size_t nResponses = get(model)->getNumberOfResponses();
    return checkNumericTable(get(model)->getBeta().get(),  betaStr(), 0, 0, nBeta, nResponses);
}


Result::Result(size_t nElements) : regression::prediction::Result(nElements) {}

NumericTablePtr Result::get(ResultId id) const
{
    return regression::prediction::Result::get(regression::prediction::ResultId(id));
}

void Result::set(ResultId id, const NumericTablePtr &value)
{
    regression::prediction::Result::set(regression::prediction::ResultId(id), value);
}

Status Result::check(const daal::algorithms::Input * input, const daal::algorithms::Parameter * par, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, regression::prediction::Result::check(input, par, method));
    const Input* in = static_cast<const Input *>(input);
    size_t nResponses = in->get(model)->getNumberOfResponses();

    DAAL_CHECK_EX(get(prediction)->getNumberOfColumns() == nResponses, ErrorIncorrectNumberOfFeatures, ArgumentName, predictionStr());
    return s;
}
}
}
}
}
}
