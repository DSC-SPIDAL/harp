/* file: engine_types.cpp */
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

//++
//  Implementation of initializer types.
//--

#include "engines/engine_types.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace engines
{
namespace interface1
{

Input::Input() : daal::algorithms::Input(1) {}

Input::Input(const Input& other) : daal::algorithms::Input(other) {}

data_management::NumericTablePtr Input::get(InputId id) const
{
    return data_management::NumericTable::cast(Argument::get(id));
}

void Input::set(InputId id, const data_management::NumericTablePtr &ptr)
{
    Argument::set(id, ptr);
}

services::Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    DAAL_CHECK(Argument::size() == 1, services::ErrorIncorrectNumberOfInputNumericTables);
    return data_management::checkNumericTable(get(tableToFill).get(), tableToFillStr());
}

Result::Result() : daal::algorithms::Result(1) {}

data_management::NumericTablePtr Result::get(ResultId id) const
{
    return data_management::NumericTable::cast(Argument::get(id));
}

void Result::set(ResultId id, const data_management::NumericTablePtr &ptr)
{
    Argument::set(id, ptr);
}

services::Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const
{
    DAAL_CHECK(Argument::size() == 1, services::ErrorIncorrectNumberOfInputNumericTables);
    const Input *algInput = static_cast<const Input *>(input);
    DAAL_CHECK(algInput, services::ErrorNullInput);
    return data_management::checkNumericTable(get(randomNumbers).get(), randomNumbersStr());
}

} // namespace interface1
} // namespace engines
} // namespace algorithms
} // namespace daal
