/* file: elu_layer_backward.cpp */
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
//  Implementation of ELU calculation algorithm and types methods.
//--
*/

#include "elu_layer_backward_types.h"
#include "elu_layer_types.h"
#include "serialization_utils.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace elu
{
namespace backward
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_NEURAL_NETWORKS_LAYERS_ELU_BACKWARD_RESULT_ID);
/** \brief Default constructor */
Input::Input() {};
Input::Input(const Input& other) : super(other) {}

/**
 * Returns an input object for the backward ELU layer
 * \param[in] id    Identifier of the input object
 * \return          %Input object that corresponds to the given identifier
 */
data_management::TensorPtr Input::get(LayerDataId id) const
{
    LayerDataPtr layerData = get(layers::backward::inputFromForward);
    if (!layerData) { return data_management::TensorPtr(); }
    return data_management::Tensor::cast((*layerData)[id]);
}

/**
 * Sets an input object for the backward ELU layer
 * \param[in] id     Identifier of the input object
 * \param[in] value  Pointer to the input object
 */
void Input::set(LayerDataId id, const data_management::TensorPtr &value)
{
    layers::LayerDataPtr layerData = get(layers::backward::inputFromForward);
    if (layerData) { (*layerData)[id] = value; }
}

/**
 * Checks an input object of the backward ELU layer
 * \param[in] par     Algorithm parameter
 * \param[in] method  Computation method
 */
services::Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    const layers::Parameter *param = static_cast<const layers::Parameter *>(par);
    if (!param->propagateGradient) { return services::Status(); }

    services::Status status;
    DAAL_CHECK_STATUS(status, layers::backward::Input::check(par, method));

    auto &inputGradientDimensions = get(layers::backward::inputGradient)->getDimensions();
    DAAL_CHECK_TENSOR(status, get(elu::auxData).get(), auxDataStr(), &inputGradientDimensions);

    // DAAL_CHECK_TENSOR(status, get(elu::auxIntermediateValue).get(), auxIntermediateValueStr(), &inputGradientDimensions);

    return status;
}

/** \brief Default constructor */
Result::Result() : layers::backward::Result() {};

/**
 * Checks the result of the backward ELU layer
 * \param[in] input   %Input object for the algorithm
 * \param[in] par     %Parameter of the algorithm
 * \param[in] method  Computation method
 */
services::Status Result::check(const daal::algorithms::Input *input,
                               const daal::algorithms::Parameter *par, int method) const
{
    using namespace daal::services;
    using daal::data_management::Tensor;

    Status status;

    const layers::Parameter *param = static_cast<const layers::Parameter *>(par);
    if (!param->propagateGradient) { return services::Status(); }

    const Input *in = static_cast<const Input *>(input);
    const Tensor *auxDataTensor = in->get(elu::auxData).get();
    DAAL_CHECK(auxDataTensor, Error::create(ErrorNullTensor, ArgumentName, inputGradientStr()));

    auto &auxDataDimensions = auxDataTensor->getDimensions();
    DAAL_CHECK_TENSOR(status, get(layers::backward::gradient).get(), gradientStr(), &auxDataDimensions);

    return status;
}

}// namespace interface1
}// namespace backward
}// namespace elu
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
