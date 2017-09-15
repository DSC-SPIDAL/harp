/* file: service_thread_pinner.cpp */
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

#include "daal_defines.h"

#if !(defined DAAL_THREAD_PINNING_DISABLED)

#include "service_memory.h"
#include "service_thread_pinner.h"

// #define _DBG_
// #define _TRACE_

#if (!defined __ERROR_PRINT__)
    #if (defined _DBG_)
        #define __ERROR_PRINT__(x) x
    #else
        #define __ERROR_PRINT__(x)
    #endif
#endif

#if (!defined __TRACE_PRINT__)
    #if (defined _TRACE_)
        #define __TRACE_PRINT__(x) x
    #else
        #define __TRACE_PRINT__(x)
    #endif
#endif


namespace daal
{
namespace services
{
namespace interface1
{

cpu_mask_t::cpu_mask_t()
{

    status = 0;

#if defined __PINNER_LINUX__

    ncpus  = 0;
    bit_parts_size = 0;
    cpu_set = NULL;
    for ( ncpus = sizeof(cpu_set_t)/CHAR_BIT; ncpus < 16*1024; ncpus <<= 1 )
    {
        cpu_set = CPU_ALLOC( ncpus );
        if ( cpu_set == NULL ) break;

        bit_parts_size = CPU_ALLOC_SIZE( ncpus );
        CPU_ZERO_S( bit_parts_size, cpu_set );

        const int err = sched_getaffinity( 0, bit_parts_size, cpu_set );
        if ( err == 0 ) break;

        CPU_FREE( cpu_set );
        cpu_set = NULL;
        if ( errno != EINVAL ) break;
    }

    if( cpu_set == NULL)
#else // defined __PINNER_WINDOWS__
    bool retval = GetThreadGroupAffinity(GetCurrentThread(), &ga );
    if(!retval)
#endif
    {
        __ERROR_PRINT__(fprintf(stderr,"ERROR (cpu_mask_t): failed to create cpu_mask_t\n"));
        status--;
    }

    return;
} // cpu_mask_t()

int cpu_mask_t::get_thread_affinity()
{

    if( status == 0 )
    {
#if defined __PINNER_LINUX__
        int err = pthread_getaffinity_np( pthread_self(), bit_parts_size, cpu_set );
        if ( err )
#else // defined __PINNER_WINDOWS__
        bool retval = GetThreadGroupAffinity(GetCurrentThread(), &ga );
        if(!retval)
#endif
        {
            __ERROR_PRINT__(fprintf(stderr,"ERROR (cpu_mask_t): failed to get current thread affinity\n"));
            status--;
        }
    }

    return status;
} // int get_thread_affinity()

int cpu_mask_t::set_thread_affinity()
{
    if( status == 0 )
    {
#if defined __PINNER_LINUX__

        int err = pthread_setaffinity_np( pthread_self(), bit_parts_size, cpu_set );
        if ( err )
#else // defined __PINNER_WINDOWS__

        bool retval = SetThreadGroupAffinity(GetCurrentThread(), &ga, NULL);
        if(!retval)
#endif
        {
            __ERROR_PRINT__(fprintf(stderr,"ERROR (cpu_mask_t): failed to set current thread affinity\n"));
            status--;
        }
    }

    return status;
} // int set_thread_affinity()

int cpu_mask_t::set_cpu_index(int cpu_idx)
{

    if( status == 0)
    {
#if defined __PINNER_LINUX__
        CPU_ZERO_S( bit_parts_size, cpu_set );
        CPU_SET_S( cpu_idx, bit_parts_size, cpu_set );
#else // defined __PINNER_WINDOWS__
        ga.Group = cpu_idx / MASK_WIDTH;
        ga.Mask  = cpu_idx % MASK_WIDTH;
#endif
    }

    return status;
} // int set_cpu_index(int cpu_idx)

int cpu_mask_t::get_status()
{
    return status;
} // int get_status()

cpu_mask_t::~cpu_mask_t()
{
#if defined __PINNER_LINUX__
    if(cpu_set != NULL)
    {
        CPU_FREE( cpu_set );
    }
#endif

    return;
} // ~cpu_mask_t()


thread_pinner_t::thread_pinner_t( ) : pinner_arena( nthreads = daal::threader_get_threads_number() ), tbb::task_scheduler_observer(pinner_arena)
{
    __TRACE_PRINT__(fprintf(stderr,"nthreads = %d\n",(int)nthreads));

    do_pinning = ( nthreads > 0 )?true:false;
    is_pinning.set(0);
    read_topology();

    return;
}/* thread_pinner_t() */


void thread_pinner_t::read_topology(  )
{
    status      = 0;
    max_threads = 0;
    cpu_queue   = NULL;

    if(do_pinning == false) return;

    /* Maximum cpu's amount */
    max_threads = _internal_daal_GetSysLogicalProcessorCount();

    /* Allocate memory for CPU queue */
    cpu_queue = (int*)daal::services::daal_malloc( max_threads * sizeof(int), 64);
    if(!cpu_queue)
    {
        status--;
        __ERROR_PRINT__(printf("ERROR (thread_pinner_t): cannot allocate memory for cpu_queue\n"));
        return;
    }

    /* Create cpu queue */
    _internal_daal_GetLogicalProcessorQueue(cpu_queue);

    /* Free all arrays created to read topology */
    _internal_daal_FreeArrays();

    /* Check if errors happened during topology reading */
    if( _internal_daal_GetStatus() !=0 )
    {
        status--;
        __ERROR_PRINT__(printf("ERROR (thread_pinner_t): error in topology reading\n"));
        return;
    }

    /* Start observing */
    observe( true );

    return;
}/* read_topology() */

void thread_pinner_t::on_scheduler_entry( bool )  /*override*/
{
    if ( do_pinning == false || status < 0 ) return;

    // read current thread index
    int thr_idx = tbb::task_arena::current_thread_index();

    // Get next cpu from topology queue
    int cpu_idx = cpu_queue[ thr_idx % max_threads ];

    __TRACE_PRINT__(fprintf(stderr,"\tINFO: set thread affinity: Thread %d on CPU %d\n", thr_idx, cpu_idx));


    // Allocate source and target affinity masks
    cpu_mask_t *target_mask = new cpu_mask_t;
    cpu_mask_t *source_mask = thread_mask.local();

    // Create source mask if it wasn't created for the tread before
    if (source_mask == NULL)
    {
        source_mask = new cpu_mask_t( );
        thread_mask.local() = source_mask;
    }

    // save source affinity mask to restore on exit
    status -= source_mask->get_thread_affinity();

    // Set ine bit corresponding to CPU to pin the thread
    status -=target_mask->set_cpu_index(cpu_idx);

    // Set thread affinity mask to 1 non-zero bit in corresponding to cpu_idx position
    status -= target_mask->set_thread_affinity();

    delete target_mask;

    return;
} /* void on_scheduler_entry()  */

void thread_pinner_t::on_scheduler_exit( bool )  /*override*/
{
    if ( do_pinning == false  || status < 0 ) return;

    // get current thread original mask
    cpu_mask_t *source_mask = thread_mask.local();

    if (source_mask == NULL)
    {
        status--;
        return;
    }
    else
    {
        // restore original thread affinity mask
        status -= source_mask->set_thread_affinity();
        if ( status < 0 )
        {
            status--;
            return;
        }
    }

    return;
} /* void on_scheduler_exit( bool ) */

int thread_pinner_t::get_status()
{
    return status;
} /* int get_status() */

bool thread_pinner_t::get_pinning()
{
    return do_pinning;
} /* bool get_pinning() */

bool thread_pinner_t::set_pinning(bool p)
{
    bool old_pinning = do_pinning;
    if(status == 0)do_pinning = p;

    return old_pinning;
} /* bool set_pinning(bool p) */


thread_pinner_t::~thread_pinner_t()
{
    observe( false );

    if(cpu_queue)daal::services::daal_free(cpu_queue);

    thread_mask.combine_each([] (cpu_mask_t * &source_mask){ delete source_mask; });

    return;
} /* ~thread_pinner_t() */


thread_pinner_t* getThreadPinner(bool create_pinner)
{
    static bool pinner_created = false;

    if(create_pinner == true || pinner_created == true)
    {
        static thread_pinner_t thread_pinner;
        if(thread_pinner.get_status() == 0)
        {
            pinner_created = true;
            return &thread_pinner;
        }
    }

    return NULL;
} /* thread_pinner_t* getThreadPinner() */


}
}
}

#endif /* #if !(defined DAAL_THREAD_PINNING_DISABLED) */
