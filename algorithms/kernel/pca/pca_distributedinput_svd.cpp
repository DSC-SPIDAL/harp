/* file: pca_distributedinput_svd.cpp */
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
//  Implementation of PCA algorithm interface.
//--
*/

#include "algorithms/pca/pca_types.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace interface1
{

DistributedInput<svdDense>::DistributedInput() : InputIface(lastStep2MasterInputId + 1)
{
    Argument::set(partialResults, DataCollectionPtr(new DataCollection()));
}
DistributedInput<svdDense>::DistributedInput(const DistributedInput<svdDense>& other) : InputIface(other){}

/**
 * Sets input objects for the PCA on the second step in the distributed processing mode
 * \param[in] id    Identifier of the input object
 * \param[in] ptr   Input object that corresponds to the given identifier
 */
void DistributedInput<svdDense>::set(Step2MasterInputId id, const DataCollectionPtr &ptr)
{
    Argument::set(id, ptr);
}

/**
 * Gets input objects for the PCA algorithm on the second step in the distributed processing mode
 * \param[in] id    Identifier of the input object
 * \return          Input object that corresponds to the given identifier
 */
DataCollectionPtr DistributedInput<svdDense>::get(Step2MasterInputId id) const
{
    return staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
}

/**
 * Adds input objects of the PCA algorithm on the second step in the distributed processing mode
 * \param[in] id      Identifier of the input object
 * \param[in] value   Pointer to the input object
 */
void DistributedInput<svdDense>::add(Step2MasterInputId id, const SharedPtr<PartialResult<svdDense> > &value)
{
    DataCollectionPtr collection = staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
    collection->push_back(value);
}

/**
 * Retrieves specific partial result from the input objects of the PCA algorithm on the second step in the distributed processing mode
 * \param[in] id      Identifier of the partial result
 */
SharedPtr<PartialResult<svdDense> > DistributedInput<svdDense>::getPartialResult(size_t id) const
{
    DataCollectionPtr partialResultsCollection = staticPointerCast<DataCollection, SerializationIface>(Argument::get(partialResults));
    if(partialResultsCollection->size() <= id)
    {
        return SharedPtr<PartialResult<svdDense> >();
    }
    return staticPointerCast<PartialResult<svdDense>, SerializationIface>((*partialResultsCollection)[id]);
}

/**
* Checks the input of the PCA algorithm
* \param[in] parameter Algorithm %parameter
* \param[in] method    Computation  method
*/
Status DistributedInput<svdDense>::check(const daal::algorithms::Parameter *parameter, int method) const
{
    DataCollectionPtr collection = DataCollection::cast(Argument::get(partialResults));
    DAAL_CHECK(collection, ErrorNullPartialResultDataCollection);
    size_t nBlocks = collection->size();
    DAAL_CHECK(nBlocks > 0, ErrorIncorrectNumberOfInputNumericTables);

    for(size_t i = 0; i < nBlocks; i++)
    {
        SharedPtr<PartialResult<svdDense> > partRes = staticPointerCast<PartialResult<svdDense>, SerializationIface>((*collection)[i]);
        DAAL_CHECK(partRes, ErrorIncorrectElementInPartialResultCollection);
    }

    int packedLayouts = packed_mask;
    int csrLayout = (int)NumericTableIface::csrArray;

    Status s;
    for(size_t j = 0; j < nBlocks; j++)
    {
        NumericTablePtr nObservationsSVD = getPartialResult(j)->get(pca::nObservationsSVD);
        DAAL_CHECK_STATUS(s, checkNumericTable(nObservationsSVD.get(), nObservationsSVDStr(), csrLayout, 0, 1, 1));

        NumericTablePtr sumSquaresSVD = getPartialResult(j)->get(pca::sumSquaresSVD);
        DAAL_CHECK_STATUS(s, checkNumericTable(sumSquaresSVD.get(), sumSquaresSVDStr(), packedLayouts, 0, 0, 1));

        size_t nFeatures = getPartialResult(0)->get(pca::sumSquaresSVD)->getNumberOfColumns();
        NumericTablePtr sumSVD = getPartialResult(j)->get(pca::sumSVD);

        DAAL_CHECK_STATUS(s, checkNumericTable(sumSVD.get(), sumSVDStr(), packedLayouts, 0, nFeatures, 1));
        DAAL_CHECK(sumSquaresSVD->getNumberOfColumns() == sumSVD->getNumberOfColumns(), ErrorIncorrectNumberOfColumns);

        DataCollectionPtr auxiliaryData = getPartialResult(j)->get(pca::auxiliaryData);
        DAAL_CHECK(auxiliaryData, ErrorNullAuxiliaryDataCollection);
        DAAL_CHECK(auxiliaryData->size() > 0, ErrorEmptyAuxiliaryDataCollection);

        for(size_t i = 0; i < auxiliaryData->size(); i++)
        {
            NumericTablePtr table = NumericTable::cast((*auxiliaryData)[i]);
            DAAL_CHECK(table, ErrorIncorrectElementInNumericTableCollection);
            DAAL_CHECK_STATUS(s, checkNumericTable(table.get(), auxiliaryDataStr(), csrLayout, 0, nFeatures, nFeatures));
        }
    }
    return s;
}

/**
 * Returns the number of columns in the input data set
 * \return Number of columns in the input data set
 */
size_t DistributedInput<svdDense>::getNFeatures() const
{
    return getPartialResult(0)->get(pca::sumSVD)->getNumberOfColumns();
}

} // namespace interface1
} // namespace pca
} // namespace algorithms
} // namespace daal
