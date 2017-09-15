/* file: decision_tree_impl.i */
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
//  Common functions for Decision tree
//--
*/

#ifndef __DECISION_TREE_IMPL_I__
#define __DECISION_TREE_IMPL_I__

#include "services/daal_memory.h"
#include "services/env_detect.h"
#include "numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace decision_tree
{
namespace internal
{

using namespace daal::services;

template <typename T>
class allocation
{
public:
    static DAAL_FORCEINLINE T * malloc(size_t size, size_t alignment = DAAL_MALLOC_DEFAULT_ALIGNMENT)
    {
        return static_cast<T *>(daal_malloc(size * sizeof(T), alignment));
    }
};

template <>
class allocation<void>
{
public:
    static DAAL_FORCEINLINE void * malloc(size_t size, size_t alignment = DAAL_MALLOC_DEFAULT_ALIGNMENT)
    {
        return daal_malloc(size, alignment);
    }
};

template <typename T>
DAAL_FORCEINLINE T * daal_alloc(size_t size, size_t alignment)
{
    return allocation<T>::malloc(size, alignment);
}

template <typename T>
DAAL_FORCEINLINE T * daal_alloc(size_t size)
{
    return allocation<T>::malloc(size);
}

typedef size_t FeatureIndex;

class FeatureTypesCache
{
public:
    FeatureTypesCache(const data_management::NumericTable & table)
        : _size(table.getNumberOfColumns()),
          _types(daal_alloc<data_management::data_feature_utils::FeatureType>(_size))
    {
        for (FeatureIndex i = 0; i < _size; ++i)
        {
            _types[i] = table.getFeatureType(i);
        }
    }

    ~FeatureTypesCache()
    {
        daal_free(_types);
    }

    FeatureTypesCache(const FeatureTypesCache &) = delete;
    FeatureTypesCache & operator= (const FeatureTypesCache &) = delete;

    data_management::data_feature_utils::FeatureType operator[] (FeatureIndex index) const
    {
        DAAL_ASSERT(index < _size);
        return _types[index];
    }

private:
    size_t _size;
    data_management::data_feature_utils::FeatureType * _types;
};

} // namespace internal
} // namespace decision_tree
} // namespace algorithms
} // namespace daal

#endif
