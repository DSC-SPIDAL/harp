/* file: df_feature_type_helper.i */
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
//  Cpu-dependent initialization of service data structure
//--
*/
#include "df_feature_type_helper.h"
#include "threading.h"
#include "service_error_handling.h"
#include "service_sort.h"

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace internal
{

template <typename IndexType, typename algorithmFPType, CpuType cpu>
struct ColIndexTask
{
    DAAL_NEW_DELETE();
    ColIndexTask(size_t nRows) : _index(nRows), maxNumDiffValues(0){}
    bool isValid() const { return _index.get(); }

    struct FeatureIdx
    {
        algorithmFPType key;
        IndexType val;
        static int compare(const void *a, const void *b)
        {
            if(static_cast<const FeatureIdx*>(a)->key < static_cast<const FeatureIdx*>(b)->key)
                return -1;
            return static_cast<const FeatureIdx*>(a)->key > static_cast<const FeatureIdx*>(b)->key;
        }
    };

    services::Status makeIndex(NumericTable& nt, IndexType* aRes, size_t iCol, size_t nRows)
    {
        const algorithmFPType* pBlock = _block.set(&nt, iCol, 0, nRows);
        DAAL_CHECK_BLOCK_STATUS(_block);
        FeatureIdx* index = _index.get();
        for(size_t i = 0; i < nRows; ++i)
        {
            index[i].key = pBlock[i];
            index[i].val = i;
        }
        daal::algorithms::internal::qSort<FeatureIdx, cpu>(nRows, index, FeatureIdx::compare);
        IndexType& nDiffValues = aRes[0];
        ++aRes;
        size_t iUnique = 0;
        aRes[index[0].val] = iUnique;
        algorithmFPType prev = index[0].key;
        for(size_t i = 1; i < nRows; ++i)
        {
            const IndexType idx = index[i].val;
            if(index[i].key == prev)
                aRes[idx] = iUnique;
            else
            {
                aRes[idx] = ++iUnique;
                prev = index[i].key;
            }
        }
        ++iUnique;
        nDiffValues = iUnique;
        if(maxNumDiffValues < iUnique)
            maxNumDiffValues = iUnique;
        return services::Status();
    }

public:
    size_t maxNumDiffValues;

protected:
    daal::internal::ReadColumns<algorithmFPType, cpu> _block;
    TVector<FeatureIdx, cpu, DefaultAllocator> _index;
};

template <typename algorithmFPType, CpuType cpu>
services::Status SortedFeaturesHelper::init(const NumericTable& nt)
{
    _maxNumDiffValues = 0;
    services::Status s = alloc(nt.getNumberOfColumns(), nt.getNumberOfRows());
    if(!s)
        return s;
    const size_t nC = nt.getNumberOfColumns();
    const size_t nR = nt.getNumberOfRows();
    typedef ColIndexTask<IndexType, algorithmFPType, cpu> TlsTask;
    daal::tls<TlsTask*> tlsData([=, &nt]()->TlsTask*
    {
        TlsTask* res = new TlsTask(nt.getNumberOfRows());
        if(res && !res->isValid())
        {
            delete res;
            res = nullptr;
        }
        return res;
    });

    SafeStatus safeStat;
    daal::threader_for(nC, nC, [&](size_t iCol)
    {
        //in case of single thread no need to allocate
        TlsTask* task = tlsData.local();
        DAAL_CHECK_THR(task, services::ErrorMemoryAllocationFailed);
        safeStat |= task->makeIndex(const_cast<NumericTable&>(nt), _data + iCol*(nRows() + 1), iCol, nRows());
    });
    tlsData.reduce([&](TlsTask* task)-> void
    {
        if(_maxNumDiffValues < task->maxNumDiffValues)
            _maxNumDiffValues = task->maxNumDiffValues;
        delete task;
    });
    return safeStat.detach();
}

} /* namespace internal */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */
