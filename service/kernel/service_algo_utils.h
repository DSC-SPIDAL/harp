/* file: service_algo_utils.h */
/*******************************************************************************
* Copyright 2015-2018 Intel Corporation
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
//  Declaration of service utilities used with services structures
//--
*/
#ifndef __SERVICE_ALGO_UTILS_H__
#define __SERVICE_ALGO_UTILS_H__

#include "services/host_app.h"
#include "error_indexes.h"
#include "algorithms/algorithm_types.h"

namespace daal
{
namespace services
{
namespace internal
{

services::HostAppIface* hostApp(algorithms::Input& inp);
void setHostApp(const services::SharedPtr<services::HostAppIface>& pHostApp, algorithms::Input& inp);
services::HostAppIfacePtr getHostApp(daal::algorithms::Input& inp);

inline bool isCancelled(services::Status& s, services::HostAppIface* pHostApp)
{
    if(!pHostApp || !pHostApp->isCancelled())
        return false;
    s.add(services::ErrorUserCancelled);
    return true;
}

//////////////////////////////////////////////////////////////////////////////////////////
// Helper class handling cancellation status depending on the number of jobs to be done
//////////////////////////////////////////////////////////////////////////////////////////
class HostAppHelper
{
public:
    HostAppHelper(HostAppIface* hostApp, size_t maxJobsBeforeCheck) :
        _hostApp(hostApp), _maxJobsBeforeCheck(maxJobsBeforeCheck),
        _nJobsAfterLastCheck(0)
    {
    }
    bool isCancelled(services::Status& s, size_t nJobsToDo)
    {
        if(!_hostApp)
            return false;
        _nJobsAfterLastCheck += nJobsToDo;
        if(_nJobsAfterLastCheck < _maxJobsBeforeCheck)
            return false;
        _nJobsAfterLastCheck = 0;
        return services::internal::isCancelled(s, _hostApp);
    }

    void setup(size_t maxJobsBeforeCheck)
    {
        _maxJobsBeforeCheck = maxJobsBeforeCheck;
    }

    void reset(size_t maxJobsBeforeCheck)
    {
        setup(maxJobsBeforeCheck);
        _nJobsAfterLastCheck = 0;
    }

private:
    services::HostAppIface* _hostApp;
    size_t _maxJobsBeforeCheck; //granularity
    size_t _nJobsAfterLastCheck;
};

} // namespace internal
} // namespace services
} // namespace daal

#endif
