/* file: multiclass_confusion_matrix_types.cpp */
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
//  Declaration of data types for computing the multiclass confusion matrix.
//--
*/


#include "multiclass_confusion_matrix_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace classifier
{
namespace quality_metric
{
namespace multiclass_confusion_matrix
{
namespace interface1
{

__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_CLASSIFIER_MULTICLASS_CONFUSION_MATRIX_RESULT_ID);
Parameter::Parameter(size_t nClasses, double beta) : nClasses(nClasses), beta(beta) {}

Status Parameter::check() const
{
    DAAL_CHECK_EX(beta > 0, ErrorIncorrectParameter, ParameterName, betaStr());
    DAAL_CHECK_EX(nClasses > 1, ErrorIncorrectParameter, ParameterName, nClassesStr());
    return Status();
}


Input::Input() : daal::algorithms::Input(lastInputId + 1) {}

/**
 * Returns the input object of the quality metric of the classification algorithm
 * \param[in] id   Identifier of the input object, \ref InputId
 * \return         Input object that corresponds to the given identifier
 */
NumericTablePtr Input::get(InputId id) const
{
    return staticPointerCast<NumericTable, SerializationIface>(Argument::get(id));
}

/**
 * Sets the input object of the quality metric of the classification algorithm
 * \param[in] id    Identifier of the input object, \ref InputId
 * \param[in] value Pointer to the input object
 */
void Input::set(InputId id, const NumericTablePtr &value)
{
    Argument::set(id, value);
}

/**
 * Checks the correctness of the input object
 * \param[in] parameter Pointer to the structure of the algorithm parameters
 * \param[in] method    Computation method
 */
Status Input::check(const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);
    NumericTablePtr predictedLabelsTable = get(predictedLabels);
    NumericTablePtr groundTruthLabelsTable = get(groundTruthLabels);
    const int unexpectedLayouts = (int)packed_mask;

    DAAL_CHECK_STATUS(s, checkNumericTable(predictedLabelsTable.get(), predictedLabelsStr(), unexpectedLayouts, 0, 1));

    const size_t nRows = predictedLabelsTable->getNumberOfRows();
    return checkNumericTable(groundTruthLabelsTable.get(), groundTruthLabelsStr(), unexpectedLayouts, 0, 1, nRows);
}


Result::Result() : daal::algorithms::Result(lastResultId + 1) {}

/**
 * Returns the quality metric of the classification algorithm
 * \param[in] id    Identifier of the result, \ref ResultId
 * \return          Quality metric of the classification algorithm
 */
NumericTablePtr Result::get(ResultId id) const
{
    return staticPointerCast<NumericTable, SerializationIface>(Argument::get(id));
}

/**
 * Sets the result of the quality metric of the classification algorithm
 * \param[in] id    Identifier of the result, \ref ResultId
 * \param[in] value Pointer to the training result
 */
void Result::set(ResultId id, const NumericTablePtr &value)
{
    Argument::set(id, value);
}

/**
 * Checks the correctness of the Result object
 * \param[in] input     Pointer to the input structure
 * \param[in] parameter Pointer to the structure of the algorithm parameters
 * \param[in] method    Computation method
 */
Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);
    NumericTablePtr confusionMatrixTable = get(confusionMatrix);
    NumericTablePtr multiClassMetricsTable = get(multiClassMetrics);

    const size_t nClasses = algParameter->nClasses;
    const int unexpectedLayouts = (int)packed_mask;

    DAAL_CHECK_STATUS(s, checkNumericTable(confusionMatrixTable.get(), confusionMatrixStr(), unexpectedLayouts, 0, nClasses, nClasses));
    return checkNumericTable(multiClassMetricsTable.get(), multiClassMetricsStr(), unexpectedLayouts, 0, 8, 1);
}

} // namespace interface1
} // multiclass_confusion_matrix
} // namespace quality_metric
} // namespace classifier
} // namespace algorithms
} // namespace daal
