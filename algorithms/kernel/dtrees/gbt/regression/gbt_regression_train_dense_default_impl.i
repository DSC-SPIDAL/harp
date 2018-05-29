/* file: gbt_regression_train_dense_default_impl.i */
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
//  Implementation of auxiliary functions for gradient boosted trees regression
//  (defaultDense) method.
//--
*/

#ifndef __GBT_REGRESSION_TRAIN_DENSE_DEFAULT_IMPL_I__
#define __GBT_REGRESSION_TRAIN_DENSE_DEFAULT_IMPL_I__

#include "gbt_regression_train_kernel.h"
#include "gbt_regression_model_impl.h"
#include "gbt_train_dense_default_impl.i"
#include "gbt_train_tree_builder.i"

using namespace daal::algorithms::dtrees::training::internal;
using namespace daal::algorithms::gbt::training::internal;

namespace daal
{
namespace algorithms
{
namespace gbt
{
namespace regression
{
namespace training
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// Squared loss function, L(y,f)=1/2([y-f(x)]^2)
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class SquaredLoss : public LossFunction<algorithmFPType, cpu>
{
public:
    virtual void getGradients(size_t n, const algorithmFPType* y, const algorithmFPType* f,
        const IndexType* sampleInd,
        algorithmFPType* gh) DAAL_C11_OVERRIDE
    {
        if(sampleInd)
        {
            PRAGMA_IVDEP
            PRAGMA_VECTOR_ALWAYS
            for(size_t i = 0; i < n; ++i)
            {
                gh[2 * i] = f[sampleInd[i]] - y[i]; //gradient
                gh[2 * i + 1] = 1; //hessian
            }
        }
        else
        {
            PRAGMA_IVDEP
            PRAGMA_VECTOR_ALWAYS
            for(size_t i = 0; i < n; ++i)
            {
                gh[2 * i] = f[i] - y[i]; //gradient
                gh[2 * i + 1] = 1; //hessian
            }
        }
    }
};

//////////////////////////////////////////////////////////////////////////////////////////
// TrainBatchTask for regression
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, gbt::regression::training::Method method, CpuType cpu>
class TrainBatchTask : public TrainBatchTaskBaseXBoost<algorithmFPType, cpu>
{
    typedef TrainBatchTaskBaseXBoost<algorithmFPType, cpu> super;
public:
    TrainBatchTask(HostAppIface* pHostApp, const NumericTable *x, const NumericTable *y,
        const gbt::training::Parameter& par,
        const dtrees::internal::FeatureTypes& featTypes,
        const dtrees::internal::IndexedFeatures* indexedFeatures,
        engines::internal::BatchBaseImpl& engine, size_t dummy) :
        super(pHostApp, x, y, par, featTypes, indexedFeatures, engine, 1),
        _builder(nullptr)
    {
        _builder = TreeBuilder<algorithmFPType, cpu>::create(*this);
    }
    ~TrainBatchTask() { delete _builder; }
    bool done() { return false; }
    virtual services::Status init() DAAL_C11_OVERRIDE
    {
        auto s = super::init();
        if(s)
            s = _builder->init();
        return s;
    }

protected:
    virtual void initLossFunc() DAAL_C11_OVERRIDE
    {
        switch(static_cast<const gbt::regression::training::Parameter&>(this->_par).loss)
        {
        case squared:
            this->_loss = new SquaredLoss<algorithmFPType, cpu>(); break;
        default:
            DAAL_ASSERT(false);
        }
    }

    virtual bool getInitialF(algorithmFPType& val) DAAL_C11_OVERRIDE
    {
        const auto py = this->_dataHelper.y();
        const size_t n = this->_dataHelper.data()->getNumberOfRows();
        const algorithmFPType div = algorithmFPType(1.) / algorithmFPType(n);
        val = algorithmFPType(0);
        for(size_t i = 0; i < n; ++i)
            val += div*py[i];
        return true;
    }

    virtual services::Status buildTrees(dtrees::internal::DecisionTreeTable** aTbl) DAAL_C11_OVERRIDE
    {
        this->_nParallelNodes.inc();
        return _builder->run(aTbl[0], 0);
    }

protected:
    TreeBuilder<algorithmFPType, cpu>* _builder;
};

//////////////////////////////////////////////////////////////////////////////////////////
// RegressionTrainBatchKernel
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, gbt::regression::training::Method method, CpuType cpu>
services::Status RegressionTrainBatchKernel<algorithmFPType, method, cpu>::compute(
    HostAppIface* pHostApp, const NumericTable *x, const NumericTable *y, gbt::regression::Model& m, Result& res, const Parameter& par,
    engines::internal::BatchBaseImpl& engine)
{
    return computeImpl<algorithmFPType, cpu,
        TrainBatchTask<algorithmFPType, method, cpu >>
        (pHostApp, x, y, *static_cast<daal::algorithms::gbt::regression::internal::ModelImpl*>(&m), par, engine, 1);
}

} /* namespace internal */
} /* namespace training */
} /* namespace regression */
} /* namespace gbt */
} /* namespace algorithms */
} /* namespace daal */

#endif
