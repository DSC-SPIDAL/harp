/* file: service_math.h */
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
//  Template wrappers for math functions.
//--
*/

#ifndef __SERVICE_MATH_H__
#define __SERVICE_MATH_H__

#include "service_defines.h"
#include "service_math_mkl.h"

namespace daal
{
namespace internal
{

/*
// Template functions definition
*/
template<typename fpType, CpuType cpu, template<typename, CpuType> class _impl=mkl::MklMath>
struct Math
{
    typedef typename _impl<fpType,cpu>::SizeType SizeType;

    static fpType sFabs(fpType in)
    {
        return _impl<fpType,cpu>::sFabs(in);
    }

    static fpType sMin(fpType in1, fpType in2)
    {
        return _impl<fpType,cpu>::sMin(in1, in2);
    }

    static fpType sMax(fpType in1, fpType in2)
    {
        return _impl<fpType,cpu>::sMax(in1, in2);
    }

    static fpType sSqrt(fpType in)
    {
        return _impl<fpType,cpu>::sSqrt(in);
    }

    static fpType sPowx(fpType in, fpType in1)
    {
        return _impl<fpType,cpu>::sPowx(in, in1);
    }

    static fpType sCeil(fpType in)
    {
        return _impl<fpType,cpu>::sCeil(in);
    }

    static fpType sErfInv(fpType in)
    {
        return _impl<fpType,cpu>::sErfInv(in);
    }

    static fpType sErf(fpType in)
    {
        return _impl<fpType,cpu>::sErf(in);
    }

    static fpType sLog(fpType in)
    {
        return _impl<fpType,cpu>::sLog(in);
    }

    static fpType sCdfNormInv(fpType in)
    {
        return _impl<fpType,cpu>::sCdfNormInv(in);
    }

    static void vPowx(SizeType n, const fpType *in, fpType in1, fpType *out)
    {
        _impl<fpType,cpu>::vPowx(n, in, in1, out);
    }

    static void vPowxAsLnExp(SizeType n, const fpType *in, fpType in1, fpType *out)
    {
        _impl<fpType,cpu>::vLog(n, in, out);
        for(size_t i = 0; i < n ; i++) {out[i] *= in1;}
        _impl<fpType,cpu>::vExp(n, out, out);
    }

    static void vCeil(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vCeil(n, in, out);
    }

    static void vErfInv(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vErfInv(n, in, out);
    }

    static void vErf(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vErf(n, in, out);
    }

    static void vExp(SizeType n, const fpType *in, fpType* out)
    {
        _impl<fpType,cpu>::vExp(n, in, out);
    }

    static fpType vExpThreshold()
    {
        return _impl<fpType,cpu>::vExpThreshold();
    }

    static void vTanh(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vTanh(n, in, out);
    }

    static void vSqrt(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vSqrt(n, in, out);
    }

    static void vLog(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vLog(n, in, out);
    }

    static void vLog1p(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vLog1p(n, in, out);
    }

    static void vCdfNormInv(SizeType n, const fpType *in, fpType *out)
    {
        _impl<fpType,cpu>::vCdfNormInv(n, in, out);
    }
};

} // namespace internal
} // namespace daal

#endif
