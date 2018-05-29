/* file: naivebayes_train_result.cpp */
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
//  Implementation of multinomial naive bayes algorithm and types methods.
//--
*/

#include "multinomial_naive_bayes_training_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace multinomial_naive_bayes
{

namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_NAIVE_BAYES_MODEL_ID);
}

namespace training
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_NAIVE_BAYES_RESULT_ID);
Result::Result() {}

/**
 * Returns the model trained with the naive Bayes training algorithm
 * \param[in] id    Identifier of the result, \ref classifier::training::ResultId
 * \return          Model trained with the classification algorithm
 */
multinomial_naive_bayes::ModelPtr Result::get(classifier::training::ResultId id) const
{
    return services::staticPointerCast<multinomial_naive_bayes::Model, data_management::SerializationIface>(Argument::get(id));
}

/**
* Checks the correctness of Result object
* \param[in] partialResult Pointer to the partial results structure
* \param[in] parameter     Parameter of the algorithm
* \param[in] method        Computation method
*/
services::Status Result::check(const daal::algorithms::PartialResult *partialResult, const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;

    const PartialResult *pres = static_cast<const PartialResult *>(partialResult);
    size_t nFeatures = pres->getNumberOfFeatures();

    DAAL_CHECK_STATUS(s,checkImpl(nFeatures,parameter));

    return s;
}

/**
 * Checks the final result of the naive Bayes training algorithm
 * \param[in] input      %Input of algorithm
 * \param[in] parameter  %Parameter of algorithm
 * \param[in] method     Computation method
 */
services::Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, classifier::training::Result::checkImpl(input, parameter));

    const classifier::training::InputIface *algInput = static_cast<const classifier::training::InputIface *>(input);

    size_t nFeatures = algInput->getNumberOfFeatures();

    DAAL_CHECK_STATUS(s, checkImpl(nFeatures,parameter));

    return s;
}

services::Status Result::checkImpl(size_t nFeatures,const daal::algorithms::Parameter* parameter) const
{
    Status s;
    ModelPtr resModel = get(classifier::training::model);
    DAAL_CHECK(resModel, ErrorNullModel);

    const size_t trainingDataFeatures = resModel->getNFeatures();
    DAAL_CHECK(trainingDataFeatures, services::ErrorModelNotFullInitialized);

    const multinomial_naive_bayes::Parameter *algPar = static_cast<const multinomial_naive_bayes::Parameter *>(parameter);
    size_t nClasses = algPar->nClasses;

    s |= checkNumericTable(resModel->getLogP().get(), logPStr(), 0, 0, 1, nClasses);
    s |= checkNumericTable(resModel->getLogTheta().get(), logThetaStr(), 0, 0, nFeatures, nClasses);

    if(algPar->alpha)
    {
        s |= checkNumericTable(algPar->alpha.get(), alphaStr(), 0, 0, nFeatures, 1);
    }

    return s;
}

}// namespace interface1
}// namespace training
}// namespace multinomial_naive_bayes
}// namespace algorithms
}// namespace daal
