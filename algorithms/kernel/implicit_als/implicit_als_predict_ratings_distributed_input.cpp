/* file: implicit_als_predict_ratings_distributed_input.cpp */
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
//  Implementation of implicit als algorithm and types methods.
//--
*/

#include "implicit_als_predict_ratings_types.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace implicit_als
{
namespace prediction
{
namespace ratings
{
namespace interface1
{

DistributedInput<step1Local>::DistributedInput() : InputIface(2) {}

/**
 * Returns an input object for the rating prediction stage of the implicit ALS algorithm
 * \param[in] id    Identifier of the input object
 * \return          Input object that corresponds to the given identifier
 */
PartialModelPtr DistributedInput<step1Local>::get(PartialModelInputId id) const
{
    return services::staticPointerCast<PartialModel, data_management::SerializationIface>(Argument::get(id));
}

/**
 * Sets an input object for the rating prediction stage of the implicit ALS algorithm
 * \param[in] id    Identifier of the input object
 * \param[in] ptr   Pointer to the new input object value
 */
void DistributedInput<step1Local>::set(PartialModelInputId id, const PartialModelPtr &ptr)
{
    Argument::set(id, ptr);
}

/**
 * Returns the number of rows in the input numeric table
 * \return Number of rows in the input numeric table
 */
size_t DistributedInput<step1Local>::getNumberOfUsers() const
{
    PartialModelPtr usersModel = get(usersPartialModel);
    if(usersModel)
    {
        data_management::NumericTablePtr factors = usersModel->getFactors();
        if(factors)
            return factors->getNumberOfRows();
    }
    return 0;
}

/**
 * Returns the number of columns in the input numeric table
 * \return Number of columns in the input numeric table
 */
size_t DistributedInput<step1Local>::getNumberOfItems() const
{
    PartialModelPtr itemsModel = get(itemsPartialModel);
    if(itemsModel)
    {
        data_management::NumericTablePtr factors = itemsModel->getFactors();
        if(factors)
            return factors->getNumberOfRows();
    }
    return 0;
}

/**
 * Checks the parameters of the rating prediction stage of the implicit ALS algorithm
 * \param[in] parameter     Algorithm %parameter
 * \param[in] method        Computation method for the algorithm
 */
services::Status DistributedInput<step1Local>::check(const daal::algorithms::Parameter *parameter, int method) const
{
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);
    size_t nFactors = algParameter->nFactors;

    PartialModelPtr usersModel = get(usersPartialModel);
    PartialModelPtr itemsModel = get(itemsPartialModel);
    DAAL_CHECK(usersModel, ErrorNullPartialModel);
    DAAL_CHECK(itemsModel, ErrorNullPartialModel);

    const int unexpectedLayouts = (int)NumericTableIface::upperPackedTriangularMatrix |
        (int)NumericTableIface::lowerPackedTriangularMatrix |
        (int)NumericTableIface::upperPackedSymmetricMatrix |
        (int)NumericTableIface::lowerPackedSymmetricMatrix;

    const int unexpectedLayoutsIndices = unexpectedLayouts | (int)NumericTableIface::csrArray;
    services::Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(usersModel->getFactors().get(), usersFactorsStr(), unexpectedLayouts, 0, nFactors));
    const size_t nRowsUsersModel = usersModel->getFactors()->getNumberOfRows();
    DAAL_CHECK_STATUS(s, checkNumericTable(usersModel->getIndices().get(), usersIndicesStr(), unexpectedLayoutsIndices, 0, 1, nRowsUsersModel));

    DAAL_CHECK_STATUS(s, checkNumericTable(itemsModel->getFactors().get(), itemsFactorsStr(), unexpectedLayouts, 0, nFactors));
    const size_t nRowsItemsModel = itemsModel->getFactors()->getNumberOfRows();
    return checkNumericTable(itemsModel->getIndices().get(), itemsIndicesStr(), unexpectedLayoutsIndices, 0, 1, nRowsItemsModel);
}

}// namespace interface1
}// namespace ratings
}// namespace prediction
}// namespace implicit_als
}// namespace algorithms
}// namespace daal
