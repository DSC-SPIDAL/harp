/* file: pooling2d_layer_internal_parameter.cpp */
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
//  Common classes for 2D pooling layers
//--
*/

#include "pooling2d_layer_internal_parameter.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace pooling2d
{
namespace internal
{

Parameter::Parameter(const size_t *indices, const size_t *padding, const size_t *stride, const size_t *kernelSize,
              const Tensor &dataTensor, const Collection<size_t> &dims, const Collection<size_t> &valueDims) :
        firstIndex(indices[0]), secondIndex(indices[1]), firstPadding(padding[0]), secondPadding(padding[1]),
        firstStride(stride[0]), secondStride(stride[1]), firstKernelSize(kernelSize[0]), secondKernelSize(kernelSize[1])
    {
        if (firstIndex > secondIndex)
        {
            swap(firstIndex,   secondIndex);
            swap(firstPadding, secondPadding);
            swap(firstStride,  secondStride);
            swap(firstKernelSize, secondKernelSize);
        }

        size_t nDims = dims.size();
        offsetBefore = (firstIndex == 0 ? 1 : dataTensor.getSize(0, firstIndex));
        firstSize = dims[firstIndex];
        firstOutSize = valueDims[firstIndex];
        offsetBetween = (firstIndex + 1 == secondIndex ? 1 : dataTensor.getSize(firstIndex + 1, secondIndex - firstIndex - 1));
        secondSize = dims[secondIndex];
        secondOutSize = valueDims[secondIndex];
        offsetAfter = (secondIndex == nDims - 1 ? 1 : dataTensor.getSize(secondIndex + 1, nDims - secondIndex - 1));
    }

}
}
}
}
}
}
