/* file: split_layer_forward_impl.i */
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
//  Implementation of split algorithm
//--
*/

#ifndef __SPLIT_LAYER_FORWARD_IMPL_I__
#define __SPLIT_LAYER_FORWARD_IMPL_I__

#include "service_blas.h"
#include "threading.h"

#include "service_mkl_tensor.h"
#include "service_error_handling.h"

using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace split
{
namespace forward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status SplitKernel<algorithmFPType, method, cpu>::compute(Tensor *inputTensor, Tensor *resultTensors[], size_t nOutputs)
{
    MklTensor<algorithmFPType> *inputMklTensor = dynamic_cast<MklTensor<algorithmFPType>*>(inputTensor);

    const services::Collection<size_t> &dims = inputTensor->getDimensions();
    const size_t nInputRows = dims[0];

    size_t nBlocks = nInputRows / _nRowsInBlock;
    nBlocks += (nBlocks * _nRowsInBlock != nInputRows);

    SafeStatus safeStat;
    for(int i = 0; i < nOutputs; i++)
    {
        Tensor *resultTensor = resultTensors[i];
        MklTensor<algorithmFPType> *resultMklTensor = dynamic_cast<MklTensor<algorithmFPType>*>(resultTensor);

        if (resultTensor != inputTensor)
        {
            if (inputMklTensor != 0 && resultMklTensor != 0)
            {
                size_t dstChannelSize[1] = {dims[1]};
                dnnPrimitive_t splitPrim;
                dnnError_t err;

                dnnLayout_t inputLayout = (dnnLayout_t)inputMklTensor->getDnnLayout();
                err = dnn::xSplitCreate(&splitPrim, 1, inputLayout, dstChannelSize); ON_ERR(err);

                dnnLayout_t resultLayout;
                err = dnn::xLayoutCreateFromPrimitive(&resultLayout, splitPrim, dnnResourceSrc); ON_ERR(err);

                resultMklTensor->setDnnLayout(resultLayout);

                algorithmFPType *inputArray = inputMklTensor->getDnnArray();
                algorithmFPType *resultArray = resultMklTensor->getDnnArray();

                size_t inputSize = dnn::xLayoutGetMemorySize(inputLayout);
                services::daal_memcpy_s(resultArray, inputSize, inputArray, inputSize);

                dnn::xDelete(splitPrim);
            }
            else
            {
                __DAAL_MAKE_TENSOR_THREADSAFE(inputTensor)
                __DAAL_MAKE_TENSOR_THREADSAFE(resultTensor)

                daal::threader_for(nBlocks, nBlocks, [ =,&safeStat ](int block)
                {
                    size_t nRowsToProcess = _nRowsInBlock;
                    if( block == nBlocks - 1 )
                    {
                        nRowsToProcess = nInputRows - block * _nRowsInBlock;
                    }

                    safeStat |=processBlock(inputTensor, block * _nRowsInBlock, nRowsToProcess, resultTensor);
                } );
                if(!safeStat) return safeStat.detach();
            }
        }
    }
    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
inline Status SplitKernel<algorithmFPType, method, cpu>::processBlock(Tensor *inputTensor,
                                                                    size_t nProcessedRows, size_t nRowsInCurrentBlock,
                                                                    Tensor *resultTensor)
{
    ReadSubtensor<algorithmFPType, cpu, Tensor> inputSubtensor(inputTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(inputSubtensor);
    const algorithmFPType *inputArray = inputSubtensor.get();

    WriteSubtensor<algorithmFPType, cpu, Tensor> resultSubtensor(resultTensor, 0, 0, nProcessedRows, nRowsInCurrentBlock);
    DAAL_CHECK_BLOCK_STATUS(resultSubtensor);
    algorithmFPType *resultArray = resultSubtensor.get();

    const algorithmFPType zero = (algorithmFPType)0;
    const size_t nDataElements = inputSubtensor.getSize();
    for(size_t i = 0; i < nDataElements; i++)
    {
        resultArray[i] = inputArray[i];
    }

    return Status();
}

} // namespace internal
} // namespace forward
} // namespace split
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
