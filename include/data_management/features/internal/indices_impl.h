/* file: indices_impl.h */
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

#ifndef __DATA_MANAGEMENT_FEATURES_INTERNAL_INDICES_IMPL_H__
#define __DATA_MANAGEMENT_FEATURES_INTERNAL_INDICES_IMPL_H__

#include <map>
#include <string>

#include "services/collection.h"
#include "services/internal/utilities.h"
#include "services/internal/error_handling_helpers.h"

#include "data_management/features/indices.h"

namespace daal
{
namespace data_management
{
namespace features
{
namespace internal
{

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__INTERNAL__FEATUREINDICESLIST"></a>
 * \brief Implementation of FeatureIndices to store a list of feature indices
 */
class FeatureIndicesList : public FeatureIndices
{
public:
    static services::SharedPtr<FeatureIndicesList> create(services::Status *status = NULL)
    {
        return services::internal::wrapSharedAndTryThrow<FeatureIndicesList>(new FeatureIndicesList(), status);
    }

    virtual size_t size() const DAAL_C11_OVERRIDE
    {
        return _indices.size();
    }

    virtual bool isPlainRange() const DAAL_C11_OVERRIDE
    {
        return false;
    }

    virtual bool areRawFeatureIndicesAvailable() const DAAL_C11_OVERRIDE
    {
        return true;
    }

    virtual FeatureIndex getFirst() const DAAL_C11_OVERRIDE
    {
        if (!size()) { return FeatureIndexTraits::invalid(); }
        return _indices[0];
    }

    virtual FeatureIndex getLast() const DAAL_C11_OVERRIDE
    {
        if (!size()) { return FeatureIndexTraits::invalid(); }
        return _indices[_indices.size() - 1];
    }

    virtual services::BufferView<FeatureIndex> getRawFeatureIndices() DAAL_C11_OVERRIDE
    {
        return services::BufferView<FeatureIndex>(_indices.data(), _indices.size());
    }

    services::Status add(FeatureIndex index)
    {
        if (index > FeatureIndexTraits::maxIndex() || index == FeatureIndexTraits::invalid())
        {
            return services::throwIfPossible(services::ErrorIncorrectDataRange);
        }

        if ( !_indices.safe_push_back(index) )
        {
            return services::throwIfPossible(services::ErrorMemoryAllocationFailed);
        }

        return services::Status();
    }

private:
    FeatureIndicesList() { }

    services::Collection<FeatureIndex> _indices;
};
typedef services::SharedPtr<FeatureIndicesList> FeatureIndicesListPtr;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__INTERNAL__FEATUREINDICESRANGE"></a>
 * \brief Implementation of FeatureIndices to store a range of feature indices
 */
class FeatureIndicesRange : public FeatureIndices
{
public:
    static services::SharedPtr<FeatureIndicesRange> create(FeatureIndex begin, FeatureIndex end,
                                                           services::Status *status = NULL)
    {
        if (begin == FeatureIndexTraits::invalid() ||
            end == FeatureIndexTraits::invalid())
        {
            services::internal::tryAssignStatusAndThrow(status, services::ErrorIncorrectIndex);
            return services::SharedPtr<FeatureIndicesRange>();
        }
        return services::internal::wrapSharedAndTryThrow<FeatureIndicesRange>(
            new FeatureIndicesRange(begin, end), status);
    }

    virtual size_t size() const DAAL_C11_OVERRIDE
    {
        return services::internal::maxValue(_begin, _end) -
               services::internal::minValue(_begin, _end) + 1;
    }

    virtual bool isPlainRange() const DAAL_C11_OVERRIDE
    {
        return true;
    }

    virtual bool areRawFeatureIndicesAvailable() const DAAL_C11_OVERRIDE
    {
        return false;
    }

    virtual FeatureIndex getFirst() const DAAL_C11_OVERRIDE
    {
        return _begin;
    }

    virtual FeatureIndex getLast() const DAAL_C11_OVERRIDE
    {
        return _end;
    }

    virtual services::BufferView<FeatureIndex> getRawFeatureIndices() DAAL_C11_OVERRIDE
    {
        return services::BufferView<FeatureIndex>();
    }

private:
    explicit FeatureIndicesRange(FeatureIndex begin, FeatureIndex end) :
        _begin(begin),
        _end(end) { }

    FeatureIndex _begin;
    FeatureIndex _end;
};
typedef services::SharedPtr<FeatureIndicesList> FeatureIndicesListPtr;

} // namespace internal
} // namespace features
} // namespace data_management
} // namespace daal

#endif
