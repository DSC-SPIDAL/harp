/* file: implicit_als_model.cpp */
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
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace implicit_als
{

Model::Model() { }

services::Status Parameter::check() const
{
    if(nFactors == 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, nFactorsStr()));
    }
    if(maxIterations == 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, maxIterationsStr()));
    }
    if(alpha < 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, alphaStr()));
    }
    if(lambda < 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, lambdaStr()));
    }
    if(preferenceThreshold < 0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ParameterName, preferenceThresholdStr()));
    }
    return services::Status();
}

PartialModel::PartialModel(const data_management::NumericTablePtr &factors,
                           const data_management::NumericTablePtr &indices,
                           services::Status &st) :
    _factors(factors), _indices(indices) { }

/**
 * Constructs a partial implicit ALS model from the indices and factors stored in the numeric tables
 * \param[in] factors   Pointer to the numeric table with factors stored in row-major order
 * \param[in] indices   Pointer to the numeric table with the indices of factors
 * \param[out] stat     Status of the model construction
 * \return Partial implicit ALS model with the specified indices and factors
 */
PartialModelPtr PartialModel::create(const data_management::NumericTablePtr &factors,
                                     const data_management::NumericTablePtr &indices,
                                     services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(PartialModel, factors, indices);
}


}// namespace implicit_als
}// namespace algorithms
}// namespace daal
