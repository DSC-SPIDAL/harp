/* file: elu_layer_backward_kernel.h */
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
//  Declaration of template function that calculate relus.
//--


#ifndef __ELU_LAYER_BACKWARD_KERNEL_H__
#define __ELU_LAYER_BACKWARD_KERNEL_H__

#include "kernel.h"
#include "elu_common.h"
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
namespace backward
{
namespace internal
{

using namespace daal::services;
using namespace daal::data_management;

using elu::internal::BlockSizeType;
using elu::internal::ScalableTlsBuffer;

/**
 *  \brief Kernel for ELU calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class ELUKernel : public Kernel
{
private:
    ScalableTlsBuffer<algorithmFPType, cpu> _intermediateValuesTls;
    ScalableTlsBuffer<BlockSizeType, cpu> _indicesTls;

public:
    ELUKernel() : _intermediateValuesTls( elu::internal::getMaxBlockSize<algorithmFPType, cpu>() ),
                  _indicesTls( elu::internal::getMaxBlockSize<algorithmFPType, cpu>() ) { }

    Status compute(const Parameter &parameter,
                   const Tensor &inputGradientTensor,
                   const Tensor &auxDataTensor,
                   const Tensor *auxValueTensor,
                         Tensor &gradientTensor);

private:

    Status computeLayoutAgnostic(const Tensor &inputGradientTensor,
                                 const Tensor &auxDataTensor,
                                 const Tensor &auxValueTensor,
                                       Tensor &gradientTensor);

    Status computeInMklLayout(const Tensor &inputGradientTensor,
                              const Tensor &auxDataTensor,
                              const Tensor &auxValueTensor,
                                    Tensor &gradientTensor);

    void computeInRawLayout(const algorithmFPType *inputGradient,
                            const algorithmFPType *auxData,
                            const algorithmFPType *auxValue,
                                  algorithmFPType *gradient,
                                  size_t dataSize);

    void computeBlock(const algorithmFPType *inputGradient,
                      const algorithmFPType *auxData,
                      const algorithmFPType *auxValue,
                            algorithmFPType *gradient,
                            size_t blockSize);

    Status computeWithoutAuxValues(const Tensor &inputGradientTensor,
                                   const Tensor &auxDataTensor,
                                         Tensor &gradientTensor,
                                         algorithmFPType alpha);

    void computeBlockWithoutAuxValues(const algorithmFPType *inputGradient,
                                      const algorithmFPType *auxData,
                                            algorithmFPType *gradient,
                                            algorithmFPType alpha,
                                            size_t blockSize);

};

} // internal
} // backward
} // elu
} // layers
} // neural_networks
} // algorithms
} // daal

#endif
