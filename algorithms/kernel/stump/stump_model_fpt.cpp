/* file: stump_model_fpt.cpp */
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
//  Implementation of the decision stump model constructor.
//--
*/

#include "algorithms/stump/stump_model.h"

namespace daal
{
namespace algorithms
{
namespace stump
{
/**
 * Constructs the decision stump model
 * \tparam modelFPType  Data type to store decision stump model data, double or float
 * \param[in] dummy     Dummy variable for the templated constructor
 */
template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nFeatures, modelFPType dummy) :
    _values(new data_management::Matrix<double>(3, 1, data_management::NumericTable::doAllocate)),
    _nFeatures(nFeatures), _splitFeature(0)
{}

template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nFeatures, modelFPType dummy, services::Status &st) :
    _nFeatures(nFeatures), _splitFeature(0)
{
   _values = data_management::Matrix<double>::create(3, 1, data_management::NumericTable::doAllocate, st);
}

template<typename modelFPType>
DAAL_EXPORT services::SharedPtr<Model> Model::create(size_t nFeatures, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, nFeatures, (modelFPType)0.0);
}

template<typename modelFPType>
DAAL_EXPORT modelFPType Model::getSplitValue()
{
    return (*_values)[0][0];
}

template<typename modelFPType>
DAAL_EXPORT void Model::setSplitValue(modelFPType splitValue)
{
    (*_values)[0][0] = splitValue;
}

template<typename modelFPType>
DAAL_EXPORT modelFPType Model::getLeftSubsetAverage()
{
    return (*_values)[0][1];
}

template<typename modelFPType>
DAAL_EXPORT void Model::setLeftSubsetAverage(modelFPType leftSubsetAverage)
{
    (*_values)[0][1] = leftSubsetAverage;
}

template<typename modelFPType>
DAAL_EXPORT modelFPType Model::getRightSubsetAverage()
{
    return (*_values)[0][2];
}

template<typename modelFPType>
DAAL_EXPORT void Model::setRightSubsetAverage(modelFPType rightSubsetAverage)
{
    (*_values)[0][2] = rightSubsetAverage;
}

template DAAL_EXPORT Model::Model(size_t, DAAL_FPTYPE);
template DAAL_EXPORT Model::Model(size_t, DAAL_FPTYPE, services::Status &);
template DAAL_EXPORT services::SharedPtr<Model> Model::create<DAAL_FPTYPE>(size_t, services::Status *);
template DAAL_EXPORT DAAL_FPTYPE Model::getSplitValue();
template DAAL_EXPORT void Model::setSplitValue(DAAL_FPTYPE);
template DAAL_EXPORT DAAL_FPTYPE Model::getLeftSubsetAverage();
template DAAL_EXPORT void Model::setLeftSubsetAverage(DAAL_FPTYPE);
template DAAL_EXPORT DAAL_FPTYPE Model::getRightSubsetAverage();
template DAAL_EXPORT void Model::setRightSubsetAverage(DAAL_FPTYPE);

}// namespace stump
}// namespace algorithms
}// namespace daal
