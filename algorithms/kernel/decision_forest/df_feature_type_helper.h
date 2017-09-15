/* file: df_feature_type_helper.h */
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
//  Implementation of a service class that provides optimal access to the feature types
//--
*/

#ifndef __DF_FEATURE_TYPE_HELPER_H__
#define __DF_FEATURE_TYPE_HELPER_H__

#include "service_memory.h"
#include "service_numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// FeatureTypeHelper, provides optimal access to the feature types
//////////////////////////////////////////////////////////////////////////////////////////
template <CpuType cpu>
class FeatureTypeHelper
{
public:
    FeatureTypeHelper(): _bAllUnordered(false){}
    bool init(const NumericTable* data)
    {
        size_t count = 0;
        _firstUnordered = -1;
        _lastUnordered = -1;
        const size_t p = data->getNumberOfColumns();
        for(size_t i = 0; i < p; ++i)
        {
            if(data->getFeatureType(i) != data_management::data_feature_utils::DAAL_CATEGORICAL)
                continue;
            if(_firstUnordered < 0)
                _firstUnordered = i;
            _lastUnordered = i;
            ++count;
        }
        _bAllUnordered = (p == count);
        if(_bAllUnordered)
        {
            _aCatFeatures.reset(0);
            return true;
        }
        if(!count)
            return true;
        _aCatFeatures.reset(_lastUnordered - _firstUnordered + 1);
        if(!_aCatFeatures.get())
            return false;
        for(size_t i = _firstUnordered; i < _lastUnordered + 1; ++i)
        {
            _aCatFeatures[i - _firstUnordered] = (data->getFeatureType(i) == data_management::data_feature_utils::DAAL_CATEGORICAL);
        }
        return true;
    }

    bool isUnordered(size_t iFeature) const
    {
        if(_bAllUnordered)
            return true;
        if(!_aCatFeatures.size() || iFeature < _firstUnordered)
            return false;
        const size_t i = iFeature - _firstUnordered;
        if(i < _aCatFeatures.size())
            return _aCatFeatures[i];
        DAAL_ASSERT(iFeature > _lastUnordered);
        return false;
    }

    bool hasUnorderedFeatures() const { return (_bAllUnordered || _aCatFeatures.size()); }

private:
    daal::internal::TArray<bool, cpu> _aCatFeatures;
    bool _bAllUnordered;
    int _firstUnordered;
    int _lastUnordered;
};

//////////////////////////////////////////////////////////////////////////////////////////
// SortedFeaturesHelper. Creates and stores index of every feature
// Sorts every feature and creates the mapping: features value -> index of the value
// in the sorted array of unique values of the feature in increasing order
//////////////////////////////////////////////////////////////////////////////////////////
class SortedFeaturesHelper
{
public:
    typedef size_t IndexType;
    SortedFeaturesHelper() : _data(nullptr), _nCols(0), _nRows(0), _capacity(0), _maxNumDiffValues(0){}
    ~SortedFeaturesHelper();

    template <typename algorithmFPType, CpuType cpu>
    services::Status init(const NumericTable& nt);

    IndexType getMaxNumberOfDiffValues(size_t iCol) const
    {
        return _data[iCol*(_nRows + 1)];
    }

    //get number of different values for all features
    IndexType getMaxNumberOfDiffValues() const { return _maxNumDiffValues;  }

    //for low-level optimization
    const IndexType* data(size_t iFeature) const { return _data + (_nRows + 1)*iFeature + 1; }

    size_t nRows() const { return _nRows; }
    size_t nCols() const { return _nCols; }

protected:
    services::Status alloc(size_t nCols, size_t nRows);

protected:
    IndexType* _data;
    size_t _nRows;
    size_t _nCols;
    size_t _capacity;
    size_t _maxNumDiffValues;
};

} /* namespace internal */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */

#endif
