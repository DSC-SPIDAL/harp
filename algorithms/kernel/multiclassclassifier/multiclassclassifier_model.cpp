/* file: multiclassclassifier_model.cpp */
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
//  Implementation of multi class classifier model.
//--
*/

#include "multi_class_classifier_model.h"
#include "serialization_utils.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace multi_class_classifier
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_MULTI_CLASS_CLASSIFIER_MODEL_ID);

Model::Model(size_t nFeatures, const ParameterBase *par) :
    _modelsArray(NULL),
    _models(new data_management::DataCollection(par->nClasses * (par->nClasses - 1) / 2)),
    _nFeatures(nFeatures)
{
}

Model::Model(size_t nFeatures, const ParameterBase *par, services::Status &st) :
    _modelsArray(NULL),
    _nFeatures(nFeatures)
{
    _models.reset(new data_management::DataCollection(par->nClasses * (par->nClasses - 1) / 2));
    if (!_models)
        st.add(services::ErrorMemoryAllocationFailed);
}

Model::Model() : _modelsArray(NULL), _models(new data_management::DataCollection()), _nFeatures(0)
{
}

ModelPtr Model::create(size_t nFeatures, const ParameterBase *par, services::Status* stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, nFeatures, par);
}

Model::~Model()
{
    delete[] _modelsArray;
}

classifier::ModelPtr *Model::getTwoClassClassifierModels()
{
    if(!_modelsArray)
    {
        _modelsArray = new classifier::ModelPtr[_models->size()];
        for(size_t i = 0; i < _models->size(); i++)
        {
            _modelsArray[i] = services::staticPointerCast<classifier::Model, data_management::SerializationIface>((*_models)[i]);
        }
    }
    return _modelsArray;
}

void Model::setTwoClassClassifierModel(size_t idx, const classifier::ModelPtr& model)
{
    (*_models)[idx] = model;
}

classifier::ModelPtr Model::getTwoClassClassifierModel(size_t idx) const
{
    if(idx < _models->size())
    {
        return services::staticPointerCast<classifier::Model, data_management::SerializationIface>((*_models)[idx]);
    }
    return classifier::ModelPtr();
}

services::Status Parameter::check() const
{
    services::Status s;
    DAAL_CHECK_STATUS(s, ParameterBase::check());
    DAAL_CHECK_EX((accuracyThreshold > 0) && (accuracyThreshold < 1), services::ErrorIncorrectParameter, services::ParameterName, accuracyThresholdStr());
    DAAL_CHECK_EX(maxIterations, services::ErrorIncorrectParameter, services::ParameterName, maxIterationsStr());
    return s;
}
}
}
}
}
