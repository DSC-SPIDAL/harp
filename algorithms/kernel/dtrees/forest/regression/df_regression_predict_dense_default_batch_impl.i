/* file: df_regression_predict_dense_default_batch_impl.i */
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
//  Common functions for decision forest regression predictions calculation
//--
*/

#ifndef __DF_REGRESSION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__
#define __DF_REGRESSION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__

#include "algorithm.h"
#include "numeric_table.h"
#include "df_regression_predict_dense_default_batch.h"
#include "threading.h"
#include "daal_defines.h"
#include "df_regression_model_impl.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"
#include "service_memory.h"
#include "dtrees_regression_predict_dense_default_impl.i"
#include "service_algo_utils.h"

using namespace daal::internal;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace regression
{
namespace prediction
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// PredictRegressionTask
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class PredictRegressionTask : public dtrees::regression::prediction::internal::PredictRegressionTaskBase<algorithmFPType, cpu>
{
public:
    typedef dtrees::regression::prediction::internal::PredictRegressionTaskBase<algorithmFPType, cpu> super;
    PredictRegressionTask(const NumericTable *x, NumericTable *y): super(x, y){}

    services::Status run(const decision_forest::regression::internal::ModelImpl* m, services::HostAppIface* pHostApp);
};

//////////////////////////////////////////////////////////////////////////////////////////
// PredictKernel
//////////////////////////////////////////////////////////////////////////////////////////
template<typename algorithmFPType, prediction::Method method, CpuType cpu>
services::Status PredictKernel<algorithmFPType, method, cpu>::compute(services::HostAppIface* pHostApp, const NumericTable *x,
    const regression::Model *m, NumericTable *r)
{
    const daal::algorithms::decision_forest::regression::internal::ModelImpl* pModel =
        static_cast<const daal::algorithms::decision_forest::regression::internal::ModelImpl*>(m);
    PredictRegressionTask<algorithmFPType, cpu> task(x, r);
    return task.run(pModel, pHostApp);
}

template <typename algorithmFPType, CpuType cpu>
services::Status PredictRegressionTask<algorithmFPType, cpu>::run(const decision_forest::regression::internal::ModelImpl* m,
    services::HostAppIface* pHostApp)
{
    DAAL_CHECK_MALLOC(this->_featHelper.init(*this->_data));
    const auto nTreesTotal = m->size();
    this->_aTree.reset(nTreesTotal);
    DAAL_CHECK_MALLOC(this->_aTree.get());
    for(size_t i = 0; i < nTreesTotal; ++i)
        this->_aTree[i] = m->at(i);
    const algorithmFPType div = algorithmFPType(1) / algorithmFPType(nTreesTotal);
    return super::run(pHostApp, div);
}

} /* namespace internal */
} /* namespace prediction */
} /* namespace regression */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */

#endif
