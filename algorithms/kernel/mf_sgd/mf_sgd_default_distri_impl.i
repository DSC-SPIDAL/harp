/* file: mf_sgd_default_distri_impl.i */
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
//  Implementation of distributed mode mf_sgd method
//--
*/

#ifndef __MF_SGD_KERNEL_DISTRI_IMPL_I__
#define __MF_SGD_KERNEL_DISTRI_IMPL_I__

#include <time.h>
#include <math.h>       
#include <algorithm>
#include <cstdlib> 
#include <vector>
#include <iostream>
#include <cstdio> 
#include <algorithm>
#include <ctime>        
#include <omp.h>
#include <immintrin.h>

#include "service_lapack.h"
#include "service_memory.h"
#include "service_math.h"
#include "service_defines.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"

#include "threading.h"
#include "tbb/tick_count.h"
#include "task_scheduler_init.h"
#include "tbb/concurrent_hash_map.h"
#include "tbb/concurrent_vector.h"

#include "blocked_range.h"
#include "parallel_for.h"
#include "queuing_mutex.h"

#include "mf_sgd_default_impl.i"

using namespace tbb;
using namespace daal::internal;
using namespace daal::services::internal;


typedef queuing_mutex currentMutex_t;
typedef tbb::concurrent_hash_map<int, int> ConcurrentModelMap;
typedef tbb::concurrent_hash_map<int, std::vector<int> > ConcurrentDataMap;

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
namespace internal
{
    
/**
 * @brief compute MF-SGD in distributed mode
 * 1) computation of training process
 * 2) computation of test process
 * controlled by par->_isTrain flag
 *
 * @tparam interm
 * @tparam method
 * @tparam cpu
 * @param WPos
 * @param HPos
 * @param Val
 * @param WPosTest
 * @param HPosTest
 * @param ValTest
 * @param r[]
 * @param parameter
 * @param col_ids
 * @param hMat_native_mem
 */
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
daal::services::interface1::Status MF_SGDDistriKernel<interm, method, cpu>::compute(NumericTable** WPos, 
                                                      NumericTable** HPos, 
                                                      NumericTable** Val, 
                                                      NumericTable** WPosTest,
                                                      NumericTable** HPosTest, 
                                                      NumericTable** ValTest, 
                                                      NumericTable *r[], Parameter* &parameter, long* &col_ids,
                                                      interm** &hMat_native_mem)
{/*{{{*/

    services::Status status;

    //flag to trigger training or testing process
    const int isTrain = parameter->_isTrain;

    //feature dimension of model data
    const int dim_r = parameter->_Dim_r;

    if (isTrain == 1)
    {/*{{{*/

        /* dim_set is the number of training points */
        FeatureMicroTable<int, readOnly, cpu> workflowW_ptr(WPos[0]);
        FeatureMicroTable<int, readOnly, cpu> workflowH_ptr(HPos[0]);
        FeatureMicroTable<interm, readOnly, cpu> workflow_ptr(Val[0]);

        size_t dim_set =  Val[0]->getNumberOfRows();

        int *workWPos = 0;
        workflowW_ptr.getBlockOfColumnValues(0, 0, dim_set, &workWPos);

        int *workHPos = 0;
        workflowH_ptr.getBlockOfColumnValues(0, 0, dim_set, &workHPos);

        interm *workV;
        workflow_ptr.getBlockOfColumnValues(0, 0, dim_set, &workV);

        /* ---------------- Retrieve Model W ---------------- */
        //row size of model W matrix
        long dim_w = r[2]->getNumberOfRows();
        assert(dim_w == parameter->_Dim_w);

        BlockMicroTable<interm, readWrite, cpu> mtWDataTable(r[2]);

        interm* mtWDataPtr = 0;
        mtWDataTable.getBlockOfRows(0, dim_w, &mtWDataPtr);

        // Model H is stored in hMat_native_mem
    #ifdef USE_OMP
        MF_SGDDistriKernel<interm, method, cpu>::compute_train_omp(workWPos,workHPos,workV, dim_set, mtWDataPtr, col_ids, hMat_native_mem,  parameter ); 
    #else
        MF_SGDDistriKernel<interm, method, cpu>::compute_train_tbb(workWPos,workHPos,workV, dim_set, mtWDataPtr, col_ids, hMat_native_mem,  parameter ); 
    #endif


    }/*}}}*/
    else
    {/*{{{*/

        // dim_set is the number of training points 
        FeatureMicroTable<int, readOnly, cpu> workflowW_ptr(WPosTest[0]);
        FeatureMicroTable<int, readOnly, cpu> workflowH_ptr(HPosTest[0]);
        FeatureMicroTable<interm, readOnly, cpu> workflow_ptr(ValTest[0]);

        size_t dim_set =  ValTest[0]->getNumberOfRows();

        int *workWPos = 0;
        workflowW_ptr.getBlockOfColumnValues(0, 0, dim_set, &workWPos);

        int *workHPos = 0;
        workflowH_ptr.getBlockOfColumnValues(0, 0, dim_set, &workHPos);

        interm *workV;
        workflow_ptr.getBlockOfColumnValues(0, 0, dim_set, &workV);

        size_t dim_w = r[2]->getNumberOfRows();
        assert(dim_w == parameter->_Dim_w);

        // ---------------- Retrieve Model W ---------------- 
        BlockMicroTable<interm, readWrite, cpu> mtWDataTable(r[2]);
        interm* mtWDataPtr = 0;
        mtWDataTable.getBlockOfRows(0, dim_w, &mtWDataPtr);

        interm* mtRMSEPtr = 0;
        BlockMicroTable<interm, readWrite, cpu> mtRMSETable(r[3]);
        mtRMSETable.getBlockOfRows(0, 1, &mtRMSEPtr);

#ifdef USE_OMP
        MF_SGDDistriKernel<interm, method, cpu>::compute_test_omp(workWPos,workHPos,workV, dim_set, mtWDataPtr, mtRMSEPtr, parameter, col_ids, hMat_native_mem);
#else
        MF_SGDDistriKernel<interm, method, cpu>::compute_test_tbb(workWPos,workHPos,workV, dim_set, mtWDataPtr, mtRMSEPtr, parameter, col_ids, hMat_native_mem);
#endif

    }/*}}}*/

    return status;

}/*}}}*/


template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDDistriKernel<interm, method, cpu>::compute_train_omp(int* &workWPos, 
                                                                int* &workHPos, 
                                                                interm* &workV, 
                                                                const int dim_set,
                                                                interm* &mtWDataPtr, 
                                                                long* &col_ids,
                                                                interm** &hMat_native_mem,
                                                                Parameter* &parameter)
{/*{{{*/

#ifdef USE_OMP

    // retrieve members of parameter 
    const int dim_r = parameter->_Dim_r;
    const long dim_w = parameter->_Dim_w;
    const long dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    int thread_num = parameter->_thread_num;

    // ------------------- Starting OpenMP based Training  -------------------
    if (thread_num == 0)
        thread_num = omp_get_max_threads();

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    // shared vars 
    // ConcurrentDataMap* map_train = parameter->_train_map;
    ConcurrentModelMap* map_h = parameter->_hMat_map;

    //store the col pos of each sub-task queue
    std::vector<int>* task_queue_colPos = new std::vector<int>();

    //store the size of each sub-task queue
    std::vector<int>* task_queue_size = new std::vector<int>();

    //store the pointer to each sub-task queue
    std::vector<int*>* task_queue_ids = new std::vector<int*>();

    const int tasks_queue_len = 100;

    //loop over all the columns from local training points
    int col_id = 0;
    int col_pos = 0;
    int sub_len = 0;
    int iterator = 0;
    int residue = 0;
    for(int k=0;k<parameter->_train_list_len;k++)
    {
        col_id = parameter->_train_list_ids[k];
        col_pos = -1;

        //check if this local col appears in the rotated columns
        ConcurrentModelMap::accessor pos_h; 
        if (map_h->find(pos_h, col_id))
        {
            col_pos = pos_h->second;
            pos_h.release();
        }
        else
        {
            pos_h.release();
            continue;
        }

        sub_len = (parameter->_train_sub_len)[k];

        if (sub_len > 0)
        {
            iterator = 0; 
            while (((iterator+1)*tasks_queue_len) <= sub_len)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(tasks_queue_len);
                task_queue_ids->push_back(&((parameter->_train_list[k])[iterator*tasks_queue_len]));
                iterator++;
            }

            //add the last sub task queue
            residue = sub_len - iterator*tasks_queue_len;
            if (residue > 0)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(residue);
                task_queue_ids->push_back(&((parameter->_train_list[k])[iterator*tasks_queue_len]));
            }
        }

    }

    int task_queues_num = (int)task_queue_ids->size();
    int* queue_cols_ptr = &(*task_queue_colPos)[0];
    int* queue_size_ptr = &(*task_queue_size)[0];
    int** queue_ids_ptr = &(*task_queue_ids)[0];

    // std::printf("Col num: %ld, Tasks num: %d\n", dim_h, task_queues_num);
    // std::fflush(stdout);

    long* partialTrainedNumV = (long*)calloc(task_queues_num, sizeof(long));
    long totalTrainedNumV = 0;
    tbb::tick_count timeStart = tbb::tick_count::now();
    
    // convert from milliseconds to seconds 
    double timeout = (parameter->_timeout)/1000; 

    //shuffle the tasks
    int* execute_seq = (int*)calloc(task_queues_num, sizeof(int));
    for(int k=0;k<task_queues_num;k++)
        execute_seq[k] = k;

    std::srand(time(0));
    std::random_shuffle(&(execute_seq[0]), &(execute_seq[task_queues_num]));

    //start the training loop
    clock_gettime(CLOCK_MONOTONIC, &ts1);
    #pragma omp parallel for schedule(guided) num_threads(thread_num) 
    for(int k=0;k<task_queues_num;k++)
    {
        //get local pointers
        int* workWPosLocal = workWPos; 
        int* workHPosLocal = workHPos; 
        interm* workVLocal = workV; 
        // after shuffling
        int* execution_order = execute_seq;

        int task_id =  execution_order[k];
        // convert shared var to local var
        double timeoutLocal = timeout;

        interm *WMat = 0; 
        interm HMat[dim_r];

        interm* mtWDataLocal = mtWDataPtr;
        interm** mtHDataLocal = hMat_native_mem;   

        size_t stride_w = dim_r;
        // h matrix has a sentinel as the first element of each row
        // size_t stride_h = dim_r + 1;  
        size_t stride_h = dim_r;  

        interm learningRateLocal = learningRate;
        interm lambdaLocal = lambda;

        int col_pos = queue_cols_ptr[task_id];
        int squeue_size = queue_size_ptr[task_id];
        int* ids_ptr = queue_ids_ptr[task_id];

        // ------- start the training process -----------
        // check the timer set up
        if ( (timeoutLocal == 0) || ((tbb::tick_count::now() - timeStart).seconds() < timeoutLocal) ) 
        {
            //data copy 
            //mtHDataLocal contains a first sentinal element of col id
            //sentinal deprecated
            memcpy(HMat, mtHDataLocal[col_pos], dim_r*sizeof(interm));

            for(int j=0;j<squeue_size;j++)
            {
                int data_id = ids_ptr[j];
                size_t row_pos = workWPosLocal[data_id];
                WMat = mtWDataLocal + row_pos*stride_w;
                //use avx intrinsics
                updateMF_explicit<interm, cpu>(WMat, HMat, workVLocal, data_id, dim_r, learningRateLocal, lambdaLocal);
            }

            partialTrainedNumV[task_id] = squeue_size;
            //data copy 
            //mtHDataLocal contains a first sentinal element of col id
            //sentinal deprecated
            // memcpy(mtHDataLocal[col_pos]+1, HMat, dim_r*sizeof(interm));
            memcpy(mtHDataLocal[col_pos], HMat, dim_r*sizeof(interm));
        }

    }

    //reduce the trained num of points
    for(int k=0;k<task_queues_num;k++)
        totalTrainedNumV += partialTrainedNumV[k];

    parameter->_trainedNumV = totalTrainedNumV;

    delete task_queue_colPos;
    delete task_queue_size;
    delete task_queue_ids;
    free(execute_seq);
    free(partialTrainedNumV);

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    train_time = (double)(diff)/1000000L;

    parameter->_compute_task_time += (size_t)train_time; 

    std::printf("Training time this iteration: %f\n", train_time);
    std::fflush(stdout);

#else

    std::printf("Error: OpenMP module is not enabled\n");
    std::fflush(stdout);

#endif

    return;

}/*}}}*/

// implementation of a TBB version of train step
template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDDistriKernel<interm, method, cpu>::compute_train_tbb(int* &workWPos, 
                                                                int* &workHPos, 
                                                                interm* &workV, 
                                                                const int dim_set,
                                                                interm* &mtWDataPtr, 
                                                                long* &col_ids,
                                                                interm** &hMat_native_mem,
                                                                Parameter* &parameter)
{/*{{{*/

    // retrieve members of parameter 
    const int dim_r = parameter->_Dim_r;
    const long dim_w = parameter->_Dim_w;
    const long dim_h = parameter->_Dim_h;
    const double learningRate = parameter->_learningRate;
    const double lambda = parameter->_lambda;
    int thread_num = parameter->_thread_num;

    struct timespec ts1;
    struct timespec ts2;
    int64_t diff = 0;
    double train_time = 0;

    // shared vars 
    ConcurrentModelMap* map_h = parameter->_hMat_map;

    //store the col pos of each sub-task queue
    std::vector<int>* task_queue_colPos = new std::vector<int>();

    //store the size of each sub-task queue
    std::vector<int>* task_queue_size = new std::vector<int>();

    //store the pointer to each sub-task queue
    std::vector<int*>* task_queue_ids = new std::vector<int*>();

    const int tasks_queue_len = 100;

    //loop over all the columns from local training points
    int col_id = 0;
    int col_pos = 0;
    int sub_len = 0;
    int iterator = 0;
    int residue = 0;
    for(int k=0;k<parameter->_train_list_len;k++)
    {
        col_id = parameter->_train_list_ids[k];
        col_pos = -1;

        //check if this local col appears in the rotated columns
        ConcurrentModelMap::accessor pos_h; 
        if (map_h->find(pos_h, col_id))
        {
            col_pos = pos_h->second;
            pos_h.release();
        }
        else
        {
            pos_h.release();
            continue;
        }

        sub_len = (parameter->_train_sub_len)[k];

        if (sub_len > 0)
        {
            iterator = 0; 
            while (((iterator+1)*tasks_queue_len) <= sub_len)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(tasks_queue_len);
                task_queue_ids->push_back(&((parameter->_train_list[k])[iterator*tasks_queue_len]));
                iterator++;
            }

            //add the last sub task queue
            residue = sub_len - iterator*tasks_queue_len;
            if (residue > 0)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(residue);
                task_queue_ids->push_back(&((parameter->_train_list[k])[iterator*tasks_queue_len]));
            }
        }

    }

    int task_queues_num = (int)task_queue_ids->size();
    int* queue_cols_ptr = &(*task_queue_colPos)[0];
    int* queue_size_ptr = &(*task_queue_size)[0];
    int** queue_ids_ptr = &(*task_queue_ids)[0];

    long* partialTrainedNumV = (long*)calloc(task_queues_num, sizeof(long));
    long totalTrainedNumV = 0;
    tbb::tick_count timeStart = tbb::tick_count::now();

    // convert from milliseconds to seconds 
    double timeout = (parameter->_timeout)/1000; 

    //shuffle the tasks
    int* execute_seq = (int*)calloc(task_queues_num, sizeof(int));
    for(int k=0;k<task_queues_num;k++)
        execute_seq[k] = k;

    std::srand(time(0));
    std::random_shuffle(&(execute_seq[0]), &(execute_seq[task_queues_num]));

    //start the training loop
    clock_gettime(CLOCK_MONOTONIC, &ts1);
    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();

    //enabling thread pinning for TBB
    services::Environment::getInstance()->enableThreadPinning(true);

    SafeStatus safeStat;
    daal::threader_for(task_queues_num, task_queues_num, [=, &safeStat](int k)
    {

            //get local pointers
            int* workWPosLocal = workWPos; 
            int* workHPosLocal = workHPos; 
            interm* workVLocal = workV; 
            // after shuffling
            int* execution_order = execute_seq;

            int task_id =  execution_order[k];
            // convert shared var to local var
            double timeoutLocal = timeout;

            interm *WMat = 0; 
            interm HMat[dim_r];

            interm* mtWDataLocal = mtWDataPtr;
            interm** mtHDataLocal = hMat_native_mem;   

            size_t stride_w = dim_r;
            // h matrix has a sentinel as the first element of each row
            // sentinal deprecated
            size_t stride_h = dim_r;  

            interm learningRateLocal = learningRate;
            interm lambdaLocal = lambda;

            int col_pos = queue_cols_ptr[task_id];
            int squeue_size = queue_size_ptr[task_id];
            int* ids_ptr = queue_ids_ptr[task_id];

            // ------- start the training process -----------
            // check the timer set up
            if ( (timeoutLocal == 0) || ((tbb::tick_count::now() - timeStart).seconds() < timeoutLocal) ) 
            {
                //data copy 
                //mtHDataLocal contains a first sentinal element of col id
                //sentinal deprecated
                // memcpy(HMat, (mtHDataLocal[col_pos]+1), dim_r*sizeof(interm));
                memcpy(HMat, mtHDataLocal[col_pos], dim_r*sizeof(interm));

                for(int j=0;j<squeue_size;j++)
                {
                    int data_id = ids_ptr[j];
                    size_t row_pos = workWPosLocal[data_id];

                    WMat = mtWDataLocal + row_pos*stride_w;
                    //use avx intrinsics
                    updateMF_explicit<interm, cpu>(WMat, HMat, workVLocal, data_id, dim_r, learningRateLocal, lambdaLocal);

                }

                partialTrainedNumV[task_id] = squeue_size;
                //data copy 
                //mtHDataLocal contains a first sentinal element of col id
                //sentinal deprecated
                // memcpy(mtHDataLocal[col_pos]+1, HMat, dim_r*sizeof(interm));
                memcpy(mtHDataLocal[col_pos], HMat, dim_r*sizeof(interm));
            }
    });

    safeStat.detach();

    services::Environment::getInstance()->enableThreadPinning(false);

    //reduce the trained num of points
    for(int k=0;k<task_queues_num;k++)
        totalTrainedNumV += partialTrainedNumV[k];

    parameter->_trainedNumV = totalTrainedNumV;

    delete task_queue_colPos;
    delete task_queue_size;
    delete task_queue_ids;
    free(execute_seq);
    free(partialTrainedNumV);

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    train_time = (double)(diff)/1000000L;

    parameter->_compute_task_time += (size_t)train_time; 

    std::printf("Training time this iteration: %f\n", train_time);
    std::fflush(stdout);

    return;

}/*}}}*/

template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDDistriKernel<interm, method, cpu>::compute_test_omp(int* workWPos, 
                                                               int* workHPos, 
                                                               interm* workV, 
                                                               const int dim_set,
                                                               interm* mtWDataPtr, 
                                                               interm* mtRMSEPtr,
                                                               Parameter *parameter,
                                                               long* col_ids,
                                                               interm** hMat_native_mem)

{/*{{{*/

#ifdef USE_OMP

    /* retrieve members of parameter */
    const int dim_r = parameter->_Dim_r;
    const long dim_w = parameter->_Dim_w;
    const long dim_h = parameter->_Dim_h;
    int thread_num = parameter->_thread_num;

    //create tbb mutex
    currentMutex_t* mutex_w = new currentMutex_t[dim_w];
    currentMutex_t* mutex_h = new currentMutex_t[dim_h];

    // ------------------- Starting OpenMP based testing  -------------------
    if (thread_num == 0)
        thread_num = omp_get_max_threads();

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double test_time = 0;

    clock_gettime(CLOCK_MONOTONIC, &ts1);

    // shared vars 
    ConcurrentModelMap* map_h = parameter->_hMat_map;

    //store the col pos of each sub-task queue
    std::vector<int>* task_queue_colPos = new std::vector<int>();

    //store the size of each sub-task queue
    std::vector<int>* task_queue_size = new std::vector<int>();

    //store the pointer to each sub-task queue
    std::vector<int*>* task_queue_ids = new std::vector<int*>();
    
    const int tasks_queue_len = 20;

    //loop over all the columns from local test points 
    int sub_len = 0;
    int iterator = 0;
    int residue = 0;
    for(int k=0;k<parameter->_test_list_len;k++)
    {
        int col_id = parameter->_test_list_ids[k];
        int col_pos = -1;

        //check if this local col appears in the rotated columns
        ConcurrentModelMap::accessor pos_h; 
        if (map_h->find(pos_h, col_id))
        {
            col_pos = pos_h->second;
            pos_h.release();
        }
        else
        {
            pos_h.release();
            continue;
        }

        sub_len = (parameter->_test_sub_len)[k];

        if (sub_len > 0 && col_pos != -1)
        {
            iterator = 0; 
            while (((iterator+1)*tasks_queue_len) <= sub_len)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(tasks_queue_len);
                task_queue_ids->push_back(&((parameter->_test_list[k])[iterator*tasks_queue_len]));
                iterator++;
            }

            //add the last sub task queue
            residue = sub_len - iterator*tasks_queue_len;
            if (residue > 0)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(residue);
                task_queue_ids->push_back(&((parameter->_test_list[k])[iterator*tasks_queue_len]));
            }
        }

    }

    int task_queues_num = (int)task_queue_ids->size();
    int* queue_cols_ptr = &(*task_queue_colPos)[0];
    int* queue_size_ptr = &(*task_queue_size)[0];
    int** queue_ids_ptr = &(*task_queue_ids)[0];

    // std::printf("Col num: %ld, Test Tasks num: %d\n", dim_h, task_queues_num);
    // std::fflush(stdout);

    //RMSE value from computed points
    interm totalRMSE = 0;
    
    //the effective computed num of test V points
    int totalTestV = 0;

    //an array to record partial rmse values
    interm* partialRMSE = (interm*)calloc(task_queues_num, sizeof(interm));
    int* partialTestV = (int*)calloc(task_queues_num, sizeof(int));

    #pragma omp parallel for schedule(guided) num_threads(thread_num) 
    for(int k=0;k<task_queues_num;k++)
    {
        int* workWPosLocal = workWPos; 
        int* workHPosLocal = workHPos; 
        interm* workVLocal = workV; 

        interm *WMat = 0; 
        interm HMat[dim_r];

        interm Mult = 0;
        interm Err = 0;
        interm WMatVal = 0;
        interm HMatVal = 0;
        int p = 0;
        interm rmse = 0;
        int testV = 0;

        interm* mtWDataLocal = mtWDataPtr;
        interm** mtHDataLocal = hMat_native_mem;  

        size_t stride_w = dim_r;
        // h matrix has a sentinel as the first element of each row
        // sentinal deprecated
        // int stride_h = dim_r + 1;  
        int stride_h = dim_r;  

        int col_pos = queue_cols_ptr[k];
        int squeue_size = queue_size_ptr[k];
        int* ids_ptr = queue_ids_ptr[k];
        
        if (col_pos >=0 && col_pos < dim_h)
        {
            //---------- copy hmat data ---------------
            currentMutex_t::scoped_lock lock_h(mutex_h[col_pos]);
            //attention to the first sentinel element
            //sentinal deprecated
            // memcpy(HMat, mtHDataLocal[col_pos]+1, dim_r*sizeof(interm));
            memcpy(HMat, mtHDataLocal[col_pos], dim_r*sizeof(interm));
            lock_h.release();

            for(int j=0;j<squeue_size;j++)
            {
                int data_id = ids_ptr[j];
                size_t row_pos = workWPosLocal[data_id];
                if (row_pos < 0 || row_pos >= dim_w)
                    continue;

                Mult = 0;

                currentMutex_t::scoped_lock lock_w(mutex_w[row_pos]);
                WMat = mtWDataLocal + row_pos*stride_w;

                for(p = 0; p<dim_r; p++)
                    Mult += (WMat[p]*HMat[p]);

                lock_w.release();

                Err = workVLocal[data_id] - Mult;

                rmse += (Err*Err);
                testV++;

            }

            partialRMSE[k] = rmse;
            partialTestV[k] = testV;

        }

    }

    //sum up the rmse and testV values
    for(int k=0;k<task_queues_num;k++)
    {
        totalRMSE += partialRMSE[k];
        totalTestV += partialTestV[k];
    }

    delete task_queue_colPos;
    delete task_queue_size;
    delete task_queue_ids;
    free(partialRMSE);
    free(partialTestV);

    delete[] mutex_w;
    delete[] mutex_h;

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    test_time += (double)(diff)/1000000L;

    mtRMSEPtr[0] = totalRMSE;

    parameter->setTestV(totalTestV);

    std::printf("local RMSE value: %f, test time: %f\n", totalRMSE, test_time);
    std::fflush(stdout);

   
#else

    std::printf("Error: OpenMP module is not enabled\n");
    std::fflush(stdout);

#endif

    return;

}/*}}}*/

template <typename interm, daal::algorithms::mf_sgd::Method method, CpuType cpu>
void MF_SGDDistriKernel<interm, method, cpu>::compute_test_tbb(int* workWPos, 
                                                               int* workHPos, 
                                                               interm* workV, 
                                                               const int dim_set,
                                                               interm* mtWDataPtr, 
                                                               interm* mtRMSEPtr,
                                                               Parameter *parameter,
                                                               long* col_ids,
                                                               interm** hMat_native_mem)

{/*{{{*/

    /* retrieve members of parameter */
    const int dim_r = parameter->_Dim_r;
    const long dim_w = parameter->_Dim_w;
    const long dim_h = parameter->_Dim_h;
    int thread_num = parameter->_thread_num;

    //create tbb mutex
    currentMutex_t* mutex_w = new currentMutex_t[dim_w];
    currentMutex_t* mutex_h = new currentMutex_t[dim_h];

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double test_time = 0;

    clock_gettime(CLOCK_MONOTONIC, &ts1);

    // shared vars 
    ConcurrentModelMap* map_h = parameter->_hMat_map;

    //store the col pos of each sub-task queue
    std::vector<int>* task_queue_colPos = new std::vector<int>();

    //store the size of each sub-task queue
    std::vector<int>* task_queue_size = new std::vector<int>();

    //store the pointer to each sub-task queue
    std::vector<int*>* task_queue_ids = new std::vector<int*>();
    
    const int tasks_queue_len = 20;

    //loop over all the columns from local test points 
    int sub_len = 0;
    int iterator = 0;
    int residue = 0;
    for(int k=0;k<parameter->_test_list_len;k++)
    {
        int col_id = parameter->_test_list_ids[k];
        int col_pos = -1;

        //check if this local col appears in the rotated columns
        ConcurrentModelMap::accessor pos_h; 
        if (map_h->find(pos_h, col_id))
        {
            col_pos = pos_h->second;
            pos_h.release();
        }
        else
        {
            pos_h.release();
            continue;
        }

        sub_len = (parameter->_test_sub_len)[k];

        if (sub_len > 0 && col_pos != -1)
        {
            iterator = 0; 
            while (((iterator+1)*tasks_queue_len) <= sub_len)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(tasks_queue_len);
                task_queue_ids->push_back(&((parameter->_test_list[k])[iterator*tasks_queue_len]));
                iterator++;
            }

            //add the last sub task queue
            residue = sub_len - iterator*tasks_queue_len;
            if (residue > 0)
            {
                task_queue_colPos->push_back(col_pos);
                task_queue_size->push_back(residue);
                task_queue_ids->push_back(&((parameter->_test_list[k])[iterator*tasks_queue_len]));
            }
        }

    }

    int task_queues_num = (int)task_queue_ids->size();
    int* queue_cols_ptr = &(*task_queue_colPos)[0];
    int* queue_size_ptr = &(*task_queue_size)[0];
    int** queue_ids_ptr = &(*task_queue_ids)[0];

    //RMSE value from computed points
    interm totalRMSE = 0;
    
    //the effective computed num of test V points
    int totalTestV = 0;

    //an array to record partial rmse values
    interm* partialRMSE = (interm*)calloc(task_queues_num, sizeof(interm));
    int* partialTestV = (int*)calloc(task_queues_num, sizeof(int));

    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();

    //enabling thread pinning for TBB
    services::Environment::getInstance()->enableThreadPinning(true);

    SafeStatus safeStat;
    daal::threader_for(task_queues_num, task_queues_num, [&](int k)
    {
        int* workWPosLocal = workWPos; 
        int* workHPosLocal = workHPos; 
        interm* workVLocal = workV; 

        interm *WMat = 0; 
        interm HMat[dim_r];

        interm Mult = 0;
        interm Err = 0;
        interm WMatVal = 0;
        interm HMatVal = 0;
        int p = 0;
        interm rmse = 0;
        int testV = 0;

        interm* mtWDataLocal = mtWDataPtr;
        interm** mtHDataLocal = hMat_native_mem;  

        size_t stride_w = dim_r;
        // h matrix has a sentinel as the first element of each row
        // sentinal deprecated
        // int stride_h = dim_r + 1;  
        int stride_h = dim_r;  

        int col_pos = queue_cols_ptr[k];
        int squeue_size = queue_size_ptr[k];
        int* ids_ptr = queue_ids_ptr[k];
        
        if (col_pos >=0 && col_pos < dim_h)
        {
            //---------- copy hmat data ---------------
            currentMutex_t::scoped_lock lock_h(mutex_h[col_pos]);
            //attention to the first sentinel element
            //sentinal deprecated
            // memcpy(HMat, mtHDataLocal[col_pos]+1, dim_r*sizeof(interm));
            memcpy(HMat, mtHDataLocal[col_pos], dim_r*sizeof(interm));
            lock_h.release();

            for(int j=0;j<squeue_size;j++)
            {
                int data_id = ids_ptr[j];
                size_t row_pos = workWPosLocal[data_id];
                if (row_pos < 0 || row_pos >= dim_w)
                    continue;

                Mult = 0;

                currentMutex_t::scoped_lock lock_w(mutex_w[row_pos]);
                WMat = mtWDataLocal + row_pos*stride_w;

                for(p = 0; p<dim_r; p++)
                    Mult += (WMat[p]*HMat[p]);

                lock_w.release();

                Err = workVLocal[data_id] - Mult;

                rmse += (Err*Err);
                testV++;

            }

            partialRMSE[k] = rmse;
            partialTestV[k] = testV;

        }

    });

    safeStat.detach();
    services::Environment::getInstance()->enableThreadPinning(false);

    //sum up the rmse and testV values
    for(int k=0;k<task_queues_num;k++)
    {
        totalRMSE += partialRMSE[k];
        totalTestV += partialTestV[k];
    }

    delete task_queue_colPos;
    delete task_queue_size;
    delete task_queue_ids;
    free(partialRMSE);
    free(partialTestV);

    delete[] mutex_w;
    delete[] mutex_h;

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    test_time += (double)(diff)/1000000L;

    mtRMSEPtr[0] = totalRMSE;

    parameter->setTestV(totalTestV);

    std::printf("local RMSE value: %f, test time: %f\n", totalRMSE, test_time);
    std::fflush(stdout);

    return;

}/*}}}*/


} // namespace daal::internal
}
}
} // namespace daal

#endif
