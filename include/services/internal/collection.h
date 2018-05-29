/* file: collection.h */
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

#ifndef __SERVICES_INTERNAL_COLLECTION_H__
#define __SERVICES_INTERNAL_COLLECTION_H__

#include "services/base.h"
#include "services/collection.h"
#include "services/internal/error_handling_helpers.h"

namespace daal
{
namespace services
{
namespace internal
{
/**
 * <a name="DAAL-CLASS-SERVICES__OBJECTPTRCOLLECTION"></a>
 * \brief  Class that implements functionality of collection container and holds pointers
 *         to objects of specified type, also provides automatic objects disposal
 * \tparam T Type of objects which are stored in the container
 * \tparam Deleter Type of deleter to be called on collection disposal
 */
template<typename T, typename Deleter = ObjectDeleter<T> >
class ObjectPtrCollection : public Base
{
public:
    ObjectPtrCollection() { }

    ObjectPtrCollection(const Deleter &deleter) :
        _deleter(deleter) { }

    virtual ~ObjectPtrCollection()
    {
        for (size_t i = 0; i < _objects.size(); i++)
        { _deleter( (const void *)_objects[i] ); }
    }

    T &operator [] (size_t index) const
    {
        DAAL_ASSERT( index < _objects.size() );
        return *(_objects[index]);
    }

    size_t size() const
    {
        return _objects.size();
    }

    bool push_back(T *object)
    {
        if (!object)
        { return false; }

        return _objects.safe_push_back(object);
    }

    template<typename U>
    bool safe_push_back()
    {
        return _objects.push_back(new U());
    }

private:
    ObjectPtrCollection(const ObjectPtrCollection &);
    ObjectPtrCollection &operator = (const ObjectPtrCollection &);

private:
    Deleter _deleter;
    services::Collection<T *> _objects;
};

/**
 *  <a name="DAAL-CLASS-SERVICES__INTERNAL__HEAPALLOCATABLECOLLECTION"></a>
 *  \brief   Wrapper for services::Collection that allocates and deallocates
 *           memory using internal new/delete operators
 *  \tparam  T  Type of an object stored in the container
 */
template<typename T>
class HeapAllocatableCollection : public Base, public services::Collection<T>
{
public:
    static SharedPtr<HeapAllocatableCollection<T> > create(services::Status *status = NULL)
    {
        typedef SharedPtr<HeapAllocatableCollection<T> > PtrType;

        HeapAllocatableCollection<T> *collection = new internal::HeapAllocatableCollection<T>();
        if (!collection)
        {
            services::internal::tryAssignStatusAndThrow(status, services::ErrorMemoryAllocationFailed);
            return PtrType();
        }

        return PtrType(collection);
    }

    static SharedPtr<HeapAllocatableCollection<T> > create(size_t n, services::Status *status = NULL)
    {
        typedef SharedPtr<HeapAllocatableCollection<T> > PtrType;

        HeapAllocatableCollection<T> *collection = new internal::HeapAllocatableCollection<T>(n);
        if (!collection || !collection->data())
        {
            delete collection;
            services::internal::tryAssignStatusAndThrow(status, services::ErrorMemoryAllocationFailed);
            return PtrType();
        }

        return PtrType(collection);
    }

    HeapAllocatableCollection() { }

    explicit HeapAllocatableCollection(size_t n) :
        services::Collection<T>(n) { }
};

/**
 *  <a name="DAAL-CLASS-SERVICES__INTERNAL__COLLECTIONPTR"></a>
 *  \brief   Shared pointer to the Collection object
 *  \tparam  T  Type of an object stored in the container
 */
template<class T>
class CollectionPtr : public SharedPtr<HeapAllocatableCollection<T> >
{
private:
    typedef SharedPtr<HeapAllocatableCollection<T> > super;

public:
    CollectionPtr() { }

    template<class U>
    CollectionPtr(const SharedPtr<U> &other) : super(other) { }

    template<class U>
    explicit CollectionPtr(U *ptr) : super(ptr) { }

    template<class U, class D>
    explicit CollectionPtr(U *ptr, const D& deleter) : super(ptr, deleter) { }
};

} // namespace internal
} // namespace services
} // namespace daal

#endif
