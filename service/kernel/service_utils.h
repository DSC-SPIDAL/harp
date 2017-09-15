/* file: service_utils.h */
/*******************************************************************************
* Copyright 2015-2017 Intel Corporation
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
//  Declaration of service utilities
//--
*/
#ifndef __SERVICE_UTILS_H__
#define __SERVICE_UTILS_H__

#include "env_detect.h"

namespace daal
{
namespace services
{
namespace internal
{

template<CpuType cpu, typename T>
struct RemoveReference { typedef T type; };

template<CpuType cpu, typename T>
struct RemoveReference<cpu, T&> { typedef T type; };

template<CpuType cpu, typename T>
struct RemoveReference<cpu, T&&> { typedef T type; };

template<CpuType cpu, typename T>
inline typename RemoveReference<cpu, T>::type &&move(T &&object)
{ return static_cast<typename RemoveReference<cpu, T>::type &&>(object); }

template<CpuType cpu, typename T>
inline void swap(T & x, T & y)
{
    T tmp = x;
    x = y;
    y = tmp;
}

template <CpuType cpu, typename ForwardIterator1, typename ForwardIterator2>
DAAL_FORCEINLINE void iterSwap(ForwardIterator1 a, ForwardIterator2 b)
{
    swap<cpu>(*a, *b);
}

template <CpuType cpu, typename ForwardIterator, typename T>
ForwardIterator lowerBound(ForwardIterator first, ForwardIterator last, const T & value)
{
    ForwardIterator it;
    auto count = last - first; // distance.
    while (count > 0)
    {
        it = first;
        const auto step = count / 2;
        it += step; // advance.
        if (*it < value)
        {
            first = ++it;
            count -= step + 1;
        }
        else
        {
            count = step;
        }
    }
    return first;
}

template <CpuType cpu, typename ForwardIterator, typename T, typename Compare>
ForwardIterator lowerBound(ForwardIterator first, ForwardIterator last, const T & value, Compare compare)
{
    ForwardIterator it;
    auto count = last - first; // distance.
    while (count > 0)
    {
        it = first;
        const auto step = count / 2;
        it += step; // advance.
        if (compare(*it, value))
        {
            first = ++it;
            count -= step + 1;
        }
        else
        {
            count = step;
        }
    }
    return first;
}

template <CpuType cpu, typename ForwardIterator, typename T>
ForwardIterator upperBound(ForwardIterator first, ForwardIterator last, const T & value)
{
    ForwardIterator it;
    auto count = last - first; // distance.
    while (count > 0)
    {
        it = first;
        const auto step = count / 2;
        it += step; // advance.
        if (!(value < *it))
        {
            first = ++it;
            count -= step + 1;
        }
        else
        {
            count = step;
        }
    }
    return first;
}

template <CpuType cpu, typename ForwardIterator, typename T, typename Compare>
ForwardIterator upperBound(ForwardIterator first, ForwardIterator last, const T & value, Compare compare)
{
    ForwardIterator it;
    auto count = last - first; // distance.
    while (count > 0)
    {
        it = first;
        const auto step = count / 2;
        it += step; // advance.
        if (!compare(value, *it))
        {
            first = ++it;
            count -= step + 1;
        }
        else
        {
            count = step;
        }
    }
    return first;
}

template <CpuType cpu, typename T>
inline const T & min(const T & a, const T & b) { return !(b < a) ? a : b; }

template <CpuType cpu, typename T>
inline const T & max(const T & a, const T & b) { return (a < b) ? b : a; }

template <CpuType cpu, typename BidirectionalIterator, typename Compare>
BidirectionalIterator partition(BidirectionalIterator first, BidirectionalIterator last, Compare compare)
{
    while (first != last)
    {
        while (compare(*first))
        {
            if (++first == last) return first;
        }
        do
        {
            if (--last == first) return first;
        } while (!compare(*last));
        swap<cpu>(*first, *last);
        ++first;
    }
    return first;
}

template <CpuType cpu, typename ForwardIterator>
ForwardIterator maxElement(ForwardIterator first, ForwardIterator last)
{
    if (first == last) { return last; }
    auto largest = first;
    while (++first != last)
    {
        if (*largest < *first) { largest = first; }
    }
    return largest;
}

template <CpuType cpu, typename ForwardIterator, typename Compare>
ForwardIterator maxElement(ForwardIterator first, ForwardIterator last, Compare compare)
{
    if (first == last) { return last; }
    auto largest = first;
    while (++first != last)
    {
        if (compare(*largest, *first)) { largest = first; }
    }
    return largest;
}

} // namespace internal
} // namespace services
} // namespace daal

#endif
