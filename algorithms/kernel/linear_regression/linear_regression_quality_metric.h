/* file: linear_regression_quality_metric.h */
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
//  Implementation of the class defining the linear regression model
//--
*/

#ifndef __LIN_REG_QUALITY_METRIC_
#define __LIN_REG_QUALITY_METRIC_

#include "algorithms/linear_regression/linear_regression_single_beta_types.h"

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace quality_metric
{
namespace single_beta
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
    const data_management::NumericTablePtr dependentVariableTable = (static_cast<const Input *>(input))->get(expectedResponses);
    const size_t nDepVariable = dependentVariableTable->getNumberOfColumns();

    services::Status st;
    set(rms,
        data_management::HomogenNumericTable<algorithmFPType>::create(nDepVariable, 1, data_management::NumericTableIface::doAllocate, 0, &st));
    DAAL_CHECK_STATUS_VAR(st);
    set(variance,
        data_management::HomogenNumericTable<algorithmFPType>::create(nDepVariable, 1, data_management::NumericTableIface::doAllocate, 0, &st));
    DAAL_CHECK_STATUS_VAR(st);

    const size_t nBeta = (static_cast<const Input *>(input))->get(model)->getBeta()->getNumberOfColumns();
    data_management::DataCollectionPtr coll(new data_management::DataCollection());

    for (size_t i = 0; i < nDepVariable; ++i)
    {
        coll->push_back(data_management::HomogenNumericTable<algorithmFPType>::create(nBeta, nBeta, data_management::NumericTableIface::doAllocate, 0, &st));
    }

    set(betaCovariances, coll);

    set(zScore,
        data_management::HomogenNumericTable<algorithmFPType>::create(nBeta, nDepVariable, data_management::NumericTableIface::doAllocate, 0, &st));
    DAAL_CHECK_STATUS_VAR(st);

    set(confidenceIntervals,
        data_management::HomogenNumericTable<algorithmFPType>::create(2*nBeta, nDepVariable, data_management::NumericTableIface::doAllocate, 0, &st));
    DAAL_CHECK_STATUS_VAR(st);

    set(inverseOfXtX,
        data_management::HomogenNumericTable<algorithmFPType>::create(nBeta, nBeta, data_management::NumericTableIface::doAllocate, 0, &st));

    return st;
}

} // namespace single_beta
} // namespace quality_metric
} // namespace linear_regression
} // namespace algorithms
} // namespace daal

#endif
