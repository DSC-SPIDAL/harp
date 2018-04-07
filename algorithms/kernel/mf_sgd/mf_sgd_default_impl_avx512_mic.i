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
//  AVX512-MIC optimization of auxiliary functions used in default method 
//  of mf_sgd_batch 
//--
*/
#include <iostream>

template<> void updateMF_explicit<DAAL_FPTYPE, avx512_mic>(DAAL_FPTYPE* WMat, DAAL_FPTYPE* HMat, DAAL_FPTYPE* workV, int idx, const long dim_r, const DAAL_FPTYPE rate, const DAAL_FPTYPE lambda)
{

    DAAL_FPTYPE Mult = 0;
    DAAL_FPTYPE Err = 0;
    int j;

#if( __FPTYPE__(DAAL_FPTYPE) == __float__ )

    /* Unrolled by 16 loop */
    int n16 = dim_r & ~(16-1); 

    __m512 wVal;
    __m512 hVal;
    __m512 tmp1;
    __m512 tmp2;

    __m512 tmpzero = _mm512_set1_ps (0);
    __mmask16 mask = (1 << (dim_r - n16)) - 1;

    DAAL_FPTYPE mul_res;

    for (j = 0; j < n16; j+=16)
    {
        wVal        = _mm512_load_ps (&(WMat[j]));
        hVal        = _mm512_load_ps (&(HMat[j]));
        tmp1        = _mm512_mul_ps (wVal, hVal);
        mul_res     = _mm512_reduce_add_ps (tmp1);

        Mult += mul_res;
    }

    //for rest of dim_r
    if ( n16 < dim_r  )
    {
        wVal = _mm512_mask_load_ps(tmpzero, mask, &(WMat[n16]));
        hVal = _mm512_mask_load_ps(tmpzero, mask, &(HMat[n16]));
        tmp1 = _mm512_mul_ps (wVal, hVal);
        mul_res = _mm512_reduce_add_ps (tmp1);
        Mult += mul_res;
    }

    Err = workV[idx] - Mult;

    __m512  err_v  = _mm512_set1_ps (Err);
    __m512  rate_v = _mm512_set1_ps (rate);
    __m512  lambda_v = _mm512_set1_ps (-lambda);

    
    for (j = 0; j < n16; j+=16)
    {

        wVal        = _mm512_load_ps (&(WMat[j]));
        hVal        = _mm512_load_ps (&(HMat[j]));

        tmp1        = _mm512_mul_ps (lambda_v, wVal);
        tmp2        = _mm512_mul_ps (err_v, wVal);

        /* update w model */
        tmp1        = _mm512_fmadd_ps (err_v, hVal, tmp1);
        wVal        = _mm512_fmadd_ps (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm512_fmadd_ps (lambda_v, hVal, tmp2);
        hVal        = _mm512_fmadd_ps (rate_v, tmp2, hVal);

        _mm512_store_ps (&(WMat[j]), wVal);
        _mm512_store_ps (&(HMat[j]), hVal);

    }

    //for the rest of dim_r
     if ( n16 < dim_r  )
     {
         wVal = _mm512_mask_load_ps(tmpzero, mask, &(WMat[n16]));
         hVal = _mm512_mask_load_ps(tmpzero, mask, &(HMat[n16]));

         tmp1 = _mm512_mul_ps (lambda_v, wVal);
         tmp2 = _mm512_mul_ps (err_v, wVal);

         /* update w model */
         tmp1 = _mm512_fmadd_ps (err_v, hVal, tmp1);
         wVal = _mm512_fmadd_ps (rate_v, tmp1, wVal);

         /* update h model */
         tmp2 = _mm512_fmadd_ps (lambda_v, hVal, tmp2);
         hVal = _mm512_fmadd_ps (rate_v, tmp2, hVal);

         _mm512_mask_store_ps (&(WMat[n16]), mask, wVal);
         _mm512_mask_store_ps (&(HMat[n16]), mask, hVal);
     }

#elif( __FPTYPE__(DAAL_FPTYPE) == __double__ )

    /* Unrolled by 8 loop */
    int n8 = dim_r & ~(8-1); 

    __m512d wVal;
    __m512d hVal;
    __m512d tmp1;
    __m512d tmp2;

    __m512d tmpzero = _mm512_set1_pd (0);
    __mmask8 mask = (1 << (dim_r - n8)) - 1;

    DAAL_FPTYPE mul_res;

    for (j = 0; j < n8; j+=8)
    {
        wVal        = _mm512_load_pd (&(WMat[j]));
        hVal        = _mm512_load_pd (&(HMat[j]));
        tmp1        = _mm512_mul_pd (wVal, hVal);
        mul_res     = _mm512_reduce_add_pd (tmp1);
        Mult += mul_res;
    }

    if ( n8 < dim_r  )
    {
        wVal = _mm512_mask_load_pd(tmpzero, mask, &(WMat[n8]));
        hVal = _mm512_mask_load_pd(tmpzero, mask, &(HMat[n8]));
        tmp1 = _mm512_mul_pd (wVal, hVal);
        mul_res = _mm512_reduce_add_pd (tmp1);
        Mult += mul_res;
    }

    Err = workV[idx] - Mult;

    __m512d  err_v  = _mm512_set1_pd (Err);
    __m512d  rate_v = _mm512_set1_pd (rate);
    __m512d  lambda_v = _mm512_set1_pd (-lambda);

    
    for (j = 0; j < n8; j+=8)
    {
        wVal        = _mm512_load_pd (&(WMat[j]));
        hVal        = _mm512_load_pd (&(HMat[j]));

        tmp1        = _mm512_mul_pd (lambda_v, wVal);
        tmp2        = _mm512_mul_pd (err_v, wVal);

        /* update w model */
        tmp1        = _mm512_fmadd_pd (err_v, hVal, tmp1);
        wVal        = _mm512_fmadd_pd (rate_v, tmp1, wVal);

        /* update h model */
        tmp2        = _mm512_fmadd_pd (lambda_v, hVal, tmp2);
        hVal        = _mm512_fmadd_pd (rate_v, tmp2, hVal);


        _mm512_store_pd (&(WMat[j]), wVal);
        _mm512_store_pd (&(HMat[j]), hVal);

    }

    if (n8 < dim_r )
    {
        wVal = _mm512_mask_load_pd(tmpzero, mask, &(WMat[n8]));
        hVal = _mm512_mask_load_pd(tmpzero, mask, &(HMat[n8]));

        tmp1 = _mm512_mul_pd (lambda_v, wVal);
        tmp2 = _mm512_mul_pd (err_v, wVal);

        /* update w model */
        tmp1 = _mm512_fmadd_pd (err_v, hVal, tmp1);
        wVal = _mm512_fmadd_pd (rate_v, tmp1, wVal);

        /* update h model */
        tmp2 = _mm512_fmadd_pd (lambda_v, hVal, tmp2);
        hVal = _mm512_fmadd_pd (rate_v, tmp2, hVal);

        _mm512_mask_store_pd (&(WMat[n8]), mask, wVal);
        _mm512_mask_store_pd (&(HMat[n8]), mask, hVal);

    }


#else
    #error "DAAL_FPTYPE must be defined to float or double"
#endif

}

