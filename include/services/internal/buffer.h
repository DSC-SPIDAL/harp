/* file: buffer.h */
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

#ifndef __SERVICES_INTERNAL_BUFFER_H__
#define __SERVICES_INTERNAL_BUFFER_H__

#include "services/base.h"
#include "services/buffer_view.h"

namespace daal
{
namespace services
{
namespace internal
{
/**
 * @ingroup memory
 * @{
 */

/**
 * <a name="DAAL-CLASS-SERVICES__INTERNAL__BUFFER"></a>
 * \brief  Class that provides simple memory management routines for handling blocks
 *         of continues memory, also provides automatic memory deallocation. Note this
 *         class doesn't provide functionality for objects constructions and simply allocates
 *         and deallocates memory. In case of objects consider Collection or ObjectPtrCollection
 * \tparam T Type of elements which are stored in the buffer
 */
template<typename T>
class Buffer : public Base
{
public:
    Buffer() :
        _buffer(NULL),
        _size(0) { }

    explicit Buffer(size_t size, services::Status *status = NULL)
    {
        services::Status localStatus = reallocate(size);
        services::internal::tryAssignStatusAndThrow(status, localStatus);
    }

    virtual ~Buffer()
    {
        destroy();
    }

    void destroy()
    {
        services::daal_free((void *)_buffer);
        _buffer = NULL;
        _size = 0;
    }

    services::Status reallocate(size_t size, bool copy = false)
    {
        if (_size == size)
        { return services::Status(); }

        T *buffer = (T *)services::daal_malloc( sizeof(T) * size );
        if (!buffer)
        { return services::throwIfPossible(services::ErrorMemoryAllocationFailed); }

        if (copy)
        {
            for (size_t i = 0; i < _size; i++)
            { _buffer[i] = buffer[i]; }
        }

        destroy();

        _size   = size;
        _buffer = buffer;
        return services::Status();
    }

    services::Status enlarge(size_t factor = 2, bool copy = false)
    {
        return reallocate(_size * factor, copy);
    }

    size_t size() const
    {
        return _size;
    }

    T *data() const
    {
        return _buffer;
    }

    T *offset(size_t elementsOffset) const
    {
        DAAL_ASSERT( elementsOffset <= _size );
        return _buffer + elementsOffset;
    }

    T &operator [] (size_t index)
    {
        DAAL_ASSERT( index < _size );
        return _buffer[index];
    }

    const T &operator [] (size_t index) const
    {
        DAAL_ASSERT( index < _size );
        return _buffer[index];
    }

    services::BufferView<T> view() const
    {
        return services::BufferView<T>(_buffer, _size);
    }

private:
    Buffer(const Buffer &);
    Buffer &operator = (const Buffer &);

private:
    T *_buffer;
    size_t _size;
};
/** @} */

} // namespace internal
} // namespace services
} // namespace daal

#endif
