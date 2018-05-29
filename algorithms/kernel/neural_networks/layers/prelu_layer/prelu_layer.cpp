/* file: prelu_layer.cpp */
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
//  Implementation of prelu calculation algorithm and types methods.
//--
*/

#include "prelu_layer_types.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace prelu
{
namespace interface1
{
/**
*  Constructs parameters of the prelu layer
*  \param[in] _dataDimension    Starting data dimension index to apply weight
*  \param[in] _weightsDimension Number of weight dimensions
*/
Parameter::Parameter(const size_t _dataDimension, const size_t _weightsDimension) : dataDimension(_dataDimension),
    weightsDimension(_weightsDimension)
{};

/**
 * Checks the correctness of the parameter
 */
services::Status Parameter::check() const
{
    if(weightsDimension == (size_t)0)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ArgumentName, weightsDimensionStr()));
    }
    return services::Status();
}

}// namespace interface1
}// namespace prelu
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
