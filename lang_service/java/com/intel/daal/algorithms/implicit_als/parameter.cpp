/* file: parameter.cpp */
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

#include "implicit_als/JParameter.h"

using namespace daal;
using namespace daal::algorithms::implicit_als;

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cSetNFactors
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cSetNFactors
(JNIEnv *, jobject, jlong parAddr, jlong nFactors)
{
    ((Parameter *)parAddr)->nFactors = nFactors;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cGetNFactors
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cGetNFactors
(JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->nFactors;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cSetMaxIterations
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cSetMaxIterations
(JNIEnv *, jobject, jlong parAddr, jlong maxIterations)
{
    ((Parameter *)parAddr)->maxIterations = maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cGetMaxIterations
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cGetMaxIterations
(JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cSetAlpha
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cSetAlpha
(JNIEnv *, jobject, jlong parAddr, jdouble alpha)
{
    ((Parameter *)parAddr)->alpha = alpha;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cGetAlpha
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cGetAlpha
(JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->alpha;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cSetLambda
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cSetLambda
(JNIEnv *, jobject, jlong parAddr, jdouble lambda)
{
    ((Parameter *)parAddr)->lambda = lambda;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cGetLambda
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cGetLambda
(JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->lambda;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cSetPreferenceThreshold
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cSetPreferenceThreshold
(JNIEnv *, jobject, jlong parAddr, jdouble preferenceThreshold)
{
    ((Parameter *)parAddr)->preferenceThreshold = preferenceThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_Parameter
 * Method:    cGetPreferenceThreshold
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_implicit_1als_Parameter_cGetPreferenceThreshold
(JNIEnv *, jobject, jlong parAddr)
{
    return ((Parameter *)parAddr)->preferenceThreshold;
}
