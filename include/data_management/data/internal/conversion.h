/* file: conversion.h */
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

#ifndef __DATA_MANAGEMENT_DATA_INTERNAL_CONVERSION_H__
#define __DATA_MANAGEMENT_DATA_INTERNAL_CONVERSION_H__

#include "data_management/features/defines.h"

namespace daal
{
namespace data_management
{
namespace internal
{

/* Renamed from InternalNumType */
enum ConversionDataType
{
    DAAL_SINGLE = 0,
    DAAL_DOUBLE = 1,
    DAAL_INT32  = 2,
    DAAL_OTHER  = 0xfffffff
};

/**
 * \return Internal numeric type
 */
template<typename T>
inline ConversionDataType getConversionDataType()          { return DAAL_OTHER;  }
template<>
inline ConversionDataType getConversionDataType<int>()     { return DAAL_INT32;  }
template<>
inline ConversionDataType getConversionDataType<double>()  { return DAAL_DOUBLE; }
template<>
inline ConversionDataType getConversionDataType<float>()   { return DAAL_SINGLE; }


typedef void(*vectorConvertFuncType)(size_t n, const void *src,
                                               void *dst);

typedef void(*vectorStrideConvertFuncType)(size_t n, const void *src, size_t srcByteStride,
                                                     void *dst, size_t dstByteStride);

DAAL_EXPORT vectorConvertFuncType getVectorUpCast(int, int);
DAAL_EXPORT vectorConvertFuncType getVectorDownCast(int, int);

DAAL_EXPORT vectorStrideConvertFuncType getVectorStrideUpCast(int, int);
DAAL_EXPORT vectorStrideConvertFuncType getVectorStrideDownCast(int, int);

} // namespace internal
} // namespace data_management
} // namespace daal

#endif
