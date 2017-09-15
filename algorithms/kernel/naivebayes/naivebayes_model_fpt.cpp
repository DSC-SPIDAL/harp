/* file: naivebayes_model_fpt.cpp */
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
//  Implementation of class defining naive bayes algorithm model
//--
*/

#include "algorithms/naive_bayes/multinomial_naive_bayes_model.h"

namespace daal
{
namespace algorithms
{
namespace multinomial_naive_bayes
{

/**
 * Constructs multinomial naive Bayes model
 * \param[in] nFeatures  The number of features
 * \param[in] parameter  The multinomial naive Bayes parameter
 * \param[in] dummy      Dummy variable for the templated constructor
 * \DAAL_DEPRECATED_USE{ Model::create }
 */
template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nFeatures, const Parameter &parameter, modelFPType dummy)
{
    using namespace data_management;

    const Parameter *par = &parameter;
    if(par->nClasses == 0 || nFeatures == 0)
    {
        return;
    }

    _logP     = NumericTablePtr(new HomogenNumericTable<modelFPType>(1,         par->nClasses, NumericTable::doAllocate));
    _logTheta = NumericTablePtr(new HomogenNumericTable<modelFPType>(nFeatures, par->nClasses, NumericTable::doAllocate));
    _auxTable = NumericTablePtr(new HomogenNumericTable<modelFPType>(nFeatures, par->nClasses, NumericTable::doAllocate));
}

template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nFeatures, const Parameter &parameter, modelFPType dummy, services::Status &st)
{
    using namespace data_management;

    if (parameter.nClasses == 0)
    {
        st.add(services::ErrorIncorrectNumberOfClasses);
        return;
    }
    if (nFeatures == 0)
    {
        st.add(services::ErrorIncorrectNumberOfFeatures);
        return;
    }

    _logP     = HomogenNumericTable<modelFPType>::create(1,         parameter.nClasses, NumericTable::doAllocate, &st);
    if (!st)
        return;
    _logTheta = HomogenNumericTable<modelFPType>::create(nFeatures, parameter.nClasses, NumericTable::doAllocate, &st);
    if (!st)
        return;
    _auxTable = HomogenNumericTable<modelFPType>::create(nFeatures, parameter.nClasses, NumericTable::doAllocate, &st);
    if (!st)
        return;
}

/**
 * Constructs multinomial naive Bayes model
 * \param[in] nFeatures  The number of features
 * \param[in] parameter  The multinomial naive Bayes parameter
 * \param[out] stat      Status of the model construction
 */
template<typename modelFPType>
DAAL_EXPORT ModelPtr Model::create(size_t nFeatures, const Parameter &parameter, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, nFeatures, parameter, (modelFPType)0.0);
}

/**
 * Constructs multinomial naive Bayes partial model
 * \param[in] nFeatures  The number of features
 * \param[in] parameter  Multinomial naive Bayes parameter
 * \param[in] dummy      Dummy variable for the templated constructor
 * \DAAL_DEPRECATED_USE{ PartialModel::create }
 */
template<typename modelFPType>
DAAL_EXPORT PartialModel::PartialModel(size_t nFeatures, const Parameter &parameter, modelFPType dummy) : _nObservations(0)
{
    using namespace data_management;
    const Parameter *par = &parameter;
    if(par->nClasses == 0 || nFeatures == 0)
    {
        return;
    }

    _classSize     = NumericTablePtr(new HomogenNumericTable<int>(1,         par->nClasses, NumericTable::doAllocate));
    _classGroupSum = NumericTablePtr(new HomogenNumericTable<int>(nFeatures, par->nClasses, NumericTable::doAllocate));
}

template<typename modelFPType>
DAAL_EXPORT PartialModel::PartialModel(size_t nFeatures, const Parameter &parameter,
                                       modelFPType dummy, services::Status &st) : _nObservations(0)
{
    using namespace data_management;

    if (parameter.nClasses == 0)
    {
        st.add(services::ErrorIncorrectNumberOfClasses);
        return;
    }
    if (nFeatures == 0)
    {
        st.add(services::ErrorIncorrectNumberOfFeatures);
        return;
    }

    _classSize     = HomogenNumericTable<int>::create(1,         parameter.nClasses, NumericTable::doAllocate, &st);
    if (!st)
        return;

    _classGroupSum = HomogenNumericTable<int>::create(nFeatures, parameter.nClasses, NumericTable::doAllocate, &st);
    if (!st)
        return;
}

/**
 * Constructs multinomial naive Bayes partial model
 * \param[in] nFeatures  The number of features
 * \param[in] parameter  The multinomial naive Bayes parameter
 * \param[out] stat      Status of the model construction
 * \return Multinomial naive Bayes partial model
 */
template<typename modelFPType>
DAAL_EXPORT PartialModelPtr PartialModel::create(size_t nFeatures, const Parameter &parameter, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(PartialModel, nFeatures, parameter, (modelFPType)0.0);
}

template DAAL_EXPORT Model::Model(size_t, const Parameter&, DAAL_FPTYPE);
template DAAL_EXPORT Model::Model(size_t, const Parameter&, DAAL_FPTYPE, services::Status&);

template DAAL_EXPORT ModelPtr Model::create<DAAL_FPTYPE>(size_t, const Parameter&, services::Status*);

template DAAL_EXPORT PartialModel::PartialModel(size_t, const Parameter&, DAAL_FPTYPE);
template DAAL_EXPORT PartialModel::PartialModel(size_t, const Parameter&, DAAL_FPTYPE, services::Status&);

template DAAL_EXPORT PartialModelPtr PartialModel::create<DAAL_FPTYPE>(size_t, const Parameter&, services::Status*);

}// namespace multinomial_naive_bayes
}// namespace algorithms
}// namespace daal
