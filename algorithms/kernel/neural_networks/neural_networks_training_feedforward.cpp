/* file: neural_networks_training_feedforward.cpp */
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
//  Declaration of common functions for using optimizaion solver
//  in feedforward neural network
//--
*/

#include "neural_networks_training_feedforward.h"
#include "daal_strings.h"
#include "services/daal_memory.h"

using namespace daal::services;
using namespace daal::data_management;
using namespace daal::algorithms::neural_networks;
using namespace daal::algorithms::neural_networks::layers;

daal::algorithms::neural_networks::internal::LearnableLayerIndices::LearnableLayerIndices(
                ForwardLayers *forwardLayers)
{
    size_t nLayers = forwardLayers->size();
    nLearnableLayers = 0;
    for(size_t layerId = 0; layerId < nLayers; layerId++)
    {
        forward::Input *forwardInput = forwardLayers->get(layerId)->getLayerInput();
        TensorPtr wTensor = forwardInput->get(forward::weights);
        if (!wTensor) { continue; }
        TensorPtr bTensor = forwardInput->get(forward::biases);
        if (!bTensor) { continue; }
        if (wTensor->getSize() + bTensor->getSize() > 0)
        {
            nLearnableLayers++;
        }
    }
    layerIndices.reset(nLearnableLayers);
    if (!layerIndices.get())
        return;
    size_t iLayer = 0;
    for(size_t layerId = 0; layerId < nLayers; layerId++)
    {
        forward::Input *forwardInput = forwardLayers->get(layerId)->getLayerInput();
        TensorPtr wTensor = forwardInput->get(forward::weights);
        if (!wTensor) { continue; }
        TensorPtr bTensor = forwardInput->get(forward::biases);
        if (!bTensor) { continue; }
        if (wTensor->getSize() + bTensor->getSize() > 0)
        {
            layerIndices[iLayer++] = layerId;
        }
    }
}

daal::algorithms::neural_networks::internal::LearnableLayerIndices::~LearnableLayerIndices()
{}

size_t daal::algorithms::neural_networks::internal::LearnableLayerIndices::nLearnable() const
{
    return nLearnableLayers;
}

size_t daal::algorithms::neural_networks::internal::LearnableLayerIndices::layerIndex(size_t idx) const
{
    return layerIndices[idx];
}

bool daal::algorithms::neural_networks::internal::LearnableLayerIndices::isValid() const
{
    return layerIndices.get();
}
