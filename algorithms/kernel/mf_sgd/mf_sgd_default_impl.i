/* file: mf_sgd_dense_default_impl.i */
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
//  Implementation of mf_sgds
//--
*/

#ifndef __MF_SGD_UTILS_IMPL_I__
#define __MF_SGD_UTILS_IMPL_I__

#include <algorithm>
#include <math.h>       
#include <cstdlib> 
#include <iostream>
#include <time.h>

#include "service_blas.h"
#include "service_lapack.h"
#include "service_memory.h"
#include "service_math.h"
#include "service_defines.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"

#include "threading.h"
#include "tbb/tick_count.h"
#include "task_scheduler_init.h"
#include "blocked_range.h"
#include "parallel_for.h"
#include "queuing_mutex.h"

using namespace tbb;
using namespace daal::internal;
using namespace daal::services::internal;

typedef queuing_mutex currentMutex_t;

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
namespace internal
{

template<typename interm, CpuType cpu>
void updateMF(interm *WMat,interm *HMat, interm* workV, const int idx, const long dim_r, const interm rate, const interm lambda);

template<typename interm, CpuType cpu>
void updateMF_explicit(interm *WMat,interm *HMat, interm* workV, int idx, const long dim_r, const interm rate, const interm lambda);

template<typename interm, CpuType cpu>
void computeRMSE(interm *WMat,interm *HMat, interm* testV, interm* testRMSE, int idx, const long dim_r);

template<typename interm, CpuType cpu>
void computeRMSE_explicit(interm *WMat,interm *HMat, interm* testV, interm* testRMSE, int idx, const long dim_r);

/* Max number of blocks depending on arch */
#if( __CPUID__(DAAL_CPU) >= __avx512_mic__ )
    #define DEF_MAX_BLOCKS 256
#else
    #define DEF_MAX_BLOCKS 128
#endif

// CPU intrinsics for Intel Compiler only
#if defined (__INTEL_COMPILER) && defined(__linux__) && defined(__x86_64__)
    #include <immintrin.h>
#endif

template<typename interm, CpuType cpu>
MFSGDTBB<interm, cpu>::MFSGDTBB(
        interm* mtWDataTable,
        interm* mtHDataTable,
        int* workWPos,
        int* workHPos,
        interm *workV,
        const long Dim,
        const interm learningRate,
        const interm lambda,
        currentMutex_t* mutex_w,
        currentMutex_t* mutex_h,
        const int Avx_explicit,
        const int step,
        const int dim_train

)
{/*{{{*/

    _mtWDataTable = mtWDataTable;
    _mtHDataTable = mtHDataTable;
    
    _workWPos = workWPos;
    _workHPos = workHPos;

    _workV = workV;
    _Dim = Dim;
    _learningRate = learningRate;
    _lambda = lambda;

    _mutex_w = mutex_w;
    _mutex_h = mutex_h;
    _Avx_explicit = Avx_explicit;

    _step = step;
    _dim_train = dim_train;

    _itr = 0;

}/*}}}*/

template<typename interm, CpuType cpu>
MFSGDTBBREORDER<interm, cpu>::MFSGDTBBREORDER(
            interm* mtWDataTable,
            interm* mtHDataTable,
            int* queueWPos,
            int* queueLength,
            int** queueHPos,
            interm** queueVVal,
            const long Dim,
            const interm learningRate,
            const interm lambda,
            currentMutex_t* mutex_w,
            currentMutex_t* mutex_h,
            const int Avx_explicit
)
{/*{{{*/

    _mtWDataTable = mtWDataTable;
    _mtHDataTable = mtHDataTable;

    _queueWPos = queueWPos;
    _queueLength = queueLength;
    _queueHPos = queueHPos;
    _queueVVal = queueVVal;

    _Dim = Dim;
    _learningRate = learningRate;
    _lambda = lambda;

    _mutex_w = mutex_w;
    _mutex_h = mutex_h;
    _Avx_explicit = Avx_explicit;

}/*}}}*/

template<typename interm, CpuType cpu>
void MFSGDTBB<interm, cpu>::operator()( const blocked_range<int>& range ) const 
{/*{{{*/

    interm *WMat = 0;
    interm *HMat = 0;

    interm Mult = 0;
    interm Err = 0;
    interm WMatVal = 0;
    interm HMatVal = 0;

    /* using local variables */
    interm* mtWDataTable = _mtWDataTable;
    interm* mtHDataTable = _mtHDataTable;

    int* workWPos = _workWPos;
    int* workHPos = _workHPos;
    interm* workV = _workV;

    int* order = _order;

    long Dim = _Dim;
    interm learningRate = _learningRate;
    interm lambda = _lambda;

    int step = _step;
    int dim_train = _dim_train;
    int itr = _itr;

    int index;

    tbb::tick_count timeStart = _timeStart;
    double timeOut = _timeOut;

    for( int i=range.begin(); i!=range.end(); ++i )
    {
        /* check whether time is out */
        if ( (timeOut != 0) && ((tbb::tick_count::now() - timeStart).seconds() > timeOut)) 
            return;

        /* index is ajusted according to the iteration id and ratio of  */
        /* computed tasks in each iteration */
        /* if (order == NULL) */
        index = (i + itr*step)%dim_train;
        /* else */
            /* index = order[(i + itr*step)%dim_train]; */

        WMat = mtWDataTable + workWPos[index]*Dim;
        HMat = mtHDataTable + workHPos[index]*Dim;

        /* disable mutual lock to use the asynchronous model pattern */
        currentMutex_t::scoped_lock lock_w(_mutex_w[_workWPos[index]]);
        currentMutex_t::scoped_lock lock_h(_mutex_h[_workHPos[index]]);
        
        if (_Avx_explicit == 1)
            updateMF_explicit<interm, cpu>(WMat, HMat, workV, index, Dim, learningRate, lambda);
        else
            updateMF<interm, cpu>(WMat, HMat, workV, index, Dim, learningRate, lambda);

        lock_w.release();
        lock_h.release();

    }

}/*}}}*/

template<typename interm, CpuType cpu>
void MFSGDTBBREORDER<interm, cpu>::operator()( const blocked_range<int>& range ) const 
{/*{{{*/

    interm *WMat = 0;
    interm *HMat = 0;
    interm* workV = 0;

    interm Mult = 0;
    interm Err = 0;
    interm WMatVal = 0;
    interm HMatVal = 0;

    /* using local variables */
    interm* mtWDataTable = _mtWDataTable;
    interm* mtHDataTable = _mtHDataTable;

    int* queueWPos = _queueWPos;
    int* queueLength = _queueLength;
    int** queueHPos = _queueHPos;
    interm** queueVVal = _queueVVal;

    long Dim = _Dim;
    interm learningRate = _learningRate;
    interm lambda = _lambda;

    for( int i=range.begin(); i!=range.end(); ++i )
    {

        /* get the row_id for a single queue */
        int row_id = queueWPos[i];
        WMat = mtWDataTable + row_id*Dim;

        int qLength = queueLength[i];
        int* col_ids = queueHPos[i];
        interm* vals = queueVVal[i];

        for(int j= 0; j<qLength; j++)
        {
            HMat = mtHDataTable + col_ids[j]*Dim;
            updateMF<interm, cpu>(WMat, HMat, vals, j, Dim, learningRate, lambda);
        }

        /* WMat = mtWDataTable + workWPos[index]*Dim; */
        /* disable mutual lock to use the asynchronous model pattern */
        //currentMutex_t::scoped_lock lock_w(_mutex_w[_workWPos[index]]);
        //currentMutex_t::scoped_lock lock_h(_mutex_h[_workHPos[index]]);
        
        //if (_Avx_explicit == 1)
         //   updateMF_explicit<interm, cpu>(WMat, HMat, workV, index, Dim, learningRate, lambda);
        //else
         //   updateMF<interm, cpu>(WMat, HMat, workV, index, Dim, learningRate, lambda);

        //lock_w.release();
        //lock_h.release();

    }

}/*}}}*/

template<typename interm, CpuType cpu>
MFSGDTBB_TEST<interm, cpu>::MFSGDTBB_TEST(
        interm* mtWDataTable,
        interm* mtHDataTable,
        int* testWPos,
        int* testHPos,
        interm *testV,
        const long Dim,
        interm* testRMSE,
        currentMutex_t* mutex_w,
        currentMutex_t* mutex_h,
        const int Avx_explicit

)
{/*{{{*/

    _mtWDataTable = mtWDataTable;
    _mtHDataTable = mtHDataTable;
    
    _testWPos = testWPos;
    _testHPos = testHPos;

    _testV = testV;
    _Dim = Dim;

    _testRMSE = testRMSE;

    _mutex_w = mutex_w;
    _mutex_h = mutex_h;

    _Avx_explicit = Avx_explicit;

}/*}}}*/

template<typename interm, CpuType cpu>
void MFSGDTBB_TEST<interm, cpu>::operator()( const blocked_range<int>& range ) const 
{/*{{{*/

    interm *WMat = 0;
    interm *HMat = 0;

    interm* mtWDataTable = _mtWDataTable;
    interm* mtHDataTable = _mtHDataTable;

    interm* testV = _testV;

    int* testWPos = _testWPos;
    int* testHPos = _testHPos;

    long Dim = _Dim;
    interm* testRMSE = _testRMSE;


    for( int i=range.begin(); i!=range.end(); ++i )
    {

        if (testWPos[i] != -1 && testHPos[i] != -1)
        {

            WMat = mtWDataTable + testWPos[i]*Dim;
            HMat = mtHDataTable + testHPos[i]*Dim;

            currentMutex_t::scoped_lock lock_w(_mutex_w[_testWPos[i]]);
            currentMutex_t::scoped_lock lock_h(_mutex_h[_testHPos[i]]);

            if (_Avx_explicit == 1)
                computeRMSE_explicit<interm, cpu>(WMat, HMat, testV, testRMSE, i, Dim);
            else
                computeRMSE<interm, cpu>(WMat, HMat, testV, testRMSE, i, Dim);

            lock_w.release();
            lock_h.release();

        }
        else
            testRMSE[i] = 0;

    }

}/*}}}*/

/**
 * @brief compiler based vectorization according to different CpuType
 *
 * @tparam interm
 * @tparam cpu
 * @param WMat
 * @param HMat
 * @param workV
 * @param seq
 * @param idx
 * @param dim_r
 * @param rate
 * @param lambda
 */
template<typename interm, CpuType cpu>
void updateMF(interm *WMat,interm *HMat, interm* workV, const int idx, const long dim_r, const interm rate, const interm lambda)
{/*{{{*/

    interm Mult = 0;
    interm Err = 0;
    interm WMatVal = 0;
    interm HMatVal = 0;

    for(int p = 0; p<dim_r; p++)
        Mult += (WMat[p]*HMat[p]);

    Err = workV[idx] - Mult;

    for(int p = 0;p<dim_r;p++)
    {
        WMatVal = WMat[p];
        HMatVal = HMat[p];

        WMat[p] = WMatVal + rate*(Err*HMatVal - lambda*WMatVal);
        HMat[p] = HMatVal + rate*(Err*WMatVal - lambda*HMatVal);

    }

}/*}}}*/


/**
 * @brief use explicit vectorization codes by immintrin.h
 * if explicit vectorization for a CpuType is absent, retreat to 
 * the compiler generated vectorization
 *
 * @tparam interm
 * @tparam cpu
 * @param WMat
 * @param HMat
 * @param workV
 * @param seq
 * @param idx
 * @param dim_r
 * @param rate
 * @param lambda
 */
template<typename interm, CpuType cpu>
void updateMF_explicit(interm *WMat,interm *HMat, interm* workV, int idx, const long dim_r, const interm rate, const interm lambda)
{/*{{{*/

    std::printf("AVX updateMF_explicti is not used\n");
    std::fflush;

    interm Mult = 0;
    interm Err = 0;
    interm WMatVal = 0;
    interm HMatVal = 0;

    for(int p = 0; p<dim_r; p++)
        Mult += (WMat[p]*HMat[p]);

    Err = workV[idx] - Mult;

    for(int p = 0;p<dim_r;p++)
    {
        WMatVal = WMat[p];
        HMatVal = HMat[p];

        WMat[p] = WMatVal + rate*(Err*HMatVal - lambda*WMatVal);
        HMat[p] = HMatVal + rate*(Err*WMatVal - lambda*HMatVal);

    }

}/*}}}*/

/**
 * @brief compiler based vectorization according to different CpuType
 *
 * @tparam interm
 * @tparam cpu
 * @param WMat
 * @param HMat
 * @param testV
 * @param testRMSE
 * @param idx
 * @param dim_r
 */
template<typename interm, CpuType cpu>
void computeRMSE(interm *WMat,interm *HMat, interm* testV, interm* testRMSE, int idx, const long dim_r)
{/*{{{*/
    int p;
    interm Mult = 0;
    interm Err;

    for(p = 0; p<dim_r; p++)
        Mult += (WMat[p]*HMat[p]);

    Err = testV[idx] - Mult;

    testRMSE[idx] = Err*Err;

}/*}}}*/

/**
 * @brief use explicit vectorization codes by immintrin.h
 * if explicit vectorization for a CpuType is absent, retreat to 
 * the compiler generated vectorization
 * @tparam interm
 * @tparam cpu
 * @param WMat
 * @param HMat
 * @param testV
 * @param testRMSE
 * @param idx
 * @param dim_r
 */
template<typename interm, CpuType cpu>
void computeRMSE_explicit(interm *WMat,interm *HMat, interm* testV, interm* testRMSE, int idx, const long dim_r)
{/*{{{*/

    int p;
    interm Mult = 0;
    interm Err;

    for(p = 0; p<dim_r; p++)
        Mult += (WMat[p]*HMat[p]);

    Err = testV[idx] - Mult;

    testRMSE[idx] = Err*Err;

}/*}}}*/


/* AVX optimization via template specialization (Intel compiler only) */
#if defined (__INTEL_COMPILER) && defined(__linux__) && defined(__x86_64__) && ( __CPUID__(DAAL_CPU) == __avx__ )
    #include "mf_sgd_default_impl_avx.i"
#endif

/* AVX512-MIC optimization via template specialization (Intel compiler only) */
#if defined (__INTEL_COMPILER) && defined(__linux__) && defined(__x86_64__) && ( __CPUID__(DAAL_CPU) == __avx512_mic__ )
    #include "mf_sgd_default_impl_avx512_mic.i"
#endif

} // namespace daal::internal
}
}
} // namespace daal

#endif
