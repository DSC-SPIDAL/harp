/* file: lcn_layer.cpp */
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
//  Implementation of lcn calculation algorithm and types methods.
//--
*/

#include "lcn_layer_types.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace lcn
{

namespace interface1
{
/**
 *  Default constructor
 */
Parameter::Parameter() : indices(2, 3), sigmaDegenerateCasesThreshold(0.0001)
{
    services::Collection<size_t> dims(2);
    dims[0] = 5;
    dims[1] = 5;

    kernel = data_management::TensorPtr(
                    new data_management::HomogenTensor<float>(dims, data_management::Tensor::doAllocate, 0.04f));

    sumDimension = data_management::NumericTablePtr(
                    new data_management::HomogenNumericTable<float>(1, 1, data_management::NumericTableIface::doAllocate, (float)(1)));
}

/**
 * Checks the correctness of the parameter
 */
services::Status Parameter::check() const
{
    services::Status s;
    if(indices.dims[0] > 3 || indices.dims[1] > 3)
    {
        return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ArgumentName, indicesStr()));
    }
    if(sumDimension)
    {
        DAAL_CHECK_STATUS(s, data_management::checkNumericTable(sumDimension.get(), dimensionStr(), 0, 0, 1, 1));

        data_management::NumericTablePtr dimensionTable = sumDimension;

        data_management::BlockDescriptor<int> block;
        dimensionTable->getBlockOfRows(0, 1, data_management::readOnly, block);
        int *dataInt = block.getBlockPtr();
        size_t dim = dataInt[0];

        if(dim > 1)
        {
            return services::Status(services::Error::create(services::ErrorIncorrectParameter, services::ArgumentName, dimensionStr()));
        }
        dimensionTable->releaseBlockOfRows(block);
    }
    return s;
}

}// namespace interface1
}// namespace lcn
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
