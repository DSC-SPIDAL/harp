/* file: transposed_conv2d_layer_backward_impl.i */
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
//  Implementation of transposed_conv2d algorithm
//--
*/

#include "service_dnn.h"
#include "service_dnn_internal.h"
#include "service_tensor.h"
#include "service_numeric_table.h"

#include "transposed_conv2d_layer.h"
#include "transposed_conv2d_layer_types.h"

#include "convolution2d_layer_forward.h"
#include "convolution2d_layer_forward_kernel.h"

#include "convolution2d_layer_backward.h"
#include "convolution2d_layer_backward_kernel.h"

using namespace daal::internal;
using namespace daal::services;

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
namespace backward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status TransposedConv2dKernel<algorithmFPType, method, cpu>::compute(const Tensor &inGradTensor, const Tensor &xTensor, const Tensor &wTensor,
        const transposed_conv2d::Parameter &parameter, Tensor &wDerTensor, Tensor &bDerTensor, Tensor &resultTensor)
{
    Status s;
    const size_t convNKernels = xTensor.getDimensionSize(parameter.groupDimension);

    {
        // compute gradient w.r.t. weights
        convolution2d::Parameter bwdConvParameter;
        bwdConvParameter.indices.dims[0] = parameter.indices.dims[0];
        bwdConvParameter.indices.dims[1] = parameter.indices.dims[1];
        bwdConvParameter.nGroups = parameter.nGroups;
        bwdConvParameter.strides.size[0] = parameter.strides.size[0];
        bwdConvParameter.strides.size[1] = parameter.strides.size[1];
        bwdConvParameter.groupDimension = parameter.groupDimension;
        bwdConvParameter.nKernels = convNKernels;
        bwdConvParameter.kernelSizes.size[0] = parameter.kernelSizes.size[0];
        bwdConvParameter.kernelSizes.size[1] = parameter.kernelSizes.size[1];
        bwdConvParameter.paddings.size[0] = parameter.paddings.size[0];
        bwdConvParameter.paddings.size[1] = parameter.paddings.size[1];
        bwdConvParameter.propagateGradient = false;

        convolution2d::backward::internal::Convolution2dKernel<algorithmFPType, neural_networks::layers::convolution2d::defaultDense, cpu> dconvKernel;
        DAAL_CHECK_STATUS(s, dconvKernel.initialize(false, true, false));
        DAAL_CHECK_STATUS(s, dconvKernel.compute(const_cast<Tensor *>(&xTensor), const_cast<Tensor *>(&inGradTensor), const_cast<Tensor *>(&wTensor), bwdConvParameter, &wDerTensor, NULL, NULL));
        DAAL_CHECK_STATUS(s, dconvKernel.reset());
    }

    {
        // compute gradient w.r.t. biases
        const size_t dimsArray[4] = { 0, parameter.groupDimension, parameter.indices.dims[0], parameter.indices.dims[1] };
        TensorOffsetLayout gTargetInLayout = inGradTensor.createDefaultSubtensorLayout();
        gTargetInLayout.shuffleDimensions( services::Collection<size_t>( 4, dimsArray ) );

        ReadSubtensor<algorithmFPType, cpu> inGradBlock(const_cast<Tensor &>(inGradTensor), 0, 0, 0, inGradTensor.getDimensionSize(0), gTargetInLayout);
        DAAL_CHECK_BLOCK_STATUS(inGradBlock);
        const algorithmFPType *inGradArray = inGradBlock.get();

        WriteOnlySubtensor<algorithmFPType, cpu> bDerBlock(bDerTensor, 0, 0, 0, bDerTensor.getDimensionSize(0));
        DAAL_CHECK_BLOCK_STATUS(bDerBlock);
        algorithmFPType *bDerArray = bDerBlock.get();

        const size_t batchSize = inGradTensor.getDimensionSize(0);
        const size_t nKernels = parameter.nKernels;
        const size_t channelSize = inGradTensor.getDimensionSize(2) * inGradTensor.getDimensionSize(3);
        for(size_t j = 0; j < nKernels; j++)
        {
            bDerArray[j] = 0;
        }
        for(size_t i = 0; i < batchSize; i++)
        {
            for(size_t j = 0; j < nKernels; j++)
            {
                for(size_t k = 0; k < channelSize; k++)
                {
                    bDerArray[j] += inGradArray[i * nKernels * channelSize + j * channelSize + k];
                }
            }
        }
        algorithmFPType invBatchSize = 1.0 / batchSize;
        for(size_t j = 0; j < nKernels; j++)
        {
            bDerArray[j] *= invBatchSize;
        }
    }

    if(parameter.propagateGradient) // compute gradient w.r.t. data
    {
        convolution2d::Parameter fwdConvParameter;
        fwdConvParameter.indices.dims[0] = parameter.indices.dims[0];
        fwdConvParameter.indices.dims[1] = parameter.indices.dims[1];
        fwdConvParameter.nGroups = parameter.nGroups;
        fwdConvParameter.strides.size[0] = parameter.strides.size[0];
        fwdConvParameter.strides.size[1] = parameter.strides.size[1];
        fwdConvParameter.groupDimension = parameter.groupDimension;
        fwdConvParameter.nKernels = convNKernels;
        fwdConvParameter.kernelSizes.size[0] = parameter.kernelSizes.size[0];
        fwdConvParameter.kernelSizes.size[1] = parameter.kernelSizes.size[1];
        fwdConvParameter.paddings.size[0] = parameter.paddings.size[0];
        fwdConvParameter.paddings.size[1] = parameter.paddings.size[1];

        {
            const services::Collection<size_t> &inDimsFull = inGradTensor.getDimensions();
            const services::Collection<size_t> &wDims = wTensor.getDimensions();
            const services::Collection<size_t> &outDimsFull = resultTensor.getDimensions();

            TArrayCalloc<algorithmFPType, cpu> bArray(convNKernels);
            DAAL_CHECK_MALLOC(bArray.get());
            Collection<size_t> dummyBiasDimensions;
            dummyBiasDimensions.push_back(convNKernels);
            SharedPtr<HomogenTensor<algorithmFPType> > bTensorPtr = HomogenTensor<algorithmFPType>::create(dummyBiasDimensions, bArray.get(), &s);
            HomogenTensor<algorithmFPType> bTensor = *bTensorPtr;
            DAAL_CHECK_STATUS_VAR(s);

            convolution2d::forward::internal::Convolution2dKernel<algorithmFPType, neural_networks::layers::convolution2d::defaultDense, cpu> convKernel;
            DAAL_CHECK_STATUS(s, convKernel.initialize(inDimsFull, wDims, fwdConvParameter, outDimsFull));
            DAAL_CHECK_STATUS(s, convKernel.compute(const_cast<Tensor *>(&inGradTensor), const_cast<Tensor *>(&wTensor), &bTensor, fwdConvParameter, &resultTensor));
            DAAL_CHECK_STATUS(s, convKernel.reset());
        }
    }
    return s;
}

} // internal
} // backward
} // namespace transposed_conv2d
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal
