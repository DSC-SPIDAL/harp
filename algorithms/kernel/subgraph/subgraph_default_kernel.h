/* file: subgraph_dense_default_kernel.h */
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
//  Declaration of template function that calculate subgraphs.
//--
*/

#ifndef __SUBGRAPH_FPK_H__
#define __SUBGRAPH_FPK_H__

#include <cstdlib> 
#include <cstdio> 
#include <assert.h>
#include <random>
#include <omp.h>
#include <stdio.h>
#include <string.h>

#include "task_scheduler_init.h"
#include "blocked_range.h"
#include "parallel_for.h"
#include "queuing_mutex.h"
#include "numeric_table.h"
#include "kernel.h"
#include "service_rng.h"
#include "services/daal_defines.h"
#include "services/daal_memory.h"
#include "service_micro_table.h"
#include "service_numeric_table.h"

#include "tbb/tick_count.h"

#include "algorithms/algorithm.h"
#include "algorithms/subgraph/subgraph_types.h"
#include "data_management/data/numeric_table.h"

using namespace tbb;
using namespace daal::data_management;

typedef queuing_mutex currentMutex_t;

namespace daal
{
namespace algorithms
{
namespace subgraph
{

namespace internal
{

/**
 * @brief computation kernel for subgraph distributed mode
 *
 * @tparam interm
 * @tparam method
 * @tparam cpu
 */
template<typename interm, daal::algorithms::subgraph::Method method, CpuType cpu>
class subgraphDistriKernel: public Kernel
{
public:

    /**
     * @brief compute and update W, H model by Training data
     *
     * @param[in] WPos row id of point in W model, stored in HomogenNumericTable 
     * @param[in] HPos col id of point in H model, stored in HomogenNumericTable
     * @param[in] Val  value of point, stored in HomogenNumericTable
     * @param[in,out] r[] model W and H
     * @param[in] par
     */
    daal::services::interface1::Status compute(Parameter* &par, Input* &input);

    void computeBottom(Parameter* &par, Input* &input);
    void computeBottomTBB(Parameter* &par, Input* &input);

    void computeNonBottomNbrSplit(Parameter* &par, Input* &input);
    void computeNonBottomNbrSplitTBB(Parameter* &par, Input* &input);

    void updateRemoteCountsNbrSplit(Parameter* &par, Input* &input);
    void updateRemoteCountsNbrSplitTBB(Parameter* &par, Input* &input);

    void updateRemoteCountsPipNbrSplit(Parameter* &par, Input* &input);
    void updateRemoteCountsPipNbrSplitTBB(Parameter* &par, Input* &input);

    void process_mem_usage(double& resident_set)
    {
        resident_set = 0.0;

        FILE *fp;
        long vmrss;
        int BUFFERSIZE=80;
        char *buf= new char[85];
        if((fp = fopen("/proc/self/status","r")))
        {
            while(fgets(buf, BUFFERSIZE, fp) != NULL)
            {
                if(strstr(buf, "VmRSS") != NULL)
                {
                    if (sscanf(buf, "%*s %ld", &vmrss) == 1){
                        // printf("VmSize is %dKB\n", vmrss);
                        resident_set = (double)vmrss;
                    }
                }
            }
        }

        fclose(fp);
        delete[] buf;
    }

};



} // namespace daal::internal
}
}
} // namespace daal

#endif
