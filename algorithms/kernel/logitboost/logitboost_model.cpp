/* file: logitboost_model.cpp */
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
//  Implementation of class defining LogitBoost model.
//--
*/

#include "algorithms/boosting/logitboost_model.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace logitboost
{
namespace interface1
{

__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_LOGITBOOST_MODEL_ID);
/** Default constructor */
Parameter::Parameter() : boosting::Parameter(), accuracyThreshold(0.0), maxIterations(10), nClasses(0),
    weightsDegenerateCasesThreshold(1e-10), responsesDegenerateCasesThreshold(1e-10) {}

/**
 * Constructs LogitBoost parameter structure
 * \param[in] wlTrain       Pointer to the training algorithm of the weak learner
 * \param[in] wlPredict     Pointer to the prediction algorithm of the weak learner
 * \param[in] acc           Accuracy of the LogitBoost training algorithm
 * \param[in] maxIter       Maximal number of terms in additive regression
 * \param[in] nC            Number of classes in the training data set
 * \param[in] wThr          Threshold to avoid degenerate cases when calculating weights W
 * \param[in] zThr          Threshold to avoid degenerate cases when calculating responses Z
 */
Parameter::Parameter(const SharedPtr<weak_learner::training::Batch>&   wlTrain,
    const SharedPtr<weak_learner::prediction::Batch>& wlPredict,
          double acc, size_t maxIter, size_t nC, double wThr, double zThr) :
    boosting::Parameter(wlTrain, wlPredict),
    accuracyThreshold(acc), maxIterations(maxIter), nClasses(nC), weightsDegenerateCasesThreshold(wThr), responsesDegenerateCasesThreshold(zThr) {}

services::Status Parameter::check() const
{
    services::Status s = boosting::Parameter::check();
    if(!s) return s;
    DAAL_CHECK_EX(accuracyThreshold >= 0 && accuracyThreshold < 1, ErrorIncorrectParameter, ParameterName, accuracyThresholdStr());
    DAAL_CHECK_EX(maxIterations > 0, ErrorIncorrectParameter, ParameterName, maxIterationsStr());
    DAAL_CHECK_EX(nClasses >= 2, ErrorIncorrectParameter, ParameterName, nClassesStr());
    DAAL_CHECK_EX(weightsDegenerateCasesThreshold > 0, ErrorIncorrectParameter, ParameterName, weightsDegenerateCasesThresholdStr());
    DAAL_CHECK_EX(responsesDegenerateCasesThreshold > 0, ErrorIncorrectParameter, ParameterName, responsesDegenerateCasesThresholdStr());
    return s;
}


Model::Model(size_t nFeatures, const Parameter *par, services::Status &st) :
    boosting::Model(nFeatures, st),
    _nIterations(par->maxIterations) { }

/**
 * Constructs the LogitBoost model
 * \param[in]  nFeatures Number of features in the dataset
 * \param[in]  par       Pointer to the parameter structure of the LogitBoost algorithm
 * \param[out] stat      Status of the model construction
 */
ModelPtr Model::create(size_t nFeatures, const Parameter *par, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, nFeatures, par);
}

/**
 * Sets the number of iterations for the algorithm
 * @param nIterations   Number of iterations
 */
void Model::setIterations(size_t nIterations)
{
    _nIterations = nIterations;
}

/**
 * Returns the number of iterations done by the training algorithm
 * \return The number of iterations done by the training algorithm
 */
size_t Model::getIterations() const
{
    return _nIterations;
}


} // namespace interface1
} // namespace logitboost
} // namespace algorithms
} // namespace daal
