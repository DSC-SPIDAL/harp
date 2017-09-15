/* file: kmeans_step2_distr_input_types.cpp */
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

DistributedStep2MasterInput::DistributedStep2MasterInput() : InputIface(lastMasterInputId + 1)
{
    Argument::set(partialResults, DataCollectionPtr(new DataCollection()));
}

/**
* Returns an input object for the K-Means algorithm in the second step of the distributed processing mode
* \param[in] id    Identifier of the input object
* \return          %Input object that corresponds to the given identifier
*/
DataCollectionPtr DistributedStep2MasterInput::get(MasterInputId id) const
{
    return staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
}

/**
* Sets an input object for the K-Means algorithm in the second step of the distributed processing mode
* \param[in] id    Identifier of the input object
* \param[in] ptr   Pointer to the object
*/
void DistributedStep2MasterInput::set(MasterInputId id, const DataCollectionPtr &ptr)
{
    Argument::set(id, staticPointerCast<SerializationIface, DataCollection>(ptr));
}

/**
 * Adds partial results computed on local nodes to the input for the K-Means algorithm
 * in the second step of the distributed processing mode
 * \param[in] id    Identifier of the input object
 * \param[in] value Pointer to the object
 */
void DistributedStep2MasterInput::add(MasterInputId id, const PartialResultPtr &value)
{
    DataCollectionPtr collection = staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
    collection->push_back( value );
}

/**
* Returns the number of features in the Input data table in the second step of the distributed processing mode
* \return Number of features in the Input data table
*/
size_t DistributedStep2MasterInput::getNumberOfFeatures() const
{
    DataCollectionPtr collection = get(partialResults);
    PartialResultPtr pres = staticPointerCast<PartialResult, SerializationIface>((*collection)[0]);
    return pres->getNumberOfFeatures();
}

/**
* Checks an input object for the K-Means algorithm in the second step of the distributed processing mode
* \param[in] par     Algorithm parameter
* \param[in] method  Computation method
*/
services::Status DistributedStep2MasterInput::check(const daal::algorithms::Parameter *par, int method) const
{
     const Parameter *kmPar = static_cast<const Parameter *>(par);

    DataCollectionPtr collection = get(partialResults);
    DAAL_CHECK(collection, ErrorNullInputDataCollection);

    size_t nBlocks = collection->size();
    DAAL_CHECK(nBlocks > 0, ErrorIncorrectNumberOfInputNumericTables);

    PartialResultPtr firstPres =
            dynamicPointerCast<PartialResult, SerializationIface>((*collection)[0]);
    DAAL_CHECK(firstPres, ErrorIncorrectElementInPartialResultCollection);

    const int unexpectedLayouts = (int)packed_mask;
    services::Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(firstPres->get(nObservations).get(), nObservationsStr(),
        unexpectedLayouts, 0, 1, kmPar->nClusters));
    DAAL_CHECK_STATUS(s, checkNumericTable(firstPres->get(partialSums).get(), partialSumsStr(),
        unexpectedLayouts, 0, 0, kmPar->nClusters));
    DAAL_CHECK_STATUS(s, checkNumericTable(firstPres->get(partialObjectiveFunction).get(), partialGoalFunctionStr(),
        unexpectedLayouts, 0, 1, 1));
    if( kmPar->assignFlag )
    {
        DAAL_CHECK_STATUS(s, checkNumericTable(firstPres->get(partialAssignments).get(), partialAssignmentsStr(),
            unexpectedLayouts, 0, 1));
    }
    const size_t inputFeatures = firstPres->get(partialSums)->getNumberOfColumns();
    for(size_t i = 1; i < nBlocks; i++)
    {
        PartialResultPtr pres =
            dynamicPointerCast<PartialResult, SerializationIface>((*collection)[i]);
        DAAL_CHECK(pres, ErrorIncorrectElementInPartialResultCollection);

        DAAL_CHECK_STATUS(s, checkNumericTable(pres->get(nObservations).get(), nObservationsStr(),
            unexpectedLayouts, 0, 1, kmPar->nClusters));
        DAAL_CHECK_STATUS(s, checkNumericTable(pres->get(partialSums).get(), partialSumsStr(),
            unexpectedLayouts, 0, inputFeatures, kmPar->nClusters));
        DAAL_CHECK_STATUS(s, checkNumericTable(pres->get(partialObjectiveFunction).get(), partialGoalFunctionStr(),
            unexpectedLayouts, 0, 1, 1));
        if( kmPar->assignFlag )
        {
            DAAL_CHECK_STATUS(s, checkNumericTable(pres->get(partialAssignments).get(), partialAssignmentsStr(),
                unexpectedLayouts, 0, 1));
        }
    }
    return s;
}

} // namespace interface1
} // namespace kmeans
} // namespace algorithm
} // namespace daal
