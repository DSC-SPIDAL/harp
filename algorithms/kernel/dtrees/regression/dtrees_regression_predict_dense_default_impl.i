/* file: dtrees_regression_predict_dense_default_impl.i */
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
//  Implementation of auxiliary functions for dtrees regression predict algorithms
//  (defaultDense) method.
//--
*/

#ifndef __DTREES_REGRESSION_PREDICT_DENSE_DEFAULT_IMPL_I__
#define __DTREES_REGRESSION_PREDICT_DENSE_DEFAULT_IMPL_I__

#include "dtrees_model_impl.h"
#include "service_data_utils.h"
#include "dtrees_feature_type_helper.h"
#include "service_environment.h"
#include "dtrees_predict_dense_default_impl.i"
#include "service_algo_utils.h"

using namespace daal::internal;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace dtrees
{
namespace regression
{
namespace prediction
{
namespace internal
{

using namespace dtrees::internal;
//////////////////////////////////////////////////////////////////////////////////////////
// PredictRegressionTaskBase
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class PredictRegressionTaskBase
{
public:
    typedef dtrees::internal::TreeImpRegression<> TreeType;
    PredictRegressionTaskBase(const NumericTable *x, NumericTable *y) : _data(x), _res(y){}

protected:
    static algorithmFPType predict(const dtrees::internal::DecisionTreeTable& t,
        const dtrees::internal::FeatureTypes& featTypes, const algorithmFPType* x)
    {
        const typename dtrees::internal::DecisionTreeNode* pNode =
            dtrees::prediction::internal::findNode<algorithmFPType, TreeType, cpu>(t, featTypes, x);
        DAAL_ASSERT(pNode);
        return pNode ? pNode->featureValueOrResponse : 0.;
    }

    algorithmFPType predictByTrees(size_t iFirstTree, size_t nTrees, const algorithmFPType* x)
    {
        algorithmFPType val = 0;
        for(size_t iTree = iFirstTree, iLastTree = iFirstTree + nTrees; iTree < iLastTree; ++iTree)
            val += predict(*_aTree[iTree], _featHelper, x);
        return val;
    }
    services::Status run(services::HostAppIface* pHostApp, algorithmFPType factor);

protected:
    dtrees::internal::FeatureTypes _featHelper;
    TArray<const dtrees::internal::DecisionTreeTable*, cpu> _aTree;
    const NumericTable* _data;
    NumericTable* _res;
};

template <typename algorithmFPType, CpuType cpu>
services::Status PredictRegressionTaskBase<algorithmFPType, cpu>::run(services::HostAppIface* pHostApp, algorithmFPType factor)
{
    const auto nTreesTotal = _aTree.size();
    const auto treeSize = _aTree[0]->getNumberOfRows()*sizeof(dtrees::internal::DecisionTreeNode);

    dtrees::prediction::internal::TileDimensions<algorithmFPType> dim(*_data, nTreesTotal, treeSize);
    WriteOnlyRows<algorithmFPType, cpu> resBD(_res, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(resBD);
    services::internal::service_memset<algorithmFPType, cpu>(resBD.get(), 0, dim.nRowsTotal);
    const size_t nThreads = daal::threader_get_threads_number();
    SafeStatus safeStat;
    services::Status s;
    HostAppHelper host(pHostApp, 100);
    for(size_t iTree = 0; iTree < nTreesTotal; iTree += dim.nTreesInBlock)
    {
        if(!s || host.isCancelled(s, 1))
            return s;
        size_t nTreesToUse = ((iTree + dim.nTreesInBlock) < nTreesTotal ? dim.nTreesInBlock : (nTreesTotal - iTree));
        daal::threader_for(dim.nDataBlocks, dim.nDataBlocks, [&](size_t iBlock)
        {
            const size_t iStartRow = iBlock*dim.nRowsInBlock;
            const size_t nRowsToProcess = (iBlock == dim.nDataBlocks - 1) ? dim.nRowsTotal - iBlock * dim.nRowsInBlock : dim.nRowsInBlock;
            ReadRows<algorithmFPType, cpu> xBD(const_cast<NumericTable*>(_data), iStartRow, nRowsToProcess);
            DAAL_CHECK_BLOCK_STATUS_THR(xBD);
            algorithmFPType* res = resBD.get() + iStartRow;
            if(nRowsToProcess < 2 * nThreads)
            {
                for(size_t iRow = 0; iRow < nRowsToProcess; ++iRow)
                    res[iRow] += factor*predictByTrees(iTree, nTreesToUse, xBD.get() + iRow*dim.nCols);
            }
            else
            {
                daal::threader_for(nRowsToProcess, nRowsToProcess, [&](size_t iRow)
                {
                    res[iRow] += factor*predictByTrees(iTree, nTreesToUse, xBD.get() + iRow*dim.nCols);
                });
            }
        });
        s = safeStat.detach();
    }
    return s;
}

} /* namespace internal */
} /* namespace prediction */
} /* namespace regression */
} /* namespace dtrees */
} /* namespace algorithms */
} /* namespace daal */

#endif
