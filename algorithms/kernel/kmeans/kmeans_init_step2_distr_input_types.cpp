/* file: kmeans_init_step2_distr_input_types.cpp */
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
//  Implementation of kmeans classes.
//--
*/

#include "algorithms/kmeans/kmeans_init_types.h"
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
namespace init
{
namespace interface1
{

DistributedStep2MasterInput::DistributedStep2MasterInput() : InputIface(lastDistributedStep2MasterInputId + 1)
{
    Argument::set(partialResults, DataCollectionPtr(new DataCollection()));
}

/**
* Returns an input object for computing initial clusters for the K-Means algorithm
* in the second step of the distributed processing mode
* \param[in] id    Identifier of the input object
* \return          %Input object that corresponds to the given identifier
*/
DataCollectionPtr DistributedStep2MasterInput::get(DistributedStep2MasterInputId id) const
{
    return staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
}

/**
* Sets an input object for computing initial clusters for the K-Means algorithm
* in the second step of the distributed processing mode
* \param[in] id    Identifier of the input object
* \param[in] ptr   Pointer to the object
*/
void DistributedStep2MasterInput::set(DistributedStep2MasterInputId id, const DataCollectionPtr &ptr)
{
    Argument::set(id, staticPointerCast<SerializationIface, DataCollection>(ptr));
}

/**
 * Adds a value to the data collection of input objects for computing initial clusters for the K-Means algorithm
 * in the second step of the distributed processing mode
 * \param[in] id    Identifier of the parameter
 * \param[in] value Pointer to the new parameter value
 */
void DistributedStep2MasterInput::add(DistributedStep2MasterInputId id, const PartialResultPtr &value)
{
    DataCollectionPtr collection
        = staticPointerCast<DataCollection, SerializationIface>(Argument::get(id));
    collection->push_back( value );
}

static size_t getNumFeatures(const SerializationIfacePtr& partResVal)
{
    PartialResultPtr partRes = dynamicPointerCast<PartialResult, SerializationIface>(partResVal);
    auto nt = partRes->get(partialClusters);
    return nt.get() ? nt->getNumberOfColumns() : 0;
}

/**
* Returns the number of features in the Input data table in the second step of the distributed processing mode
* \return Number of features in the Input data table
*/
size_t DistributedStep2MasterInput::getNumberOfFeatures() const
{
    DataCollectionPtr collection = get(partialResults);
    const size_t nBlocks = collection->size();
    size_t nFeatures = 0;
    for(size_t i = 0; i < nBlocks && !nFeatures; i++)
    {
        nFeatures = getNumFeatures((*collection)[i]);
    }
    return nFeatures;
}

template <typename Type>
Type getSingleValue(NumericTable& tbl)
{
    BlockDescriptor<Type> block;
    tbl.getBlockOfRows(0, 1, readOnly, block);
    Type value = block.getBlockPtr()[0];
    tbl.releaseBlockOfRows(block);
    return value;
}

static services::Status checkPartialResult(const SerializationIfacePtr& ptr, const Parameter *kmPar,
    int unexpectedLayouts, size_t nFeatures, int& nClusters)
{
    PartialResultPtr pPres = dynamicPointerCast<PartialResult, SerializationIface>(ptr);
    DAAL_CHECK(pPres, ErrorIncorrectElementInPartialResultCollection);

    auto pPartialClustersNumber = pPres->get(partialClustersNumber);
    services::Status s;
    DAAL_CHECK_STATUS(s, checkNumericTable(pPartialClustersNumber.get(), partialClustersNumberStr(), unexpectedLayouts, 0, 1, 1));

    const int nPartClusters = getSingleValue<int>(*pPartialClustersNumber);
    DAAL_CHECK((nPartClusters >= 0) && (nPartClusters <= kmPar->nClusters), ErrorIncorrectNumberOfPartialClusters);
    auto pPartialClusters = pPres->get(partialClusters);
    if(pPartialClusters.get())
    {
        DAAL_CHECK(pPartialClusters->getNumberOfRows() >= nPartClusters, ErrorIncorrectNumberOfPartialClusters);
        DAAL_CHECK_STATUS(s, checkNumericTable(pPartialClusters.get(), partialClustersStr(),
            unexpectedLayouts, 0, nFeatures, pPartialClusters->getNumberOfRows()));
        nClusters += nPartClusters;
    }
    else if(nPartClusters)
    {
        return services::Status(ErrorIncorrectNumberOfPartialClusters);
    }
    return s;
}

/**
* Checks an input object for computing initial clusters for the K-Means algorithm
* in the second step of the distributed processing mode
* \param[in] par     %Parameter of the algorithm
* \param[in] method  Computation method of the algorithm
*/
services::Status DistributedStep2MasterInput::check(const daal::algorithms::Parameter *par, int method) const
{
    const Parameter *kmPar = static_cast<const Parameter *>(par);
    DataCollectionPtr collection = get(partialResults);

    DAAL_CHECK(collection, ErrorNullInputDataCollection);
    const size_t nBlocks = collection->size();
    DAAL_CHECK(nBlocks > 0, ErrorIncorrectNumberOfInputNumericTables);

    const int unexpectedLayouts = (int)packed_mask;
    int nClusters = 0;
    int nFeatures = 0;
    services::Status s;
    for(size_t i = 0; i < nBlocks; i++)
    {
        s |= checkPartialResult((*collection)[i], kmPar, unexpectedLayouts, nFeatures, nClusters);
        if(s && !nFeatures)
            nFeatures = getNumFeatures((*collection)[i]);
    }
    if(!s) return s;
    DAAL_CHECK(nClusters == kmPar->nClusters, ErrorIncorrectTotalNumberOfPartialClusters);
    return s;
}

} // namespace interface1
} // namespace init
} // namespace kmeans
} // namespace algorithm
} // namespace daal
