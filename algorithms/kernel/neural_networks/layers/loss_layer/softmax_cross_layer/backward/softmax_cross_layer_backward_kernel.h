/* file: softmax_cross_layer_backward_kernel.h */
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
//  Implementation of the backward softmax cross layer
//--


#ifndef __SOFTMAX_CROSS_LAYER_BACKWARD_KERNEL_H__
#define __SOFTMAX_CROSS_LAYER_BACKWARD_KERNEL_H__

#include "neural_networks/layers/loss/softmax_cross_layer.h"
#include "neural_networks/layers/loss/softmax_cross_layer_types.h"
#include "neural_networks/layers/loss/softmax_cross_layer_backward_types.h"
#include "kernel.h"
#include "service_math.h"
#include "service_error_handling.h"
#include "service_tensor.h"
#include "numeric_table.h"
#include "threading.h"
#include "layers_threading.h"

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
namespace loss
{
namespace softmax_cross
{
namespace backward
{
namespace internal
{

/**
 *  \brief Kernel for softmax_cross calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class SoftmaxCrossKernel : public Kernel
{
public:
    Status compute(
        const Tensor &probTensor,
        const Tensor &groundTruthTensor,
        const softmax_cross::Parameter &parameter,
        Tensor &resultTensor);

private:
    const size_t _nRowsInBlock = 5000;
    Status processBlock(
        const Tensor &probTensor,
        const Tensor &groundTruthTensor,
        const size_t nProcessedRows,
        const size_t nRowsInCurrentBlock,
        const size_t dim,
        Tensor &gradientTensor);

};

} // namespace internal
} // namespace backward
} // namespace softmax_cross
} // namespace loss
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
