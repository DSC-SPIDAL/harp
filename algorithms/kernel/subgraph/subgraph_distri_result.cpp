/* file: subgraph_distri_result.cpp */
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
//  Implementation of subgraph classes.
//  non-template funcs
//--
*/

#include "algorithms/subgraph/subgraph_types.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace subgraph
{
namespace interface1
{

/** Default constructor */
DistributedPartialResult::DistributedPartialResult() : daal::algorithms::PartialResult(4) {

        thread_num = 0;
        cc_ato = NULL;
        count_local_root = NULL;
        count_comm_root = NULL;

}

NumericTablePtr DistributedPartialResult::get(DistributedPartialResultId id) const
{
    return staticPointerCast<NumericTable, SerializationIface>(Argument::get(id));
}

void DistributedPartialResult::set(DistributedPartialResultId id, const NumericTablePtr &value)
{
    Argument::set(id, value);
}

daal::services::interface1::Status DistributedPartialResult::check(const daal::algorithms::Parameter *parameter, int method) const {services::Status s; return s;}
daal::services::interface1::Status DistributedPartialResult::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const {services::Status s; return s;}

void DistributedPartialResult::init_model(int threads)
{
    thread_num = threads;

    cc_ato = new double[thread_num];
    count_local_root = new double[thread_num];
    count_comm_root = new double[thread_num];

}

} // namespace interface1
} // namespace subgraph
} // namespace algorithm
} // namespace daal
