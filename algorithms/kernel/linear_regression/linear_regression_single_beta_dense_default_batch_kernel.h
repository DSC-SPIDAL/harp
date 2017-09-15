/* file: linear_regression_single_beta_dense_default_batch_kernel.h */
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
//  Declaration of template class that computes linear regression quality metrics.
//--
*/

#ifndef __LINEAR_REGRESSION_SINGLE_BETA_DENSE_DEFAULT_BATCH_KERNEL_H__
#define __LINEAR_REGRESSION_SINGLE_BETA_DENSE_DEFAULT_BATCH_KERNEL_H__

#include "linear_regression_single_beta_types.h"
#include "kernel.h"
#include "numeric_table.h"
#include "algorithm_base_common.h"


namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace quality_metric
{
namespace single_beta
{
namespace internal
{

using namespace daal::data_management;
struct SingleBetaOutput
{
    NumericTable* rms;
    NumericTable* variance;
    NumericTable** betaCovariances;
    NumericTable* zScore;
    NumericTable* confidenceIntervals;
    NumericTable* inverseOfXtX;

    SingleBetaOutput(size_t nResponses);
    ~SingleBetaOutput();
};


template<Method method, typename algorithmFPType, CpuType cpu>
class SingleBetaKernel : public daal::algorithms::Kernel
{
public:
    virtual ~SingleBetaKernel() {}
    services::Status compute(const NumericTable* y, const NumericTable* z, size_t p,
        const NumericTable* betas, const NumericTable* xtx, bool bModelNe,
        algorithmFPType accuracyThreshold, algorithmFPType alpha, SingleBetaOutput& out);

protected:
    static const size_t _nRowsInBlock = 1024;
    services::Status computeTestStatistics(const NumericTable* betas, const algorithmFPType* v,
        algorithmFPType alpha, algorithmFPType accuracyThreshold, SingleBetaOutput& out);
    services::Status computeRmsVariance(const NumericTable* y, const NumericTable* z, size_t p, NumericTable* rms, NumericTable* variance);
    services::Status computeInverseXtX(const NumericTable* xtx, bool bModelNe, NumericTable* xtxInv);
};

}
}
}
}
}
}

#endif
