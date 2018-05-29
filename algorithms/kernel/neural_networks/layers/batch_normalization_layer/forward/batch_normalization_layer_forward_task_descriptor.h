/* file: batch_normalization_layer_forward_task_descriptor.h */
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

#ifndef __BATCH_NORMALIZATION_LAYER_FORWARD_TASK_DESCRIPTOR_H__
#define __BATCH_NORMALIZATION_LAYER_FORWARD_TASK_DESCRIPTOR_H__

#include "neural_networks/layers/batch_normalization/batch_normalization_layer_forward.h"
#include "neural_networks/layers/batch_normalization/batch_normalization_layer_forward_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace batch_normalization
{
namespace forward
{
namespace internal
{

class BatchNormalizationTaskDescriptor
{
public:
    BatchNormalizationTaskDescriptor(Input *in, Result *re, Parameter *pa);

    data_management::Tensor *input;
    data_management::Tensor *weights;
    data_management::Tensor *biases;
    data_management::Tensor *inPopMean;
    data_management::Tensor *inPopVariance;
    data_management::Tensor *value;
    data_management::Tensor *auxMean;
    data_management::Tensor *auxStd;
    data_management::Tensor *auxPopMean;
    data_management::Tensor *auxPopVariance;
    Parameter *parameter;
};

} // internal
} // forward
} // batch_normalization
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
