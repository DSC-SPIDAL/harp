/* file: training_init_parameter.cpp */
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

#include "daal.h"

#include "implicit_als/training/init/JInitParameter.h"

using namespace daal;
using namespace daal::algorithms::implicit_als::training::init;
using namespace daal::data_management;
using namespace daal::services;

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cSetFullNUsers
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cSetFullNUsers
  (JNIEnv *, jobject, jlong parAddr, jlong fullNUsers)
{
    ((Parameter *)parAddr)->fullNUsers = fullNUsers;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cGetFullNUsers
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cGetFullNUsers
  (JNIEnv *, jobject, jlong parAddr)
{
    return (jlong)(((Parameter *)parAddr)->fullNUsers);
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cSetNFactors
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cSetNFactors
  (JNIEnv *, jobject, jlong parAddr, jlong nFactors)
{
    ((Parameter *)parAddr)->nFactors = nFactors;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cGetNFactors
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cGetNFactors
  (JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->nFactors;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cSetSeed
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cSetSeed
  (JNIEnv *, jobject, jlong parAddr, jlong seed)
{
    ((Parameter *)parAddr)->seed = seed;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cGetSeed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cGetSeed
    (JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->seed;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_training_init_InitParameter
 * Method:    cSetEngine
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_training_init_InitParameter_cSetEngine
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong engineAddr)
{
    (((Parameter *)cParameter))->engine = staticPointerCast<algorithms::engines::BatchBase, algorithms::AlgorithmIface> (*(SharedPtr<algorithms::AlgorithmIface> *)engineAddr);
}
