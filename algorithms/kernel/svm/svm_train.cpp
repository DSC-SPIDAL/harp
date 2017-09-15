/* file: svm_train.cpp */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
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
//  Implementation of svm algorithm and types methods.
//--
*/

#include "svm_train_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace svm
{

namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_SVM_MODEL_ID);

services::Status Parameter::check() const
{
    services::Status s;
    DAAL_CHECK_STATUS(s, classifier::Parameter::check());
    if(C <= 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, cBoundStr()));
    }
    if(accuracyThreshold <= 0 || accuracyThreshold >= 1)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, accuracyThresholdStr()));
    }
    if(tau <= 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, tauStr()));
    }
    if(maxIterations == 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, maxIterationsStr()));
    }
    if(!kernel.get())
    {
        return services::Status(services::Error::create(services::ErrorNullAuxiliaryAlgorithm, services::ParameterName, kernelFunctionStr()));
    }
    if(shrinkingStep == 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, shrinkingStepStr()));
    }
    return s;
}

}

namespace training
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_SVM_TRAINING_RESULT_ID);
Result::Result() : classifier::training::Result() {}

/**
 * Returns the model trained with the SVM algorithm
 * \param[in] id    Identifier of the result, \ref classifier::training::ResultId
 * \return          Model trained with the SVM algorithm
 */
daal::algorithms::svm::ModelPtr Result::get(classifier::training::ResultId id) const
{
    return services::staticPointerCast<daal::algorithms::svm::Model, data_management::SerializationIface>(Argument::get(id));
}

Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const
{
    Status s;
    DAAL_CHECK_STATUS(s, classifier::training::Result::check(input, parameter, method));
    daal::algorithms::svm::ModelPtr m = get(classifier::training::model);
    if(!m->getSupportVectors())
        s.add(services::Error::create(ErrorModelNotFullInitialized, services::ArgumentName, supportVectorsStr()));
    if(!m->getClassificationCoefficients())
        s.add(services::Error::create(ErrorModelNotFullInitialized, services::ArgumentName, classificationCoefficientsStr()));
    return s;
}

}// namespace interface1
}// namespace training
}// namespace svm
}// namespace algorithms
}// namespace daal
