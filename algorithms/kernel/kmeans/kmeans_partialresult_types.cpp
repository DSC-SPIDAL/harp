/* file: kmeans_partialresult_types.cpp */
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
//  Implementation of kmeans classes.
//--
*/

#include "algorithms/kmeans/kmeans_types.h"
#include "daal_defines.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace kmeans
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(PartialResult, SERIALIZATION_KMEANS_PARTIAL_RESULT_ID);
PartialResult::PartialResult() : daal::algorithms::PartialResult(lastPartialResultId + 1) {}

/**
 * Returns a partial result of the K-Means algorithm
 * \param[in] id   Identifier of the partial result
 * \return         Partial result that corresponds to the given identifier
 */
NumericTablePtr PartialResult::get(PartialResultId id) const
{
    return staticPointerCast<NumericTable, SerializationIface>(Argument::get(id));
}

/**
 * Sets a partial result of the K-Means algorithm
 * \param[in] id    Identifier of the partial result
 * \param[in] ptr   Pointer to the object
 */
void PartialResult::set(PartialResultId id, const NumericTablePtr &ptr)
{
    Argument::set(id, ptr);
}

/**
* Returns the number of features in the Input data table
* \return Number of features in the Input data table
*/

size_t PartialResult::getNumberOfFeatures() const
{
    NumericTablePtr sums = get(partialSums);
    return sums->getNumberOfColumns();
}

/**
* Checks partial results of the K-Means algorithm
* \param[in] input   %Input object of the algorithm
* \param[in] par     Algorithm parameter
* \param[in] method  Computation method
*/
services::Status PartialResult::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const
{
    size_t inputFeatures = static_cast<const InputIface *>(input)->getNumberOfFeatures();
    const Parameter *kmPar = static_cast<const Parameter *>(par);
    const int unexpectedLayouts = (int)packed_mask;

    services::Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(get(nObservations).get(), nObservationsStr(), unexpectedLayouts, 0, 1, kmPar->nClusters));
    DAAL_CHECK_STATUS(s, checkNumericTable(get(partialSums).get(), partialSumsStr(), unexpectedLayouts, 0, inputFeatures, kmPar->nClusters));
    DAAL_CHECK_STATUS(s, checkNumericTable(get(partialGoalFunction).get(), partialGoalFunctionStr(), unexpectedLayouts, 0, 1, 1));
    if( kmPar->assignFlag )
    {
        Input *algInput = dynamic_cast<Input*>(const_cast<daal::algorithms::Input *>(input));
        if( !algInput ) { return s; }
        const size_t nRows = algInput->get(data)->getNumberOfRows();
        s = checkNumericTable(get(partialAssignments).get(), partialAssignmentsStr(), unexpectedLayouts, 0, 1, nRows);
    }
    return s;
}

/**
 * Checks partial results of the K-Means algorithm
 * \param[in] par     Algorithm parameter
 * \param[in] method  Computation method
 */
services::Status PartialResult::check(const daal::algorithms::Parameter *par, int method) const
{
    const Parameter *kmPar = static_cast<const Parameter *>(par);
    const int unexpectedLayouts = (int)packed_mask;
    services::Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(get(nObservations).get(), nObservationsStr(), unexpectedLayouts, 0, 1, kmPar->nClusters));
    DAAL_CHECK_STATUS(s, checkNumericTable(get(partialSums).get(), partialSumsStr(), unexpectedLayouts, 0, 0, kmPar->nClusters));
    return s;
}

} // namespace interface1
} // namespace kmeans
} // namespace algorithm
} // namespace daal
