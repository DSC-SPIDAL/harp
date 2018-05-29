/* file: gbt_regression_predict_types.cpp */
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
//  Implementation of gradient boosted trees algorithm classes.
//--
*/

#include "algorithms/gradient_boosted_trees/gbt_regression_predict_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"
#include "gbt_regression_model_impl.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace gbt
{
namespace regression
{
namespace prediction
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_DECISION_FOREST_REGRESSION_PREDICTION_RESULT_ID);

/** Default constructor */
Input::Input() : algorithms::regression::prediction::Input(lastModelInputId + 1) {}
Input::Input(const Input &other) : algorithms::regression::prediction::Input(other) {}

/**
 * Returns an input object for making gradient boosted trees model-based prediction
 * \param[in] id    Identifier of the input object
 * \return          %Input object that corresponds to the given identifier
 */
NumericTablePtr Input::get(NumericTableInputId id) const
{
    return algorithms::regression::prediction::Input::get(algorithms::regression::prediction::NumericTableInputId(id));
}

/**
 * Returns an input object for making gradient boosted trees model-based prediction
 * \param[in] id    Identifier of the input object
 * \return          %Input object that corresponds to the given identifier
 */
gbt::regression::ModelPtr Input::get(ModelInputId id) const
{
    return staticPointerCast<gbt::regression::Model, SerializationIface>(Argument::get(id));
}

/**
 * Sets an input object for making gradient boosted trees model-based prediction
 * \param[in] id      Identifier of the input object
 * \param[in] value   %Input object
 */
void Input::set(NumericTableInputId id, const NumericTablePtr &value)
{
    algorithms::regression::prediction::Input::set(algorithms::regression::prediction::NumericTableInputId(id), value);
}

/**
 * Sets an input object for making gradient boosted trees model-based prediction
 * \param[in] id      Identifier of the input object
 * \param[in] value   %Input object
 */
void Input::set(ModelInputId id, const gbt::regression::ModelPtr &value)
{
    algorithms::regression::prediction::Input::set(algorithms::regression::prediction::ModelInputId(id), value);
}

/**
 * Checks an input object for making gradient boosted trees model-based prediction
 */
services::Status Input::check(const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, algorithms::regression::prediction::Input::check(parameter, method));
    ModelPtr m = get(prediction::model);
    const daal::algorithms::gbt::regression::internal::ModelImpl* pModel =
        static_cast<const daal::algorithms::gbt::regression::internal::ModelImpl*>(m.get());
    DAAL_ASSERT(pModel);
    DAAL_CHECK(pModel->numberOfTrees(), services::ErrorNullModel);
    const Parameter* pPrm = static_cast<const Parameter*>(parameter);
    auto maxNIterations = pModel->numberOfTrees();
    DAAL_CHECK((pPrm->nIterations == 0) || (pPrm->nIterations <= pModel->numberOfTrees()), services::ErrorGbtPredictIncorrectNumberOfIterations);
    return s;
}

Result::Result() : algorithms::regression::prediction::Result(lastResultId + 1) {};

/**
 * Returns the result of gradient boosted trees model-based prediction
 * \param[in] id    Identifier of the result
 * \return          Result that corresponds to the given identifier
 */
NumericTablePtr Result::get(ResultId id) const
{
    return algorithms::regression::prediction::Result::get(algorithms::regression::prediction::ResultId(id));
}

/**
 * Sets the result of gradient boosted trees model-based prediction
 * \param[in] id      Identifier of the input object
 * \param[in] value   %Input object
 */
void Result::set(ResultId id, const NumericTablePtr &value)
{
    algorithms::regression::prediction::Result::set(algorithms::regression::prediction::ResultId(id), value);
}

/**
 * Checks the result of gradient boosted trees model-based prediction
 * \param[in] input   %Input object
 * \param[in] par     %Parameter of the algorithm
 * \param[in] method  Computation method
 */
services::Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, algorithms::regression::prediction::Result::check(input, par, method));
    DAAL_CHECK_EX(get(prediction)->getNumberOfColumns() == 1, ErrorIncorrectNumberOfColumns, ArgumentName, predictionStr());
    return s;
}


} // namespace interface1
} // namespace prediction
} // namespace regression
} // namespace gbt
} // namespace algorithms
} // namespace daal
