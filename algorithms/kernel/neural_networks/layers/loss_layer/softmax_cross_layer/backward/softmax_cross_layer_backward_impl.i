/* file: softmax_cross_layer_backward_impl.i */
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
//  Implementation of the backward softmax cross layer
//--
*/

#ifndef __SOFTMAX_CROSS_LAYER_BACKWARD_IMPL_I__
#define __SOFTMAX_CROSS_LAYER_BACKWARD_IMPL_I__

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
namespace loss
{
namespace softmax_cross
{
namespace backward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status SoftmaxCrossKernel<algorithmFPType, method, cpu>::compute(
    const Tensor &probTensor,
    const Tensor &groundTruthTensor,
    const softmax_cross::Parameter &parameter,
    Tensor &resultTensor)
{
    size_t nRows = groundTruthTensor.getDimensionSize(0);
    const size_t dim = parameter.dimension;

    size_t nBlocks = nRows / _nRowsInBlock;
    nBlocks += (nBlocks * _nRowsInBlock != nRows);

    __DAAL_MAKE_TENSOR_THREADSAFE(const_cast<Tensor *>(&probTensor))
    __DAAL_MAKE_TENSOR_THREADSAFE(const_cast<Tensor *>(&groundTruthTensor))

    SafeStatus safeStat;
    daal::threader_for(nBlocks, nBlocks, [ =, &probTensor, &groundTruthTensor, &resultTensor, &safeStat ](int block)
    {
        size_t nRowsToProcess = _nRowsInBlock;
        if( block == nBlocks - 1 )
        {
            nRowsToProcess = nRows - block * _nRowsInBlock;
        }

        DAAL_CHECK_STATUS_THR(processBlock(probTensor, groundTruthTensor, block * _nRowsInBlock, nRowsToProcess, dim, resultTensor));
    }
                      );
    return safeStat.detach();
}

template<typename algorithmFPType, Method method, CpuType cpu>
inline Status SoftmaxCrossKernel<algorithmFPType, method, cpu>::processBlock(
    const Tensor &probTensor,
    const Tensor &groundTruthTensor,
    const size_t nProcessedRows,
    const size_t nRowsInCurrentBlock,
    const size_t dim,
    Tensor &gradientTensor)
{
    WriteOnlySubtensor<algorithmFPType, cpu> gradientBlock(gradientTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(gradientBlock);
    algorithmFPType *gradientArray = gradientBlock.get();

    {
        ReadSubtensor<algorithmFPType, cpu> probBlock(const_cast<Tensor &>(probTensor), 0, 0, nProcessedRows, nRowsInCurrentBlock);
        DAAL_CHECK_BLOCK_STATUS(probBlock);
        const algorithmFPType *probArray = probBlock.get();

        size_t nDataElements = probBlock.getSize();
        for(size_t i = 0; i < nDataElements; i++)
        {
            gradientArray[i] = probArray[i];
        }
    }

    ReadSubtensor<int, cpu> groundTruthBlock(const_cast<Tensor &>(groundTruthTensor), 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(groundTruthBlock);
    const int *groundTruthArray = groundTruthBlock.get();

    const algorithmFPType one = 1.0;
    const size_t dimensionSize = probTensor.getDimensionSize(dim);
    const size_t offsetInclude = probTensor.getSize(dim, probTensor.getNumberOfDimensions() - dim);
    const size_t offsetAfter = offsetInclude / dimensionSize;
    const size_t offsetBeforeInRow = probTensor.getSize() / offsetInclude / probTensor.getDimensionSize(0);

    for(size_t j = 0; j < nRowsInCurrentBlock * offsetBeforeInRow; j++)
    {
        for(size_t k = 0; k < offsetAfter; k++)
        {
            gradientArray[(j * dimensionSize + groundTruthArray[j * offsetAfter + k])*offsetAfter + k] -= one;
        }
    }
    return Status();
}

} // internal
} // backward
} // namespace softmax_cross
} // namespace loss
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
