/* file: compression_parameter.cpp */
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

#include <jni.h>

#include "JCompressionParameter.h"
#include "daal.h"

using namespace daal;
using namespace daal::data_management;

#include "compression_types.i"

/*
 * Class:     com_intel_daal_data_1management_compression_CompressionParameter
 * Method:    cInit
 * Signature:(I)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_compression_CompressionParameter_cSetCompressionLevel
(JNIEnv *env, jobject, jlong parAddr, jint cLevel)
{
    switch(cLevel)
    {
    case DefaultLevel:
        (*((CompressionParameter *)parAddr)).level = defaultLevel;
        break;
    case Level0:
        (*((CompressionParameter *)parAddr)).level = level0;
        break;
    case Level1:
        (*((CompressionParameter *)parAddr)).level = level1;
        break;
    case Level2:
        (*((CompressionParameter *)parAddr)).level = level2;
        break;
    case Level3:
        (*((CompressionParameter *)parAddr)).level = level3;
        break;
    case Level4:
        (*((CompressionParameter *)parAddr)).level = level4;
        break;
    case Level5:
        (*((CompressionParameter *)parAddr)).level = level5;
        break;
    case Level6:
        (*((CompressionParameter *)parAddr)).level = level6;
        break;
    case Level7:
        (*((CompressionParameter *)parAddr)).level = level7;
        break;
    case Level8:
        (*((CompressionParameter *)parAddr)).level = level8;
        break;
    case Level9:
        (*((CompressionParameter *)parAddr)).level = level9;
        break;
    default:
        break;
    }
}

/*
* Class:     com_intel_daal_data_1management_compression_CompressionParameter
* Method:    cGetCompressionLevel
* Signature:(J)I
*/
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_compression_CompressionParameter_cGetCompressionLevel
(JNIEnv *env, jobject, jlong parameterAddress)
{
    CompressionLevel level = ((CompressionParameter *)parameterAddress)->level;
    switch(level)
    {
    case defaultLevel:
        return DefaultLevel;
    case level0:
        return Level0;
    case level1:
        return Level1;
    case level2:
        return Level2;
    case level3:
        return Level3;
    case level4:
        return Level4;
    case level5:
        return Level5;
    case level6:
        return Level6;
    case level7:
        return Level7;
    case level8:
        return Level8;
    case level9:
        return Level9;
    default:
        return DefaultLevel;
    }
}
