/* file: relu_layer_backward_impl.i */
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
//  Implementation of relu algorithm
//--
*/

#ifndef __RELU_LAYER_BACKWARD_IMPL_I__
#define __RELU_LAYER_BACKWARD_IMPL_I__

#include "service_mkl_tensor.h"

using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace relu
{
namespace backward
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
services::Status ReLUKernel<algorithmFPType, method, cpu>::compute(const Tensor &inputGradientTensor, const Tensor &forwardDataTensor, Tensor &resultTensor)
{
    MklTensor<algorithmFPType> *inputGradientMklTensor = dynamic_cast<MklTensor<algorithmFPType>*>(const_cast<Tensor *>(&inputGradientTensor));
    MklTensor<algorithmFPType> *forwardDataMklTensor = dynamic_cast<MklTensor<algorithmFPType>*>(const_cast<Tensor *>(&forwardDataTensor));
    MklTensor<algorithmFPType> *resultMklTensor = dynamic_cast<MklTensor<algorithmFPType>*>(&resultTensor);

    Status s;

    if (inputGradientMklTensor != 0 && forwardDataMklTensor != 0 && resultMklTensor != 0)
    {
        dnnLayout_t inputLayout = (dnnLayout_t)forwardDataMklTensor->getDnnLayout();
        dnnLayout_t inputGradLayout = (dnnLayout_t)inputGradientMklTensor->getDnnLayout();
        dnnLayout_t resultLayout;
        dnnError_t err;

        if (reluPrim == NULL)
        {
            err = dnn::xReLUCreateBackward( &reluPrim, inputGradLayout, inputLayout, (algorithmFPType)0.0); ON_ERR(err);
        }

        err = dnn::xLayoutCreateFromPrimitive(&resultLayout, reluPrim, dnnResourceDiffSrc); ON_ERR(err);
        resultMklTensor->setDnnLayout(resultLayout);

        algorithmFPType* reluRes[dnnResourceNumber] = {0};

        reluRes[dnnResourceDiffDst] = inputGradientMklTensor->getDnnArray();
        reluRes[dnnResourceDiffSrc] = resultMklTensor->getDnnArray();
        reluRes[dnnResourceSrc] = forwardDataMklTensor->getDnnArray();

        err = dnn::xExecute(reluPrim, (void**)reluRes); ON_ERR(err);
    }
    else
    {
        __DAAL_MAKE_TENSOR_THREADSAFE(const_cast<Tensor *>(&forwardDataTensor))
        __DAAL_MAKE_TENSOR_THREADSAFE(&resultTensor)

        s = computeImpl<cpu>(inputGradientTensor, [=, &inputGradientTensor, &forwardDataTensor, &resultTensor](size_t fDimN, size_t *fDims, size_t nRowsToProcess, const TensorOffsetLayout &layout) -> Status
        {
            ReadSubtensor<algorithmFPType, cpu, Tensor> inputGradientBlock(const_cast<Tensor &>(inputGradientTensor), fDimN, fDims, 0, nRowsToProcess, layout);
            DAAL_CHECK_BLOCK_STATUS(inputGradientBlock);
            const algorithmFPType *inputGradientArray = inputGradientBlock.get();

            ReadSubtensor<algorithmFPType, cpu, Tensor> forwardBlock(const_cast<Tensor &>(forwardDataTensor), fDimN, fDims, 0, nRowsToProcess, layout);
            DAAL_CHECK_BLOCK_STATUS(forwardBlock);
            const algorithmFPType *forwardDataArray = forwardBlock.get();

            WriteSubtensor<algorithmFPType, cpu, Tensor> resultBlock(resultTensor, fDimN, fDims, 0, nRowsToProcess, layout);
            DAAL_CHECK_BLOCK_STATUS(resultBlock);
            algorithmFPType *resultArray = resultBlock.get();

            algorithmFPType zero = (algorithmFPType)0;
            size_t nDataElements = inputGradientBlock.getSize();

           PRAGMA_IVDEP
           PRAGMA_VECTOR_ALWAYS
            for(size_t i = 0; i < nDataElements; i++)
            {
                if(forwardDataArray[i] > zero)
                {
                    resultArray[i] = inputGradientArray[i];
                }
                else
                {
                    resultArray[i] = zero;
                }
            }
            return Status();
        });
    }
    return s;
}

} // namespace internal
} // namespace backward
} // namespace relu
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
