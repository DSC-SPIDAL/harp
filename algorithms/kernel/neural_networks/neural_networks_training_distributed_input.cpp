/* file: neural_networks_training_distributed_input.cpp */
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

#include "neural_networks_training_input.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace training
{
DistributedInput<step1Local>::DistributedInput(size_t nElements) : Input(nElements) {};
DistributedInput<step1Local>::DistributedInput(const DistributedInput& other) : Input(other){}

ModelPtr DistributedInput<step1Local>::get(Step1LocalInputId id) const
{
    return Model::cast(Argument::get(id));
}

void DistributedInput<step1Local>::set(Step1LocalInputId id, const ModelPtr &value)
{
    Argument::set(id, value);
}

Status DistributedInput<step1Local>::check(const daal::algorithms::Parameter *par, int method) const
{
    ModelPtr model = get(inputModel);
    TensorPtr dataTensor = get(data);
    Status s;
    DAAL_CHECK_STATUS(s, checkTensor(dataTensor.get(), dataStr()))

    size_t nSamples = dataTensor->getDimensionSize(0);
    size_t modelBatchSize = model->getForwardLayer(0)->getLayerInput()->get(layers::forward::data)->getDimensionSize(0);
    DAAL_CHECK_EX(nSamples >= modelBatchSize, ErrorIncorrectParameter, ParameterName, batchSizeStr());

    DAAL_CHECK_STATUS(s, checkImpl(par, method));
    return s;
}


DistributedInput<step2Master>::DistributedInput() : daal::algorithms::Input(lastStep2MasterInputId + 1)
{
    set(partialResults, KeyValueDataCollectionPtr(new KeyValueDataCollection()));
}

DistributedInput<step2Master>::DistributedInput(const DistributedInput& other) : daal::algorithms::Input(other){}

KeyValueDataCollectionPtr DistributedInput<step2Master>::get(Step2MasterInputId id) const
{
    return KeyValueDataCollection::cast(Argument::get(id));
}

void DistributedInput<step2Master>::set(Step2MasterInputId id, const KeyValueDataCollectionPtr &value)
{
    Argument::set(id, value);
}

void DistributedInput<step2Master>::add(Step2MasterInputId id, size_t key, const PartialResultPtr &value)
{
    KeyValueDataCollectionPtr collection = get(id);
    if (!collection) { return; }
    (*collection)[key] = value;
}

Status DistributedInput<step2Master>::check(const daal::algorithms::Parameter *par, int method) const
{
    return Status();
}

}
}
}
}
