/* file: covariance_partialresult.h */
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
//  Implementation of covariance algorithm and types methods.
//--
*/

#ifndef __COVARIANCE_PARTIALRESULT_
#define __COVARIANCE_PARTIALRESULT_

#include "covariance_types.h"

using namespace daal::data_management;
namespace daal
{
namespace algorithms
{
namespace covariance
{

/**
 * Allocates memory to store partial results of the correlation or variance-covariance matrix algorithm
 * \param[in] input     %Input objects of the algorithm
 * \param[in] parameter Parameters of the algorithm
 * \param[in] method    Computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const InputIface *algInput = static_cast<const InputIface *>(input);
    size_t nColumns = algInput->getNumberOfFeatures();

    set(nObservations, NumericTablePtr(new HomogenNumericTable<size_t>(1, 1, NumericTable::doAllocate)));
    set(crossProduct, NumericTablePtr(new HomogenNumericTable<algorithmFPType>(nColumns, nColumns, NumericTable::doAllocate)));
    set(sum, NumericTablePtr(new HomogenNumericTable<algorithmFPType>(nColumns, 1, NumericTable::doAllocate)));
    return services::Status();
}

template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::initialize(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const InputIface *algInput = static_cast<const InputIface *>(input);
    size_t nColumns = algInput->getNumberOfFeatures();

    get(nObservations)->assign((algorithmFPType)0.0);
    get(crossProduct)->assign((algorithmFPType)0.0);
    get(sum)->assign((algorithmFPType)0.0);
    return services::Status();
}

} // namespace covariance
} // namespace algorithms
} // namespace daal

#endif
