/* file: average_pooling2d_layer_backward_kernel.h */
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
//  Declaration of template function that calculate backward pooling layer relults.
//--


#ifndef __AVERAGE_POOLING2D_LAYER_BACKWARD_KERNEL_H__
#define __AVERAGE_POOLING2D_LAYER_BACKWARD_KERNEL_H__

#include "neural_networks/layers/pooling2d/average_pooling2d_layer_backward.h"
#include "neural_networks/layers/pooling2d/average_pooling2d_layer_backward_types.h"
#include "pooling2d_layer_internal_parameter.h"
#include "tensor.h"
#include "pooling2d_layer_backward_impl.i"
#include "service_dnn.h"
#include "service_dnn_internal.h"
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
namespace average_pooling2d
{
namespace backward
{
namespace internal
{

/**
 *  \brief Kernel for backward pooling layer results computation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class PoolingKernel : public pooling2d::backward::internal::PoolingKernel<algorithmFPType, cpu>
{
public:
    services::Status compute(const Tensor &inputGradTensor,
                             const pooling2d::Parameter &parameter,
                             Tensor &gradTensor,
                             const Tensor *dataTensor);

    services::Status initialize(const services::Collection<size_t> &inDimsFull,
                                const services::Collection<size_t> &outDimsFull);

    ~PoolingKernel()
    {
        if (avePoolPrim)
        {
            dnn::xDelete(avePoolPrim);
        }
    }

protected:
    using pooling2d::backward::internal::PoolingKernel<algorithmFPType, cpu>::defaultCompute;

    virtual void defaultInnerLoop(const pooling2d::internal::Parameter &par,
                                  DAAL_INT i, DAAL_INT f, DAAL_INT k, DAAL_INT s,
                                  const algorithmFPType *inputGradPtr, const int *selectedPosPtr,
                                  algorithmFPType *grad);

private:
    typedef daal::internal::Dnn<algorithmFPType, cpu> dnn;
    typedef daal::internal::DnnLayout<algorithmFPType, cpu> xDnnLayout;

    dnnPrimitive_t avePoolPrim = NULL;

    TArray<size_t, cpu> inputSizePtr;
    size_t* inputSize = NULL;

    TArray<size_t, cpu> inputStridesPtr;
    size_t* inputStrides = NULL;

    TArray<size_t, cpu> outputSizePtr;
    size_t* outputSize = NULL;

    TArray<size_t, cpu> outputStridesPtr;
    size_t* outputStrides = NULL;

    xDnnLayout ltUserInput;
    xDnnLayout ltUserOutput;
};

} // internal
} // backward
} // average_pooling2d
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
