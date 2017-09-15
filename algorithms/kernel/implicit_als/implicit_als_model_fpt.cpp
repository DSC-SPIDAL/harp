/* file: implicit_als_model_fpt.cpp */
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
//  Implementation of the class defining the implicit als model
//--
*/

#include "implicit_als_model.h"

namespace daal
{
namespace algorithms
{
namespace implicit_als
{

template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nUsers, size_t nItems, const Parameter &parameter, modelFPType dummy)
{
    const size_t nFactors = parameter.nFactors;
    _usersFactors.reset(new data_management::HomogenNumericTable<modelFPType>(nFactors, nUsers, data_management::NumericTableIface::doAllocate, 0));
    _itemsFactors.reset(new data_management::HomogenNumericTable<modelFPType>(nFactors, nItems, data_management::NumericTableIface::doAllocate, 0));
}

template<typename modelFPType>
DAAL_EXPORT Model::Model(size_t nUsers, size_t nItems, const Parameter &parameter, modelFPType dummy, services::Status &st)
{
    using namespace daal::data_management;
    const size_t nFactors = parameter.nFactors;

    _usersFactors = HomogenNumericTable<modelFPType>::create(nFactors, nUsers, NumericTableIface::doAllocate, 0, &st);
    if (!st) { return; }

    _itemsFactors = HomogenNumericTable<modelFPType>::create(nFactors, nItems, NumericTableIface::doAllocate, 0, &st);
    if (!st) { return; }
}

/**
 * Constructs the implicit ALS model
 * \param[in]  nUsers    Number of users in the input data set
 * \param[in]  nItems    Number of items in the input data set
 * \param[in]  parameter Implicit ALS parameters
 * \param[out] stat      Status of the model construction
 */
template<typename modelFPType>
DAAL_EXPORT ModelPtr Model::create(size_t nUsers, size_t nItems,
                                   const Parameter &parameter,
                                   services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, nUsers, nItems, parameter, (modelFPType)0 /* dummy */);
}

template DAAL_EXPORT Model::Model(size_t, size_t, const Parameter&, DAAL_FPTYPE);
template DAAL_EXPORT Model::Model(size_t, size_t, const Parameter&, DAAL_FPTYPE, services::Status&);
template DAAL_EXPORT ModelPtr Model::create<DAAL_FPTYPE>(size_t, size_t,
                                                         const Parameter&, services::Status*);

}// namespace implicit_als
}// namespace algorithms
}// namespace daal
