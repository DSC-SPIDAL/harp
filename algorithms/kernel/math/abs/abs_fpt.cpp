/* file: abs_fpt.cpp */
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
//  Implementation of abs algorithm and types methods.
//--
*/

#include "abs_types.h"
#include "service_numeric_table.h"

using namespace daal::services;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace math
{
namespace abs
{
namespace interface1
{
/**
 * Allocates memory to store the result of the absolute value function
 * \param[in] input  %Input object for the absolute value function
 * \param[in] par    %Parameter of the absolute value function
 * \param[in] method Computation method of the absolute value function
 */
template <typename algorithmFPType>
DAAL_EXPORT Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method)
{
    Status s;
    Input *algInput = static_cast<Input *>(const_cast<daal::algorithms::Input *>(input));
    DAAL_CHECK(algInput, ErrorNullInput);

    NumericTablePtr inputTable = algInput->get(data);
    DAAL_CHECK(inputTable.get(), ErrorNullInputNumericTable);

    if(method == fastCSR)
    {
        NumericTableIface::StorageLayout layout = inputTable->getDataLayout();
        DAAL_CHECK(layout == NumericTableIface::csrArray, ErrorIncorrectTypeOfInputNumericTable);

        CSRNumericTablePtr resTable;

        DAAL_CHECK_STATUS(s, createSparseTable<algorithmFPType>(algInput->get(data), resTable));
        Argument::set(value, staticPointerCast<SerializationIface, CSRNumericTable>(resTable));
    }
    else
    {
        Argument::set(value, HomogenNumericTable<algorithmFPType>::create(inputTable->getNumberOfColumns(), inputTable->getNumberOfRows(), NumericTable::doAllocate, &s));
    }
    return s;
}

template DAAL_EXPORT Status Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method);

}// namespace interface1
}// namespace abs
}// namespace math
}// namespace algorithms
}// namespace daal
