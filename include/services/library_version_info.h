/* file: library_version_info.h */
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
//  Intel(R) DAAL version information.
//--
*/

#ifndef __LIBRARY_VERSION_INFO_H__
#define __LIBRARY_VERSION_INFO_H__


#define __INTEL_DAAL_BUILD_DATE 21990101

#define __INTEL_DAAL__          2199
#define __INTEL_DAAL_MINOR__    9
#define __INTEL_DAAL_UPDATE__   9

#define INTEL_DAAL_VERSION (__INTEL_DAAL__ * 10000 + __INTEL_DAAL_MINOR__ * 100 + __INTEL_DAAL_UPDATE__)


#include "services/base.h"

namespace daal
{
/**
 * @defgroup services Services
 * \copydoc daal::services
 * @{
 */
namespace services
{

namespace interface1
{
/**
 * @defgroup library_version_info Extracting Version Information
 * \brief Provides information about the version of Intel(R) Data Analytics Acceleration Library.
 * @ingroup services
 * @{
 */
/**
 * <a name="DAAL-CLASS-SERVICES__LIBRARYVERSIONINFO"></a>
 * \brief Provides information about the version of Intel(R) Data Analytics Acceleration Library.
 * <!-- \n<a href="DAAL-REF-LIBRARYVERSIONINFO-STRUCTURE">LibraryVersionInfo structure details and Optimization Notice</a> -->
 */
class DAAL_EXPORT LibraryVersionInfo: public Base
{
public:
    const int    majorVersion;   /*!< Major library version */
    const int    minorVersion;   /*!< Minor library version */
    const int    updateVersion;  /*!< Update library version */
    const char *productStatus;   /*!< Product library status */
    const char *build;           /*!< Library build */
    const char *build_rev;       /*!< Library build revision */
    const char *name;            /*!< Library name */
    const char *processor;       /*!< Instruction set supported by the processor */

    LibraryVersionInfo();
    ~LibraryVersionInfo();
};
/** @} */
} // namespace interface1
using interface1::LibraryVersionInfo;

}
/** @} */
}
#endif
