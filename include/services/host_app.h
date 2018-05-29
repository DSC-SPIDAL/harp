/* file: host_app.h */
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
//  Interface of host application class used by the library
//--
*/

#ifndef __DAAL_HOST_APP_H__
#define __DAAL_HOST_APP_H__

#include "services/daal_defines.h"
#include "services/base.h"
#include "services/daal_shared_ptr.h"

namespace daal
{
namespace services
{

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{

/**
 *  <a name="DAAL-CLASS-SERVICES__HOSTAPPIFACE"></a>
 *  \brief Abstract class which defines callback interface for the host application of this library
 *         to enable such features as computation cancelling, progress bar, status bar, verbose, etc.
 */
class DAAL_EXPORT HostAppIface : public Base
{
public:
    DAAL_NEW_DELETE();
    HostAppIface();
    virtual ~HostAppIface();
    /**
     * This callback is called by compute() methods of the library algorithms.
     * If it returns true then compute() stops and returns 'ErrorUserCancelled' status
     * \return True when algorithm should be aborted
     */
    virtual bool isCancelled() = 0;

private:
    Base* _impl;
};
typedef services::SharedPtr<HostAppIface> HostAppIfacePtr;

} // namespace interface1
using interface1::HostAppIface;
using interface1::HostAppIfacePtr;

}
}
#endif //__DAAL_HOST_APP_H__
