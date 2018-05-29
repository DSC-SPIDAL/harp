/* file: elu_layer_forward_impl.i */
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
//  Implementation of forward ELU layer
//--
*/

#ifndef __ELU_LAYER_FORWARD_IMPL_I__
#define __ELU_LAYER_FORWARD_IMPL_I__

#include "service_math.h"
#include "service_tensor.h"
#include "service_mkl_tensor.h"
#include "service_numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace elu
{
namespace forward
{
namespace internal
{

using namespace daal::internal;

template<typename algorithmFPType, Method method, CpuType cpu>
Status ELUKernel<algorithmFPType, method, cpu>::compute(const Parameter &parameter,
                                                        const Tensor &dataTensor,
                                                              Tensor &valueTensor,
                                                              Tensor *auxValueTensor)
{
    const algorithmFPType alpha = parameter.alpha;
    const bool predictionStage  = parameter.predictionStage;

    /* if auxValues == nullptr we assume that layer is on prediction stage */
    Tensor *auxValues = (predictionStage) ? nullptr : auxValueTensor;

    if (elu::internal::canComputeInMklLayout<algorithmFPType, cpu>(dataTensor, valueTensor))
    {
        return computeInMKLLayout(dataTensor, valueTensor, auxValues, alpha);
    }
    else
    {
        return computeLayoutAgnostic(dataTensor, valueTensor, auxValues, alpha);
    }
}

template<typename algorithmFPType, Method method, CpuType cpu>
Status ELUKernel<algorithmFPType, method, cpu>::computeLayoutAgnostic(const Tensor &dataTensor,
                                                                            Tensor &valueTensor,
                                                                            Tensor *auxValueTensor,
                                                                            algorithmFPType alpha)
{
    ReadSubtensor<algorithmFPType, cpu> dataBlock(const_cast<Tensor &>(dataTensor));
    DAAL_CHECK_BLOCK_STATUS(dataBlock);

    WriteSubtensor<algorithmFPType, cpu> valueBlock(valueTensor);
    DAAL_CHECK_BLOCK_STATUS(valueBlock);

    if (auxValueTensor)
    {
        WriteSubtensor<algorithmFPType, cpu> auxValueBlock(auxValueTensor);
        DAAL_CHECK_BLOCK_STATUS(auxValueBlock);

        computeInRawLayout(dataBlock.get(), valueBlock.get(), auxValueBlock.get(),
                           alpha, dataTensor.getSize());
    }
    else
    {
        computeInRawLayoutPrediction(dataBlock.get(), valueBlock.get(),
                                     alpha, dataTensor.getSize());
    }

    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
Status ELUKernel<algorithmFPType, method, cpu>::computeInMKLLayout(const Tensor &dataTensor,
                                                                         Tensor &valueTensor,
                                                                         Tensor *auxValueTensor,
                                                                         algorithmFPType alpha)
{
    using MklTensorType = MklTensor<algorithmFPType>;

    /* We assume tensros can be casted and check was performed by the caller of this function */
    auto &dataMklTensor  = const_cast<MklTensorType &>(static_cast<const MklTensorType &>(dataTensor));
    auto &valueMklTensor = static_cast<MklTensorType &>(valueTensor);
    valueMklTensor.setDnnLayout(dataMklTensor.getSharedDnnLayout());

    const algorithmFPType *data = dataMklTensor.getDnnArray();
    algorithmFPType *value = valueMklTensor.getDnnArray();

    if (auxValueTensor)
    {
        WriteSubtensor<algorithmFPType, cpu> auxValueBlock(auxValueTensor);
        DAAL_CHECK_BLOCK_STATUS(auxValueBlock);

        computeInRawLayout(data, value, auxValueBlock.get(), alpha, dataTensor.getSize());
    }
    else
    {
        computeInRawLayoutPrediction(data, value, alpha, dataTensor.getSize());
    }

    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
void ELUKernel<algorithmFPType, method, cpu>::computeInRawLayout(const algorithmFPType *data,
                                                                       algorithmFPType *value,
                                                                       algorithmFPType *auxValue,
                                                                       algorithmFPType alpha,
                                                                       size_t dataSize)
{
    elu::internal::computeThreaded<algorithmFPType, cpu>(dataSize,
    [ & ](size_t offset, size_t blockSize)
    {
        computeBlock(data + offset, value + offset, auxValue + offset, alpha, blockSize);
    });
}

template<typename algorithmFPType, Method method, CpuType cpu>
void ELUKernel<algorithmFPType, method, cpu>::computeInRawLayoutPrediction(const algorithmFPType *data,
                                                                                 algorithmFPType *value,
                                                                                 algorithmFPType alpha,
                                                                                 size_t dataSize)
{
    elu::internal::computeThreaded<algorithmFPType, cpu>(dataSize,
    [ & ](size_t offset, size_t blockSize)
    {
        computeBlockPrediction(data + offset, value + offset, alpha, blockSize);
    });
}

template<typename algorithmFPType, Method method, CpuType cpu>
void ELUKernel<algorithmFPType, method, cpu>::computeBlock(const algorithmFPType *data,
                                                                 algorithmFPType *value,
                                                                 algorithmFPType *auxValue,
                                                                 algorithmFPType alpha,
                                                                 size_t blockSize)
{
    BlockSizeType expValuesSize = 0;
    BlockSizeType *indices = _indicesTls.local();

    for (BlockSizeType i = 0; i < blockSize; i++)
    {
        if (data[i] < (algorithmFPType)0.0)
        {
            indices[expValuesSize] = i;
            auxValue[expValuesSize++] = data[i];
        }

        value[i] = data[i];
    }

    if (expValuesSize)
    {
        Math<algorithmFPType, cpu>::vExp(expValuesSize, auxValue, auxValue);
    }

  PRAGMA_VECTOR_ALWAYS
    for (BlockSizeType i = 0; i < expValuesSize; i++)
    {
        auxValue[i] *= alpha;
    }

  PRAGMA_IVDEP
    for (BlockSizeType i = 0; i < expValuesSize; i++)
    {
        value[indices[i]] = auxValue[i] - alpha;
    }
}

template<typename algorithmFPType, Method method, CpuType cpu>
void ELUKernel<algorithmFPType, method, cpu>::computeBlockPrediction(const algorithmFPType *data,
                                                                           algorithmFPType *value,
                                                                           algorithmFPType alpha,
                                                                           size_t blockSize)
{
    algorithmFPType *expValues = _intermediateValuesTls.local();
    BlockSizeType *indices = _indicesTls.local();

    BlockSizeType expValuesSize = 0;
    for (BlockSizeType i = 0; i < blockSize; i++)
    {
        if (data[i] < (algorithmFPType)0.0)
        {
            indices[expValuesSize] = i;
            expValues[expValuesSize++] = data[i];
        }

        value[i] = data[i];
    }

    if (expValuesSize)
    {
        Math<algorithmFPType, cpu>::vExp(expValuesSize, expValues, expValues);
    }

  PRAGMA_VECTOR_ALWAYS
    for (BlockSizeType i = 0; i < expValuesSize; i++)
    {
        expValues[i] = expValues[i] * alpha - alpha;
    }

    for (BlockSizeType i = 0; i < expValuesSize; i++)
    {
        value[indices[i]] = expValues[i];
    }
}

} // namespace internal
} // namespace forward
} // namespace elu
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
