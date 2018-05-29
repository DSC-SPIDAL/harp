/* file: lcn_layer_backward_kernel.h */
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

//++
//  Declaration of template function that calculate local contrast normalization.
//--


#ifndef __LCN_LAYER_BACKWARD_KERNEL_H__
#define __LCN_LAYER_BACKWARD_KERNEL_H__

#include "neural_networks/layers/lcn/lcn_layer.h"
#include "neural_networks/layers/lcn/lcn_layer_types.h"
#include "convolution2d_layer_backward.h"
#include "kernel.h"
#include "service_math.h"
#include "service_tensor.h"
#include "service_numeric_table.h"
#include "convolution2d_layer_backward_kernel.h"
#include "threading.h"
#include "service_memory.h"
#include "layers_threading.h"

using namespace daal::algorithms::neural_networks::layers::convolution2d::backward::internal;
using namespace daal::data_management;
using namespace daal::services;
using namespace daal::services::internal;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace lcn
{
namespace backward
{
namespace internal
{
/**
 *  \brief Kernel for lcn calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class LCNKernel : public Kernel
{
public:
    services::Status compute(const Tensor &auxCenteredDataTensor, const Tensor &auxSigmaTensor, const Tensor &auxCTensor,
                                                      const Tensor &auxInvMaxTensor, const Tensor &kernelTensor, const Tensor &inGradTensor,
                                                      Tensor &gradientTensor, const lcn::Parameter &parameter);
    services::Status initialize(const Tensor &auxCenteredDataTensor, const Tensor &auxSigmaTensor, const Tensor &auxCTensor,
                                                         const Tensor &kernelTensor, const lcn::Parameter &parameter);
    services::Status reset();

private:
    size_t nDims;
    size_t nDataRows;
    size_t nSigmaRows;
    size_t nCRows;
    size_t nKernelRows;

    services::Collection<size_t> dataDims;
    services::Collection<size_t> kernelDims;
    Collection<size_t> sigmaDims;

    size_t nDataElements;
    size_t nKernelElements;
    size_t nWeightsElements;
    size_t nCElements;

    size_t dataOffsetBeforeDim;
    size_t dataOffsetAfterDim;

    size_t batchDimension;
    size_t sumDimension;
    size_t firstDim;
    size_t secondDim;

    size_t initialFirstDim;
    size_t initialSecondDim;
    size_t initialSumDimension;
    size_t fDimN;

    double sigmaThreshold;

    convolution2d::Parameter convParameter;

    void getFixedDimsIndexes(size_t *fDims, size_t i);
};

} // internal
} // backward
} // lcn
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
