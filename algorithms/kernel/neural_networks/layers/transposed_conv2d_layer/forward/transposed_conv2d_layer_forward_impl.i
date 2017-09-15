/* file: transposed_conv2d_layer_forward_impl.i */
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

/*
//++
//  Implementation of transposed_conv2d algorithm
//--
*/

#include "service_tensor.h"
#include "service_numeric_table.h"
#include "convolution2d_layer_backward.h"
#include "convolution2d_layer_backward_kernel.h"

using namespace daal::internal;
using namespace daal::services;
using namespace daal::algorithms::neural_networks::layers::convolution2d::backward::internal;


namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace transposed_conv2d
{
namespace forward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status TransposedConv2dKernel<algorithmFPType, method, cpu>::compute(const Tensor &inputTensor, const Tensor &wTensor,
        const Tensor &bTensor, const transposed_conv2d::Parameter &parameter, Tensor &resultTensor)
{
    Status s;
    convolution2d::Parameter bwdConvParameter;
    bwdConvParameter.indices.dims[0] = parameter.indices.dims[0];
    bwdConvParameter.indices.dims[1] = parameter.indices.dims[1];
    bwdConvParameter.nGroups = parameter.nGroups;
    bwdConvParameter.strides.size[0] = parameter.strides.size[0];
    bwdConvParameter.strides.size[1] = parameter.strides.size[1];
    bwdConvParameter.groupDimension = parameter.groupDimension;
    bwdConvParameter.nKernels = inputTensor.getDimensionSize(parameter.groupDimension);
    bwdConvParameter.kernelSizes.size[0] = parameter.kernelSizes.size[0];
    bwdConvParameter.kernelSizes.size[1] = parameter.kernelSizes.size[1];
    bwdConvParameter.paddings.size[0] = parameter.paddings.size[0];
    bwdConvParameter.paddings.size[1] = parameter.paddings.size[1];

    {
        Convolution2dKernel<algorithmFPType, neural_networks::layers::convolution2d::defaultDense, cpu> dconvKernel;
        DAAL_CHECK_STATUS(s, dconvKernel.initialize(true, false, false));
        DAAL_CHECK_STATUS(s, dconvKernel.compute(const_cast<Tensor *>(&inputTensor), NULL, const_cast<Tensor *>(&wTensor), bwdConvParameter, NULL, NULL, &resultTensor));
        DAAL_CHECK_STATUS(s, dconvKernel.reset());
    }

    ReadSubtensor<algorithmFPType, cpu> bBlock(const_cast<Tensor &>(bTensor), 0, 0, 0, bTensor.getDimensionSize(0));
    DAAL_CHECK_BLOCK_STATUS(bBlock);
    const algorithmFPType *bArray = bBlock.get();

    WriteSubtensor<algorithmFPType, cpu> resultBlock(resultTensor, 0, 0, 0, resultTensor.getDimensionSize(0));
    DAAL_CHECK_BLOCK_STATUS(resultBlock);
    algorithmFPType *resultArray = resultBlock.get();

    const size_t batchSize = resultTensor.getDimensionSize(0);
    const size_t nKernels = resultTensor.getDimensionSize(1);
    const size_t channelSize = resultTensor.getDimensionSize(2) * resultTensor.getDimensionSize(3);
    for(size_t i = 0; i < batchSize; i++)
    {
        for(size_t j = 0; j < nKernels; j++)
        {
            for(size_t k = 0; k < channelSize; k++)
            {
                resultArray[i * nKernels * channelSize + j * channelSize + k] += bArray[j];
            }
        }
    }
    return s;
}


} // internal
} // forward
} // namespace transposed_conv2d
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal
