/* file: zscore_result_fpt.cpp */
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
//  Implementation of zscore algorithm and types methods.
//--
*/

#include "zscore_result.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace normalization
{
namespace zscore
{

namespace interface2
{
/**
* Allocates memory to store final results of the z-score normalization algorithms
* \param[in] input     Input objects for the z-score normalization algorithm
* \param[in] parameter Pointer to algorithm parameter
*/
template <typename algorithmFPType>
Status ResultImpl::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter)
{
    Status status = interface1::ResultImpl::allocate<algorithmFPType>(input);
    DAAL_CHECK_STATUS_VAR(status);

    const Input *in = static_cast<const Input *>(input);
    DAAL_CHECK(in, ErrorNullInput);

    NumericTablePtr dataTable = in->get(zscore::data);
    DAAL_CHECK(dataTable, ErrorNullInputNumericTable);

    const size_t nFeatures = dataTable->getNumberOfColumns();
    const size_t nVectors = dataTable->getNumberOfRows();

    if (parameter != NULL)
    {
        const BaseParameter *algParameter = static_cast<const BaseParameter *>(parameter);
        DAAL_CHECK(algParameter, ErrorNullParameterNotSupported);

        if (algParameter->resultsToCompute & mean)
        {
            (*this)[means] = HomogenNumericTable<algorithmFPType>::create
                    (nFeatures, 1, NumericTableIface::doAllocate, algorithmFPType(0.), &status);
            DAAL_CHECK_STATUS_VAR(status);
        }
        if (algParameter->resultsToCompute & variance)
        {
            (*this)[variances] = HomogenNumericTable<algorithmFPType>::create
                    (nFeatures, 1, NumericTableIface::doAllocate, algorithmFPType(0.), &status);
            DAAL_CHECK_STATUS_VAR(status);
        }
    }

    return status;
}

template DAAL_EXPORT Status ResultImpl::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter);

}// namespace interface2

}// namespace zscore
}// namespace normalization
}// namespace algorithms
}// namespace daal
