/* file: covariance_partialresult.cpp */
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
//  Implementation of covariance algorithm and types methods.
//--
*/

#include "covariance_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace covariance
{
namespace interface1
{

__DAAL_REGISTER_SERIALIZATION_CLASS(PartialResult, SERIALIZATION_COVARIANCE_PARTIAL_RESULT_ID);
PartialResult::PartialResult() : daal::algorithms::PartialResult(lastPartialResultId + 1)
    {}

/**
 * Gets the number of columns in the partial result of the correlation or variance-covariance matrix algorithm
 * \return Number of columns in the partial result
 */
size_t PartialResult::getNumberOfFeatures() const
{
    NumericTablePtr ntPtr = NumericTable::cast(Argument::get(crossProduct));
    if(ntPtr)
    {
        return ntPtr->getNumberOfColumns();
    }
    return 0;
}

/**
 * Returns the partial result of the correlation or variance-covariance matrix algorithm
 * \param[in] id   Identifier of the partial result, \ref PartialResultId
 * \return Partial result that corresponds to the given identifier
 */
NumericTablePtr PartialResult::get(PartialResultId id) const
{
    return staticPointerCast<NumericTable, SerializationIface>(Argument::get(id));
}

/**
 * Sets the partial result of the correlation or variance-covariance matrix algorithm
 * \param[in] id    Identifier of the partial result
 * \param[in] ptr   Pointer to the partial result
 */
void PartialResult::set(PartialResultId id, const NumericTablePtr &ptr)
{
    Argument::set(id, ptr);
}

/**
 * Check correctness of the partial result
 * \param[in] input     Pointer to the structure with input objects
 * \param[in] parameter Pointer to the structure of algorithm parameters
 * \param[in] method    Computation method
 */
services::Status PartialResult::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const
{
    const InputIface *algInput = static_cast<const InputIface *>(input);
    size_t nFeatures = algInput->getNumberOfFeatures();
    return checkImpl(nFeatures);
}

/**
 * Check the correctness of PartialResult object
 * \param[in] parameter Pointer to the structure of the parameters of the algorithm
 * \param[in] method    Computation method
 */
services::Status PartialResult::check(const daal::algorithms::Parameter *parameter, int method) const
{
    size_t nFeatures = getNumberOfFeatures();
    return checkImpl(nFeatures);
}

services::Status PartialResult::checkImpl(size_t nFeatures) const
{
    int unexpectedLayouts;
    services::Status s;

    unexpectedLayouts = (int)NumericTableIface::csrArray;
    s |= checkNumericTable(get(nObservations).get(),  nObservationsStr(), unexpectedLayouts, 0, 1, 1);
    if(!s) return s;

    unexpectedLayouts |= (int)NumericTableIface::upperPackedTriangularMatrix |
                         (int)NumericTableIface::lowerPackedTriangularMatrix;
    s |= checkNumericTable(get(crossProduct).get(), crossProductCorrelationStr(), unexpectedLayouts, 0, nFeatures, nFeatures);
    if(!s) return s;

    unexpectedLayouts |= (int)NumericTableIface::upperPackedSymmetricMatrix |
                         (int)NumericTableIface::lowerPackedSymmetricMatrix;
    s |= checkNumericTable(get(sum).get(), sumStr(), unexpectedLayouts, 0, nFeatures, 1);
    return s;
}

}//namespace interface1

}//namespace covariance
}// namespace algorithms
}// namespace daal
