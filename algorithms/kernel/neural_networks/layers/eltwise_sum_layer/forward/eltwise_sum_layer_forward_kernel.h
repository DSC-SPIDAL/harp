/* file: eltwise_sum_layer_forward_kernel.h */
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
//  Declaration of template function that calculate element-wise sum.
//--


#ifndef __ELTWISE_SUM_LAYER_FORWARD_KERNEL_H__
#define __ELTWISE_SUM_LAYER_FORWARD_KERNEL_H__

#include "neural_networks/layers/eltwise_sum/eltwise_sum_layer.h"
#include "neural_networks/layers/eltwise_sum/eltwise_sum_layer_types.h"

#include "kernel.h"
#include "layers_threading.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"

using namespace daal::services;
using namespace daal::data_management;
using namespace daal::algorithms::neural_networks::layers::internal;

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
namespace forward
{
namespace internal
{
/**
 *  \brief Kernel for element-wise sum calculation
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class EltwiseSumKernel : public Kernel
{
public:
    services::Status compute(Tensor **inputs, Tensor *value, Tensor *coefficients,
        Tensor *auxCoefficients, NumericTable *numberOfCoefficients, size_t nInputs);

private:
    services::Status computeGeneric(Tensor **inputs, Tensor *value,
        const algorithmFPType *coefficients, size_t nInputs);

    services::Status makeResultForBackward(Tensor *coefficients, Tensor *auxCoefficients,
        NumericTable *numberOfCoefficients, size_t nInputs);
};
} // namespace internal
} // namespace forward

} // namespace eltwise_sum
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
