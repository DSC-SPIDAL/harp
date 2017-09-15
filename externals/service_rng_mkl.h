/* file: service_rng_mkl.h */
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
//  Template wrappers for common Intel(R) MKL functions.
//--
*/

#ifndef __SERVICE_RNG_MKL_H__
#define __SERVICE_RNG_MKL_H__

#include "vmlvsl.h"
#include "service_stat_rng_mkl.h"
#include "service_rng_common.h"

// RNGs
#define __DAAL_BRNG_MT19937                     VSL_BRNG_MT19937
#define __DAAL_RNG_METHOD_UNIFORM_STD           VSL_RNG_METHOD_UNIFORM_STD
#define __DAAL_RNG_METHOD_BERNOULLI_ICDF        VSL_RNG_METHOD_BERNOULLI_ICDF
#define __DAAL_RNG_METHOD_GAUSSIAN_BOXMULLER    VSL_RNG_METHOD_GAUSSIAN_BOXMULLER
#define __DAAL_RNG_METHOD_GAUSSIAN_BOXMULLER2   VSL_RNG_METHOD_GAUSSIAN_BOXMULLER2
#define __DAAL_RNG_METHOD_GAUSSIAN_ICDF         VSL_RNG_METHOD_GAUSSIAN_ICDF

namespace daal
{
namespace internal
{
namespace mkl
{
/* Uniform distribution generator functions */
template<typename T, CpuType cpu>
int uniformRNG(const size_t n, T* r, void* stream, const T a, const T b, const int method);

template<CpuType cpu>
int uniformRNG(const size_t n, int* r, void* stream, const int a, const int b, const int method)
{
    int errcode = 0;
    int    nn   = (int)n;
    int*   rr   = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, iRngUniform, ( method, stream, nn, rr, a, b ), errcode);
    return errcode;
}

template<CpuType cpu>
int uniformRNG(const size_t n, float* r, void* stream, const float a, const float b, const int method)
{
    int errcode = 0;
    int    nn   = (int)n;
    float* rr   = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, sRngUniform, ( method, stream, nn, rr, a, b ), errcode);
    return errcode;
}

template<CpuType cpu>
int uniformRNG(const size_t n, double* r, void* stream, const double a, const double b, const int method)
{
    int errcode = 0;
    int     nn  = (int)n;
    double* rr  = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, dRngUniform, ( method, stream, nn, rr, a, b ), errcode);
    return errcode;
}

/* Gaussian distribution generator functions */
template<typename T, CpuType cpu>
int gaussianRNG(const size_t n, T* r, void* stream, const T a, const T sigma, const int method);

template<CpuType cpu>
int gaussianRNG(const size_t n, float* r, void* stream, const float a, const float sigma, const int method)
{
    int errcode = 0;
    int     nn  = (int)n;
    float*  rr  = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, sRngGaussian, ( method, stream, nn, rr, a, sigma ), errcode);
    return errcode;
}

template<CpuType cpu>
int gaussianRNG(const size_t n, double* r, void* stream, const double a, const double sigma, const int method)
{
    int errcode = 0;
    int     nn  = (int)n;
    double* rr  = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, dRngGaussian, ( method, stream, nn, rr, a, sigma ), errcode);
    return errcode;
}

/* Bernoulli distribution generator functions */
template<typename T, CpuType cpu>
int bernoulliRNG(const size_t n, T* r, void *stream, const double p, const int method);

template<CpuType cpu>
int bernoulliRNG(const size_t n, int* r, void *stream, const double p, const int method)
{
    int errcode = 0;
    int     nn  = (int)n;
    int*    rr  = r;
    __DAAL_VSLFN_CALL_NR_WHILE(fpk_vsl_kernel, iRngBernoulli, (method, stream, nn, rr, p), errcode);
    return errcode;
}

template<CpuType cpu>
class BaseRNG : public BaseRNGIface<cpu>
{
public:
    BaseRNG(const unsigned int _seed, const int _brngId) : stream(0)
    {
        int errcode = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslNewStreamEx, ( &stream, _brngId, 1, &_seed ), errcode);
    }

    BaseRNG(const size_t n, const unsigned int* _seed, const int _brngId = __DAAL_BRNG_MT19937) : stream(0)
    {
        int errcode = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslNewStreamEx, ( &stream, _brngId, n, _seed ), errcode);
    }

    ~BaseRNG()
    {
        int errcode = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslDeleteStream, ( &stream ), errcode);
    }

    int getStateSize()
    {
        int res = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslGetStreamSize, (stream), res);
        return res;
    }

    int saveState(void* dest)
    {
        int errcode = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslSaveStreamM, (stream, (char*)dest), errcode);
        return errcode;
    }

    int loadState(const void* src)
    {
        int errcode = 0;
        __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslDeleteStream, (&stream), errcode);
        if(!errcode)
            __DAAL_VSLFN_CALL_NR(fpk_vsl_sub_kernel, vslLoadStreamM, (&stream, (const char*)src), errcode);
        return errcode;
    }

    void* getState()
    {
        return stream;
    }

private:
   void* stream;
};

/*
// Generator functions definition
*/
template<typename Type, CpuType cpu>
class RNGs
{
public:
    typedef DAAL_INT SizeType;
    typedef BaseRNG<cpu> BaseType;

    RNGs() {}

    int uniform(const SizeType n, Type* r, BaseType &brng, const Type a, const Type b, const int method)
    {
        return uniformRNG<cpu>(n, r, brng.getState(), a, b, method);
    }

    int uniform(const SizeType n, Type* r, void *state, const Type a, const Type b, const int method)
    {
        return uniformRNG<cpu>(n, r, state, a, b, method);
    }

    int bernoulli(const SizeType n, Type* r, BaseType &brng, const double p, const int method)
    {
        return bernoulliRNG<cpu>(n, r, brng.getState(), p, method);
    }

    int bernoulli(const SizeType n, Type* r, void *state, const double p, const int method)
    {
        return bernoulliRNG<cpu>(n, r, state, p, method);
    }

    int gaussian(const SizeType n, Type* r, BaseType &brng, const Type a, const Type sigma, const int method)
    {
        return gaussianRNG<cpu>(n, r, brng.getState(), a, sigma, method);
    }

    int gaussian(const SizeType n, Type* r, void *state, const Type a, const Type sigma, const int method)
    {
        return gaussianRNG<cpu>(n, r, state, a, sigma, method);
    }
};

}
}
}

#endif
