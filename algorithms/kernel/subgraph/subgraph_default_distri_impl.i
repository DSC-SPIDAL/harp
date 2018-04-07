/* file: subgraph_default_distri_impl.i */
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
//  Implementation of distributed mode subgraph method
//--
*/

#ifndef __SUBGRAPH_KERNEL_DISTRI_IMPL_I__
#define __SUBGRAPH_KERNEL_DISTRI_IMPL_I__

#include <time.h>
#include <math.h>       
#include <algorithm>
#include <string>
#include <cstring>
#include <cstdlib> 
#include <vector>
#include <iostream>
#include <cstdio> 
#include <algorithm>
#include <ctime>        
#include <omp.h>
#include <immintrin.h>
#include <stdlib.h>
#include <cmath>

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
#include "tbb/queuing_mutex.h"

#include "blocked_range.h"
#include "parallel_for.h"

#include "subgraph_default_impl.i"
#include "service_thread_pinner.h"
#include "harp_numeric_table.h"

using namespace tbb;
using namespace daal::internal;
using namespace daal::services::internal;
using namespace daal::algorithms::subgraph::interface1;

namespace daal
{
namespace algorithms
{
namespace subgraph
{
namespace internal
{
    
const double overflow_val = 1.0e+30;
//memsure the RSS mem used by this process


template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
daal::services::interface1::Status subgraphDistriKernel<interm, method, cpu>::compute(Parameter* &par, Input* &input)
{
    services::Status status;
    int stage = par->_stage;
 
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();
    int color_num = input->getColorNum();
    int s = par->_sub_itr;
    int cur_sub_vert = part->get_num_verts_sub(s);
    int cur_comb_num = input->choose_table[color_num][cur_sub_vert];
 
    if (stage == 0)
    {
#ifdef USE_OMP
        computeBottom(par, input);
#else
        computeBottomTBB(par, input);
#endif
    }
    else if (stage == 1)
    {
#ifdef USE_OMP
        computeNonBottomNbrSplit(par, input);
#else
        computeNonBottomNbrSplitTBB(par, input);
#endif
    }
    else if (stage == 2)
    {
#ifdef USE_OMP
        updateRemoteCountsNbrSplit(par, input);
#else
        updateRemoteCountsNbrSplitTBB(par, input);
#endif
    }
    else if (stage == 3)
    {
#ifdef USE_OMP
        updateRemoteCountsPipNbrSplit(par, input);
#else
        updateRemoteCountsPipNbrSplitTBB(par, input);
#endif

    }
    else
        ;   

    return status;
}

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::computeBottom(Parameter* &par, Input* &input)
{/*{{{*/

#ifdef USE_OMP
    std::printf("Start Distrikernel compute bottom\n");
    std::fflush;

    std::string omp_schel = par->_omp_schedule;
    std::printf("Use omp schedule: %s\n", omp_schel.c_str());
    std::fflush;

    //setup omp affinity and scheduling policy
    int set_flag_affinity = -1;
    int set_flag_schedule = -1;

    if (par->_affinity == 1)
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=core,scatter",1);
    else
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=fine,compact",1);

    set_flag_schedule = setenv("OMP_SCHEDULE", omp_schel.c_str(), 1);

    if (set_flag_affinity == 0)
    {
        std::printf("omp affinity bind successful\n");
        std::fflush;
    }

    if (set_flag_schedule == 0)
    {
        std::printf("omp schedule setup successful\n");
        std::fflush;
    }

    int* colors = input->getColorsG();

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int** comb_idx_set = input->comb_num_indexes_set;
    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int s = par->_sub_itr;

    //start omp function
    if (thread_num == 0)
        thread_num = omp_get_max_threads();

    #pragma omp parallel for schedule(guided) num_threads(thread_num) 
    for(int v=0;v<num_vert_g;v++)
    {
        int n = colors[v];
        dt->set(v, comb_idx_set[s][n], 1.0);
    }

    std::printf("Finish Distrikernel compute bottom\n");
    std::fflush;

#else
#endif

}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::computeBottomTBB(Parameter* &par, Input* &input)
{/*{{{*/

    std::printf("Start Distrikernel compute bottom\n");
    std::fflush;

    services::Environment::getInstance()->enableThreadPinning(true);

    int* colors = input->getColorsG();

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int** comb_idx_set = input->comb_num_indexes_set;
    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int s = par->_sub_itr;

    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();

    SafeStatus safeStat;
    daal::threader_for(num_vert_g, num_vert_g, [&](int v)
    {
        int n = colors[v];
        dt->set(v, comb_idx_set[s][n], 1.0);
    });
    safeStat.detach();

    std::printf("Finish Distrikernel compute bottom\n");
    services::Environment::getInstance()->enableThreadPinning(false);
    std::fflush;

}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::computeNonBottomNbrSplit(Parameter* &par, Input* &input)
{/*{{{*/

#ifdef USE_OMP
    std::printf("Start Distrikernel compute nonlast\n");
    std::fflush;

    std::string omp_schel = par->_omp_schedule;
    std::printf("Use omp schedule: %s\n", omp_schel.c_str());
    std::fflush;

    //setup omp affinity and scheduling policy
    int set_flag_affinity = -1;
    int set_flag_schedule = -1;

    if (par->_affinity == 1)
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=core,scatter",1);
    else
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=fine,compact",1);

    set_flag_schedule = setenv("OMP_SCHEDULE", omp_schel.c_str(), 1);

    if (set_flag_affinity == 0)
    {
        std::printf("omp affinity bind successful\n");
        std::fflush;
    }

    if (set_flag_schedule == 0)
    {
        std::printf("omp schedule setup successful\n");
        std::fflush;
    }

    struct timespec ts1;
    struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    clock_gettime(CLOCK_MONOTONIC, &ts1);

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int** comb_idx_set = input->comb_num_indexes_set;
    int**** comb_num_idx = input->comb_num_indexes;

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int color_num = input->getColorNum();
    int s = par->_sub_itr;
    int cur_sub_vert = part->get_num_verts_sub(s);
    int active_sub_vert = part->get_num_verts_active(s);
    int cur_a_comb_num = input->choose_table[cur_sub_vert][active_sub_vert];
    int cur_comb_num = input->choose_table[color_num][cur_sub_vert];
    int mapper_num = input->mapper_num;
    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    double* thdwork_record = input->thdwork_record;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    //start omp function
    if (thread_num == 0)
        thread_num = omp_get_max_threads();

    double total_count_cursub = 0.0;

    //need a container to store all the valid_nbrs 
    int** valid_nbrs_map = new int*[num_vert_g];
    int* valid_nbrs_map_size = new int[num_vert_g];
    std::memset(valid_nbrs_map_size, 0, num_vert_g*sizeof(int));

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    #pragma omp parallel for schedule(guided) num_threads(thread_num) 
    for(int v=0;v<num_vert_g;v++)
    {
        // v is relative v_id from 0 to num_verts -1 
        // std::vector<int> valid_nbrs;
        if( dt->is_vertex_init_active(v))
        {
            int* valid_nbrs = new int[g->out_degree(v)];
            int valid_nbrs_count = 0;

            //adjs is absolute v_id
            int* adjs_abs = g->adjacent_vertices(v);
            int end = g->out_degree(v);

            //loop overall its neighbours
            int nbr_comm_itr = 0;
            for(int i = 0; i < end; ++i)
            {
                int adj_i = g->get_relative_v_id(adjs_abs[i]);
                //how to determine whether adj_i is in the current passive table
                if( adj_i >=0 && dt->is_vertex_init_passive(adj_i))
                {
                    // valid_nbrs.push_back(adj_i);
                    valid_nbrs[valid_nbrs_count++] = adj_i;
                }
                if (mapper_num > 1 && adj_i < 0)
                    update_map[v].get()[nbr_comm_itr++] = adjs_abs[i];
            }
            if (mapper_num > 1)
                update_map_size.get()[v] = nbr_comm_itr;

            valid_nbrs_map[v] = valid_nbrs;
            valid_nbrs_map_size[v] = valid_nbrs_count;
            // delete[] valid_nbrs;
            if (s != 0 && valid_nbrs_map_size[v] != 0)
                dt->set_init(v);
        }
        else
            valid_nbrs_map[v] = NULL;

    }

    std::printf("Start the breakdown of nbrs list task\n");
    std::fflush;
    //breakdown all the valid nbrs list into small task stored in task_list vector  
    int nbr_task_len = 0;
    for(int v=0;v<num_vert_g;v++)
    {
        if (dt->is_vertex_init_active(v) && valid_nbrs_map_size[v] != 0)
        {
            int* nbrs_pos = valid_nbrs_map[v];
            int nbrs_len = valid_nbrs_map_size[v];
           
            nbr_task_len = (par->_nbr_task_len > 0) ? par->_nbr_task_len : nbrs_len;
            //create the task
            while(nbrs_len > 0)
            {
                int task_len = (nbrs_len < nbr_task_len) ? nbrs_len : nbr_task_len;
                input->task_list.push_back(new task_nbr(v, task_len, nbrs_pos));
                nbrs_len -= task_len;
                if (nbrs_len > 0)
                    nbrs_pos = nbrs_pos + task_len;
            }
        }
    }

    //check the nb of tasks in vector
    //shuffle the task list or not ???
    std::random_shuffle(input->task_list.begin(), input->task_list.end());
    int cur_task_list_len = input->task_list.size();
    std::printf("Num of vert is %d, Task nbrs breakdown is %d, thread num: %d\n", num_vert_g, cur_task_list_len, thread_num);
    std::fflush;

    task_nbr** compute_list = &(input->task_list)[0];

    #pragma omp parallel for schedule(runtime) num_threads(thread_num) 
    for(int t = 0; t< cur_task_list_len; t++)
    {
        //replace shared ptr/var by local (private) one 
        //to avoid race condition
        int ompthd_id = omp_get_thread_num();

        task_nbr* task_atomic = compute_list[t];
        int**** comb_num_idx_thd = comb_num_idx;
        int** comb_idx_set_thd = comb_idx_set;

        int v = task_atomic->_vertex;
        int valid_nbrs_task_num = task_atomic->_nbr_size;
        int* valid_nbrs_task_ptr = task_atomic->_nbr_ptr; 

        //get ptr to v table entry
        //counts of v at active child
        float* counts_a = dt->get_active(v);
        float** passiv_table = dt->get_passive_table();

        float* v_dt_ptr = NULL;
        if (s != 0)
            v_dt_ptr = dt->get_table(s, v);

        int cur_a_comb_num_thd = cur_a_comb_num;
        int cur_comb_num_thd = cur_comb_num; 
        double last_sub_count = 0.0;

        double mult_active = 0.0;
        double mult_passive = 0.0;
        double color_count = 0.0;
        for(int n = 0; n < cur_comb_num_thd; ++n)
        {
            int* comb_indexes_a = comb_num_idx_thd[0][s][n];
            int* comb_indexes_p = comb_num_idx_thd[1][s][n];
            int p = cur_a_comb_num_thd -1;

            color_count = 0.0;
            // second loop on different color_combs of active/passive children
            // a+p == num_combinations_ato 
            // (total colorscombs for both of active child and pasive child)
            for(int a = 0; a < cur_a_comb_num_thd; ++a, --p)
            {
                mult_active = counts_a[comb_indexes_a[a]];
                if( mult_active != 0)
                {
                    //check overflow of count_a
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    // for(int j=0;j<valid_nbrs_task_num;j++)
                    //     color_count += ((double)count_a*passiv_table[valid_nbrs_task_ptr[j]][comb_indexes_p[p]]);
                    for(int j=0;j<valid_nbrs_task_num;j++)
                    {
                        mult_passive = passiv_table[valid_nbrs_task_ptr[j]][comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;
                            
                        color_count += (mult_active*mult_passive);
                    }

                    //record workload for this thread
                    // thdwork_record[ompthd_id] += valid_nbrs_task_num;

                }
            }

            //update n pos of v
            if (s != 0)
            {
                if (color_count > overflow_val)
                    color_count = (-1.0)*color_count/overflow_val;

                #pragma omp atomic
                v_dt_ptr[comb_idx_set_thd[s][n]] += (float)color_count;
            }
            else
            {
                // #pragma omp atomic
                last_sub_count += color_count;
            }

        }

        if (s == 0)
        {

            #pragma omp atomic
            total_count_cursub += last_sub_count;
        }

        delete task_atomic;
        task_atomic = NULL;

    }

    //clear task_list
    input->task_list.clear();

    //delete valid_nbrs_map
    for(int i=0;i<num_vert_g; i++)
    {
        if (valid_nbrs_map[i] != NULL)
            delete[] valid_nbrs_map[i];
    }

    delete[] valid_nbrs_map;
    delete[] valid_nbrs_map_size;

    std::printf("Finish Distrikernel compute for s: %d\n", s);
    std::fflush;

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    compute_time = (double)(diff)/1000000L;

    par->_count_time += compute_time;
    par->_total_counts = total_count_cursub;

    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", s, compute_mem);
    std::fflush;
	  
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);

    if (s == 0)
    {
        std::printf("Finish Final compute with total count %e\n", total_count_cursub);
        std::printf("Omp total compute time: %f ms\n", par->_count_time);
        std::fflush;
    }
#else
#endif
}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::computeNonBottomNbrSplitTBB(Parameter* &par, Input* &input)
{/*{{{*/

    std::printf("Start Distrikernel compute nonlast\n");
    std::fflush;

    struct timespec ts1;
    struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    clock_gettime(CLOCK_MONOTONIC, &ts1);

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int** comb_idx_set = input->comb_num_indexes_set;
    int**** comb_num_idx = input->comb_num_indexes;

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int color_num = input->getColorNum();
    int s = par->_sub_itr;
    int cur_sub_vert = part->get_num_verts_sub(s);
    int active_sub_vert = part->get_num_verts_active(s);
    int cur_a_comb_num = input->choose_table[cur_sub_vert][active_sub_vert];
    int cur_comb_num = input->choose_table[color_num][cur_sub_vert];
    int mapper_num = input->mapper_num;
    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    double* thdwork_record = input->thdwork_record;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();

    tbb::atomic<double> total_count_cursub_atomic = 0.0;
    
    //need a container to store all the valid_nbrs 
    int** valid_nbrs_map = new int*[num_vert_g];
    int* valid_nbrs_map_size = new int[num_vert_g];
    std::memset(valid_nbrs_map_size, 0, num_vert_g*sizeof(int));

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    //enabling thread pinning for TBB
    services::Environment::getInstance()->enableThreadPinning(true);

    SafeStatus safeStat;
    daal::threader_for(num_vert_g, num_vert_g, [&](int v)
    {
        // v is relative v_id from 0 to num_verts -1 
        // std::vector<int> valid_nbrs;
        if( dt->is_vertex_init_active(v))
        {
            int* valid_nbrs = new int[g->out_degree(v)];
            int valid_nbrs_count = 0;

            //adjs is absolute v_id
            int* adjs_abs = g->adjacent_vertices(v);
            int end = g->out_degree(v);

            //loop overall its neighbours
            int nbr_comm_itr = 0;
            for(int i = 0; i < end; ++i)
            {
                int adj_i = g->get_relative_v_id(adjs_abs[i]);
                //how to determine whether adj_i is in the current passive table
                if( adj_i >=0 && dt->is_vertex_init_passive(adj_i))
                {
                    valid_nbrs[valid_nbrs_count++] = adj_i;
                }
                if (mapper_num > 1 && adj_i < 0)
                    update_map[v].get()[nbr_comm_itr++] = adjs_abs[i];
            }
            if (mapper_num > 1)
                update_map_size.get()[v] = nbr_comm_itr;

            valid_nbrs_map[v] = valid_nbrs;
            valid_nbrs_map_size[v] = valid_nbrs_count;
            if (s != 0 && valid_nbrs_map_size[v] != 0)
                dt->set_init(v);
        }
        else
            valid_nbrs_map[v] = NULL;

    });

    safeStat.detach();
    
    std::printf("Start the breakdown of nbrs list task\n");
    std::fflush;
    //breakdown all the valid nbrs list into small task stored in task_list vector  
    int nbr_task_len = 0;
    for(int v=0;v<num_vert_g;v++)
    {
        if (dt->is_vertex_init_active(v) && valid_nbrs_map_size[v] != 0)
        {
            int* nbrs_pos = valid_nbrs_map[v];
            int nbrs_len = valid_nbrs_map_size[v];
           
            nbr_task_len = (par->_nbr_task_len > 0) ? par->_nbr_task_len : nbrs_len;
            //create the task
            while(nbrs_len > 0)
            {
                int task_len = (nbrs_len < nbr_task_len) ? nbrs_len : nbr_task_len;
                input->task_list.push_back(new task_nbr(v, task_len, nbrs_pos));
                nbrs_len -= task_len;
                if (nbrs_len > 0)
                    nbrs_pos = nbrs_pos + task_len;
            }
        }
    }

    //check the nb of tasks in vector
    //shuffle the task list or not ???
    std::random_shuffle(input->task_list.begin(), input->task_list.end());
    int cur_task_list_len = input->task_list.size();
    std::printf("Num of vert is %d, Task nbrs breakdown is %d, thread num: %d\n", num_vert_g, cur_task_list_len, thread_num);
    std::fflush;

    task_nbr** compute_list = &(input->task_list)[0];

    SafeStatus safeStat2;
    daal::threader_for(cur_task_list_len, cur_task_list_len, [&](int t)
    {
        task_nbr* task_atomic = compute_list[t];
        int**** comb_num_idx_thd = comb_num_idx;
        int** comb_idx_set_thd = comb_idx_set;

        int v = task_atomic->_vertex;
        int valid_nbrs_task_num = task_atomic->_nbr_size;
        int* valid_nbrs_task_ptr = task_atomic->_nbr_ptr; 

        //get ptr to v table entry
        //counts of v at active child
        float* counts_a = dt->get_active(v);
        float** passiv_table = dt->get_passive_table();

        tbb::atomic<float>* v_dt_ptr_atomic = NULL;
        if (s != 0)
            v_dt_ptr_atomic = reinterpret_cast<tbb::atomic<float>*>(dt->get_table(s, v));

        int cur_a_comb_num_thd = cur_a_comb_num;
        int cur_comb_num_thd = cur_comb_num; 
        double last_sub_count = 0.0;

        double mult_active = 0.0;
        double mult_passive = 0.0;
        double color_count = 0.0;
        for(int n = 0; n < cur_comb_num_thd; ++n)
        {
            int* comb_indexes_a = comb_num_idx_thd[0][s][n];
            int* comb_indexes_p = comb_num_idx_thd[1][s][n];
            int p = cur_a_comb_num_thd -1;

            color_count = 0.0;
            // second loop on different color_combs of active/passive children
            // a+p == num_combinations_ato 
            // (total colorscombs for both of active child and pasive child)
            for(int a = 0; a < cur_a_comb_num_thd; ++a, --p)
            {
                mult_active = counts_a[comb_indexes_a[a]];
                if( mult_active != 0)
                {
                    //check overflow of count_a
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    // for(int j=0;j<valid_nbrs_task_num;j++)
                    //     color_count += ((double)count_a*passiv_table[valid_nbrs_task_ptr[j]][comb_indexes_p[p]]);
                    for(int j=0;j<valid_nbrs_task_num;j++)
                    {
                        mult_passive = passiv_table[valid_nbrs_task_ptr[j]][comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;
                            
                        color_count += (mult_active*mult_passive);
                    }

                    //record workload for this thread
                    // thdwork_record[ompthd_id] += valid_nbrs_task_num;

                }
            }

            //update n pos of v
            if (s != 0)
            {
                if (color_count > overflow_val)
                    color_count = (-1.0)*color_count/overflow_val;

                float atomic_tmp;
                int atomic_update_idx = comb_idx_set_thd[s][n];

                do{
                    atomic_tmp = v_dt_ptr_atomic[atomic_update_idx]; 
                }while(v_dt_ptr_atomic[atomic_update_idx].compare_and_swap(atomic_tmp+(float)color_count, atomic_tmp) != atomic_tmp);
            }
            else
                last_sub_count += color_count;

        }

        if (s == 0)
        {

            //replace this by TBB atomic
            // #pragma omp atomic
            double atomic_tmp = 0.0;
            do{
                atomic_tmp = total_count_cursub_atomic;
            }while(total_count_cursub_atomic.compare_and_swap(atomic_tmp+last_sub_count, atomic_tmp) != atomic_tmp);
        }

        delete task_atomic;
        task_atomic = NULL;

    });

    safeStat2.detach();

    services::Environment::getInstance()->enableThreadPinning(false);

    //clear task_list
    input->task_list.clear();

    //delete valid_nbrs_map
    for(int i=0;i<num_vert_g; i++)
    {
        if (valid_nbrs_map[i] != NULL)
            delete[] valid_nbrs_map[i];
    }

    delete[] valid_nbrs_map;
    delete[] valid_nbrs_map_size;

    std::printf("Finish Distrikernel compute for s: %d\n", s);
    std::fflush;

    clock_gettime(CLOCK_MONOTONIC, &ts2);
    diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
    compute_time = (double)(diff)/1000000L;

    par->_count_time += compute_time;
    par->_total_counts = total_count_cursub_atomic;

    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", s, compute_mem);
    std::fflush;
	  
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);

    if (s == 0)
    {
        std::printf("Finish Final compute with total count %e\n", par->_total_counts);
        std::printf("Omp total compute time: %f ms\n", par->_count_time);
        std::fflush;
    }

}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::updateRemoteCountsNbrSplit(Parameter* &par, Input* &input)
{/*{{{*/

#ifdef USE_OMP
    std::printf("Distrikernel updateRemoteCounts\n");
    std::fflush;

    std::string omp_schel = par->_omp_schedule;
    std::printf("Use omp schedule: %s\n", omp_schel.c_str());
    std::fflush;

    //setup omp affinity and scheduling policy
    int set_flag_affinity = -1;
    int set_flag_schedule = -1;

    if (par->_affinity == 1)
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=core,scatter",1);
    else
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=fine,compact",1);

    set_flag_schedule = setenv("OMP_SCHEDULE", omp_schel.c_str(), 1);

    if (set_flag_affinity == 0)
    {
        std::printf("omp affinity bind successful\n");
        std::fflush;
    }

    if (set_flag_schedule == 0)
    {
        std::printf("omp schedule setup successful\n");
        std::fflush;
    }   

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int sub_id = par->_sub_itr;
    int num_colors = input->getColorNum();

    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    services::SharedPtr<int>* map_ids_cache_pip = input->map_ids_cache_pip;
    services::SharedPtr<int>* chunk_ids_cache_pip = input->chunk_ids_cache_pip;
    services::SharedPtr<int>* chunk_internal_offsets_cache_pip = input->chunk_internal_offsets_cache_pip;

    HarpBlockDescriptor<int>** update_queue_pos = input->update_queue_pos;
    HarpBlockDescriptor<float>** update_queue_counts = input->update_queue_counts;
    HarpBlockDescriptor<int>** update_queue_index = input->update_queue_index;
    decompressElem** update_queue_counts_decompress = input->update_queue_counts_decompress;

    int**** comb_num_indexes = input->comb_num_indexes;
    int** comb_num_indexes_set = input->comb_num_indexes_set;
    double* thdwork_record = input->thdwork_record;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    //move the kernel from input class to here
    int active_child = part->get_active_index(sub_id);
    int passive_child = part->get_passive_index(sub_id);

    int num_combinations_verts_sub = input->choose_table[num_colors][input->num_verts_table[sub_id]];
    int active_index = part->get_active_index(sub_id);
    int num_verts_a = input->num_verts_table[active_index];

    // colorset combinations from active child
    // combination of colors for active child
    int num_combinations_active_ato = input->choose_table[input->num_verts_table[sub_id]][num_verts_a];

    // to calculate chunk size
    int comb_len = dt->get_num_color_set(part->get_passive_index(sub_id)); 

    std::printf("Update Comb len: %d\n", comb_len);
    std::fflush;

    // start update 
    // first loop over local v
    int effect_count = 0;
    double total_update_counts = 0.0;

    // prepare the task_list_update 
    int nbr_task_len = 0;
    for (int v = 0; v < num_vert_g; ++v) 
    {
        int adj_list_size = update_map_size.get()[v];
        if (dt->is_vertex_init_active(v) && adj_list_size > 0)
        {
            int* map_ids = map_ids_cache_pip[v].get();
            int* chunk_ids = chunk_ids_cache_pip[v].get(); 
            int* chunk_internal_offsets = chunk_internal_offsets_cache_pip[v].get();

            if (sub_id != 0)
                dt->set_init(v);

            nbr_task_len = (par->_nbr_task_len > 0 ) ? par->_nbr_task_len : adj_list_size;
            while(adj_list_size > 0)
            {
                int task_len = (adj_list_size < nbr_task_len) ? adj_list_size : nbr_task_len;
                input->task_list_update.push_back(new task_nbr_update(v, task_len, map_ids, chunk_ids, chunk_internal_offsets));
                adj_list_size -= task_len;
                if (adj_list_size > 0)
                {
                    map_ids = map_ids + task_len;
                    chunk_ids = chunk_ids + task_len; 
                    chunk_internal_offsets = chunk_internal_offsets + task_len;
                }
            }
        }
    }

    //shuffle 
    std::random_shuffle(input->task_list_update.begin(), input->task_list_update.end());

    task_nbr_update** task_update_queue = &(input->task_list_update)[0];
    int task_update_queue_size = input->task_list_update.size(); 

    std::printf("Task breakdown subid %d: initial v num is %d, task num is %d\n", sub_id, num_vert_g, task_update_queue_size);
    std::fflush;

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    //loop over all the task_list_updates
    #pragma omp parallel for schedule(runtime) num_threads(thread_num) 
    for (int t_id = 0; t_id < task_update_queue_size; ++t_id) 
    {

        //replace shared var by local vars
        int**** comb_num_indexes_thd = comb_num_indexes; 
        int** comb_num_indexes_set_thd = comb_num_indexes_set;
        task_nbr_update* task_update_atomic = task_update_queue[t_id];
        int v = task_update_atomic->_vertex;

        //record thdworkload
        int ompthd_id = omp_get_thread_num();

        float* v_update_ptr = NULL;
        if (sub_id != 0)
            v_update_ptr = dt->get_table(sub_id, v);

        float* counts_a = dt->get_active(v);
        int adj_list_size = task_update_atomic->_adj_size;
        int* map_ids = task_update_atomic->_map_ids_atom;
        int* chunk_ids = task_update_atomic->_chunk_ids_atom;
        int* chunk_internal_offsets = task_update_atomic->_chunk_internal_offsets_atom; 
        int num_combinations_active_ato_thd = num_combinations_active_ato;
        int num_combinations_verts_sub_thd =  num_combinations_verts_sub;
        int sub_id_thd = sub_id;

        HarpBlockDescriptor<int>** update_queue_pos_thd = update_queue_pos;
        decompressElem** update_queue_counts_decompress_thd = update_queue_counts_decompress;

        std::vector<float*> adj_list_valid;
        for(int i=0;i<adj_list_size;i++)
        {

            int* adj_offset_list = (update_queue_pos_thd[map_ids[i]][chunk_ids[i]]).getBlockPtr();
            int start_pos = adj_offset_list[chunk_internal_offsets[i]];
            int end_pos = adj_offset_list[chunk_internal_offsets[i] + 1];

            if (start_pos != end_pos)
            {
                float* decompress_exp = update_queue_counts_decompress_thd[map_ids[i]][chunk_ids[i]]._data;
                adj_list_valid.push_back(decompress_exp + start_pos);
            }
        }

        double last_sub_count = 0.0;
        double partial_local_counts = 0.0;
        double mult_active = 0.0;
        double mult_passive = 0.0;

        // ----------- Finish decompress process -----------
        //third loop over comb_num for cur subtemplate
        for(int n = 0; n< num_combinations_verts_sub_thd; n++)
        {

            //local counts 
            partial_local_counts = 0.0;

            // more details
            int* comb_indexes_a = comb_num_indexes_thd[0][sub_id_thd][n];
            int* comb_indexes_p = comb_num_indexes_thd[1][sub_id_thd][n];

            // for passive 
            int p = num_combinations_active_ato_thd -1;

            // fourth loop over comb_num for active/passive subtemplates
            for(int a = 0; a < num_combinations_active_ato_thd; ++a, --p)
            {
                mult_active = counts_a[comb_indexes_a[a]];
                if (mult_active != 0)
                {    
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    for(int i=0; i<adj_list_valid.size(); i++)
                    {
                        mult_passive = (adj_list_valid[i])[comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;
       
                        partial_local_counts += (mult_active*mult_passive);
                    }

                    // thdwork_record[ompthd_id] += adj_list_valid.size();
                }
            }

            //set value to table
            if (sub_id_thd != 0)
            {
                if (partial_local_counts > overflow_val)
                    partial_local_counts = (-1.0)*partial_local_counts/overflow_val;

                #pragma omp atomic
                v_update_ptr[comb_num_indexes_set_thd[sub_id_thd][n]] +=  (float)partial_local_counts;
            }
            else
                last_sub_count += partial_local_counts;

        } // finish all combination sets for cur template

        if (sub_id_thd == 0)
        {
            #pragma omp atomic
            total_update_counts += last_sub_count; 
        }

        // clear the task 
        delete task_update_atomic;
        task_update_atomic = NULL;

    } // finish all the v on thread

    input->task_list_update.clear();

    if (sub_id == 0)
    {
        std::printf("Final updated counts is %e\n", total_update_counts);
        std::printf("Finish task split pre-decompress\n");
        std::fflush;
    }

    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", sub_id, compute_mem);
    std::fflush;
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    par->_update_counts = total_update_counts;

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);
#else
#endif
}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::updateRemoteCountsNbrSplitTBB(Parameter* &par, Input* &input)
{/*{{{*/

    std::printf("Distrikernel updateRemoteCounts\n");
    std::fflush;

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int sub_id = par->_sub_itr;
    int num_colors = input->getColorNum();

    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    services::SharedPtr<int>* map_ids_cache_pip = input->map_ids_cache_pip;
    services::SharedPtr<int>* chunk_ids_cache_pip = input->chunk_ids_cache_pip;
    services::SharedPtr<int>* chunk_internal_offsets_cache_pip = input->chunk_internal_offsets_cache_pip;

    HarpBlockDescriptor<int>** update_queue_pos = input->update_queue_pos;
    HarpBlockDescriptor<float>** update_queue_counts = input->update_queue_counts;
    HarpBlockDescriptor<int>** update_queue_index = input->update_queue_index;
    decompressElem** update_queue_counts_decompress = input->update_queue_counts_decompress;

    int**** comb_num_indexes = input->comb_num_indexes;
    int** comb_num_indexes_set = input->comb_num_indexes_set;
    double* thdwork_record = input->thdwork_record;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();
    
    //move the kernel from input class to here
    int active_child = part->get_active_index(sub_id);
    int passive_child = part->get_passive_index(sub_id);

    int num_combinations_verts_sub = input->choose_table[num_colors][input->num_verts_table[sub_id]];
    int active_index = part->get_active_index(sub_id);
    int num_verts_a = input->num_verts_table[active_index];

    // colorset combinations from active child
    // combination of colors for active child
    int num_combinations_active_ato = input->choose_table[input->num_verts_table[sub_id]][num_verts_a];

    // to calculate chunk size
    int comb_len = dt->get_num_color_set(part->get_passive_index(sub_id)); 

    std::printf("Update Comb len: %d\n", comb_len);
    std::fflush;

    // start update 
    // first loop over local v
    int effect_count = 0;
    // double total_update_counts = 0.0;
    tbb::atomic<double> total_update_counts_atomic = 0.0;

    // prepare the task_list_update 
    int nbr_task_len = 0;
    for (int v = 0; v < num_vert_g; ++v) 
    {
        int adj_list_size = update_map_size.get()[v];
        if (dt->is_vertex_init_active(v) && adj_list_size > 0)
        {
            int* map_ids = map_ids_cache_pip[v].get();
            int* chunk_ids = chunk_ids_cache_pip[v].get(); 
            int* chunk_internal_offsets = chunk_internal_offsets_cache_pip[v].get();

            if (sub_id != 0)
                dt->set_init(v);

            nbr_task_len = (par->_nbr_task_len > 0 ) ? par->_nbr_task_len : adj_list_size;
            while(adj_list_size > 0)
            {
                int task_len = (adj_list_size < nbr_task_len) ? adj_list_size : nbr_task_len;
                input->task_list_update.push_back(new task_nbr_update(v, task_len, map_ids, chunk_ids, chunk_internal_offsets));
                adj_list_size -= task_len;
                if (adj_list_size > 0)
                {
                    map_ids = map_ids + task_len;
                    chunk_ids = chunk_ids + task_len; 
                    chunk_internal_offsets = chunk_internal_offsets + task_len;
                }
            }
        }
    }

    //shuffle 
    std::random_shuffle(input->task_list_update.begin(), input->task_list_update.end());

    task_nbr_update** task_update_queue = &(input->task_list_update)[0];
    int task_update_queue_size = input->task_list_update.size(); 

    std::printf("Task breakdown subid %d: initial v num is %d, task num is %d\n", sub_id, num_vert_g, task_update_queue_size);
    std::fflush;

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    //enabling thread pinning for TBB
    services::Environment::getInstance()->enableThreadPinning(true);

    //loop over all the task_list_updates
    SafeStatus safeStat;

    daal::threader_for(task_update_queue_size, task_update_queue_size, [&](int t_id)
    {
        //replace shared var by local vars
        int**** comb_num_indexes_thd = comb_num_indexes; 
        int** comb_num_indexes_set_thd = comb_num_indexes_set;
        task_nbr_update* task_update_atomic = task_update_queue[t_id];
        int v = task_update_atomic->_vertex;

        //record thdworkload
        // int ompthd_id = omp_get_thread_num();

        // float* v_update_ptr = NULL;
        tbb::atomic<float>* v_update_ptr_atomic = NULL;
        if (sub_id != 0)
            v_update_ptr_atomic = reinterpret_cast<tbb::atomic<float>*>(dt->get_table(sub_id, v));
            // v_update_ptr = dt->get_table(sub_id, v);

        float* counts_a = dt->get_active(v);
        int adj_list_size = task_update_atomic->_adj_size;
        int* map_ids = task_update_atomic->_map_ids_atom;
        int* chunk_ids = task_update_atomic->_chunk_ids_atom;
        int* chunk_internal_offsets = task_update_atomic->_chunk_internal_offsets_atom; 
        int num_combinations_active_ato_thd = num_combinations_active_ato;
        int num_combinations_verts_sub_thd =  num_combinations_verts_sub;
        int sub_id_thd = sub_id;

        HarpBlockDescriptor<int>** update_queue_pos_thd = update_queue_pos;
        decompressElem** update_queue_counts_decompress_thd = update_queue_counts_decompress;

        std::vector<float*> adj_list_valid;
        for(int i=0;i<adj_list_size;i++)
        {

            int* adj_offset_list = (update_queue_pos_thd[map_ids[i]][chunk_ids[i]]).getBlockPtr();
            int start_pos = adj_offset_list[chunk_internal_offsets[i]];
            int end_pos = adj_offset_list[chunk_internal_offsets[i] + 1];

            if (start_pos != end_pos)
            {
                float* decompress_exp = update_queue_counts_decompress_thd[map_ids[i]][chunk_ids[i]]._data;
                adj_list_valid.push_back(decompress_exp + start_pos);
            }
        }

        double last_sub_count = 0.0;
        double partial_local_counts = 0.0;
        double mult_active = 0.0;
        double mult_passive = 0.0;

        // ----------- Finish decompress process -----------
        //third loop over comb_num for cur subtemplate
        for(int n = 0; n< num_combinations_verts_sub_thd; n++)
        {

            //local counts 
            partial_local_counts = 0.0;

            // more details
            int* comb_indexes_a = comb_num_indexes_thd[0][sub_id_thd][n];
            int* comb_indexes_p = comb_num_indexes_thd[1][sub_id_thd][n];

            // for passive 
            int p = num_combinations_active_ato_thd -1;

            // fourth loop over comb_num for active/passive subtemplates
            for(int a = 0; a < num_combinations_active_ato_thd; ++a, --p)
            {
                mult_active = counts_a[comb_indexes_a[a]];
                if (mult_active != 0)
                {    
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    for(int i=0; i<adj_list_valid.size(); i++)
                    {
                        mult_passive = (adj_list_valid[i])[comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;
       
                        partial_local_counts += (mult_active*mult_passive);
                    }

                    // thdwork_record[ompthd_id] += adj_list_valid.size();
                }
            }

            //set value to table
            if (sub_id_thd != 0)
            {
                if (partial_local_counts > overflow_val)
                    partial_local_counts = (-1.0)*partial_local_counts/overflow_val;

                // tbb::queuing_mutex::scoped_lock lock_update(tbb_mutex_update);
                // v_update_ptr[comb_num_indexes_set_thd[sub_id_thd][n]] +=  (float)partial_local_counts;
                float atomic_tmp;
                int atomic_update_idx = comb_num_indexes_set_thd[sub_id_thd][n];
                do{
                    atomic_tmp = v_update_ptr_atomic[atomic_update_idx]; 
                }while(v_update_ptr_atomic[atomic_update_idx].compare_and_swap(atomic_tmp+(float)partial_local_counts, atomic_tmp) != atomic_tmp);

            }
            else
                last_sub_count += partial_local_counts;

        } // finish all combination sets for cur template

        if (sub_id_thd == 0)
        {
            // tbb::queuing_mutex::scoped_lock lock_update(tbb_mutex_update);
            // total_update_counts += last_sub_count; 
            double atomic_tmp = 0.0;
            do{
                atomic_tmp = total_update_counts_atomic;
            }while(total_update_counts_atomic.compare_and_swap(atomic_tmp+last_sub_count, atomic_tmp) != atomic_tmp);
        }

        // clear the task 
        delete task_update_atomic;
        task_update_atomic = NULL;

    }); // finish all the v on thread

    safeStat.detach();

    services::Environment::getInstance()->enableThreadPinning(false);

    input->task_list_update.clear();


    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", sub_id, compute_mem);
    std::fflush;
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    par->_update_counts = total_update_counts_atomic;

    if (sub_id == 0)
    {
        std::printf("Final updated counts is %e\n", par->_update_counts);
        std::printf("Finish task split pre-decompress\n");
        std::fflush;
    }

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);

}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::updateRemoteCountsPipNbrSplit(Parameter* &par, Input* &input)
{/*{{{*/

#ifdef USE_OMP
    std::printf("Distrikernel updateRemoteCounts\n");
    std::fflush;

    std::string omp_schel = par->_omp_schedule;
    std::printf("Use omp schedule: %s\n", omp_schel.c_str());
    std::fflush;

    //setup omp affinity and scheduling policy
    int set_flag_affinity = -1;
    int set_flag_schedule = -1;

    if (par->_affinity == 1)
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=core,scatter",1);
    else
        set_flag_affinity = setenv("KMP_AFFINITY","granularity=fine,compact",1);

    set_flag_schedule = setenv("OMP_SCHEDULE", omp_schel.c_str(), 1);

    if (set_flag_affinity == 0)
    {
        std::printf("omp affinity bind successful\n");
        std::fflush;
    }

    if (set_flag_schedule == 0)
    {
        std::printf("omp schedule setup successful\n");
        std::fflush;
    }    

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int update_pip_id = par->_pip_id;

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int sub_id = par->_sub_itr;
    int num_colors = input->getColorNum();

    double* thdwork_record = input->thdwork_record;

    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    services::SharedPtr<int>* map_ids_cache_pip = input->map_ids_cache_pip;
    services::SharedPtr<int>* chunk_ids_cache_pip = input->chunk_ids_cache_pip;
    services::SharedPtr<int>* chunk_internal_offsets_cache_pip = input->chunk_internal_offsets_cache_pip;

    HarpBlockDescriptor<int>** update_queue_pos = input->update_queue_pos;
    HarpBlockDescriptor<float>** update_queue_counts = input->update_queue_counts;
    HarpBlockDescriptor<int>** update_queue_index = input->update_queue_index;
    decompressElem** update_queue_counts_decompress = input->update_queue_counts_decompress;

    int**** comb_num_indexes = input->comb_num_indexes;
    int** comb_num_indexes_set = input->comb_num_indexes_set;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    //move the kernel from input class to here
    int active_child = part->get_active_index(sub_id);
    int passive_child = part->get_passive_index(sub_id);

    int num_combinations_verts_sub = input->choose_table[num_colors][input->num_verts_table[sub_id]];
    int active_index = part->get_active_index(sub_id);
    int num_verts_a = input->num_verts_table[active_index];

    // colorset combinations from active child
    // combination of colors for active child
    int num_combinations_active_ato = input->choose_table[input->num_verts_table[sub_id]][num_verts_a];

    // to calculate chunk size
    int comb_len = dt->get_num_color_set(part->get_passive_index(sub_id)); 

    std::printf("Update Comb len: %d\n", comb_len);
    std::fflush;

    // start update 
    // first loop over local v
    int effect_count = 0;
    double total_update_counts = 0.0;

    if (input->task_list_update.size() == 0)
    {
        // prepare the task_list_update 
        int nbr_task_len = 0; 
        for (int v = 0; v < num_vert_g; ++v) 
        {
            int adj_list_size = update_map_size.get()[v];
            if (dt->is_vertex_init_active(v) && adj_list_size > 0)
            {
                int* map_ids = map_ids_cache_pip[v].get();
                int* chunk_ids = chunk_ids_cache_pip[v].get(); 
                int* chunk_internal_offsets = chunk_internal_offsets_cache_pip[v].get();

                if (sub_id != 0)
                    dt->set_init(v);

                nbr_task_len = (par->_nbr_task_len > 0) ? par->_nbr_task_len : adj_list_size;
                while(adj_list_size > 0)
                {
                    int task_len = (adj_list_size < nbr_task_len) ? adj_list_size : nbr_task_len;
                    input->task_list_update.push_back(new task_nbr_update(v, task_len, map_ids, chunk_ids, chunk_internal_offsets));
                    adj_list_size -= task_len;
                    if (adj_list_size > 0)
                    {
                        map_ids = map_ids + task_len;
                        chunk_ids = chunk_ids + task_len; 
                        chunk_internal_offsets = chunk_internal_offsets + task_len;
                    }
                }
            }
        }

        //shuffle 
        std::random_shuffle(input->task_list_update.begin(), input->task_list_update.end());

    }

    task_nbr_update** task_update_queue = &(input->task_list_update)[0];
    int task_update_queue_size = input->task_list_update.size(); 

    std::printf("Task breakdown subid %d: initial v num is %d, task num is %d\n", sub_id, num_vert_g, task_update_queue_size);
    std::fflush;

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    //loop over all the task_list_updates
    // #pragma omp parallel for schedule(guided) num_threads(thread_num) 
    #pragma omp parallel for schedule(runtime) num_threads(thread_num) 
    for (int t_id = 0; t_id < task_update_queue_size; ++t_id) 
    {

        int ompthd_id = omp_get_thread_num();
        //replace shared var by local var
        int**** comb_num_indexes_thd = comb_num_indexes; 
        int** comb_num_indexes_set_thd = comb_num_indexes_set;
        float* v_update_ptr = NULL;

        task_nbr_update* task_update_atomic = task_update_queue[t_id];
        int v = task_update_atomic->_vertex;

        if (sub_id != 0)
            v_update_ptr = dt->get_table(sub_id, v);

        float* counts_a = dt->get_active(v);
        int adj_list_size = task_update_atomic->_adj_size;
        int* map_ids = task_update_atomic->_map_ids_atom;
        int* chunk_ids = task_update_atomic->_chunk_ids_atom;
        int* chunk_internal_offsets = task_update_atomic->_chunk_internal_offsets_atom; 
        int update_pip_id_thd = update_pip_id;
        HarpBlockDescriptor<int>** update_queue_pos_thd = update_queue_pos;
        decompressElem** update_queue_counts_decompress_thd = update_queue_counts_decompress;
        int num_combinations_verts_sub_thd = num_combinations_verts_sub;
        int num_combinations_active_ato_thd = num_combinations_active_ato;
        int sub_id_thd = sub_id;
        //prepare a list of ptr to decompressed array for adj_list
        std::vector<float*> adj_list_valid;
        for(int i=0;i<adj_list_size;i++)
        {

            if (map_ids[i] != update_pip_id_thd || update_queue_pos_thd[map_ids[i]] == NULL)
                continue;

            int* adj_offset_list = (update_queue_pos_thd[map_ids[i]][chunk_ids[i]]).getBlockPtr();
            int start_pos = adj_offset_list[chunk_internal_offsets[i]];
            int end_pos = adj_offset_list[chunk_internal_offsets[i] + 1];

            if (start_pos != end_pos)
            {
                float* decompress_exp = update_queue_counts_decompress_thd[map_ids[i]][chunk_ids[i]]._data;
                adj_list_valid.push_back(decompress_exp + start_pos);
            }
        }

        double last_sub_count = 0.0;
        double partial_local_counts = 0.0;
        double mult_active = 0.0;
        double mult_passive = 0.0;

        //third loop over comb_num for cur subtemplate
        for(int n = 0; n< num_combinations_verts_sub_thd; n++)
        {

            //local counts 
            partial_local_counts = 0.0;

            // more details
            int* comb_indexes_a = comb_num_indexes_thd[0][sub_id_thd][n];
            int* comb_indexes_p = comb_num_indexes_thd[1][sub_id_thd][n];

            // for passive 
            int p = num_combinations_active_ato_thd -1;

            // fourth loop over comb_num for active/passive subtemplates
            for(int a = 0; a < num_combinations_active_ato_thd; ++a, --p)
            {
                // float count_a = counts_a[comb_indexes_a[a]];
                mult_active = counts_a[comb_indexes_a[a]];
                if (mult_active != 0)
                {
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    for(int i=0; i<adj_list_valid.size(); i++)
                    {
                        mult_passive = (adj_list_valid[i])[comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;

                        partial_local_counts += (mult_active*mult_passive);

                    }

                    // thdwork_record[ompthd_id] += adj_list_valid.size();
                }

            }

            //set value to table
            if (sub_id_thd != 0)
            {
                if (partial_local_counts > overflow_val)
                    partial_local_counts = (-1.0)*partial_local_counts/overflow_val;

                #pragma omp atomic
                v_update_ptr[comb_num_indexes_set_thd[sub_id_thd][n]] +=  (float)partial_local_counts;

                // #pragma omp atomic
                // v_update_ptr[comb_num_indexes_set_thd[sub_id_thd][n]] +=  0;

            }
            else
                last_sub_count += partial_local_counts;

        } // finish all combination sets for cur template

        if (sub_id_thd == 0)
        {
            #pragma omp atomic
            total_update_counts += last_sub_count; 
        }

        // task_update list will be re-used til the last pipeline operation
        // delete task_update_atomic;
        // task_update_atomic = NULL;

    } // finish all the v on thread

    // task_update list will be re-used til the last pipeline operation
    // input->task_list_update.clear();
    if (sub_id == 0)
    {
        std::printf("Final updated counts is %e\n", total_update_counts);
        std::printf("Finish task split pre-decompress Pipeline version\n");
        std::fflush;
    }

    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", sub_id, compute_mem);
    std::fflush;
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    par->_update_counts = total_update_counts;

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);
#else
#endif
}/*}}}*/

template <typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
void subgraphDistriKernel<interm, method, cpu>::updateRemoteCountsPipNbrSplitTBB(Parameter* &par, Input* &input)
{/*{{{*/

    std::printf("Distrikernel updateRemoteCounts\n");
    std::fflush;

    struct timespec ts1;
	struct timespec ts2;
    int64_t diff = 0;
    double compute_time = 0;

    daal::algorithms::subgraph::interface1::dynamic_table_array* dt = input->getDTTable();
    daal::algorithms::subgraph::interface1::partitioner* part = input->getPartitioner();

    int update_pip_id = par->_pip_id;

    int thread_num = par->_thread_num;
    int num_vert_g = input->getLocalVNum();
    int sub_id = par->_sub_itr;
    int num_colors = input->getColorNum();

    double* thdwork_record = input->thdwork_record;

    services::SharedPtr<int>* update_map = input->update_map;
    services::SharedPtr<int> update_map_size = input->update_map_size;

    services::SharedPtr<int>* map_ids_cache_pip = input->map_ids_cache_pip;
    services::SharedPtr<int>* chunk_ids_cache_pip = input->chunk_ids_cache_pip;
    services::SharedPtr<int>* chunk_internal_offsets_cache_pip = input->chunk_internal_offsets_cache_pip;

    HarpBlockDescriptor<int>** update_queue_pos = input->update_queue_pos;
    HarpBlockDescriptor<float>** update_queue_counts = input->update_queue_counts;
    HarpBlockDescriptor<int>** update_queue_index = input->update_queue_index;
    decompressElem** update_queue_counts_decompress = input->update_queue_counts_decompress;

    int**** comb_num_indexes = input->comb_num_indexes;
    int** comb_num_indexes_set = input->comb_num_indexes_set;

    daal::algorithms::subgraph::interface1::Graph* g = input->getGraphPtr();

    // set up the threads used
    if (thread_num > 0)
        services::Environment::getInstance()->setNumberOfThreads(thread_num);
    else
        thread_num = threader_get_max_threads_number();

    //move the kernel from input class to here
    int active_child = part->get_active_index(sub_id);
    int passive_child = part->get_passive_index(sub_id);

    int num_combinations_verts_sub = input->choose_table[num_colors][input->num_verts_table[sub_id]];
    int active_index = part->get_active_index(sub_id);
    int num_verts_a = input->num_verts_table[active_index];

    // colorset combinations from active child
    // combination of colors for active child
    int num_combinations_active_ato = input->choose_table[input->num_verts_table[sub_id]][num_verts_a];

    // to calculate chunk size
    int comb_len = dt->get_num_color_set(part->get_passive_index(sub_id)); 

    std::printf("Update Comb len: %d\n", comb_len);
    std::fflush;

    // start update 
    // first loop over local v
    int effect_count = 0;
    // double total_update_counts = 0.0;
    tbb::atomic<double> total_update_counts_atomic = 0.0;

    if (input->task_list_update.size() == 0)
    {
        // prepare the task_list_update 
        int nbr_task_len = 0; 
        for (int v = 0; v < num_vert_g; ++v) 
        {
            int adj_list_size = update_map_size.get()[v];
            if (dt->is_vertex_init_active(v) && adj_list_size > 0)
            {
                int* map_ids = map_ids_cache_pip[v].get();
                int* chunk_ids = chunk_ids_cache_pip[v].get(); 
                int* chunk_internal_offsets = chunk_internal_offsets_cache_pip[v].get();

                if (sub_id != 0)
                    dt->set_init(v);

                nbr_task_len = (par->_nbr_task_len > 0) ? par->_nbr_task_len : adj_list_size;
                while(adj_list_size > 0)
                {
                    int task_len = (adj_list_size < nbr_task_len) ? adj_list_size : nbr_task_len;
                    input->task_list_update.push_back(new task_nbr_update(v, task_len, map_ids, chunk_ids, chunk_internal_offsets));
                    adj_list_size -= task_len;
                    if (adj_list_size > 0)
                    {
                        map_ids = map_ids + task_len;
                        chunk_ids = chunk_ids + task_len; 
                        chunk_internal_offsets = chunk_internal_offsets + task_len;
                    }
                }
            }
        }

        //shuffle 
        std::random_shuffle(input->task_list_update.begin(), input->task_list_update.end());

    }

    task_nbr_update** task_update_queue = &(input->task_list_update)[0];
    int task_update_queue_size = input->task_list_update.size(); 

    std::printf("Task breakdown subid %d: initial v num is %d, task num is %d\n", sub_id, num_vert_g, task_update_queue_size);
    std::fflush;

    // clear the thread workload records
    if (thdwork_record == NULL)
        thdwork_record = new double[thread_num];

    std::memset(thdwork_record, 0, thread_num*sizeof(double));

    //enabling thread pinning for TBB
    services::Environment::getInstance()->enableThreadPinning(true);

    SafeStatus safeStat;
    //loop over all the task_list_updates
    daal::threader_for(task_update_queue_size, task_update_queue_size, [&](int t_id)
    {

        // int ompthd_id = omp_get_thread_num();
        //replace shared var by local var
        int**** comb_num_indexes_thd = comb_num_indexes; 
        int** comb_num_indexes_set_thd = comb_num_indexes_set;

        // float* v_update_ptr = NULL;
        tbb::atomic<float>* v_update_ptr_atomic = NULL;

        task_nbr_update* task_update_atomic = task_update_queue[t_id];
        int v = task_update_atomic->_vertex;

        if (sub_id != 0)
            v_update_ptr_atomic = reinterpret_cast<tbb::atomic<float>*>(dt->get_table(sub_id, v));
            // v_update_ptr = dt->get_table(sub_id, v);

        float* counts_a = dt->get_active(v);
        int adj_list_size = task_update_atomic->_adj_size;
        int* map_ids = task_update_atomic->_map_ids_atom;
        int* chunk_ids = task_update_atomic->_chunk_ids_atom;
        int* chunk_internal_offsets = task_update_atomic->_chunk_internal_offsets_atom; 
        int update_pip_id_thd = update_pip_id;
        HarpBlockDescriptor<int>** update_queue_pos_thd = update_queue_pos;
        decompressElem** update_queue_counts_decompress_thd = update_queue_counts_decompress;
        int num_combinations_verts_sub_thd = num_combinations_verts_sub;
        int num_combinations_active_ato_thd = num_combinations_active_ato;
        int sub_id_thd = sub_id;
        //prepare a list of ptr to decompressed array for adj_list
        std::vector<float*> adj_list_valid;
        for(int i=0;i<adj_list_size;i++)
        {

            if (map_ids[i] != update_pip_id_thd || update_queue_pos_thd[map_ids[i]] == NULL)
                continue;

            int* adj_offset_list = (update_queue_pos_thd[map_ids[i]][chunk_ids[i]]).getBlockPtr();
            int start_pos = adj_offset_list[chunk_internal_offsets[i]];
            int end_pos = adj_offset_list[chunk_internal_offsets[i] + 1];

            if (start_pos != end_pos)
            {
                float* decompress_exp = update_queue_counts_decompress_thd[map_ids[i]][chunk_ids[i]]._data;
                adj_list_valid.push_back(decompress_exp + start_pos);
            }
        }

        double last_sub_count = 0.0;
        double partial_local_counts = 0.0;
        double mult_active = 0.0;
        double mult_passive = 0.0;

        //third loop over comb_num for cur subtemplate
        for(int n = 0; n< num_combinations_verts_sub_thd; n++)
        {

            //local counts 
            partial_local_counts = 0.0;

            // more details
            int* comb_indexes_a = comb_num_indexes_thd[0][sub_id_thd][n];
            int* comb_indexes_p = comb_num_indexes_thd[1][sub_id_thd][n];

            // for passive 
            int p = num_combinations_active_ato_thd -1;

            // fourth loop over comb_num for active/passive subtemplates
            for(int a = 0; a < num_combinations_active_ato_thd; ++a, --p)
            {
                // float count_a = counts_a[comb_indexes_a[a]];
                mult_active = counts_a[comb_indexes_a[a]];
                if (mult_active != 0)
                {
                    if (mult_active < 0.0)
                        mult_active = (-1.0)*mult_active*overflow_val;

                    for(int i=0; i<adj_list_valid.size(); i++)
                    {
                        mult_passive = (adj_list_valid[i])[comb_indexes_p[p]];
                        if (mult_passive < 0.0)
                            mult_passive = (-1.0)*mult_passive*overflow_val;

                        partial_local_counts += (mult_active*mult_passive);

                    }

                    // thdwork_record[ompthd_id] += adj_list_valid.size();
                }

            }

            //set value to table
            if (sub_id_thd != 0)
            {
                if (partial_local_counts > overflow_val)
                    partial_local_counts = (-1.0)*partial_local_counts/overflow_val;

                // tbb::queuing_mutex::scoped_lock lock_update(tbb_mutex_update);
                // v_update_ptr[comb_num_indexes_set_thd[sub_id_thd][n]] +=  (float)partial_local_counts;
                float atomic_tmp;
                int atomic_update_idx = comb_num_indexes_set_thd[sub_id_thd][n];
                do{
                    atomic_tmp = v_update_ptr_atomic[atomic_update_idx]; 
                }while(v_update_ptr_atomic[atomic_update_idx].compare_and_swap(atomic_tmp+(float)partial_local_counts, atomic_tmp) != atomic_tmp);

            }
            else
                last_sub_count += partial_local_counts;

        } // finish all combination sets for cur template

        if (sub_id_thd == 0)
        {
            // tbb::queuing_mutex::scoped_lock lock_update(tbb_mutex_update);
            // total_update_counts += last_sub_count; 
            double atomic_tmp = 0.0;
            do{
                atomic_tmp = total_update_counts_atomic;
            }while(total_update_counts_atomic.compare_and_swap(atomic_tmp+last_sub_count, atomic_tmp) != atomic_tmp);
        }

        // task_update list will be re-used til the last pipeline operation
        // delete task_update_atomic;
        // task_update_atomic = NULL;

    }); // finish all the v on thread

    safeStat.detach();

    services::Environment::getInstance()->enableThreadPinning(false);

    // task_update list will be re-used til the last pipeline operation
    // input->task_list_update.clear();
    

    //trace the memory usage after computation
    double compute_mem = 0.0;
    process_mem_usage(compute_mem);
	compute_mem = compute_mem /(1024*1024);
    std::printf("Mem utilization compute step sub %d: %9.6lf GB\n", sub_id, compute_mem);
    std::fflush;
    input->peak_mem = (compute_mem > input->peak_mem) ? compute_mem : input->peak_mem;

    par->_update_counts = total_update_counts_atomic;

    if (sub_id == 0)
    {
        std::printf("Final updated counts is %e\n", par->_update_counts);
        std::printf("Finish task split pre-decompress Pipeline version\n");
        std::fflush;
    }

    //calculate avg thread workload and stdev of thread-workload
    double thdwork_sum = 0;
    double thdwork_avg = 0;
    for (int j=0; j<thread_num; j++)
        thdwork_sum += thdwork_record[j];

    thdwork_avg = thdwork_sum/thread_num;
    input->thdwork_avg = thdwork_avg;
    thdwork_sum = 0;
    for(int j=0;j<thread_num; j++)
        thdwork_sum += (std::pow((thdwork_record[j] - thdwork_avg),2));

    input->thdwork_stdev = std::sqrt(thdwork_sum);

}/*}}}*/

} // namespace daal::internal
}
}
} // namespace daal

#endif
