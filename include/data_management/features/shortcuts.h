/* file: shortcuts.h */
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

#ifndef __DATA_MANAGEMENT_FEATURES_SHORTCUTS_H__
#define __DATA_MANAGEMENT_FEATURES_SHORTCUTS_H__

#include <vector>

#include "services/internal/utilities.h"
#include "data_management/features/internal/identifiers_impl.h"

namespace daal
{
namespace data_management
{
namespace features
{
/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__FEATURES__FEATUREIDFACTORY"></a>
 * \brief Auxiliary class that simplifies definition of feature ids collections
 */
class IdFactory : public Base
{
public:
    /**
     * Creates feature id factory using zero feature index
     * \param[out] status  The status object
     */
    IdFactory(services::Status *status = NULL) :
        _featureId(internal::NumericFeatureId::create(0, status)) { }

    /**
     * Creates feature id factory using feature index
     * \param[in]  index   The index of the feature
     * \param[out] status  The status object
     */
    IdFactory(int index, services::Status *status = NULL) :
        _featureId(internal::NumericFeatureId::create(index, status)) { }

    /**
     * Creates feature id factory using feature index
     * \param[in]  index   The index of the feature
     * \param[out] status  The status object
     */
    IdFactory(long index, services::Status *status = NULL) :
        _featureId(internal::NumericFeatureId::create(index, status)) { }

    /**
     * Creates feature id factory using name of the feature
     * \param[in]  name    The name of the feature
     * \param[out] status  The status object
     */
    IdFactory(const services::String &name, services::Status *status = NULL) :
        _featureId(internal::StringFeatureId::create(name, status)) { }

    /**
     * Creates feature id factory using name of the feature
     * \param[in]  name    The C-style string represents a name of the feature
     * \param[out] status  The status object
     */
    IdFactory(const char *name, services::Status *status = NULL) :
        _featureId(internal::StringFeatureId::create(name, status)) { }

    /**
     * Returns appropriate feature id created by the factory
     * \return Shared pointer to the feature id
     */
    const FeatureIdIfacePtr &get() const { return _featureId; }

private:
    FeatureIdIfacePtr _featureId;
};

/**
 * Defines list of the feature identifiers. Intended for fast feature identifiers creation
 * \param[in]  id  The factory of identifier
 * \return Shared pointer to feature identifiers collection
 */
inline FeatureIdCollectionIfacePtr list(const IdFactory &id)
{
    using internal::FeatureIdList;
    using internal::FeatureIdListPtr;

    FeatureIdListPtr l = FeatureIdList::create();
    if (l) { l->add(id.get()); }
    return l;
}

/**
 * Defines list of the feature identifiers. Intended for fast feature identifiers creation
 * \param[in]  id1  The factory of identifier
 * \param[in]  id2  The factory of identifier
 * \return Shared pointer to feature identifiers collection
 */
inline FeatureIdCollectionIfacePtr list(const IdFactory &id1,
                                        const IdFactory &id2)
{
    using internal::FeatureIdList;
    using internal::FeatureIdListPtr;

    FeatureIdListPtr l = FeatureIdList::create();
    if (l) { l->add(id1.get()); l->add(id2.get()); }
    return l;
}

/**
 * Defines list of the feature identifiers. Intended for fast feature identifiers creation
 * \param[in]  id1  The factory of identifier
 * \param[in]  id2  The factory of identifier
 * \param[in]  id3  The factory of identifier
 * \return Shared pointer to feature identifiers collection
 */
inline FeatureIdCollectionIfacePtr list(const IdFactory &id1,
                                        const IdFactory &id2,
                                        const IdFactory &id3)
{
    using internal::FeatureIdList;
    using internal::FeatureIdListPtr;

    FeatureIdListPtr l = FeatureIdList::create();
    if (l) { l->add(id1.get()); l->add(id2.get()); l->add(id3.get()); }
    return l;
}

/**
 * Defines list of the feature identifiers. Intended for fast feature identifiers creation
 * \param[in]  ids   The collection of feature identifier factories
 * \return Shared pointer to feature identifiers collection
 */
inline FeatureIdCollectionIfacePtr list(const std::vector<IdFactory> &ids)
{
    using internal::FeatureIdList;
    using internal::FeatureIdListPtr;

    FeatureIdListPtr l = FeatureIdList::create();
    for (size_t i = 0; i < ids.size(); i++)
    {
        l->add( ids[i].get() );
    }
    return l;
}

/**
 * Creates a plain range of feature ids
 * \param[in]  begin  The factory for the first feature id
 * \param[in]  end    The factory for the last feature id
 * \return Shared pointer to the collection of feature ids that
 *         contains all feature ids between the \p begin and the \p end
 */
inline FeatureIdCollectionIfacePtr range(const IdFactory &begin, const IdFactory &end)
{
    return internal::FeatureIdRange::create(begin.get(), end.get());
}

/**
 * Creates a plain range of feature ids that contains all possible features in the data set
 * \return Shared pointer to the collection of feature ids that contains all feature ids in the data set
 */
inline FeatureIdCollectionIfacePtr all()
{
    return range(0, -1);
}

/**
 * Creates a plain range of feature ids that contains all possible features in the data set.
 * This function is similar to all() but stores ids in reversed order.
 * \return Shared pointer to the collection of feature ids that contains all feature ids in revered order
 */
inline FeatureIdCollectionIfacePtr allReverse()
{
    return range(-1, 0);
}

} // namespace interface1

using interface1::IdFactory;
using interface1::list;
using interface1::range;
using interface1::all;
using interface1::allReverse;

} // namespace features
} // namespace data_management
} // namespace daal

#endif
