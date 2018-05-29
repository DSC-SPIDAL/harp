/* file: pca_transform_batch_fpt.cpp */
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
//  Implementation of the regression algorithm interface
//--
*/

#include "algorithms/pca/transform/pca_transform_types.h"
#include "data_management/data/homogen_numeric_table.h"
#include "daal_strings.h"

using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace transform
{
using namespace daal::services;
using namespace daal::data_management;

template <typename algorithmFPType>
DAAL_EXPORT Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method)
{
    const Input *in = static_cast<const Input *>(input);
    const Parameter* parameter = static_cast<const Parameter *>(par);

    data_management::NumericTablePtr dataPtr = in->get(data);
    data_management::NumericTablePtr eigenvectorsPtr = in->get(eigenvectors);
    DAAL_CHECK_EX(dataPtr.get(), ErrorNullInputNumericTable, ArgumentName, dataStr())
    DAAL_CHECK_EX(eigenvectorsPtr.get(), ErrorNullInputNumericTable, ArgumentName, eigenvectorsStr())

    size_t nInputs             = dataPtr->getNumberOfRows();
    size_t nComponents         = parameter->nComponents == 0 ? eigenvectorsPtr->getNumberOfRows() : parameter->nComponents;

    services::Status status;
    set(transformedData, HomogenNumericTable<algorithmFPType>::create(nComponents, nInputs, NumericTable::doAllocate, &status));

    return status;
}

template DAAL_EXPORT Status Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method);

} // namespace transform
} // namespace pca
} // namespace algorithms
} // namespace daal
