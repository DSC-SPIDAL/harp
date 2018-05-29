/* file: neural_networks_feedforward.cpp */
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
//  Implementation of common functions for feedforward algorithm
//--
*/

#include "neural_networks_feedforward.h"
#include "services/daal_memory.h"
#include "daal_strings.h"

using namespace daal::services;
using namespace daal::data_management;

daal::algorithms::neural_networks::internal::LastLayerIndices::LastLayerIndices(
            const Collection<layers::NextLayers> *nextLayers,
            const KeyValueDataCollectionPtr &tensors) : layerIndices(NULL), tensorIndices(NULL), buffer(NULL)
{
    size_t nLayers = nextLayers->size();
    nLastLayers = 0;
    for(size_t layerId = 0; layerId < nLayers; layerId++)
    {
        if (nextLayers->get(layerId).size() == 0)
        {
            nLastLayers++;
        }
    }

    buffer = (size_t *)daal_malloc(2 * nLastLayers * sizeof(size_t));
    if (!buffer)
        return;

    layerIndices  = buffer;
    tensorIndices = buffer + nLastLayers;

    for(size_t layerId = 0, iLastLayer = 0; layerId < nLayers; layerId++)
    {
        if (nextLayers->get(layerId).size() == 0)
        {
            layerIndices      [iLastLayer] = layerId;
            tensorIndices[iLastLayer] = layerId;
            iLastLayer++;
        }
    }

    if (nLastLayers == 1 && tensors->getKeyByIndex(0) != layerIndices[0])
    {
        tensorIndices[0] = tensors->getKeyByIndex(0);
    }
}

daal::algorithms::neural_networks::internal::LastLayerIndices::~LastLayerIndices()
{
    daal_free(buffer);
}

size_t daal::algorithms::neural_networks::internal::LastLayerIndices::nLast() const
{
    return nLastLayers;
}

size_t daal::algorithms::neural_networks::internal::LastLayerIndices::layerIndex(size_t idx) const
{
    return layerIndices[idx];
}

size_t daal::algorithms::neural_networks::internal::LastLayerIndices::tensorIndex(size_t idx) const
{
    return tensorIndices[idx];
}

bool daal::algorithms::neural_networks::internal::LastLayerIndices::isValid() const
{
    return (buffer != NULL);
}


Status daal::algorithms::neural_networks::internal::processLayerErrors(size_t layerId, const Status &layerStatus)
{
    if (layerStatus)
        return layerStatus;
    Status s(Error::create(ErrorNeuralNetworkLayerCall, Layer, layerId));
    return (s |= layerStatus);
}
