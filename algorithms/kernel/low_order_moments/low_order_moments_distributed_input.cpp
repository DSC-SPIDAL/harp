/* file: low_order_moments_distributed_input.cpp */
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
//  Implementation of LowOrderMoments classes.
//--
*/

#include "algorithms/moments/low_order_moments_types.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace low_order_moments
{
namespace interface1
{

template<>
DistributedInput<step2Master>::DistributedInput() : InputIface(lastMasterInputId + 1)
{
    Argument::set(partialResults, DataCollectionPtr(new DataCollection()));
}

template<>
DistributedInput<step2Master>::DistributedInput(const DistributedInput<step2Master>& other) : InputIface(other){}

/**
 * Returns the number of columns in the input data set
 * \return Number of columns in the input data set
 */
template<>
services::Status DistributedInput<step2Master>::getNumberOfColumns(size_t& nCols) const
{
    DataCollectionPtr collectionOfPartialResults =
        staticPointerCast<DataCollection, SerializationIface>(Argument::get(partialResults));

    DAAL_CHECK(collectionOfPartialResults, ErrorNullInputDataCollection);
    DAAL_CHECK(collectionOfPartialResults->size(), ErrorIncorrectNumberOfInputNumericTables);

    PartialResultPtr partialResult = PartialResult::cast((*collectionOfPartialResults)[0]);

    DAAL_CHECK(partialResult.get(), ErrorIncorrectElementInPartialResultCollection);
    NumericTablePtr ntPtr = partialResult->get(partialMinimum);

    Status s = checkNumericTable(ntPtr.get(), partialMinimumStr());
    nCols = s ? ntPtr->getNumberOfColumns() : 0;
    return s;
}

/**
 * Adds partial result to the collection of input objects for the low order moments algorithm in the distributed processing mode.
 * \param[in] id            Identifier of the input object
 * \param[in] partialResult Partial result obtained in the first step of the distributed algorithm
 */
template<>
void DistributedInput<step2Master>::add(MasterInputId id, const PartialResultPtr &partialResult)
{
    DataCollectionPtr collection =
        staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
    collection->push_back(staticPointerCast<SerializationIface, PartialResult>(partialResult));
}
/**
 * Sets input object for the low order moments algorithm in the distributed processing mode.
 * \param[in] id  Identifier of the input object
 * \param[in] ptr Pointer to the input object
 */
template<>
void DistributedInput<step2Master>::set(MasterInputId id, const DataCollectionPtr &ptr)
{
    Argument::set(id, ptr);
}
/**
 * Returns the collection of input objects
 * \param[in] id   Identifier of the input object, \ref MasterInputId
 * \return Collection of distributed input objects
 */
template<>
DataCollectionPtr DistributedInput<step2Master>::get(MasterInputId id) const
{
    return staticPointerCast<DataCollection, SerializationIface>(Argument::get(partialResults));
}
/**
 * Checks algorithm parameters on the master node
 * \param[in] parameter Pointer to the algorithm parameters
 * \param[in] method    Computation method
 */
template<>
services::Status DistributedInput<step2Master>::check(const daal::algorithms::Parameter *parameter, int method) const
{
    services::Status s;
    DataCollectionPtr collectionPtr = DataCollection::cast(Argument::get(0));
    DAAL_CHECK(collectionPtr, ErrorNullInputDataCollection);
    size_t nBlocks = collectionPtr->size();
    DAAL_CHECK(collectionPtr->size() != 0, ErrorIncorrectNumberOfInputNumericTables);

    for(size_t j = 0; j < nBlocks; j++)
    {
        PartialResultPtr partialResult = PartialResult::cast((*collectionPtr)[j]);
        DAAL_CHECK(partialResult.get() != 0, ErrorIncorrectElementInPartialResultCollection);

        /* Checks partial number of observations */
        int unexpectedLayouts = (int)NumericTableIface::csrArray;
        DAAL_CHECK_STATUS(s, checkNumericTable(partialResult->get(nObservations).get(), nObservationsStr(), unexpectedLayouts, 0, 1, 1));

        unexpectedLayouts = (int)packed_mask;
        DAAL_CHECK_STATUS(s, checkNumericTable(partialResult->get(partialMinimum).get(), partialMinimumStr(), unexpectedLayouts));

        size_t nFeatures = partialResult->get(partialMinimum)->getNumberOfColumns();
        const char* errorMessages[] = {partialMinimumStr(), partialMaximumStr(), partialSumStr(), partialSumSquaresStr(), partialSumSquaresCenteredStr() };

        for(size_t i = 1; i < lastPartialResultId + 1; i++)
            DAAL_CHECK_STATUS(s, checkNumericTable(partialResult->get((PartialResultId)i).get(), errorMessages[i - 1], unexpectedLayouts, 0, nFeatures, 1));
        }
    return s;
}

} // namespace interface1
} // namespace low_order_moments
} // namespace algorithms
} // namespace daal
