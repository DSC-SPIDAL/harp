/* file: concat_layer_forward_fpt.cpp */
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
//  Implementation of concat calculation algorithm and types methods.
//--
*/

#include "concat_layer_forward_types.h"
#include "concat_layer_types.h"
#include "service_mkl_tensor.h"
#include "tensor.h"

using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace concat
{
namespace forward
{
namespace interface1
{
/**
* Allocates memory to store the result of the forward concat layer
* \param[in] input     Pointer to an object containing the input data
* \param[in] parameter %Parameter of the algorithm
* \param[in] method    Computation method for the algorithm
*/
template <typename algorithmFPType>
DAAL_EXPORT Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    using daal::data_management::Tensor;
    using daal::internal::MklTensor;

    const Input *in = static_cast<const Input * >(input);
    const Parameter *par = static_cast<const Parameter *>(parameter);

    data_management::TensorPtr valueTable = in->get(layers::forward::inputLayerData, 0);

    const size_t nInputs = in->get(layers::forward::inputLayerData)->size();
    const size_t concatDimension = par->concatDimension;

    size_t sum = 0;
    for (size_t i = 0; i < nInputs; i++)
    {
        const size_t dim = (in->get(layers::forward::inputLayerData, i))->getDimensionSize(concatDimension);
        sum += dim;
    }
    DAAL_CHECK(valueTable, ErrorNullInputNumericTable);

    Collection<size_t> dimsCollection = valueTable->getDimensions();
    dimsCollection[concatDimension] = sum;

    if (!get(layers::forward::value))
    {
        set(layers::forward::value, TensorPtr(new MklTensor<algorithmFPType>(dimsCollection)));
    }
    set(layers::forward::resultForBackward, LayerDataPtr(new LayerData()));

    SharedPtr<data_management::HomogenNumericTable<size_t> > auxDimTable(new data_management::HomogenNumericTable<size_t>
                                                                                   (nInputs, 1, data_management::NumericTable::doAllocate));
    size_t *auxDimArray = auxDimTable->getArray();

    for (size_t i = 0; i < nInputs; i++)
    {
        size_t dim = (in->get(layers::forward::inputLayerData, i))->getDimensionSize(concatDimension);
        auxDimArray[i] = dim;
    }

    set(layers::concat::auxInputDimensions, auxDimTable);
    return Status();
}

template DAAL_EXPORT Status Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

}// namespace interface1
}// namespace forward
}// namespace concat
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
