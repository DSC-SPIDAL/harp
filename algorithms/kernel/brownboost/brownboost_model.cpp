/* file: brownboost_model.cpp */
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
//  Implementation of class defining Brown Boost model.
//--
*/

#include "algorithms/boosting/brownboost_model.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace brownboost
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_BROWNBOOST_MODEL_ID);

/** Default constructor */
Parameter::Parameter() : boosting::Parameter(), accuracyThreshold(0.3), maxIterations(10),
    newtonRaphsonAccuracyThreshold(1.0e-3), newtonRaphsonMaxIterations(100), degenerateCasesThreshold(1.0e-2) {}

/**
 * Constructs BrownBoost parameter structure
 * \param[in] wlTrain       Pointer to the training algorithm of the weak learner
 * \param[in] wlPredict     Pointer to the prediction algorithm of the weak learner
 * \param[in] acc           Accuracy of the BrownBoost training algorithm
 * \param[in] maxIter       Maximal number of iterations of the BrownBoost training algorithm
 * \param[in] nrAcc         Accuracy threshold for Newton-Raphson iterations in the BrownBoost training algorithm
 * \param[in] nrMaxIter     Maximal number of Newton-Raphson iterations in the BrownBoost training algorithm
 * \param[in] dcThreshold          Threshold needed  to avoid degenerate cases in the BrownBoost training algorithm
 */
Parameter::Parameter(services::SharedPtr<weak_learner::training::Batch>   wlTrain,
          services::SharedPtr<weak_learner::prediction::Batch> wlPredict,
          double acc, size_t maxIter, double nrAcc, size_t nrMaxIter, double dcThreshold) :
    boosting::Parameter(wlTrain, wlPredict), accuracyThreshold(acc), maxIterations(maxIter),
    newtonRaphsonAccuracyThreshold(nrAcc), newtonRaphsonMaxIterations(nrMaxIter), degenerateCasesThreshold(dcThreshold) {}

services::Status Parameter::check() const
{
    services::Status s = boosting::Parameter::check();
    if(!s) return s;
    DAAL_CHECK_EX(accuracyThreshold > 0 && accuracyThreshold < 1, ErrorIncorrectParameter, ParameterName, accuracyThresholdStr());
    DAAL_CHECK_EX(maxIterations > 0, ErrorIncorrectParameter, ParameterName, maxIterationsStr());
    DAAL_CHECK_EX(newtonRaphsonAccuracyThreshold > 0 && newtonRaphsonAccuracyThreshold < 1, ErrorIncorrectParameter, ParameterName, newtonRaphsonAccuracyThresholdStr());
    DAAL_CHECK_EX(newtonRaphsonMaxIterations > 0, ErrorIncorrectParameter, ParameterName, newtonRaphsonMaxIterationsStr());
    DAAL_CHECK_EX(degenerateCasesThreshold > 0 && degenerateCasesThreshold < 1, ErrorIncorrectParameter, ParameterName, degenerateCasesThresholdStr());
    return s;
}

/**
 *  Returns a pointer to the array of weights of weak learners constructed
 *  during training of the brownBoost algorithm.
 *  The size of the array equals the number of weak learners
 *  \return Array of weights of weak learners.
 */
NumericTablePtr Model::getAlpha()
{
    return _alpha;
}


} // namespace interface1
} // namespace brownboost
} // namespace algorithms
} // namespace daal
