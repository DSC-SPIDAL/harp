/* file: pca_result.h */
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
 * Allocates memory for storing partial results of the PCA algorithm
 * \param[in] input Pointer to an object containing input data
 * \param[in] parameter Algorithm parameter
 * \param[in] method Computation method
 */
template<typename algorithmFPType>
DAAL_EXPORT services::Status Result::allocate(const daal::algorithms::Input *input, daal::algorithms::Parameter *parameter, const Method method)
{
    const InputIface *in = static_cast<const InputIface *>(input);
    size_t nFeatures = in->getNFeatures();

    set(eigenvalues,
        data_management::NumericTablePtr(new data_management::HomogenNumericTable<algorithmFPType>
                                                           (nFeatures, 1, data_management::NumericTableIface::doAllocate, 0)));
    set(eigenvectors,
        data_management::NumericTablePtr(new data_management::HomogenNumericTable<algorithmFPType>
                                                           (nFeatures, nFeatures, data_management::NumericTableIface::doAllocate, 0)));
    return services::Status();
}

/**
 * Allocates memory for storing partial results of the PCA algorithm     * \param[in] partialResult Pointer to an object containing input data
 * \param[in] parameter Parameter of the algorithm
 * \param[in] method        Computation method
 */
template<typename algorithmFPType>
DAAL_EXPORT services::Status Result::allocate(const daal::algorithms::PartialResult *partialResult, daal::algorithms::Parameter *parameter, const Method method)
{
    const PartialResultBase *partialRes = static_cast<const PartialResultBase *>(partialResult);
    size_t nFeatures = partialRes->getNFeatures();

    set(eigenvalues,
        data_management::NumericTablePtr(new data_management::HomogenNumericTable<algorithmFPType>
                                                           (nFeatures, 1,
                                                            data_management::NumericTableIface::doAllocate, 0)));
    set(eigenvectors,
        data_management::NumericTablePtr(new data_management::HomogenNumericTable<algorithmFPType>
                                                           (nFeatures,
                                                            nFeatures, data_management::NumericTableIface::doAllocate, 0)));
    return services::Status();
}

} // namespace pca
} // namespace algorithms
} // namespace daal

#endif
