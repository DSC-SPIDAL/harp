/* file: stochastic_pooling2d_layer_forward_kernel.h */
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
//  Declaration of template function that calculate forward pooling layer results.
//--

#ifndef __STOCHASTIC_POOLING2D_LAYER_FORWARD_KERNEL_H__
#define __STOCHASTIC_POOLING2D_LAYER_FORWARD_KERNEL_H__

#include "neural_networks/layers/pooling2d/stochastic_pooling2d_layer_forward.h"
#include "neural_networks/layers/pooling2d/stochastic_pooling2d_layer_forward_types.h"
#include "pooling2d_layer_internal_parameter.h"
#include "service_blas.h"
#include "kernel.h"
#include "tensor.h"
#include "service_memory.h"
#include "service_data_utils.h"
#include "service_tensor.h"
#include "service_numeric_table.h"

using namespace daal::data_management;
using namespace daal::services;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace stochastic_pooling2d
{
namespace forward
{
namespace internal
{


/**
 *  \brief Kernel for forward pooling layer results computation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class PoolingKernel : public Kernel
{
public:
    /* Computes the results of forward batch normalization layer */
    services::Status compute(const Tensor &dataTensor, Tensor &valueTensor,
                 Tensor *selectedPosTensor, const stochastic_pooling2d::Parameter &parameter);

private:
    algorithmFPType invIntMaxVal;

    inline void computeWeightedAverage(
    const algorithmFPType *dataSlice,
    DAAL_INT f,
    DAAL_INT s,
    algorithmFPType *kernelWeights,
    pooling2d::internal::Parameter &par,
    algorithmFPType &value);

    inline void getMultivariateRandomDataValue(
    const algorithmFPType *dataSlice,
    DAAL_INT f,
    DAAL_INT s,
    algorithmFPType *weights,
    size_t nWeights,
    pooling2d::internal::Parameter &par,
    algorithmFPType &value,
    int &selectedPos);

    inline size_t getMultinomialRandomValue(algorithmFPType *weights, size_t nWeights, const int uniformRandVal);
    Status getUniformRandFrom0to1(int* uniformRand, const size_t nUniformRand, engines::EnginePtr engine);
};

} // internal
} // forward
} // stochastic_pooling2d
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
