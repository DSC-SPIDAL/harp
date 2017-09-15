/** file algorithm_base_impl.cpp */
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
//
//--
*/

#include "algorithm_base.h"
#include "algorithm_base_mode_impl.h"
#if !(defined DAAL_THREAD_PINNING_DISABLED)
    #include "service_thread_pinner.h"
#endif

namespace daal
{
namespace algorithms
{
template<ComputeMode mode>
services::Status AlgorithmImpl<mode>::computeNoThrow()
{
    this->setParameter();

    services::Status s;
    if(this->isChecksEnabled())
    {
        s = this->checkComputeParams();
        if(!s)
            return s;
    }

    if(!this->allocatePartialResultMemory())
        return services::Status(services::ErrorMemoryAllocationFailed);

    this->_ac->setArguments(this->_in, this->_pres, this->_par);
    this->_ac->setErrorCollection(this->_errors);

    if(this->isChecksEnabled())
    {
        s = this->checkResult();
        if(!s)
            return s;
    }

    if(!this->getInitFlag())
    {
        s = this->initPartialResult();
        if(!s)
            return s;
        this->setInitFlag(true);
    }

    s = setupCompute();
    if(s)
    {
#if !(defined DAAL_THREAD_PINNING_DISABLED)
        daal::services::interface1::thread_pinner_t* pinner = daal::services::interface1::getThreadPinner(false);

        if( pinner != NULL )
        {
            pinner->execute([&]() { s |=  this->_ac->compute(); });
        }
        else
#endif
        {
            s =  this->_ac->compute();
        }
    }

    s |= resetCompute();
    return s;
}

/**
 * Computes final results of the algorithm in the %batch mode without possibility of throwing an exception.
 */
services::Status AlgorithmImpl<batch>::computeNoThrow()
{
    this->setParameter();

    if(this->isChecksEnabled())
    {
        services::Status _s = this->checkComputeParams();
        if(!_s)
            return _s;
    }

    services::Status s = this->allocateResultMemory();
    if(!s)
        return s.add(services::ErrorMemoryAllocationFailed);

    this->_ac->setArguments(this->_in, this->_res, this->_par);
    this->_ac->setErrorCollection(this->_errors);

    if(this->isChecksEnabled())
    {
        s = this->checkResult();
        if(!s)
            return s;
    }

    s = setupCompute();
    if(s)
    {
#if !(defined DAAL_THREAD_PINNING_DISABLED)
        daal::services::interface1::thread_pinner_t* pinner = daal::services::interface1::getThreadPinner(false);

        if( pinner != NULL )
        {
            pinner->execute([&]() { s |=  this->_ac->compute(); });
        }
        else
#endif
        {
            s |=  this->_ac->compute();
        }
    }

    if(resetFlag)
        s |= resetCompute();
    _res = this->_ac->getResult();
    return s;
}

template class interface1::AlgorithmImpl<online>;
template class interface1::AlgorithmImpl<distributed>;
} // namespace daal
} // namespace algorithms
