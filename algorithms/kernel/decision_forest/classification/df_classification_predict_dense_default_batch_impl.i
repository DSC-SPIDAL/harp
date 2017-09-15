/* file: df_classification_predict_dense_default_batch_impl.i */
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
//  Common functions for decision forest classification predictions calculation
//--
*/

#ifndef __DF_CLASSIFICATION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__
#define __DF_CLASSIFICATION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__

#include "algorithm.h"
#include "numeric_table.h"
#include "df_classification_predict_dense_default_batch.h"
#include "threading.h"
#include "daal_defines.h"
#include "df_classification_model_impl.h"
#include "service_numeric_table.h"
#include "service_memory.h"
#include "df_predict_dense_default_impl.i"

using namespace daal::internal;
using namespace daal::services::internal;
using namespace daal::algorithms::decision_forest::internal;

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace classification
{
namespace prediction
{
namespace internal
{

static const size_t nRowsInBlock = 500;

//////////////////////////////////////////////////////////////////////////////////////////
// PredictClassificationTask
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class PredictClassificationTask
{
public:
    typedef decision_forest::internal::TreeImpClassification<> TreeType;
    typedef TVector<ClassIndexType, cpu, ScalableAllocator<cpu>> ResponseType;

    PredictClassificationTask(const NumericTable *x, NumericTable *y, const decision_forest::internal::ModelImpl* m) :
        _data(x), _res(y), _model(m){}

    Status run(size_t nClasses);

protected:
    static void predict(const decision_forest::internal::DecisionTreeTable& t,
        const decision_forest::internal::FeatureTypeHelper<cpu>& featHelper, const algorithmFPType* x,
        ClassIndexType* val, size_t nClasses)
    {
        const decision_forest::internal::DecisionTreeNode* pNode =
            decision_forest::prediction::internal::findNode<algorithmFPType, TreeType, cpu>(t, featHelper, x);
        DAAL_ASSERT(pNode);
        val[pNode->leftIndexOrClass]++;
    }

protected:
    const NumericTable* _data;
    NumericTable* _res;
    const decision_forest::internal::ModelImpl* _model;
};

//////////////////////////////////////////////////////////////////////////////////////////
// PredictKernel
//////////////////////////////////////////////////////////////////////////////////////////
template<typename algorithmFPType, prediction::Method method, CpuType cpu>
services::Status PredictKernel<algorithmFPType, method, cpu>::compute(const NumericTable *x,
    const decision_forest::classification::Model *m, NumericTable *r, size_t nClasses)
{
    const daal::algorithms::decision_forest::classification::internal::ModelImpl* pModel =
        static_cast<const daal::algorithms::decision_forest::classification::internal::ModelImpl*>(m);
    PredictClassificationTask<algorithmFPType, cpu> task(x, r, pModel);
    return task.run(nClasses);
}

template <typename algorithmFPType, CpuType cpu>
Status PredictClassificationTask<algorithmFPType, cpu>::run(size_t nClasses)
{
    decision_forest::internal::FeatureTypeHelper<cpu> featHelper;
    DAAL_CHECK(featHelper.init(_data), services::ErrorMemoryAllocationFailed);

    const auto nRows = _data->getNumberOfRows();
    const auto nCols = _data->getNumberOfColumns();
    size_t nBlocks = nRows / nRowsInBlock;
    nBlocks += (nBlocks * nRowsInBlock != nRows);

    WriteOnlyRows<algorithmFPType, cpu> resBD(_res, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(resBD);
    daal::services::internal::service_memset<algorithmFPType, cpu>(resBD.get(), 0, nRows);

    ResponseType val(nClasses, 0);
    for(size_t iBlock = 0; iBlock < nBlocks; ++iBlock)
    {
        const size_t iStartRow = iBlock*nRowsInBlock;
        const size_t nRowsToProcess = (iBlock == nBlocks - 1) ? nRows - iBlock * nRowsInBlock : nRowsInBlock;
        ReadRows<algorithmFPType, cpu> xBD(const_cast<NumericTable*>(_data), iStartRow, nRowsToProcess);
        DAAL_CHECK_BLOCK_STATUS(xBD);
        algorithmFPType* res = resBD.get() + iStartRow;
        ResponseType val(nClasses, 0);
        for(size_t iRow = 0; iRow < nRowsToProcess; ++iRow)
        {
            const auto size = _model->size();

            daal::tls<ClassIndexType *> tlsData([=]()-> ClassIndexType*
            {
                return service_scalable_calloc<ClassIndexType, cpu>(nClasses);
            });

            for(size_t i = 0; i < nClasses; ++i)
                val[i] = 0;

            daal::threader_for(size, size, [&](size_t iTree)
            {
                predict(*_model->at(iTree), featHelper, xBD.get() + iRow*nCols, tlsData.local(), nClasses);
            });
            tlsData.reduce([=, &val](ClassIndexType* ptr)-> void
            {
                if(!ptr)
                    return;
                for(size_t i = 0; i < nClasses; ++i)
                    val[i] += ptr[i];
                service_scalable_free<ClassIndexType, cpu>(ptr);
            });

            algorithmFPType maxVal = val[0];
            size_t maxIdx = 0;
            for(size_t i = 1; i < nClasses; ++i)
            {
                if(maxVal < val[i])
                {
                    maxVal = val[i];
                    maxIdx = i;
                }
            }
            res[iRow] = algorithmFPType(maxIdx);
        }
    }
    return Status();
}

} /* namespace internal */
} /* namespace prediction */
} /* namespace classification */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */

#endif
