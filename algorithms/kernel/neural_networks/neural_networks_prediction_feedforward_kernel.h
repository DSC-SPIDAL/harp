/* file: neural_networks_prediction_feedforward_kernel.h */
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

//++
//  Declaration of template function that calculate neural networks.
//--


#ifndef __NEURAL_NETWORKS_PREDICTION_FEEDFORWARD_KERNEL_H__
#define __NEURAL_NETWORKS_PREDICTION_FEEDFORWARD_KERNEL_H__

#include "neural_networks/neural_networks_prediction.h"
#include "neural_networks/neural_networks_types.h"
#include "neural_networks/neural_networks_prediction_types.h"
#include "kernel.h"
#include "homogen_tensor.h"
#include "service_tensor.h"
#include "service_numeric_table.h"
#include "neural_networks_feedforward.h"

using namespace daal::data_management;
using namespace daal::services;
using namespace daal::internal;
using namespace daal::algorithms::neural_networks::internal;
using namespace daal::algorithms::neural_networks::layers;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace prediction
{
namespace internal
{
/**
 *  \brief Kernel for neural network calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class NeuralNetworksFeedforwardPredictionKernel : public Kernel
{
public:
    services::Status compute(const Input *input, Result *result);
    services::Status initialize(const Input *input, const neural_networks::prediction::Parameter *parameter, Result *result);
    services::Status reset();

private:
    size_t nLastLayers;
    size_t nLayers;
    size_t nSamples;
    size_t batchSize;
    UniquePtr<LastLayerIndices, cpu> lastLayersIndices;
    SharedPtr<HomogenTensor<algorithmFPType> > sample;
    TArray<ReadSubtensor<algorithmFPType, cpu>, cpu> lastLayerResults;
    TArray<WriteOnlySubtensor<algorithmFPType, cpu>, cpu> predictions;
};

} // namespace daal::internal
} // namespace prediction
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
