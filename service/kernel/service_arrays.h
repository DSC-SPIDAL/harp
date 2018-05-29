/* file: service_arrays.h */
/*******************************************************************************
* Copyright 2015-2018 Intel Corporation
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

#ifndef __SERVICE_ARRAYS_H__
#define __SERVICE_ARRAYS_H__

#include "service_allocators.h"

namespace daal
{
namespace services
{
namespace internal
{

template<typename T, typename Allocator, typename ConstructionPolicy, CpuType cpu>
class DynamicArray
{
public:
    DAAL_NEW_DELETE();

    DynamicArray() : _data(nullptr), _size(0) { }

    explicit DynamicArray(size_t size)
    {
        allocate(size);
    }

    DynamicArray(DynamicArray &&other)
    {
        moveImpl(other);
    }

    ~DynamicArray()
    {
        destroy();
    }

    DynamicArray &operator = (DynamicArray &&other)
    {
        moveImpl(other);
        return *this;
    }

    inline T &operator [] (size_t index)
    {
        return _data[index];
    }

    inline const T &operator [] (size_t index) const
    {
        return _data[index];
    }

    inline       T* get()       { return _data; }
    inline const T* get() const { return _data; }

    inline size_t size() const { return _size; }

    inline T *reset(size_t size = 0)
    {
        destroy();
        allocate(size);
        return _data;
    }

    DynamicArray(const DynamicArray &) = delete;
    DynamicArray &operator = (const DynamicArray &) = delete;

private:
    void allocate(size_t size)
    {
        _data = (size) ? Allocator::allocate(size) : nullptr;
        _size = 0;

        if (_data)
        {
            ConstructionPolicy::construct(_data, _data + size);
            _size = size;
        }
    }

    void destroy()
    {
        if (_data)
        {
            ConstructionPolicy::destroy(_data, _data + _size);
            Allocator::deallocate(_data);
        }

        _data = nullptr;
        _size = 0;
    }

    void moveImpl(DynamicArray &&other)
    {
        _data = other._data;
        _size = other._size;

        other._data = nullptr;
        other._size = 0;
    }

private:
    T *_data;
    size_t _size;
};

template<typename T, CpuType cpu, typename ConstructionPolicy = DefaultConstructionPolicy<T, cpu>>
using TArray = DynamicArray<T, DAALMalloc<T, cpu>, ConstructionPolicy, cpu>;

template<typename T, CpuType cpu, typename ConstructionPolicy = DefaultConstructionPolicy<T, cpu>>
using TArrayCalloc = DynamicArray<T, DAALCalloc<T, cpu>, ConstructionPolicy, cpu>;

template<typename T, CpuType cpu, typename ConstructionPolicy = DefaultConstructionPolicy<T, cpu>>
using TArrayScalable = DynamicArray<T, ScalableMalloc<T, cpu>, ConstructionPolicy, cpu>;

template<typename T, CpuType cpu, typename ConstructionPolicy = DefaultConstructionPolicy<T, cpu>>
using TArrayScalableCalloc = DynamicArray<T, ScalableCalloc<T, cpu>, ConstructionPolicy, cpu>;


template<typename T, size_t staticBufferSize, typename Allocator, typename ConstructionPolicy, CpuType cpu>
class StaticallyBufferedDynamicArray
{
public:
    StaticallyBufferedDynamicArray() : _data(nullptr), _size(0) { }

    explicit StaticallyBufferedDynamicArray(size_t size)
    {
        allocate(size);
    }

    ~StaticallyBufferedDynamicArray() { destroy(); }

    StaticallyBufferedDynamicArray(const StaticallyBufferedDynamicArray&) = delete;
    StaticallyBufferedDynamicArray &operator = (const StaticallyBufferedDynamicArray &) = delete;

    inline       T* get()       { return _data; }
    inline const T* get() const { return _data; }

    inline size_t size() const { return _size; }

    inline T *reset(size_t size = 0)
    {
        destroy();
        allocate(size);
        return _data;
    }

    inline T &operator [] (size_t index)
    {
        return _data[index];
    }

    inline const T &operator [] (size_t index) const
    {
        return _data[index];
    }

private:
    void allocate(size_t size)
    {
        _size = 0;

        if (size <= staticBufferSize)
        {
            _data = _buffer;
        }
        else
        {
            _data = (size) ? Allocator::allocate(size) : nullptr;
        }

        if (_data && size)
        {
            ConstructionPolicy::construct(_data, _data + size);
            _size = size;
        }
    }

    void destroy()
    {
        if (_data)
        {
            ConstructionPolicy::destroy(_data, _data + _size);
            if (_data != _buffer)
            {
                Allocator::deallocate(_data);
            }
        }

        _data = nullptr;
        _size = 0;
    }

private:
    T _buffer[staticBufferSize];
    T *_data;
    size_t _size;
};

template<typename T, size_t staticBufferSize, CpuType cpu, typename ConstructionPolicy = DefaultConstructionPolicy<T, cpu>>
using TNArray = StaticallyBufferedDynamicArray<T, staticBufferSize, DAALMalloc<T, cpu>, ConstructionPolicy, cpu>;

} // namespace internal
} // namespace services
} // namespace daal

#endif
