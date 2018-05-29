/* file: mcg59_batch_impl.h */
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

/*
//++
//  Implementation of the class defining the mcg59 engine
//--
*/

#include "engines/mcg59/mcg59.h"
#include "engine_batch_impl.h"
#include "service_rng.h"
#include "service_numeric_table.h"

static const int leapfrogMethodErrcode  = -1002;
static const int skipAheadMethodErrcode = -1003;

namespace daal
{
namespace algorithms
{
namespace engines
{
namespace mcg59
{
namespace internal
{

template<CpuType cpu, typename algorithmFPType = DAAL_ALGORITHM_FP_TYPE, Method method = defaultDense>
class BatchImpl : public algorithms::engines::mcg59::interface1::Batch<algorithmFPType, method>, public algorithms::engines::internal::BatchBaseImpl
{
public:
    typedef algorithms::engines::mcg59::interface1::Batch<algorithmFPType, method> super1;
    typedef algorithms::engines::internal::BatchBaseImpl super2;
    BatchImpl(size_t seed = 777) : baseRng(seed, __DAAL_BRNG_MCG59) {}

    void *getState() DAAL_C11_OVERRIDE
    {
        return baseRng.getState();
    }

    int getStateSize() const DAAL_C11_OVERRIDE
    {
        return baseRng.getStateSize();
    }

    services::Status saveStateImpl(byte* dest) const DAAL_C11_OVERRIDE
    {
        DAAL_CHECK(!baseRng.saveState((void *)dest), ErrorIncorrectErrorcodeFromGenerator);
        return services::Status();
    }

    services::Status loadStateImpl(const byte* src) DAAL_C11_OVERRIDE
    {
        DAAL_CHECK(!baseRng.loadState((const void *)src), ErrorIncorrectErrorcodeFromGenerator);
        return services::Status();
    }

    services::Status leapfrogImpl(size_t threadNum, size_t nThreads) DAAL_C11_OVERRIDE
    {
        int errcode = baseRng.leapfrog(threadNum, nThreads);
        services::Status s;
        if(errcode == leapfrogMethodErrcode) s.add(ErrorLeapfrogUnsupported);
        else if(errcode) s.add(ErrorIncorrectErrorcodeFromGenerator);
        return s;
    }

    services::Status skipAheadImpl(size_t nSkip) DAAL_C11_OVERRIDE
    {
        int errcode = baseRng.skipAhead(nSkip);
        services::Status s;
        if(errcode == skipAheadMethodErrcode) s.add(ErrorSkipAheadUnsupported);
        else if (errcode) s.add(ErrorIncorrectErrorcodeFromGenerator);
        return s;
    }

    virtual BatchImpl<cpu, algorithmFPType, method> *cloneImpl() const DAAL_C11_OVERRIDE
    {
        return new BatchImpl<cpu, algorithmFPType, method>(*this);
    }

    ~BatchImpl() {}

protected:
    BatchImpl(const BatchImpl<cpu, algorithmFPType, method> &other) : super1(other), super2(other), baseRng(other.baseRng) {}

    daal::internal::BaseRNGs<cpu> baseRng;
};

} // namespace interface1
} // namespace mcg59
} // namespace engines
} // namespace algorithms
} // namespace daal
