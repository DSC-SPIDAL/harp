/* file: _daal_version.h */
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

#ifndef ___DAAL_VERSION_H__
#define ___DAAL_VERSION_H__

#include "_daal_version_data.h"

#if PRODUCT_STATUS == 'A'
    #define PRODUCTSTATUS "Alpha"
    #define PRODUCTSTATUSDLL " Alpha"
    #define SUBBUILD 0
#endif
#if PRODUCT_STATUS == 'B'
    #define PRODUCTSTATUS "Beta"
    #define PRODUCTSTATUSDLL " Beta"
    #define SUBBUILD 0
#endif
#if PRODUCT_STATUS == 'P'
    #define PRODUCTSTATUS "Product"
    #define PRODUCTSTATUSDLL ""
    #define SUBBUILD 1
#endif

/* Intermediate defines */
#define FILE_VERSION1(a,b,c,d) FILE_VERSION0(a,b,c,d)
#define FILE_VERSION0(a,b,c,d) #a "." #b "." #c "." #d

#define PRODUCT_VERSION1(a,b) PRODUCT_VERSION0(a, b)
#define PRODUCT_VERSION0(a,b) #a "." #b PRODUCTSTATUSDLL

/* The next 3 defines need to use in *.rc files */
/* instead of symbolic constants like "10.0.2.0" */

#define FILE_VERSION MAJORVERSION, MINORVERSION, UPDATEVERSION, SUBBUILD
#define FILE_VERSION_STR FILE_VERSION1(MAJORVERSION,MINORVERSION,UPDATEVERSION,SUBBUILD)
#define PRODUCT_VERSION_STR PRODUCT_VERSION1(MAJORVERSION,MINORVERSION)

#define PRODUCT_NAME_STR "Intel(R) Data Analytics Acceleration Library\0"

#endif
