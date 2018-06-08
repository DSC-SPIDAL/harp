/* file: mf_sgd_default_batch_impl_avx512_mic.i */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

/*
//++
//  AVX optimization of auxiliary functions used in default method 
//  of mf_sgd_batch 
//--
*/
#include <iostream>

template<> void updateMF_explicit<DAAL_FPTYPE, avx>(DAAL_FPTYPE* WMat, DAAL_FPTYPE* HMat, DAAL_FPTYPE* workV, int idx, const long dim_r, const DAAL_FPTYPE rate, const DAAL_FPTYPE lambda)
{

    DAAL_FPTYPE Mult = 0;
    DAAL_FPTYPE Err = 0;
    int j;

#if( __FPTYPE__(DAAL_FPTYPE) == __float__ )

    /* Unrolled by 8 loop */
    int n8 = dim_r & ~(8-1); 

    __m256 wVal;
    __m256 hVal;
    __m256 tmp1;
    __m256 tmp2;

    int mask_integer[8]={0,0,0,0,0,0,0,0};
    for (int i=0;i<(dim_r - n8); i++)
        mask_integer[i] = -1;

    __m256i mask = _mm256_setr_epi32(mask_integer[0],mask_integer[1],mask_integer[2],mask_integer[3],mask_integer[4],
                mask_integer[5],mask_integer[6],mask_integer[7]);

    DAAL_FPTYPE mul_res;

    for (j = 0; j < n8; j+=8)
    {

        wVal        = _mm256_load_ps (&(WMat[j]));
        hVal        = _mm256_load_ps (&(HMat[j]));
        tmp1        = _mm256_mul_ps (wVal, hVal);

        const __m128 x128 = _mm_add_ps(_mm256_extractf128_ps(tmp1, 1), _mm256_castps256_ps128(tmp1));
        const __m128 x64 = _mm_add_ps(x128, _mm_movehl_ps(x128, x128));
        const __m128 x32 = _mm_add_ss(x64, _mm_shuffle_ps(x64, x64, 0x55));
        mul_res = _mm_cvtss_f32(x32);

        Mult += mul_res;
    }

    //for rest of dim_r
    if ( n8 < dim_r )
    {
        wVal        = _mm256_maskload_ps(&(WMat[j]), mask);
        hVal        = _mm256_maskload_ps(&(HMat[j]), mask);
        tmp1        = _mm256_mul_ps (wVal, hVal);

        const __m128 x128 = _mm_add_ps(_mm256_extractf128_ps(tmp1, 1), _mm256_castps256_ps128(tmp1));
        const __m128 x64 = _mm_add_ps(x128, _mm_movehl_ps(x128, x128));
        const __m128 x32 = _mm_add_ss(x64, _mm_shuffle_ps(x64, x64, 0x55));
        mul_res = _mm_cvtss_f32(x32);

        Mult += mul_res;

    }

    Err = workV[idx] - Mult;

    __m256  err_v  = _mm256_set1_ps (Err);
    __m256  rate_v = _mm256_set1_ps (rate);
    __m256  lambda_v = _mm256_set1_ps (-lambda);

    
    for (j = 0; j < n8; j+=8)
    {

        wVal        = _mm256_load_ps (&(WMat[j]));
        hVal        = _mm256_load_ps (&(HMat[j]));

        tmp1        = _mm256_mul_ps (lambda_v, wVal);
        tmp2        = _mm256_mul_ps (err_v, wVal);

        /* update w model */
        tmp1        = _mm256_fmadd_ps (err_v, hVal, tmp1);
        wVal        = _mm256_fmadd_ps (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm256_fmadd_ps (lambda_v, hVal, tmp2);
        hVal        = _mm256_fmadd_ps (rate_v, tmp2, hVal);

        _mm256_store_ps (&(WMat[j]), wVal);
        _mm256_store_ps (&(HMat[j]), hVal);

    }

    //for rest of dim_r
    if ( n8 < dim_r )
    {
        wVal        = _mm256_maskload_ps(&(WMat[j]), mask);
        hVal        = _mm256_maskload_ps(&(HMat[j]), mask);

        tmp1        = _mm256_mul_ps (lambda_v, wVal);
        tmp2        = _mm256_mul_ps (err_v, wVal);

        /* update w model */
        tmp1        = _mm256_fmadd_ps (err_v, hVal, tmp1);
        wVal        = _mm256_fmadd_ps (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm256_fmadd_ps (lambda_v, hVal, tmp2);
        hVal        = _mm256_fmadd_ps (rate_v, tmp2, hVal);

        _mm256_maskstore_ps(&(WMat[j]), mask, wVal);
        _mm256_maskstore_ps(&(HMat[j]), mask, hVal);

    }


#elif( __FPTYPE__(DAAL_FPTYPE) == __double__ )

    /* Unrolled by 4 loop */
    int n4 = dim_r & ~(4-1);

    __m256d wVal;
    __m256d hVal;
    __m256d tmp1;
    __m256d tmp2;

    int mask_integer[8]={0,0,0,0,0,0,0,0};
    for (int i=0;i<(dim_r - n4); i++)
        mask_integer[i] = -1;

    __m256i mask = _mm256_setr_epi32(mask_integer[0],mask_integer[1],mask_integer[2],mask_integer[3],mask_integer[4],
                mask_integer[5],mask_integer[6],mask_integer[7]);

    DAAL_FPTYPE mul_res;

    for (j = 0; j < n4; j+=4)
    {

        wVal        = _mm256_load_pd (&(WMat[j]));
        hVal        = _mm256_load_pd (&(HMat[j]));
        tmp1        = _mm256_mul_pd (wVal, hVal);

        const __m128d x128 = _mm_add_pd(_mm256_extractf128_pd(tmp1, 1), _mm256_castpd256_pd128(tmp1));
        const __m128d x64 = _mm_add_pd(x128, _mm_shuffle_pd(x128, x128, 1));
        mul_res = _mm_cvtsd_f64(x64);

        Mult += mul_res;
    }

    if ( n4 < dim_r )
    {
        wVal        = _mm256_maskload_pd(&(WMat[j]), mask);
        hVal        = _mm256_maskload_pd(&(HMat[j]), mask);
        tmp1        = _mm256_mul_pd (wVal, hVal);

        const __m128d x128 = _mm_add_pd(_mm256_extractf128_pd(tmp1, 1), _mm256_castpd256_pd128(tmp1));
        const __m128d x64 = _mm_add_pd(x128, _mm_shuffle_pd(x128, x128, 1));
        mul_res = _mm_cvtsd_f64(x64);

        Mult += mul_res;
    }

    Err = workV[idx] - Mult;

    __m256d  err_v  = _mm256_set1_pd (Err);
    __m256d  rate_v = _mm256_set1_pd (rate);
    __m256d  lambda_v = _mm256_set1_pd (-lambda);

    
    for (j = 0; j < n4; j+=4)
    {

        wVal        = _mm256_load_pd (&(WMat[j]));
        hVal        = _mm256_load_pd (&(HMat[j]));

        tmp1        = _mm256_mul_pd (lambda_v, wVal);
        tmp2        = _mm256_mul_pd (err_v, wVal);

        /* update w model */
        tmp1        = _mm256_fmadd_pd (err_v, hVal, tmp1);
        wVal        = _mm256_fmadd_pd (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm256_fmadd_pd (lambda_v, hVal, tmp2);
        hVal        = _mm256_fmadd_pd (rate_v, tmp2, hVal);


        _mm256_store_pd (&(WMat[j]), wVal);
        _mm256_store_pd (&(HMat[j]), hVal);

    }

    if ( n4 < dim_r )
    {
        wVal        = _mm256_maskload_pd(&(WMat[j]), mask);
        hVal        = _mm256_maskload_pd(&(HMat[j]), mask);

        tmp1        = _mm256_mul_pd (lambda_v, wVal);
        tmp2        = _mm256_mul_pd (err_v, wVal);

        /* update w model */
        tmp1        = _mm256_fmadd_pd (err_v, hVal, tmp1);
        wVal        = _mm256_fmadd_pd (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm256_fmadd_pd (lambda_v, hVal, tmp2);
        hVal        = _mm256_fmadd_pd (rate_v, tmp2, hVal);


        _mm256_maskstore_pd(&(WMat[j]), mask, wVal);
        _mm256_maskstore_pd(&(HMat[j]), mask, hVal);

    }

#else
    #error "DAAL_FPTYPE must be defined to float or double"
#endif

}



