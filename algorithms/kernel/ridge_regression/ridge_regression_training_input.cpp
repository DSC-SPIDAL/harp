/* file: ridge_regression_training_input.cpp */
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
//  Implementation of ridge regression algorithm classes.
//--
*/

#include "algorithms/ridge_regression/ridge_regression_training_types.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace ridge_regression
{
namespace training
{
namespace interface1
{

Input::Input() : linear_model::training::Input(lastInputId + 1) {}
Input::Input(const Input& other) : linear_model::training::Input(other){}

/**
 * Returns an input object for ridge regression model-based training
 * \param[in] id    Identifier of the input object
 * \return          %Input object that corresponds to the given identifier
 */
NumericTablePtr Input::get(InputId id) const
{
    return linear_model::training::Input::get(linear_model::training::InputId(id));
}

/**
 * Sets an input object for ridge regression model-based training
 * \param[in] id      Identifier of the input object
 * \param[in] value   Pointer to the object
 */
void Input::set(InputId id, const NumericTablePtr &value)
{
    linear_model::training::Input::set(linear_model::training::InputId(id), value);
}

/**
 * Returns the number of columns in the input data set
 * \return Number of columns in the input data set
 */
size_t Input::getNumberOfFeatures() const { return get(data)->getNumberOfColumns(); }

/**
* Returns the number of dependent variables
* \return Number of dependent variables
*/
size_t Input::getNumberOfDependentVariables() const { return get(dependentVariables)->getNumberOfColumns(); }

/**
* Checks an input object for the ridge regression algorithm
* \param[in] par     Algorithm parameter
* \param[in] method  Computation method
*
 * \return Status of computations
 */
services::Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, linear_model::training::Input::check(par, method));

    const NumericTablePtr dataTable = get(data);
    size_t nRowsInData = dataTable->getNumberOfRows();
    size_t nColumnsInData = dataTable->getNumberOfColumns();

    DAAL_CHECK(nRowsInData >= nColumnsInData, ErrorIncorrectNumberOfObservations);

    const NumericTablePtr dependentVariableTable = get(dependentVariables);
    const size_t nColumnsInDepVariable = dependentVariableTable->getNumberOfColumns();

    TrainParameter *trainParameter   = static_cast<TrainParameter *>(const_cast<daal::algorithms::Parameter *>(par));
    DAAL_CHECK_STATUS(s, trainParameter->check());

    size_t ridgeParamsNumberOfColumns = trainParameter->ridgeParameters->getNumberOfColumns();
    DAAL_CHECK((ridgeParamsNumberOfColumns == 1) || (nColumnsInDepVariable == ridgeParamsNumberOfColumns), ErrorIncorrectNumberOfColumns);
    return services::Status();
}

} // namespace interface1
} // namespace training
} // namespace ridge_regression
} // namespace algorithms
} // namespace daal
