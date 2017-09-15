/* file: kmeans_partialresult.h */
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

#ifndef __KMEANS_PARTIALRESULT_
#define __KMEANS_PARTIALRESULT_

#include "algorithms/kmeans/kmeans_types.h"

namespace daal
{
namespace algorithms
{
namespace kmeans
{

/**
 * Allocates memory to store partial results of the K-Means algorithm
 * \param[in] input        Pointer to the structure of the input objects
 * \param[in] parameter    Pointer to the structure of the algorithm parameters
 * \param[in] method       Computation method of the algorithm
 */
template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const Parameter *kmPar = static_cast<const Parameter *>(parameter);

    size_t nFeatures = static_cast<const InputIface *>(input)->getNumberOfFeatures();
    size_t nClusters = kmPar->nClusters;

    Argument::set(nObservations, data_management::SerializationIfacePtr(
                      new data_management::HomogenNumericTable<algorithmFPType>(1, nClusters, data_management::NumericTable::doAllocate)));
    Argument::set(partialSums, data_management::SerializationIfacePtr(
                      new data_management::HomogenNumericTable<algorithmFPType>(nFeatures, nClusters,
                                                                                data_management::NumericTable::doAllocate)));
    Argument::set(partialObjectiveFunction, data_management::SerializationIfacePtr(
                      new data_management::HomogenNumericTable<algorithmFPType>(1, 1, data_management::NumericTable::doAllocate)));

    const Input *step1Input = dynamic_cast<const Input *>(input);
    if (kmPar->assignFlag && step1Input)
    {
        const size_t nRows = step1Input->get(data)->getNumberOfRows();
        Argument::set(partialAssignments, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<int>(1, nRows, data_management::NumericTable::doAllocate)));
    }

    return services::Status();
}

} // namespace kmeans
} // namespace algorithms
} // namespace daal

#endif
