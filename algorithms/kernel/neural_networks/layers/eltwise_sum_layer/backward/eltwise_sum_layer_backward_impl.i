/* file: eltwise_sum_layer_backward_impl.i */
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
//  Implementation of element-wise sum algorithm
//--
*/

#ifndef __ELTWISE_SUM_LAYER_BACKWARD_IMPL_I__
#define __ELTWISE_SUM_LAYER_BACKWARD_IMPL_I__

using namespace daal::internal;
using namespace daal::services;
using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace eltwise_sum
{
namespace backward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
Status EltwiseSumKernel<algorithmFPType, method, cpu>::compute(
    Tensor *inputGradient, Tensor *coefficients, Tensor **outputs, size_t nOutputs)
{
    if (checkForInPlace(inputGradient, coefficients, outputs, nOutputs))
    {
        return Status();
    }

    __DAAL_MAKE_TENSOR_THREADSAFE(inputGradient);
    for (size_t i = 0; i < nOutputs; i++)
    {
        __DAAL_MAKE_TENSOR_THREADSAFE(outputs[i]);
    }

    algorithmFPType *coefficientsArray = nullptr;
    ReadSubtensor<algorithmFPType, cpu, Tensor> coefficientsBlock;
    if (coefficients)
    {
        DAAL_ASSERT(nOutputs == coefficients->getDimensionSize(0));

        coefficientsBlock.set(*coefficients, 0, 0, 0, nOutputs);
        DAAL_CHECK_BLOCK_STATUS(coefficientsBlock);

        coefficientsArray = const_cast<algorithmFPType *>(coefficientsBlock.get());
    }

    SafeStatus safeStat;
    daal::threader_for(nOutputs, nOutputs, [ =, &safeStat ](size_t i)
    {
        safeStat |= processOutputTensor(inputGradient, coefficientsArray, outputs[i], i);
    });
    if (!safeStat) { return safeStat.detach(); }

    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
Status EltwiseSumKernel<algorithmFPType, method, cpu>::processOutputTensor(
    Tensor *inputGradient, const algorithmFPType *coefficientsArray, Tensor *output, size_t outputIndex)
{
    SafeStatus safeStat;
    computeImpl<cpu>(output, this->_errors.get(),
    [ =, &safeStat ](size_t fDimN, size_t *fDims, size_t dimensionSize, const TensorOffsetLayout &layout)
    {
        ReadSubtensor<algorithmFPType, cpu, Tensor> inputGradientBlock(*inputGradient, fDimN, fDims, 0, dimensionSize, layout);
        WriteSubtensor<algorithmFPType, cpu, Tensor> outputBlock(*output, fDimN, fDims, 0, dimensionSize, layout);

        DAAL_CHECK_BLOCK_STATUS_THR(inputGradientBlock);
        DAAL_CHECK_BLOCK_STATUS_THR(outputBlock);

        const algorithmFPType *inputGradientArray = inputGradientBlock.get();
        algorithmFPType *outputArray              = outputBlock.get();
        const size_t inputGradientBlockSize       = inputGradientBlock.getSize();
        const size_t outputBlockSize              = outputBlock.getSize();

        DAAL_ASSERT(inputGradientBlockSize == outputBlockSize);

        if (coefficientsArray)
        {
            const algorithmFPType coefficient = coefficientsArray[outputIndex];

          PRAGMA_IVDEP
          PRAGMA_VECTOR_ALWAYS
            for (size_t i = 0; i < outputBlockSize; i++)
            {
                outputArray[i] = coefficient * inputGradientArray[i];
            }
        }
        else
        {
            for (size_t i = 0; i < outputBlockSize; i++)
            {
                outputArray[i] = inputGradientArray[i];
            }
        }
    });
    if (!safeStat) { return safeStat.detach(); }

    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
bool EltwiseSumKernel<algorithmFPType, method, cpu>::checkForInPlace(
    const Tensor *inputGradient, const Tensor *coefficients, Tensor **outputs, size_t nOutputs)
{
    if (coefficients) { return false; }
    for (size_t i = 0; i < nOutputs; i++)
    {
        if (outputs[i] != inputGradient) { return false; }
    }
    return true;
}

} // internal
} // backward
} // namespace eltwise_sum
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
