/* file: library_version_info.cpp */
/*******************************************************************************
* Copyright 2015-2017 Intel Corporation
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
//  Definitions of structures used for environment detection.
//--
*/

#include "library_version_info.h"
#include "_daal_version.h"
#include "service_defines.h"
#include "env_detect.h"
#include "mkl_daal.h"

static const char *cpu_long_names[] = {
    "Generic",
    "Supplemental Streaming SIMD Extensions 3",
    "Intel(R) Streaming SIMD Extensions 4.2",
    "Intel(R) Advanced Vector Extensions",
    "Intel(R) Advanced Vector Extensions 2",
    "Intel(R) Xeon Phi(TM) processors/coprocessors based on Intel(R) Advanced Vector Extensions 512",
    "Intel(R) Xeon(R) processors based on Intel(R) Advanced Vector Extensions 512",
    "Intel(R) Xeon Phi(TM) processors based on Intel(R) Advanced Vector Extensions 512 with support of AVX512_4FMAPS and AVX512_4VNNIW instruction groups"
};

DAAL_EXPORT daal::services::LibraryVersionInfo::LibraryVersionInfo() :
    majorVersion(MAJORVERSION), minorVersion(MINORVERSION), updateVersion(UPDATEVERSION),
    productStatus(PRODUCTSTATUS), build(BUILD), build_rev(BUILD_REV), name(PRODUCT_NAME_STR),
    processor(cpu_long_names[daal::services::Environment::getInstance()->getCpuId()+2*fpk_serv_cpuisknm()])
{
}

DAAL_EXPORT daal::services::LibraryVersionInfo::~LibraryVersionInfo()
{
}
