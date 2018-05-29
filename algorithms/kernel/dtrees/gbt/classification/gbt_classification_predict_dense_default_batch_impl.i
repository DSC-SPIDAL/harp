/* file: gbt_classification_predict_dense_default_batch_impl.i */
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
//  Common functions for gradient boosted trees classification predictions calculation
//--
*/

#ifndef __GBT_CLASSIFICATION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__
#define __GBT_CLASSIFICATION_PREDICT_DENSE_DEFAULT_BATCH_IMPL_I__

#include "algorithm.h"
#include "numeric_table.h"
#include "gbt_classification_predict_kernel.h"
#include "threading.h"
#include "daal_defines.h"
#include "gbt_classification_model_impl.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"
#include "service_memory.h"
#include "dtrees_regression_predict_dense_default_impl.i"

using namespace daal::internal;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace gbt
{
namespace classification
{
namespace prediction
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// PredictBinaryClassificationTask
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class PredictBinaryClassificationTask : public dtrees::regression::prediction::internal::PredictRegressionTaskBase<algorithmFPType, cpu>
{
public:
    typedef dtrees::regression::prediction::internal::PredictRegressionTaskBase<algorithmFPType, cpu> super;
    PredictBinaryClassificationTask(const NumericTable *x, NumericTable *y) : super(x, y){}
    services::Status run(const gbt::classification::internal::ModelImpl* m, size_t nIterations, services::HostAppIface* pHostApp)
    {
        DAAL_ASSERT(!nIterations || nIterations <= m->size());
        DAAL_CHECK_MALLOC(this->_featHelper.init(*this->_data));
        const auto nTreesTotal = (nIterations ? nIterations : m->size());
        this->_aTree.reset(nTreesTotal);
        DAAL_CHECK_MALLOC(this->_aTree.get());
        for(size_t i = 0; i < nTreesTotal; ++i)
            this->_aTree[i] = m->at(i);
        //compute raw boosted values
        auto s = super::run(pHostApp, algorithmFPType(1));
        if(!s)
            return s;
        WriteOnlyRows<algorithmFPType, cpu> resBD(this->_res, 0, 1);
        DAAL_CHECK_BLOCK_STATUS(resBD);
        const algorithmFPType label[2] = { algorithmFPType(1.), algorithmFPType(0.) };
        const auto nRows = this->_data->getNumberOfRows();
        algorithmFPType* res = resBD.get();
        //res contains raw boosted values
        for(size_t iRow = 0; iRow < nRows; ++iRow)
        {
            //probablity is a sigmoid(f) hence sign(f) can be checked
            res[iRow] = label[services::internal::SignBit<algorithmFPType, cpu>::get(res[iRow])];
        }
        return s;
    }
};

//////////////////////////////////////////////////////////////////////////////////////////
// PredictMulticlassTask
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class PredictMulticlassTask
{
public:
    typedef dtrees::internal::TreeImpRegression<> TreeType;
    typedef dtrees::prediction::internal::TileDimensions<algorithmFPType> DimType;
    typedef daal::tls<algorithmFPType *> ClassesRawBoostedTlsBase;
    class ClassesRawBoostedTls : public ClassesRawBoostedTlsBase
    {
    public:
        ClassesRawBoostedTls(size_t nClasses) : ClassesRawBoostedTlsBase([=]()-> algorithmFPType*
        {
            return service_scalable_malloc<algorithmFPType, cpu>(nClasses);
        })
        {}
        ~ClassesRawBoostedTls()
        {
            this->reduce([](algorithmFPType* ptr)-> void
            {
                if(ptr)
                    service_scalable_free<algorithmFPType, cpu>(ptr);
            });
        }
    };

    PredictMulticlassTask(const NumericTable *x, NumericTable *y) : _data(x), _res(y){}
    services::Status run(const gbt::classification::internal::ModelImpl* m, size_t nClasses, size_t nIterations,
        services::HostAppIface* pHostApp);

protected:
    void predictByTrees(algorithmFPType* res, size_t iFirstTree, size_t nTrees, size_t nClasses, const algorithmFPType* x);
    services::Status predictByAllTrees(size_t nTreesTotal, size_t nClasses, const DimType& dim);
    services::Status predictByBlocksOfTrees(services::HostAppIface* pHostApp,
        size_t nTreesTotal, size_t nClasses, const DimType& dim, algorithmFPType* aRawBoosted);
    size_t getMaxClass(const algorithmFPType* val, size_t nClasses) const
    {
        algorithmFPType maxVal = val[0];
        size_t maxIdx = 0;
        PRAGMA_VECTOR_ALWAYS
        for(size_t i = 1; i < nClasses; ++i)
        {
            if(maxVal < val[i])
            {
                maxVal = val[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

protected:
    const NumericTable* _data;
    NumericTable* _res;
    dtrees::internal::FeatureTypes _featHelper;
    TArray<const dtrees::internal::DecisionTreeTable*, cpu> _aTree;
    static const size_t s_cMaxClassesBufSize = 32;
};

//////////////////////////////////////////////////////////////////////////////////////////
// PredictKernel
//////////////////////////////////////////////////////////////////////////////////////////
template<typename algorithmFPType, prediction::Method method, CpuType cpu>
services::Status PredictKernel<algorithmFPType, method, cpu>::compute(services::HostAppIface* pHostApp,
    const NumericTable *x, const classification::Model *m, NumericTable *r, size_t nClasses, size_t nIterations)
{
    const daal::algorithms::gbt::classification::internal::ModelImpl* pModel =
        static_cast<const daal::algorithms::gbt::classification::internal::ModelImpl*>(m);
    if(nClasses == 2)
    {
        PredictBinaryClassificationTask<algorithmFPType, cpu> task(x, r);
        return task.run(pModel, nIterations, pHostApp);
    }
    PredictMulticlassTask<algorithmFPType, cpu> task(x, r);
    return task.run(pModel, nClasses, nIterations, pHostApp);
}

template <typename algorithmFPType, CpuType cpu>
services::Status PredictMulticlassTask<algorithmFPType, cpu>::run(const gbt::classification::internal::ModelImpl* m,
    size_t nClasses, size_t nIterations, services::HostAppIface* pHostApp)
{
    DAAL_ASSERT(!nIterations || nClasses*nIterations <= m->size());
    const auto nTreesTotal = (nIterations ? nIterations*nClasses : m->size());
    DAAL_CHECK_MALLOC(this->_featHelper.init(*this->_data));
    this->_aTree.reset(nTreesTotal);
    DAAL_CHECK_MALLOC(this->_aTree.get());
    for(size_t i = 0; i < nTreesTotal; ++i)
        this->_aTree[i] = m->at(i);

    const auto treeSize = _aTree[0]->getNumberOfRows()*sizeof(dtrees::internal::DecisionTreeNode);
    DimType dim(*_data, nTreesTotal, treeSize, nClasses);

    if(dim.nTreeBlocks == 1) //all fit into LL cache
        return predictByAllTrees(nTreesTotal, nClasses, dim);

    services::internal::TArrayCalloc<algorithmFPType, cpu> aRawBoosted(dim.nRowsTotal*nClasses);
    if(!aRawBoosted.get())
        return predictByAllTrees(nTreesTotal, nClasses, dim);

    return predictByBlocksOfTrees(pHostApp, nTreesTotal, nClasses, dim, aRawBoosted.get());
}

template <typename algorithmFPType, CpuType cpu>
void PredictMulticlassTask<algorithmFPType, cpu>::predictByTrees(algorithmFPType* val,
    size_t iFirstTree, size_t nTrees, size_t nClasses, const algorithmFPType* x)
{
    for(size_t iTree = iFirstTree, iLastTree = iFirstTree + nTrees; iTree < iLastTree; ++iTree)
    {
        const dtrees::internal::DecisionTreeNode* pNode =
            dtrees::prediction::internal::findNode<algorithmFPType, TreeType, cpu>(*_aTree[iTree], _featHelper, x);
        DAAL_ASSERT(pNode);
        val[iTree%nClasses] += pNode->featureValueOrResponse;
    }
}

template <typename algorithmFPType, CpuType cpu>
services::Status PredictMulticlassTask<algorithmFPType, cpu>::predictByAllTrees(size_t nTreesTotal, size_t nClasses,
    const DimType& dim)
{
    WriteOnlyRows<algorithmFPType, cpu> resBD(_res, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(resBD);

    const bool bUseTLS(nClasses > s_cMaxClassesBufSize);
    const size_t nCols(_data->getNumberOfColumns());
    ClassesRawBoostedTls lsData(nClasses);
    daal::SafeStatus safeStat;
    daal::threader_for(dim.nDataBlocks, dim.nDataBlocks, [&](size_t iBlock)
    {
        const size_t iStartRow = iBlock*dim.nRowsInBlock;
        const size_t nRowsToProcess = (iBlock == dim.nDataBlocks - 1) ? dim.nRowsTotal - iStartRow : dim.nRowsInBlock;
        ReadRows<algorithmFPType, cpu> xBD(const_cast<NumericTable*>(_data), iStartRow, nRowsToProcess);
        DAAL_CHECK_BLOCK_STATUS_THR(xBD);
        algorithmFPType* res = resBD.get() + iStartRow;
        daal::threader_for(nRowsToProcess, nRowsToProcess, [&](size_t iRow)
        {
            algorithmFPType buf[s_cMaxClassesBufSize];
            algorithmFPType* val = bUseTLS ? lsData.local() : buf;
            for(size_t i = 0; i < nClasses; ++i)
                val[i] = 0;
            predictByTrees(val, 0, nTreesTotal, nClasses, xBD.get() + iRow*nCols);
            res[iRow] = algorithmFPType(getMaxClass(val, nClasses));
        });
    });
    return safeStat.detach();
}

template <typename algorithmFPType, CpuType cpu>
services::Status PredictMulticlassTask<algorithmFPType, cpu>::predictByBlocksOfTrees(
    services::HostAppIface* pHostApp, size_t nTreesTotal,
    size_t nClasses, const DimType& dim, algorithmFPType* aRawBoosted)
{
    WriteOnlyRows<algorithmFPType, cpu> resBD(_res, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(resBD);

    const size_t nThreads = daal::threader_get_threads_number();
    daal::SafeStatus safeStat;
    services::Status s;
    HostAppHelper host(pHostApp, 100);
    for(size_t iTree = 0; iTree < nTreesTotal; iTree += dim.nTreesInBlock)
    {
        if(!s || host.isCancelled(s, 1))
            return s;
        const bool bLastGroup(nTreesTotal <= (iTree + dim.nTreesInBlock));
        const size_t nTreesToUse = (bLastGroup ? (nTreesTotal - iTree) : dim.nTreesInBlock);
        daal::threader_for(dim.nDataBlocks, dim.nDataBlocks, [&, nTreesToUse, bLastGroup](size_t iBlock)
        {
            const size_t iStartRow = iBlock*dim.nRowsInBlock;
            const size_t nRowsToProcess = (iBlock == dim.nDataBlocks - 1) ? dim.nRowsTotal - iStartRow : dim.nRowsInBlock;
            ReadRows<algorithmFPType, cpu> xBD(const_cast<NumericTable*>(_data), iStartRow, nRowsToProcess);
            DAAL_CHECK_BLOCK_STATUS_THR(xBD);
            algorithmFPType* res = resBD.get() + iStartRow;
            algorithmFPType* rawBoosted = aRawBoosted + iStartRow*nClasses;
            if(nRowsToProcess < 2 * nThreads)
            {
                for(size_t iRow = 0; iRow < nRowsToProcess; ++iRow)
                {
                    algorithmFPType* rawBoostedForTheRow = rawBoosted + iRow*nClasses;
                    predictByTrees(rawBoostedForTheRow, iTree, nTreesToUse, nClasses, xBD.get() + iRow*dim.nCols);
                    if(bLastGroup)
                        //find winning class now
                        res[iRow] = algorithmFPType(getMaxClass(rawBoostedForTheRow, nClasses));
                }
            }
            else
            {
                daal::threader_for(nRowsToProcess, nRowsToProcess, [&](size_t iRow)
                {
                    algorithmFPType* rawBoostedForTheRow = rawBoosted + iRow*nClasses;
                    predictByTrees(rawBoostedForTheRow, iTree, nTreesToUse, nClasses, xBD.get() + iRow*dim.nCols);
                    if(bLastGroup)
                        //find winning class now
                        res[iRow] = algorithmFPType(getMaxClass(rawBoostedForTheRow, nClasses));
                });
            }
        });
        s = safeStat.detach();
    }
    return s;
}

} /* namespace internal */
} /* namespace prediction */
} /* namespace classification */
} /* namespace gbt */
} /* namespace algorithms */
} /* namespace daal */

#endif
