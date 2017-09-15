/* file: neural_networks_learnable_parameters_fpt.cpp */
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
//  Implementation of classes for storing learnable parameters of neural network
//--
*/

#include "neural_networks_learnable_parameters.h"
#include "neural_networks_weights_and_biases.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{

template class TensorWeightsAndBiases<DAAL_FPTYPE>;
template class NumericTableWeightsAndBiases<DAAL_FPTYPE>;

template<typename modelFPType>
services::Status DAAL_EXPORT ModelImpl::createWeightsAndBiases(bool checkAllocation)
{
    using namespace services;
    if (_weightsAndBiasesCreated) { return services::Status(); }
    services::Status s;

    if (checkAllocation)
    {
        DAAL_CHECK_STATUS(s, checkWeightsAndBiasesAllocation());
    }

    if (_storeWeightsInTable)
    {
        _weightsAndBiases = NumericTableWeightsAndBiases<modelFPType>::create(_forwardLayers, &s);
    }
    else
    {
        _weightsAndBiases = TensorWeightsAndBiases<modelFPType>::create(_forwardLayers, &s);
    }
    if (s)
        _weightsAndBiasesCreated = true;

    return s;
}

template DAAL_EXPORT services::Status ModelImpl::createWeightsAndBiases<DAAL_FPTYPE>(bool checkAllocation);

}
}
}
