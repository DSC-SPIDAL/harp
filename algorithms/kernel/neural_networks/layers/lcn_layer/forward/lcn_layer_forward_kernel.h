/* file: lcn_layer_forward_kernel.h */
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

//++
//  Declaration of template function that calculate local contrast normalization.
//--


#ifndef __LCN_LAYER_FORWARD_KERNEL_H__
#define __LCN_LAYER_FORWARD_KERNEL_H__

#include "neural_networks/layers/lcn/lcn_layer.h"
#include "neural_networks/layers/lcn/lcn_layer_types.h"
#include "kernel.h"
#include "service_math.h"
#include "service_tensor.h"
#include "service_numeric_table.h"
#include "convolution2d_layer_forward_kernel.h"

using namespace daal::data_management;
using namespace daal::services;
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
namespace forward
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
    services::Status compute(const Tensor &inputTensor, Tensor &sigmaTensor, Tensor &cTensor, Tensor &resultTensor, Tensor &centeredDataTensor,
                                                    Tensor &invMaxTensor, const lcn::Parameter &parameter, const Tensor &kernelTensor);
    services::Status initialize(const Tensor &inputTensor, Tensor &cTensor, Tensor &invMaxTensor, const lcn::Parameter &parameter, const Tensor &kernelTensor);
    services::Status reset();
protected:
    void getConvolutionWeightsFromInputKernel(algorithmFPType *weights, const lcn::Parameter &parameter);
    services::Status calculateCenteredDataAndSigma(const lcn::Parameter &parameter, const Tensor &inputTensor);
    services::Status calculateCTensorAndGetMaxArray(Tensor &cTensor);
    void calculateResult();
private:
    size_t batchDimension;
    size_t initialFirstDim;
    size_t initialSecondDim;
    size_t initialSumDimension;
    size_t nInputRows;
    size_t nSigmaRows;
    size_t nCRows;
    size_t nKernelRows;
    const algorithmFPType *inputArray;
    algorithmFPType *resultArray;
    algorithmFPType *centeredDataArray;
    algorithmFPType *sigmaArray;
    algorithmFPType *cArray;
    algorithmFPType *invMaxArray;
    const algorithmFPType *kernelArray;
    convolution2d::Parameter convParameter;

    services::Collection<size_t> dataDims;
    services::Collection<size_t> kernelDims;
    services::Collection<size_t> weightsDims;
    services::Collection<size_t> sDims;

    size_t nSigmaElements;
    size_t nDataElements;
    size_t nKernelElements;
    size_t nWeightsElements;
    size_t dataOffsetBeforeDim;
    size_t dataOffsetAfterDim;

    size_t sumDimension;
    size_t firstDim;
    size_t secondDim;
    size_t weightsSecondDim;
    size_t dimsArray[4];
    size_t nKernels;

    convolution2d::forward::internal::Convolution2dKernel<algorithmFPType, neural_networks::layers::convolution2d::defaultDense, cpu> convKernel;
};
} // internal
} // forward

} // lcn
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
