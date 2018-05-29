/* file: neural_networks_prediction_model_fpt.cpp */
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
//  Implementation of model of the prediction stage of neural network
//--
*/

#include "neural_networks_prediction_model.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace prediction
{

/**
 * Constructs model object for the prediction stage of neural network
 * from the list of forward stages of the layers and the list of connections between the layers.
 * And allocates storage for weights and biases of the forward layers is needed.
 * \param[in] forwardLayersForModel         List of forward stages of the layers
 * \param[in] nextLayersForModel            List of next layers for each layer with corresponding index
 * \param[in] dummy                         Data type to be used to allocate storage for weights and biases
 * \param[in] storeWeightsInTable           Flag.
 *                                          If true then the storage for weights and biases is allocated as a single piece of memory,
 *                                          otherwise weights and biases are allocated as separate tensors
 * \DAAL_DEPRECATED_USE{ Model::create }
 */
template<typename modelFPType>
DAAL_EXPORT Model::Model(const neural_networks::ForwardLayersPtr &forwardLayersForModel,
                         const services::SharedPtr<services::Collection<layers::NextLayers> > &nextLayersForModel,
                         modelFPType dummy, bool storeWeightsInTable) :
    ModelImpl(forwardLayersForModel, nextLayersForModel, storeWeightsInTable), _allocatedBatchSize(0)
{
    bool checkWeightsAndBiasesAlloc = false;
    createWeightsAndBiases<modelFPType>(checkWeightsAndBiasesAlloc);
}

template<typename modelFPType>
DAAL_EXPORT Model::Model(const neural_networks::ForwardLayersPtr &forwardLayersForModel,
                         const services::SharedPtr<services::Collection<layers::NextLayers> > &nextLayersForModel,
                         modelFPType dummy, bool storeWeightsInTable, services::Status &st) :
    ModelImpl(forwardLayersForModel, nextLayersForModel, storeWeightsInTable, st), _allocatedBatchSize(0)
{
    bool checkWeightsAndBiasesAlloc = false;
    st |= createWeightsAndBiases<modelFPType>(checkWeightsAndBiasesAlloc);
}

/**
 * Constructs model object for the prediction stage of neural network
 * from the list of forward stages of the layers and the list of connections between the layers.
 * And allocates storage for weights and biases of the forward layers is needed.
 * \param[in] forwardLayersForModel  List of forward stages of the layers
 * \param[in] nextLayersForModel     List of next layers for each layer with corresponding index
 * \param[in] storeWeightsInTable    Flag.
 *                                   If true then the storage for weights and biases is allocated as a single piece of memory,
 * \param[out] stat                  Status of the model construction
 * \return Model object for the prediction stage of neural network
 */
template<typename modelFPType>
DAAL_EXPORT ModelPtr Model::create(const neural_networks::ForwardLayersPtr &forwardLayersForModel,
                                   const services::SharedPtr<services::Collection<layers::NextLayers> > &nextLayersForModel,
                                   bool storeWeightsInTable, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(Model, forwardLayersForModel, nextLayersForModel, (modelFPType)0.0, storeWeightsInTable);
}

template DAAL_EXPORT Model::Model(const neural_networks::ForwardLayersPtr &,
                                  const services::SharedPtr<services::Collection<layers::NextLayers> >&,
                                  DAAL_FPTYPE, bool);

template DAAL_EXPORT Model::Model(const neural_networks::ForwardLayersPtr &,
                                  const services::SharedPtr<services::Collection<layers::NextLayers> >&,
                                  DAAL_FPTYPE, bool, services::Status&);

template DAAL_EXPORT ModelPtr Model::create<DAAL_FPTYPE>(const neural_networks::ForwardLayersPtr&,
                                                         const services::SharedPtr<services::Collection<layers::NextLayers> >&,
                                                         bool, services::Status*);

} // namespace prediction
} // namespace neural_networks
} // namespace algorithms
} // namespace daal
