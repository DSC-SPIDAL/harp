/* file: dropout_layer_forward_impl.i */
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
//  Implementation of the forward dropout layer
//--
*/

#ifndef __DROPOUT_LAYER_FORWARD_IMPL_I__
#define __DROPOUT_LAYER_FORWARD_IMPL_I__

#include "service_tensor.h"

using namespace daal::internal;
using namespace daal::services;
using namespace daal::algorithms::distributions::bernoulli::internal;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace dropout
{
namespace forward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
Status DropoutKernel<algorithmFPType, method, cpu>::initialize(const dropout::Parameter &parameter)
{
    _engine = parameter.engine.get();
    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
Status DropoutKernel<algorithmFPType, method, cpu>::reset()
{
    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
Status DropoutKernel<algorithmFPType, method, cpu>::compute(
    const Tensor &inputTensor,
    Tensor &resultTensor,
    Tensor *maskTensor,
    const dropout::Parameter &parameter)
{
    if (parameter.predictionStage && &inputTensor == &resultTensor) { return Status(); }

    algorithmFPType inverseRetainRatio = (algorithmFPType) 1.0 / parameter.retainRatio;
    const size_t nInputRows = inputTensor.getDimensionSize(0);

    const size_t nBlocks = nInputRows / _nRowsInBlock;
    const size_t nRowsInLastBlock = nInputRows - nBlocks * _nRowsInBlock;

    const size_t nElementsInRow = inputTensor.getSize() / nInputRows;
    const size_t nRowsInBuffer = (nBlocks > 0) ? _nRowsInBlock : nRowsInLastBlock;

    TArray<int, cpu> rngBuffer(nElementsInRow * nRowsInBuffer);
    DAAL_CHECK_MALLOC(rngBuffer.get())

    Status s;
    if (parameter.predictionStage == false)
    {
        for (size_t block = 0; block < nBlocks; block++)
        {
            s |= processBlock(inputTensor, block * _nRowsInBlock, _nRowsInBlock,
                              resultTensor, maskTensor, rngBuffer.get(), inverseRetainRatio);
        }
        if (nRowsInLastBlock > 0)
        {
            s |= processBlock(inputTensor, nBlocks * _nRowsInBlock, nRowsInLastBlock,
                              resultTensor, maskTensor, rngBuffer.get(), inverseRetainRatio);
        }
    }
    else
    {
        for (size_t block = 0; block < nBlocks; block++)
        {
            s |= processBlockPrediction(inputTensor, block * _nRowsInBlock, _nRowsInBlock, resultTensor);
        }
        if (nRowsInLastBlock > 0)
        {
            s |= processBlockPrediction(inputTensor, nBlocks * _nRowsInBlock, nRowsInLastBlock, resultTensor);
        }
    }
    return s;
}

template<typename algorithmFPType, Method method, CpuType cpu>
inline Status DropoutKernel<algorithmFPType, method, cpu>::processBlock(
    const Tensor &inputTensor,
    const size_t nProcessedRows,
    const size_t nRowsInCurrentBlock,
    Tensor &resultTensor,
    Tensor *maskTensor,
    int *rngBuffer,
    const algorithmFPType inverseRetainRatio)
{
    ReadSubtensor<algorithmFPType, cpu> inputSubtensor(const_cast<Tensor &>(inputTensor), 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(inputSubtensor);
    const algorithmFPType *inputArray = inputSubtensor.get();

    WriteOnlySubtensor<algorithmFPType, cpu> resultSubtensor(resultTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(resultSubtensor);
    algorithmFPType *resultArray = resultSubtensor.get();

    WriteOnlySubtensor<algorithmFPType, cpu> maskSubtensor(*maskTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(maskSubtensor);
    algorithmFPType *maskArray = maskSubtensor.get();

    const size_t nDataElements = inputSubtensor.getSize();
    Status s;
    DAAL_CHECK_STATUS(s, (BernoulliKernelDefault<algorithmFPType, cpu>::computeInt(rngBuffer, nDataElements, _retainRatio, *_engine)));

    for (size_t i = 0; i < nDataElements; i++)
    {
        maskArray[i] = rngBuffer[i] * inverseRetainRatio;
        resultArray[i] = inputArray[i] * maskArray[i];
    }
    return s;
}

template<typename algorithmFPType, Method method, CpuType cpu>
inline Status DropoutKernel<algorithmFPType, method, cpu>::processBlockPrediction(
    const Tensor &inputTensor,
    const size_t nProcessedRows,
    const size_t nRowsInCurrentBlock,
    Tensor &resultTensor)
{
    ReadSubtensor<algorithmFPType, cpu> inputSubtensor(const_cast<Tensor &>(inputTensor), 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(inputSubtensor);
    const algorithmFPType *inputArray = inputSubtensor.get();

    WriteOnlySubtensor<algorithmFPType, cpu> resultSubtensor(resultTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(resultSubtensor);
    algorithmFPType *resultArray = resultSubtensor.get();

    const size_t nDataElements = inputSubtensor.getSize();
    for (size_t i = 0; i < nDataElements; i++)
    {
        resultArray[i] = inputArray[i];
    }
    return Status();
}

} // internal
} // forward
} // namespace dropout
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
