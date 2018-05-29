/* file: regression_training_input.cpp */
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
//  Implementation of the class defining the input objects
//  of the regression training algorithm
//--
*/

#include "algorithms/regression/regression_training_types.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace regression
{
namespace training
{
namespace interface1
{
using namespace daal::data_management;
using namespace daal::services;
Input::Input(size_t nElements) : daal::algorithms::Input(nElements)
{}
Input::Input(const Input& other) : daal::algorithms::Input(other)
{}

data_management::NumericTablePtr Input::get(InputId id) const
{
    return NumericTable::cast(Argument::get(id));
}

void Input::set(InputId id, const data_management::NumericTablePtr &value)
{
    Argument::set(id, value);
}

Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    const NumericTablePtr dataTable = get(data);
    const NumericTablePtr dependentVariableTable = get(dependentVariables);

    Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(dataTable.get(), dataStr()));

    size_t nRowsInData = dataTable->getNumberOfRows();

    return checkNumericTable(dependentVariableTable.get(), dependentVariableStr(), 0, 0, 0, nRowsInData);
}

}
}
}
}
}
