/* file: pca_partialresult_svd.h */
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
//  Implementation of PCA algorithm interface.
//--
*/

#ifndef __PCA_PARTIALRESULT_SVD_
#define __PCA_PARTIALRESULT_SVD_

#include "algorithms/pca/pca_types.h"

namespace daal
{
namespace algorithms
{
namespace pca
{

/**
 * Allocates memory for storing partial results of the PCA SVD algorithm
 * \param[in] input     Pointer to an object containing input data
 * \param[in] parameter Pointer to the structure of algorithm parameters
 * \param[in] method    Computation method
 */
template<typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult<svdDense>::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    set(nObservationsSVD,
        data_management::NumericTablePtr(
            new data_management::HomogenNumericTable<algorithmFPType>(1, 1, data_management::NumericTableIface::doAllocate, 0)));
    set(sumSquaresSVD,
        data_management::NumericTablePtr(
            new data_management::HomogenNumericTable<algorithmFPType>((static_cast<const InputIface *>(input))->getNFeatures(), 1,
                                                             data_management::NumericTableIface::doAllocate, 0)));
    set(sumSVD,
        data_management::NumericTablePtr(
            new data_management::HomogenNumericTable<algorithmFPType>((static_cast<const InputIface *>(input))->getNFeatures(), 1,
                                                             data_management::NumericTableIface::doAllocate, 0)));
    set(auxiliaryData, data_management::DataCollectionPtr(new data_management::DataCollection()));
    return services::Status();
};

template<typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult<svdDense>::initialize(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    services::Status s;
    DAAL_CHECK_STATUS(s, get(nObservationsSVD)->assign((algorithmFPType)0.0))
    DAAL_CHECK_STATUS(s, get(sumSquaresSVD)->assign((algorithmFPType)0.0))
    DAAL_CHECK_STATUS(s, get(sumSVD)->assign((algorithmFPType)0.0))
    get(auxiliaryData)->clear();
    return s;
};

} // namespace pca
} // namespace algorithms
} // namespace daal

#endif