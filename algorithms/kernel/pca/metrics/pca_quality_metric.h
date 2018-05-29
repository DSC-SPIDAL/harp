/* file: pca_quality_metric.h */
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
//  Implementation of the class defining the pca explained variance quality metric
//--
*/

#ifndef __PCA_QUALITY_METRIC_
#define __PCA_QUALITY_METRIC_

#include "algorithms/pca/pca_explained_variance_types.h"

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace quality_metric
{
namespace explained_variance
{

/**
* Allocates memory to store
* \param[in] input   %Input object
* \param[in] par     %Parameter of the algorithm
* \param[in] method  Algorithm method
*/
template <typename algorithmFPType>
DAAL_EXPORT services::Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method)
{
    size_t nComponents = (static_cast<const Parameter *>(par))->nComponents;
    if (nComponents == 0)
    {
        const Input *in = static_cast<const Input *>(input);
        nComponents = in->get(eigenvalues)->getNumberOfColumns();
    }
    services::Status status;
    set(explainedVariances,
        data_management::HomogenNumericTable<algorithmFPType>::create(nComponents, 1, data_management::NumericTableIface::doAllocate, 0, &status));
    DAAL_CHECK_STATUS_VAR(status);
    set(explainedVariancesRatios,
        data_management::HomogenNumericTable<algorithmFPType>::create(nComponents, 1, data_management::NumericTableIface::doAllocate, 0, &status));
    DAAL_CHECK_STATUS_VAR(status);
    set(noiseVariance,
        data_management::HomogenNumericTable<algorithmFPType>::create(1, 1, data_management::NumericTableIface::doAllocate, 0, &status));
    DAAL_CHECK_STATUS_VAR(status);

    return status;
}

} // namespace explained_variance
} // namespace quality_metric
} // namespace pca
} // namespace algorithms
} // namespace daal

#endif
