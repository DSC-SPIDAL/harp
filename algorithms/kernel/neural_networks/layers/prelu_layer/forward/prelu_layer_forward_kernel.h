/* file: prelu_layer_forward_kernel.h */
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

//++
//  Implementation of prelu calculation functions.
//--


#ifndef __PRELU_LAYER_FORWARD_KERNEL_H__
#define __PRELU_LAYER_FORWARD_KERNEL_H__

#include "neural_networks/layers/prelu/prelu_layer.h"
#include "neural_networks/layers/prelu/prelu_layer_types.h"
#include "kernel.h"
#include "service_tensor.h"
#include "service_numeric_table.h"
#include "threading.h"
#include "layers_threading.h"
#include "service_memory.h"

using namespace daal::data_management;
using namespace daal::services;
using namespace daal::services::internal;
using namespace daal::internal;
using namespace daal::algorithms::neural_networks::layers::internal;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace prelu
{
namespace forward
{
namespace internal
{
/**
 *  \brief Kernel for prelu calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class PReLUKernel : public Kernel
{
public:
    services::Status compute(const Tensor &inputTensor, const Tensor &wTensor, Tensor &resultTensor, const prelu::Parameter &parameter);
private:
    void getNumberOfFixedDimensions(TensorOffsetLayout &inputLayout, const Collection<size_t> &dims, size_t wEndDim, size_t &fDimN, size_t &wOffset, size_t minElementsNumInBlock);
    Status processBlock(const Tensor &inputTensor, Tensor &resultTensor, const algorithmFPType *wArray, size_t fDimN,
                                                             size_t *fDims, const TensorOffsetLayout &layout, size_t wSize,
                                                             size_t wOffset, size_t wStart, size_t wLen, const Collection<size_t> &inDims,
                                                             const Collection<size_t> &wOffsets);

    size_t _nElemsInBlock = 1000;
};
} // internal
} // forward
} // prelu
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
