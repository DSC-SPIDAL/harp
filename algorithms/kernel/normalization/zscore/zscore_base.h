/* file: zscore_base.h */
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
//  Declaration of template function that calculates zscore normalization.
//--

#ifndef __ZSCORE_BASE_H__
#define __ZSCORE_BASE_H__

#include "normalization/zscore.h"
#include "kernel.h"
#include "numeric_table.h"
#include "service_math.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"
#include "threading.h"

using namespace daal::data_management;
using namespace daal::services;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace normalization
{
namespace zscore
{
namespace internal
{
/**
 *  \brief Kernel for zscore normalization calculation
 *  in case floating point type of intermediate calculations
 *  and method of calculations are different
 */
template<typename algorithmFPType, CpuType cpu>
class ZScoreKernelBase : public Kernel
{
public:

    /**
     *  \brief Function that computes z-score normalization
     *
     *  \param inputTable[in]    Input data of the algorithm
     *  \param resultTable[out]  Table that stores algotithm's results
     *  \param parameter[in]     Parameters of the algorithm
     */
    Status compute(NumericTablePtr &inputTable, NumericTable &resultTable, const daal::algorithms::Parameter &parameter);

    virtual Status computeMeanVariance_thr(NumericTablePtr &inputTable, algorithmFPType* resultMean,
                                           algorithmFPType* resultVariance, const daal::algorithms::Parameter &parameter) = 0;

};

template <typename algorithmFPType, Method method, CpuType cpu>
class ZScoreKernel : public ZScoreKernelBase<algorithmFPType, cpu>
{};

} // namespace daal::internal
} // namespace zscore
} // namespace normalization
} // namespace algorithms
} // namespace daal

#include "zscore_impl.i"

#endif
