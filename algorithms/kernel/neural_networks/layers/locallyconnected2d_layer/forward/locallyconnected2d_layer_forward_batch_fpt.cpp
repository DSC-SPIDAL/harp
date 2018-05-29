/* file: locallyconnected2d_layer_forward_batch_fpt.cpp */
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
//  Implementation of locally connected calculation algorithm and types methods.
//--
*/
#include "locallyconnected2d_layer_forward_types.h"
#include "locallyconnected2d_layer_types.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace locallyconnected2d
{
namespace forward
{
namespace interface1
{
/**
* Allocates memory to store the result of forward  2D locally connected layer
 * \param[in] parameter %Parameter of forward 2D locally connected layer
 * \param[in] method    Computation method for the layer
*/
template <typename algorithmFPType>
services::Status DAAL_EXPORT Input::allocate(const daal::algorithms::Parameter *parameter, const int method)
{
    using daal::services::SharedPtr;
    using daal::data_management::Tensor;
    using daal::data_management::HomogenTensor;

    const Parameter *param =  static_cast<const Parameter * >(parameter);
    services::Status s;
    if( !get(layers::forward::weights) )
    {
        TensorPtr tensor = HomogenTensor<algorithmFPType>::create(getWeightsSizes(param), Tensor::doAllocate, &s);
        set(layers::forward::weights, tensor);
    }

    if( !get(layers::forward::biases) )
    {
        TensorPtr tensor = HomogenTensor<algorithmFPType>::create(getBiasesSizes(param), Tensor::doAllocate, &s);
        set(layers::forward::biases, tensor);
    }
    return s;
}

/**
 * Allocates memory to store the result of forward  2D locally connected layer
 * \param[in] input     %Input object for the algorithm
 * \param[in] parameter %Parameter of forward 2D locally connected layer
 * \param[in] method    Computation method for the layer
 */
template <typename algorithmFPType>
services::Status DAAL_EXPORT Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    using namespace data_management;
    const Input *in = static_cast<const Input * >(input);

    services::Status s;
    const services::Collection<size_t> &inDims = in->get(layers::forward::data)->getDimensions();

    if (!get(layers::forward::value))
    {
        set(layers::forward::value, HomogenTensor<algorithmFPType>::create(getValueSize(inDims, parameter, method), Tensor::doAllocate, &s));
    }

    const layers::Parameter *par = static_cast<const layers::Parameter * >(parameter);
    if(!par->predictionStage)
    {
        set(layers::forward::resultForBackward, LayerDataPtr(new LayerData()));
        s |= setResultForBackward(input);
    }
    return services::Status();
}

template DAAL_EXPORT services::Status Input::allocate<DAAL_FPTYPE>(const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT services::Status Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

}// namespace interface1
}// namespace forward
}// namespace locallyconnected2d
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
