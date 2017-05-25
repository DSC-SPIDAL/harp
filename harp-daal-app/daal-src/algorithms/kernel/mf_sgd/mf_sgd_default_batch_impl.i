/* file: mf_sgd_dense_default_batch_impl.i */
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
//  Implementation of mf_sgds batch mode
//--
*/

#ifndef __MF_SGD_KERNEL_BATCH_IMPL_I__
#define __MF_SGD_KERNEL_BATCH_IMPL_I__

#include <time.h>
#include <math.h>       
#include <algorithm>
#include <cstdlib> 
#include <cstdio> 
#include <iostream>
#include <vector>
#include <omp.h>
#include <immintrin.h>

#include "service_lapack.h"
#include "service_memory.h"
#include "service_math.h"
#include "service_defines.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"
#include "threading.h"
#include "task_scheduler_init.h"
#include "blocked_range.h"
#include "parallel_for.h"
#include "queuing_mutex.h"

#include "mf_sgd_default_impl.i"

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

template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::compute(const NumericTable** TrainSet, const NumericTable** TestSet,
                 NumericTable *r[], const daal::algorithms::Parameter *par)
{/*{{{*/

    /* retrieve members of parameter */
    const Parameter *parameter = static_cast<const Parameter *>(par);
    const int64_t dim_w = parameter->_Dim_w;
    const int64_t dim_h = parameter->_Dim_h;

    const int dim_train = TrainSet[0]->getNumberOfRows();
    const int dim_test = TestSet[0]->getNumberOfRows();

    /*  Retrieve Training Data Set */
    FeatureMicroTable<int, readOnly, cpu> workflowW_ptr(TrainSet[0]);
    FeatureMicroTable<int, readOnly, cpu> workflowH_ptr(TrainSet[0]);
    FeatureMicroTable<interm, readOnly, cpu> workflow_ptr(TrainSet[0]);

    int *workWPos = 0;
    workflowW_ptr.getBlockOfColumnValues(0, 0, dim_train, &workWPos);

    int *workHPos = 0;
    workflowH_ptr.getBlockOfColumnValues(1, 0, dim_train, &workHPos);

    interm *workV;
    workflow_ptr.getBlockOfColumnValues(2,0,dim_train,&workV);

    /*  Retrieve Test Data Set */
    FeatureMicroTable<int, readOnly, cpu> testW_ptr(TestSet[0]);
    FeatureMicroTable<int, readOnly, cpu> testH_ptr(TestSet[0]);
    FeatureMicroTable<interm, readOnly, cpu> test_ptr(TestSet[0]);

    int *testWPos = 0;
    testW_ptr.getBlockOfColumnValues(0, 0, dim_test, &testWPos);

    int *testHPos = 0;
    testH_ptr.getBlockOfColumnValues(1, 0, dim_test, &testHPos);

    interm *testV;
    test_ptr.getBlockOfColumnValues(2, 0, dim_test, &testV);

    /*  Retrieve Model W  */
    BlockMicroTable<interm, readWrite, cpu> mtWDataTable(r[0]);

    /* screen print out the size of model data */
    std::printf("model W row: %zu\n",r[0]->getNumberOfRows());
    std::fflush(stdout);
    std::printf("model W col: %zu\n",r[0]->getNumberOfColumns());
    std::fflush(stdout);

    interm* mtWDataPtr = 0;
    mtWDataTable.getBlockOfRows(0, dim_w, &mtWDataPtr);

    /*  Retrieve Model H  */
    BlockMicroTable<interm, readWrite, cpu> mtHDataTable(r[1]);

    interm* mtHDataPtr = 0;
    mtHDataTable.getBlockOfRows(0, dim_h, &mtHDataPtr);

    if (parameter->_isReorder == 1 )
    {
        /* TBB re-order */
        MF_SGDBatchKernel<interm, method, cpu>::reorder(workWPos, workHPos, workV, dim_train, parameter);
        MF_SGDBatchKernel<interm, method, cpu>::compute_thr_reordered(testWPos, testHPos, testV, dim_test, mtWDataPtr, mtHDataPtr, parameter);
        MF_SGDBatchKernel<interm, method, cpu>::free_reordered();
    }
    else if (parameter->_isReorder == 2 )
    {
        /* OpenMP re-order */
        MF_SGDBatchKernel<interm, method, cpu>::reorder(workWPos, workHPos, workV, dim_train, parameter);
        MF_SGDBatchKernel<interm, method, cpu>::compute_openmp_reordered(testWPos, testHPos, testV, dim_test, mtWDataPtr, mtHDataPtr, parameter);
        MF_SGDBatchKernel<interm, method, cpu>::free_reordered();
    }
    else if (parameter->_isReorder == 3 )
    {
        /* OpenMP no-reorder */
        MF_SGDBatchKernel<interm, method, cpu>::compute_openmp(workWPos, workHPos, workV, dim_train, testWPos, testHPos, testV, dim_test, mtWDataPtr, mtHDataPtr, parameter);
    }
    else 
    {
        /* TBB no-reorder */
        MF_SGDBatchKernel<interm, method, cpu>::compute_thr(workWPos, workHPos, workV, dim_train, testWPos, testHPos, testV, dim_test, mtWDataPtr, mtHDataPtr, parameter);
    }

}/*}}}*/

template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::reorder(int* trainWPos, int* trainHPos, interm* trainV, const int train_num, const Parameter *parameter)
{/*{{{*/

    const int length = parameter->_reorder_length;
    const int64_t dim_w = parameter->_Dim_w;
 
    services::SharedPtr<std::vector<int>* > trainHPool(new std::vector<int>*[dim_w]);
    services::SharedPtr<std::vector<interm>* > trainVPool(new std::vector<interm>*[dim_w]);

    for(int i=0;i<dim_w;i++)
    {
        (trainHPool.get())[i] = new std::vector<int>();
        (trainVPool.get())[i] = new std::vector<interm>();
    }

    services::SharedPtr<std::vector<int> > trainWQueue(new std::vector<int>());
    services::SharedPtr<std::vector<int> > trainLQueue(new std::vector<int>());
    services::SharedPtr<std::vector<int*> > trainHQueue(new std::vector<int*>());
    services::SharedPtr<std::vector<interm*> > trainVQueue(new std::vector<interm*>());

    for(int i=0;i<train_num;i++)
    {
        int w_idx = trainWPos[i];
        int h_idx = trainHPos[i];
        interm v_val = trainV[i];

        if (trainHPool.get()[w_idx]->size() == length)
        {
            /* move vector to trainWQueue */
            int* trainHVec = &(*(trainHPool.get()[w_idx]))[0];
            interm* trainVVec = &(*(trainVPool.get()[w_idx]))[0];

            trainWQueue->push_back(w_idx);
            trainLQueue->push_back(length);
            trainHQueue->push_back(trainHVec);
            trainVQueue->push_back(trainVVec);

            trainHPool.get()[w_idx] = new std::vector<int>();
            trainVPool.get()[w_idx] = new std::vector<interm>();

            /* add the new item */
            trainHPool.get()[w_idx]->push_back(h_idx);
            trainVPool.get()[w_idx]->push_back(v_val);

        }
        else
        {
            /* add the new item */
            trainHPool.get()[w_idx]->push_back(h_idx);
            trainVPool.get()[w_idx]->push_back(v_val);
        }

    }

    /* move the rest data points at pools to queue */
    for(int i=0;i<dim_w;i++)
    {
        int* trainHVec = &(*(trainHPool.get()[i]))[0];
        interm* trainVVec = &(*(trainVPool.get()[i]))[0];

        trainWQueue->push_back(i);
        trainLQueue->push_back(trainHPool.get()[i]->size());
        trainHQueue->push_back(trainHVec);
        trainVQueue->push_back(trainVVec);

    }

    std::printf("trainWQueue size: %zu\n", trainWQueue->size());
    std::fflush(stdout);

    std::printf("trainHQueue size: %zu\n", trainHQueue->size());
    std::fflush(stdout);

    std::printf("trainVQueue size: %zu\n", trainVQueue->size());
    std::fflush(stdout);

    _trainWQueue = trainWQueue;
    _trainLQueue = trainLQueue;
    _trainHQueue = trainHQueue;
    _trainVQueue = trainVQueue;

}/*}}}*/

template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::free_reordered()
{/*{{{*/

    /* free items in trainHQueue and trainVQueue */
    for(int i=0;i<_trainHQueue->size();i++)
    {
        delete (*(_trainHQueue.get()))[i]; 
        delete (*(_trainVQueue.get()))[i];
    }

}/*}}}*/

/* A multi-threading version by using TBB */
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::compute_thr_reordered(int* testWPos, int* testHPos, interm* testV, const int dim_test,
                                                                   interm* mtWDataPtr, interm* mtHDataPtr, const Parameter *parameter)
{/*{{{*/

    /* retrieve members of parameter */
    const int64_t dim_r = parameter->_Dim_r;
    const int64_t dim_w = parameter->_Dim_w;
    const int64_t dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    const int iteration = parameter->_iteration;
    const int thread_num = parameter->_thread_num;
    const int tbb_grainsize = parameter->_tbb_grainsize;
    const int Avx_explicit = parameter->_Avx_explicit;
    const size_t absent_test_num = parameter->_absent_test_num;

    const double ratio = parameter->_ratio;
    const int itr = parameter->_itr;

    /* get the reordered training data points */
    const int train_queue_size = _trainWQueue->size(); 
    int* train_queue_wPos = &(*(_trainWQueue.get()))[0];
    int* train_queue_length = &(*(_trainLQueue.get()))[0];
    int** train_queue_hPos = &(*(_trainHQueue.get()))[0];
    interm** train_queue_vVal = &(*(_trainVQueue.get()))[0];

    /* debug test the vals of reordered points */
    // for(int j=train_queue_size-1;j>train_queue_size-5;j--)
    // {
    //
    //     std::printf("Processing Row ID: %d\n", train_queue_wPos[j]);
    //     std::fflush(stdout);
    //
    //     for(int k=0;k<train_queue_length[j];k++)
    //     {
    //         std::printf("Processing Row: %d, Col: %d, Val: %f\n", train_queue_wPos[j], (train_queue_hPos[j])[k], (train_queue_vVal[j])[k]);
    //         std::fflush(stdout);
    //     }
    //
    // }


    /* create the mutex for WData and HData */
    services::SharedPtr<currentMutex_t> mutex_w(new currentMutex_t[dim_w]);
    services::SharedPtr<currentMutex_t> mutex_h(new currentMutex_t[dim_h]);

    /* RMSE value for test dataset after each iteration */
    services::SharedPtr<interm> testRMSE(new interm[dim_test]);

    interm totalRMSE = 0;

    /*  Starting TBB based Training  */
    task_scheduler_init init(task_scheduler_init::deferred);

    if (thread_num != 0)
    {
        /* use explicitly specified num of threads  */
        init.initialize(thread_num);
    }
    else
    {
        /* use automatically generated threads by TBB  */
        init.initialize();
    }

    /* if ratio != 1, dim_ratio is the ratio of computed tasks */
    // const int dim_ratio = static_cast<const int>(ratio*dim_train);

    /* step is the stride of choosing tasks in a rotated way */
    // const int step = dim_train - dim_ratio;

    /* to use affinity_partitioner */
    static affinity_partitioner ap;

    // MFSGDTBB<interm, cpu> mfsgd(mtWDataPtr, mtHDataPtr, workWPos, workHPos, workV, dim_r, learningRate, lambda, mutex_w.get(), mutex_h.get(), Avx_explicit, step, dim_train);
    MFSGDTBBREORDER<interm, cpu> mfsgd_reorder(mtWDataPtr, mtHDataPtr, train_queue_wPos, train_queue_length, train_queue_hPos,train_queue_vVal, 
            dim_r, learningRate, lambda, mutex_w.get(), mutex_h.get(), Avx_explicit);

    MFSGDTBB_TEST<interm, cpu> mfsgd_test(mtWDataPtr, mtHDataPtr, testWPos, testHPos, testV, dim_r, testRMSE.get(), mutex_w.get(), mutex_h.get(), Avx_explicit);

    /* Test MF-SGD before iteration */
    if (tbb_grainsize != 0)
    {
        /* use explicitly specified grainsize */
        parallel_for(blocked_range<int>(0, dim_test, tbb_grainsize), mfsgd_test);
    }
    else
    {
        /* use auto-partitioner by TBB */
        /* parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, auto_partitioner()); */

        /* use affinity-partitioner by TBB */
        parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, ap);

    }

    totalRMSE = 0;
    interm* testRMSE_ptr = testRMSE.get();

    for(int k=0;k<dim_test;k++)
        totalRMSE += testRMSE_ptr[k];

    totalRMSE = totalRMSE/(dim_test - absent_test_num);

    std::printf("RMSE before interation: %f\n", sqrt(totalRMSE));
    std::fflush(stdout);

    /* End of Test MF-SGD before iteration */
    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    for(int j=0;j<iteration;j++)
    {
        // mfsgd.setItr(j);
        clock_gettime(CLOCK_MONOTONIC, &ts1);

        /* training MF-SGD */
        if (tbb_grainsize != 0)
        {
            // parallel_for(blocked_range<int>(0, dim_ratio, tbb_grainsize), mfsgd);
            parallel_for(blocked_range<int>(0, train_queue_size, tbb_grainsize), mfsgd_reorder);
        }
        else
        {
            /* parallel_for(blocked_range<int>(0, dim_ratio), mfsgd, auto_partitioner()); */
            // parallel_for(blocked_range<int>(0, dim_ratio), mfsgd, ap);
            parallel_for(blocked_range<int>(0, train_queue_size), mfsgd_reorder, ap);
        }

        clock_gettime(CLOCK_MONOTONIC, &ts2);

        /* get the training time for each iteration */
        diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
	    /* train_time += (double)(diff)/1000000L; */
	    train_time += static_cast<double>((diff)/1000000L);

        /* Test MF-SGD */
        if (tbb_grainsize != 0)
        {
            parallel_for(blocked_range<int>(0, dim_test, tbb_grainsize), mfsgd_test);
        }
        else
        {
            /* parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, auto_partitioner()); */
            parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, ap);
        }

        totalRMSE = 0;
        for(int k=0;k<dim_test;k++)
            totalRMSE += testRMSE_ptr[k];

        totalRMSE = totalRMSE/(dim_test - absent_test_num);
        std::printf("RMSE after interation %d: %f, train time: %f\n", j, sqrt(totalRMSE), static_cast<double>(diff/1000000L));
        std::fflush(stdout);
    }

    init.terminate();

    std::printf("Average training time per iteration: %f, total time: %f\n", train_time/iteration, train_time);
    std::fflush(stdout);

    return;

}/*}}}*/

/* A multi-threading version by using OpenMP */
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::compute_openmp_reordered(int* testWPos, int* testHPos, interm* testV, const int dim_test,
                                                                   interm* mtWDataPtr, interm* mtHDataPtr, const Parameter *parameter)
{/*{{{*/

#ifdef _OPENMP

    /* retrieve members of parameter */
    const int64_t dim_r = parameter->_Dim_r;
    const int64_t dim_w = parameter->_Dim_w;
    const int64_t dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    const int iteration = parameter->_iteration;
    const int tbb_grainsize = parameter->_tbb_grainsize;
    const int Avx_explicit = parameter->_Avx_explicit;
    const size_t absent_test_num = parameter->_absent_test_num;

    const double ratio = parameter->_ratio;
    const int itr = parameter->_itr;

    int thread_num = parameter->_thread_num;

    /* get the reordered training data points */
    const int train_queue_size = _trainWQueue->size(); 
    int* train_queue_wPos = &(*(_trainWQueue.get()))[0];
    int* train_queue_length = &(*(_trainLQueue.get()))[0];
    int** train_queue_hPos = &(*(_trainHQueue.get()))[0];
    interm** train_queue_vVal = &(*(_trainVQueue.get()))[0];

    int num_thds_max = omp_get_max_threads();
    std::printf("Max threads number: %d\n", num_thds_max);
    std::fflush(stdout);

    if (thread_num == 0)
        thread_num = num_thds_max;

    /* RMSE value for test dataset after each iteration */
    services::SharedPtr<interm> testRMSE(new interm[dim_test]);

    interm totalRMSE = 0;

    /* Start of Test MF-SGD before iteration */
    /* create mutual locks for computing RMSE */
    services::SharedPtr<omp_lock_t> mutex_w(new omp_lock_t[dim_w]);
    services::SharedPtr<omp_lock_t> mutex_h(new omp_lock_t[dim_h]);

    omp_lock_t* mutex_w_ptr = mutex_w.get();
    for(int j=0; j<dim_w;j++)
         omp_init_lock(&(mutex_w_ptr[j]));

    omp_lock_t* mutex_h_ptr = mutex_h.get();
    for(int j=0; j<dim_h;j++)
         omp_init_lock(&(mutex_h_ptr[j]));

    #pragma omp parallel for num_threads(thread_num) 
    for(int i = 0; i<dim_test; i++)
    {

        interm *WMat = 0;
        interm *HMat = 0;

        interm* mtWDataLocal = mtWDataPtr;
        interm* mtHDataLocal = mtHDataPtr;

        interm* testVLocal = testV;

        int* testWPosLocal = testWPos;
        int* testHPosLocal = testHPos;

        long Dim = dim_r;
        interm* testRMSELocal = testRMSE.get();

        int p = 0;
        interm Mult = 0;
        interm Err = 0;

        if (testWPosLocal[i] != -1 && testHPosLocal[i] != -1)
        {

            omp_set_lock(&(mutex_w_ptr[testWPosLocal[i]]));
            omp_set_lock(&(mutex_h_ptr[testHPosLocal[i]]));

            WMat = mtWDataLocal + testWPosLocal[i]*Dim;
            HMat = mtHDataLocal + testHPosLocal[i]*Dim;
        
            for(p = 0; p<Dim; p++)
                Mult += (WMat[p]*HMat[p]);

            Err = testVLocal[i] - Mult;

            testRMSELocal[i] = Err*Err;

            omp_unset_lock(&(mutex_w_ptr[testWPosLocal[i]]));
            omp_unset_lock(&(mutex_h_ptr[testHPosLocal[i]]));

        }
        else
            testRMSELocal[i] = 0;

    }

    totalRMSE = 0;
    interm* testRMSE_ptr = testRMSE.get();

    for(int k=0;k<dim_test;k++)
        totalRMSE += testRMSE_ptr[k];

    totalRMSE = totalRMSE/(dim_test - absent_test_num);

    std::printf("RMSE before interation: %f\n", sqrt(totalRMSE));
    std::fflush(stdout);

    /* End of Test MF-SGD before iteration */
    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    for(int j=0;j<iteration;j++)
    {
        clock_gettime(CLOCK_MONOTONIC, &ts1);

        /* Start of training MF-SGD */
        /* #pragma omp parallel for schedule(dynamic, 10000) num_threads(thread_num)  */
        #pragma omp parallel for schedule(guided) num_threads(thread_num) 
        for(int k = 0; k<train_queue_size;k++)
        {

            interm* WMat = 0;
            interm* HMat = 0;
            interm* workV = 0;

            
            /* using local variables */
            interm* mtWDataLocal = mtWDataPtr;
            interm* mtHDataLocal = mtHDataPtr;

            int* queueWPos = train_queue_wPos;
            int* queueLength = train_queue_length;
            int** queueHPos = train_queue_hPos;
            interm** queueVVal = train_queue_vVal;

            long Dim = dim_r;
            interm learningRateLocal = learningRate;
            interm lambdaLocal = lambda;

            int row_id = queueWPos[k];
            WMat = mtWDataLocal + row_id*Dim;

            int qLength = queueLength[k];
            int* col_ids = queueHPos[k];
            interm* vals = queueVVal[k];

            /* #pragma prefetch vals:1:2 */
            for(int q= 0; q<qLength; q++)
            {

                interm Mult = 0;
                interm Err = 0;
                interm WMatVal = 0;
                interm HMatVal = 0;

                HMat = mtHDataLocal + col_ids[q]*Dim;

                for(int s = 0; s<Dim; s++)
                    Mult += (WMat[s]*HMat[s]);

                Err = vals[q] - Mult;

                for(int s = 0;s<Dim;s++)
                {
                    WMatVal = WMat[s];
                    HMatVal = HMat[s];

                    WMat[s] = WMatVal + learningRateLocal*(Err*HMatVal - lambdaLocal*WMatVal);
                    HMat[s] = HMatVal + learningRateLocal*(Err*WMatVal - lambdaLocal*HMatVal);

                }

            }

        }

        /* End of training MF-SGD */
        clock_gettime(CLOCK_MONOTONIC, &ts2);

        /* get the training time for each iteration */
        diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
        train_time += static_cast<double>((diff)/1000000L);

        /* Start of testing MF-SGD */
        #pragma omp parallel for num_threads(thread_num) 
        for(int i = 0; i<dim_test; i++)
        {

            interm *WMat = 0;
            interm *HMat = 0;

            interm* mtWDataLocal = mtWDataPtr;
            interm* mtHDataLocal = mtHDataPtr;

            interm* testVLocal = testV;

            int* testWPosLocal = testWPos;
            int* testHPosLocal = testHPos;

            long Dim = dim_r;
            interm* testRMSELocal = testRMSE.get();

            int p = 0;
            interm Mult = 0;
            interm Err = 0;

            if (testWPosLocal[i] != -1 && testHPosLocal[i] != -1)
            {

                omp_set_lock(&(mutex_w_ptr[testWPosLocal[i]]));
                omp_set_lock(&(mutex_h_ptr[testHPosLocal[i]]));

                WMat = mtWDataLocal + testWPosLocal[i]*Dim;
                HMat = mtHDataLocal + testHPosLocal[i]*Dim;

                for(p = 0; p<Dim; p++)
                    Mult += (WMat[p]*HMat[p]);

                Err = testVLocal[i] - Mult;

                testRMSELocal[i] = Err*Err;

                omp_unset_lock(&(mutex_w_ptr[testWPosLocal[i]]));
                omp_unset_lock(&(mutex_h_ptr[testHPosLocal[i]]));

            }
            else
                testRMSELocal[i] = 0;

        }

        /* End of testing MF-SGD */
        totalRMSE = 0;
        for(int k=0;k<dim_test;k++)
            totalRMSE += testRMSE_ptr[k];

        totalRMSE = totalRMSE/(dim_test - absent_test_num);
        std::printf("RMSE after interation %d: %f, train time: %f\n", j, sqrt(totalRMSE), static_cast<double>(diff/1000000L));
        std::fflush(stdout);
    }

    std::printf("Average training time per iteration: %f, total time: %f\n", train_time/iteration, train_time);
    std::fflush(stdout);

    /* destroy mutual locks for computing RMSE */
    for(int j=0; j<dim_w;j++)
         omp_destroy_lock(&(mutex_w_ptr[j]));

    for(int j=0; j<dim_h;j++)
         omp_destroy_lock(&(mutex_h_ptr[j]));
#else

    std::printf("Error: OpenMP module is not enabled\n");
    std::fflush(stdout);

#endif

    return;

}/*}}}*/

/* A multi-threading version by using TBB */
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::compute_thr(int* workWPos, int* workHPos, interm* workV, const int dim_train,
                                                         int* testWPos, int* testHPos, interm* testV, const int dim_test,
                                                         interm* mtWDataPtr, interm* mtHDataPtr, const Parameter *parameter)
{/*{{{*/

    /* retrieve members of parameter */
    const int64_t dim_r = parameter->_Dim_r;
    const int64_t dim_w = parameter->_Dim_w;
    const int64_t dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    const int iteration = parameter->_iteration;
    const int thread_num = parameter->_thread_num;
    const int tbb_grainsize = parameter->_tbb_grainsize;
    const int Avx_explicit = parameter->_Avx_explicit;
    const size_t absent_test_num = parameter->_absent_test_num;

    const double ratio = parameter->_ratio;
    const int itr = parameter->_itr;

    /* create the mutex for WData and HData */
    services::SharedPtr<currentMutex_t> mutex_w(new currentMutex_t[dim_w]);
    services::SharedPtr<currentMutex_t> mutex_h(new currentMutex_t[dim_h]);

    /* RMSE value for test dataset after each iteration */
    services::SharedPtr<interm> testRMSE(new interm[dim_test]);

    interm totalRMSE = 0;

    /*  Starting TBB based Training  */
    task_scheduler_init init(task_scheduler_init::deferred);

    if (thread_num != 0)
    {
        /* use explicitly specified num of threads  */
        init.initialize(thread_num);
    }
    else
    {
        /* use automatically generated threads by TBB  */
        init.initialize();
    }

    /* if ratio != 1, dim_ratio is the ratio of computed tasks */
    const int dim_ratio = static_cast<const int>(ratio*dim_train);

    /* step is the stride of choosing tasks in a rotated way */
    const int step = dim_train - dim_ratio;

    /* to use affinity_partitioner */
    static affinity_partitioner ap;

    MFSGDTBB<interm, cpu> mfsgd(mtWDataPtr, mtHDataPtr, workWPos, workHPos, workV, dim_r, learningRate, lambda, mutex_w.get(), mutex_h.get(), Avx_explicit, step, dim_train);

    MFSGDTBB_TEST<interm, cpu> mfsgd_test(mtWDataPtr, mtHDataPtr, testWPos, testHPos, testV, dim_r, testRMSE.get(), mutex_w.get(), mutex_h.get(), Avx_explicit);

    /* Test MF-SGD before iteration */
    if (tbb_grainsize != 0)
    {
        /* use explicitly specified grainsize */
        parallel_for(blocked_range<int>(0, dim_test, tbb_grainsize), mfsgd_test);
    }
    else
    {
        /* use auto-partitioner by TBB */
        /* parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, auto_partitioner()); */

        /* use affinity-partitioner by TBB */
        parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, ap);

    }

    totalRMSE = 0;
    interm* testRMSE_ptr = testRMSE.get();

    for(int k=0;k<dim_test;k++)
        totalRMSE += testRMSE_ptr[k];

    totalRMSE = totalRMSE/(dim_test - absent_test_num);

    std::printf("RMSE before interation: %f\n", sqrt(totalRMSE));
    std::fflush(stdout);

    /* End of Test MF-SGD before iteration */
    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    for(int j=0;j<iteration;j++)
    {
        mfsgd.setItr(j);
        clock_gettime(CLOCK_MONOTONIC, &ts1);

        /* training MF-SGD */
        if (tbb_grainsize != 0)
        {
            parallel_for(blocked_range<int>(0, dim_ratio, tbb_grainsize), mfsgd);
        }
        else
        {
            /* parallel_for(blocked_range<int>(0, dim_ratio), mfsgd, auto_partitioner()); */
            parallel_for(blocked_range<int>(0, dim_ratio), mfsgd, ap);
        }

        clock_gettime(CLOCK_MONOTONIC, &ts2);

        /* get the training time for each iteration */
        diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
	    /* train_time += (double)(diff)/1000000L; */
	    train_time += static_cast<double>((diff)/1000000L);

        /* Test MF-SGD */
        if (tbb_grainsize != 0)
        {
            parallel_for(blocked_range<int>(0, dim_test, tbb_grainsize), mfsgd_test);
        }
        else
        {
            /* parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, auto_partitioner()); */
            parallel_for(blocked_range<int>(0, dim_test), mfsgd_test, ap);
        }

        totalRMSE = 0;
        for(int k=0;k<dim_test;k++)
            totalRMSE += testRMSE_ptr[k];

        totalRMSE = totalRMSE/(dim_test - absent_test_num);
        std::printf("RMSE after interation %d: %f, train time: %f\n", j, sqrt(totalRMSE), static_cast<double>(diff/1000000L));
        std::fflush(stdout);
    }

    init.terminate();

    std::printf("Average training time per iteration: %f, total time: %f\n", train_time/iteration, train_time);
    std::fflush(stdout);

    return;

}/*}}}*/

/* A multi-threading version by using OpenMP */
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDBatchKernel<interm, method, cpu>::compute_openmp(int* workWPos, int* workHPos, interm* workV, const int dim_train,
                                                            int* testWPos, int* testHPos, interm* testV, const int dim_test,
                                                            interm* mtWDataPtr, interm* mtHDataPtr, const Parameter *parameter)
{/*{{{*/

#ifdef _OPENMP

    /* retrieve members of parameter */
    const int64_t dim_r = parameter->_Dim_r;
    const int64_t dim_w = parameter->_Dim_w;
    const int64_t dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    const int iteration = parameter->_iteration;
    int thread_num = parameter->_thread_num;
    const int tbb_grainsize = parameter->_tbb_grainsize;
    const int Avx_explicit = parameter->_Avx_explicit;
    const size_t absent_test_num = parameter->_absent_test_num;

    const double ratio = parameter->_ratio;
    const int itr = parameter->_itr;

    /* RMSE value for test dataset after each iteration */
    services::SharedPtr<interm> testRMSE(new interm[dim_test]);

    interm totalRMSE = 0;

    /* if ratio != 1, dim_ratio is the ratio of computed tasks */
    const int dim_ratio = static_cast<const int>(ratio*dim_train);

    /* step is the stride of choosing tasks in a rotated way */
    const int step = dim_train - dim_ratio;

    int num_thds_max = omp_get_max_threads();
    std::printf("Max threads number: %d\n", num_thds_max);
    std::fflush(stdout);

    if (thread_num == 0)
        thread_num = num_thds_max;

    /* Start of test process before first iteration of training */

    /* debug test of using OpenMP */
/*     int openmp_thd_num = 5; */
/* #ifdef _OPENMP */
/*     #pragma omp parallel for num_threads(openmp_thd_num)  */
/* #endif */
/*     for(int j = 0; j<10; j++) */
/*         printf("Test for enabling multi-threading by OpenMP iter: %d\n", j);  */

    /* create mutual locks for computing RMSE */
    services::SharedPtr<omp_lock_t> mutex_w(new omp_lock_t[dim_w]);
    services::SharedPtr<omp_lock_t> mutex_h(new omp_lock_t[dim_h]);

    omp_lock_t* mutex_w_ptr = mutex_w.get();
    for(int j=0; j<dim_w;j++)
         omp_init_lock(&(mutex_w_ptr[j]));

    omp_lock_t* mutex_h_ptr = mutex_h.get();
    for(int j=0; j<dim_h;j++)
         omp_init_lock(&(mutex_h_ptr[j]));

    #pragma omp parallel for num_threads(thread_num) 
    for(int i = 0; i<dim_test; i++)
    {

        interm *WMat = 0;
        interm *HMat = 0;

        interm* mtWDataLocal = mtWDataPtr;
        interm* mtHDataLocal = mtHDataPtr;

        interm* testVLocal = testV;

        int* testWPosLocal = testWPos;
        int* testHPosLocal = testHPos;

        long Dim = dim_r;
        interm* testRMSELocal = testRMSE.get();

        int p = 0;
        interm Mult = 0;
        interm Err = 0;

        if (testWPosLocal[i] != -1 && testHPosLocal[i] != -1)
        {

            omp_set_lock(&(mutex_w_ptr[testWPosLocal[i]]));
            omp_set_lock(&(mutex_h_ptr[testHPosLocal[i]]));

            WMat = mtWDataLocal + testWPosLocal[i]*Dim;
            HMat = mtHDataLocal + testHPosLocal[i]*Dim;
        
            for(p = 0; p<Dim; p++)
                Mult += (WMat[p]*HMat[p]);

            Err = testVLocal[i] - Mult;

            testRMSELocal[i] = Err*Err;

            omp_unset_lock(&(mutex_w_ptr[testWPosLocal[i]]));
            omp_unset_lock(&(mutex_h_ptr[testHPosLocal[i]]));

        }
        else
            testRMSELocal[i] = 0;

    }

    
    totalRMSE = 0;
    interm* testRMSE_ptr = testRMSE.get();

    for(int k=0;k<dim_test;k++)
        totalRMSE += testRMSE_ptr[k];

    totalRMSE = totalRMSE/(dim_test - absent_test_num);

    std::printf("RMSE before interation: %f\n", sqrt(totalRMSE));
    std::fflush(stdout);
    /* End of Test MF-SGD before iteration */

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    for(int j=0;j<iteration;j++)
    {
        clock_gettime(CLOCK_MONOTONIC, &ts1);

        /* start of training process by OpenMP*/
        /* #pragma omp parallel for schedule(dynamic, 10000) num_threads(thread_num)  */

        #pragma omp parallel for schedule(guided) num_threads(thread_num) 
        for(int k=0;k<dim_ratio;k++)
        {

            interm *WMat = 0;
            interm *HMat = 0;

            interm Mult = 0;
            interm Err = 0;
            interm WMatVal = 0;
            interm HMatVal = 0;
            int p = 0;

            interm* mtWDataLocal = mtWDataPtr;
            interm* mtHDataLocal = mtHDataPtr;

            int* workWPosLocal = workWPos;
            int* workHPosLocal = workHPos;
            interm* workVLocal = workV;

            long Dim = dim_r;
            interm learningRateLocal = learningRate;
            interm lambdaLocal = lambda;

            WMat = mtWDataLocal + workWPosLocal[k]*Dim;
            HMat = mtHDataLocal + workHPosLocal[k]*Dim;


            for(p = 0; p<Dim; p++)
                Mult += (WMat[p]*HMat[p]);

            /* for(p = 0; p<Dim; p++) */
            /* { */
            /*     Mult += (mtWDataLocal[workWPosLocal[k]*Dim + p]*mtHDataLocal[workHPosLocal[k]*Dim + p]); */
            /* } */

            Err = workVLocal[k] - Mult;
            /* Err = workV[k] - Mult; */

            /* #pragma prefetch WMat:1:2 */
            /* #pragma prefetch HMat:1:2 */
            for(p = 0;p<Dim;p++)
            {
                /* prefetching data by intrinsics */
                /* _mm_prefetch((const char *)&WMat[p+2],_MM_HINT_T0); */
                /* _mm_prefetch((const char *)&HMat[p+2],_MM_HINT_T0); */

                WMatVal = WMat[p];
                HMatVal = HMat[p];
                /* WMatVal = mtWDataLocal[workWPosLocal[k]*Dim + p]; */
                /* HMatVal = mtHDataLocal[workHPosLocal[k]*Dim + p]; */

                WMat[p] = WMatVal + learningRateLocal*(Err*HMatVal - lambdaLocal*WMatVal);
                HMat[p] = HMatVal + learningRateLocal*(Err*WMatVal - lambdaLocal*HMatVal);
                /* mtWDataLocal[workWPosLocal[k]*Dim + p] = WMatVal + learningRateLocal*(Err*HMatVal - lambdaLocal*WMatVal); */
                /* mtHDataLocal[workHPosLocal[k]*Dim + p] = HMatVal + learningRateLocal*(Err*WMatVal - lambdaLocal*HMatVal); */


                /* WMat[p] = WMatVal + learningRateLocal*(Err*HMatVal - lambdaLocal*WMatVal); */
                /* HMat[p] = HMatVal + learningRateLocal*(Err*WMatVal - lambdaLocal*HMatVal); */

            }


        }

        /* End of training process by OpenMP*/
        clock_gettime(CLOCK_MONOTONIC, &ts2);

        /* get the training time for each iteration */
        diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
	    train_time += static_cast<double>((diff)/1000000L);

        /* Start of Test MF-SGD by OpenMP */
        #pragma omp parallel for num_threads(thread_num) 
        for(int k = 0; k<dim_test; k++)
        {

            interm *WMat = 0;
            interm *HMat = 0;

            interm* mtWDataLocal = mtWDataPtr;
            interm* mtHDataLocal = mtHDataPtr;

            interm* testVLocal = testV;

            int* testWPosLocal = testWPos;
            int* testHPosLocal = testHPos;

            long Dim = dim_r;
            interm* testRMSELocal = testRMSE.get();

            int p = 0;
            interm Mult = 0;
            interm Err = 0;

            if (testWPosLocal[k] != -1 && testHPosLocal[k] != -1)
            {

                omp_set_lock(&(mutex_w_ptr[testWPosLocal[k]]));
                omp_set_lock(&(mutex_h_ptr[testHPosLocal[k]]));

                WMat = mtWDataLocal + testWPosLocal[k]*Dim;
                HMat = mtHDataLocal + testHPosLocal[k]*Dim;

                for(p = 0; p<Dim; p++)
                    Mult += (WMat[p]*HMat[p]);

                Err = testVLocal[k] - Mult;

                testRMSELocal[k] = Err*Err;

                omp_unset_lock(&(mutex_w_ptr[testWPosLocal[k]]));
                omp_unset_lock(&(mutex_h_ptr[testHPosLocal[k]]));

            }
            else
                testRMSELocal[k] = 0;

        }

        totalRMSE = 0;
        for(int k=0;k<dim_test;k++)
            totalRMSE += testRMSE_ptr[k];

        totalRMSE = totalRMSE/(dim_test - absent_test_num);
        std::printf("RMSE after interation %d: %f, train time: %f\n", j, sqrt(totalRMSE), static_cast<double>(diff/1000000L));
        std::fflush(stdout);

        /* End of Test MF-SGD by OpenMP */
    }

    /* destroy mutual locks for computing RMSE */
    for(int j=0; j<dim_w;j++)
         omp_destroy_lock(&(mutex_w_ptr[j]));

    for(int j=0; j<dim_h;j++)
         omp_destroy_lock(&(mutex_h_ptr[j]));

    std::printf("Average training time per iteration: %f, total time: %f\n", train_time/iteration, train_time);
    std::fflush(stdout);

#else

    std::printf("Error: OpenMP module is not enabled\n");
    std::fflush(stdout);

#endif

    return;

}/*}}}*/

} // namespace daal::internal
}
}
} // namespace daal

#endif
