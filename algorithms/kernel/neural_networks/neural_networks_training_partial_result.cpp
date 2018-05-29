/* file: neural_networks_training_partial_result.cpp */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
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

#include "neural_networks_training_partial_result.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace training
{
PartialResult::PartialResult() : daal::algorithms::PartialResult(lastStep1LocalPartialResultId + 1)
{}

NumericTablePtr PartialResult::get(Step1LocalPartialResultId id) const
{
    return NumericTable::cast(Argument::get(id));
}

void PartialResult::set(Step1LocalPartialResultId id, const NumericTablePtr &value)
{
    Argument::set(id, value);
}

Status PartialResult::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const
{
    return checkNumericTable(get(batchSize).get(), batchSizeStr(), 0, 0, 1, 1);
}

DistributedPartialResult::DistributedPartialResult() : daal::algorithms::PartialResult(lastStep2MasterPartialResultId + 1)
{
    set(resultFromMaster, training::ResultPtr(new Result()));
}

training::ResultPtr DistributedPartialResult::get(Step2MasterPartialResultId id) const
{
    return Result::cast(Argument::get(id));
}

void DistributedPartialResult::set(Step2MasterPartialResultId id, const training::ResultPtr &value)
{
    Argument::set(id, value);
}

Status DistributedPartialResult::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const
{
    return Status();
}

}
}
}
}
