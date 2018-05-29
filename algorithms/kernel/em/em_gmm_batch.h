/* file: em_gmm_batch.h */
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
//  Implementation of the EM for GMM interface.
//--
*/

#ifndef __EM_BATCH_
#define __EM_BATCH_

#include "em_gmm_types.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace em_gmm
{

/**
 * Allocates memory for storing results of the EM for GMM algorithm
 * \param[in] input     Pointer to the input structure
 * \param[in] parameter Pointer to the parameter structure
 * \param[in] method    Computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT services::Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    Input *algInput = static_cast<Input *>(const_cast<daal::algorithms::Input *>(input));
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);

    size_t nFeatures   = algInput->get(data)->getNumberOfColumns();
    size_t nComponents = algParameter->nComponents;

    services::Status status;

    set(weights, HomogenNumericTable<algorithmFPType>::create(nComponents, 1, NumericTable::doAllocate, 0, &status));
    set(means, HomogenNumericTable<algorithmFPType>::create(nFeatures, nComponents, NumericTable::doAllocate, 0, &status));

    DataCollectionPtr covarianceCollection = DataCollectionPtr(new DataCollection());
    for(size_t i = 0; i < nComponents; i++)
    {
        if(algParameter->covarianceStorage == diagonal)
        {
            covarianceCollection->push_back(HomogenNumericTable<algorithmFPType>::create(nFeatures, 1, NumericTable::doAllocate, 0, &status));
        }
        else
        {
            covarianceCollection->push_back(HomogenNumericTable<algorithmFPType>::create(nFeatures, nFeatures, NumericTable::doAllocate, 0, &status));
        }
    }
    set(covariances, covarianceCollection);

    set(goalFunction, HomogenNumericTable<algorithmFPType>::create(1, 1, NumericTable::doAllocate, 0, &status));
    set(nIterations, HomogenNumericTable<int>::create(1, 1, NumericTable::doAllocate, 0, &status));
    return status;
}

} // namespace em_gmm
} // namespace algorithms
} // namespace daal

#endif
