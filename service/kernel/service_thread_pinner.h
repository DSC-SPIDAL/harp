/* file: service_thread_pinner.h */
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
//  Implementation of thread pinner class
//--
*/

#ifndef __SERVICE_THREAD_PINNER_H__
#define __SERVICE_THREAD_PINNER_H__

#include <cstdlib>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define USE_TASK_ARENA_CURRENT_SLOT 1
#define LOG_PINNING                 1
#define TBB_PREVIEW_TASK_ARENA      1
#define TBB_PREVIEW_LOCAL_OBSERVER  1

#include "tbb/tbb.h"
#include <tbb/task_arena.h>
#include <tbb/task_scheduler_observer.h>
#include <tbb/atomic.h>
#include <tbb/task_scheduler_init.h>
#include <tbb/parallel_reduce.h>
#include <tbb/blocked_range.h>
#include <tbb/tick_count.h>
#include <tbb/scalable_allocator.h>

#if defined(_WIN32) || defined(_WIN64)
    #include <Windows.h>
    #define __PINNER_WINDOWS__

    #if defined(_WIN64)
        #define MASK_WIDTH 64
    #else
        #define MASK_WIDTH 32
    #endif

#else // LINUX
    #include <sched.h>
    #define __PINNER_LINUX__
#endif

#include "daal_atomic_int.h"

namespace daal
{
namespace services
{

namespace interface1
{


struct cpu_mask_t
{
    int status;
#if defined(_WIN32) || defined(_WIN64)
    GROUP_AFFINITY ga;
#else
    int ncpus;
    int bit_parts_size;
    cpu_set_t*  cpu_set;
#endif
    cpu_mask_t();
    int get_thread_affinity();
    int set_thread_affinity();
    int set_cpu_index(int cpu_idx);
    int get_status();
    ~cpu_mask_t();
};

class thread_pinner_t: public tbb::task_scheduler_observer
{
    int status;
    int nthreads;
    int max_threads;
    int* cpu_queue;
    bool do_pinning;
    Atomic<int> is_pinning;
    tbb::enumerable_thread_specific<cpu_mask_t *> thread_mask;
    tbb::task_arena pinner_arena;

public:

    thread_pinner_t();
    void read_topology();
    void on_scheduler_entry( bool );
    void on_scheduler_exit( bool );
    template<typename F> void execute(const F& f)
    {
        if(do_pinning && (status == 0) && (is_pinning.get() == 0))
        {
            is_pinning.set(1);
            pinner_arena.execute(f);
            is_pinning.set(0);
        }
        else
        {
            f();
        }
    }
    int  get_status();
    bool get_pinning();
    bool set_pinning(bool p);
    ~thread_pinner_t();
};

extern unsigned _internal_daal_GetSysLogicalProcessorCount();
extern unsigned _internal_daal_GetLogicalProcessorQueue(int* queue);
extern unsigned _internal_daal_GetStatus();
extern unsigned _internal_daal_FreeArrays();

extern thread_pinner_t* getThreadPinner(bool create_pinner);

}
}
}

#endif /* #ifndef __SERVICE_THREAD_PINNER_H__ */
