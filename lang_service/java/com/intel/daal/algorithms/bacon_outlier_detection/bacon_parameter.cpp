/* file: bacon_parameter.cpp */
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

#include <jni.h>/* Header for class com_intel_daal_algorithms_bacon_outlier_detection_Parameter */

#include "daal.h"
#include "bacon_outlier_detection/JParameter.h"
#include "bacon_outlier_detection/JInitializationMethod.h"

#define baconMedianValue        com_intel_daal_algorithms_bacon_outlier_detection_InitializationMethod_baconMedianValue
#define baconMahalanobisValue   com_intel_daal_algorithms_bacon_outlier_detection_InitializationMethod_baconMahalanobisValue

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cSetInitializationMethod
 * Signature:(JI)I
 */
JNIEXPORT void JNICALL
Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cSetInitializationMethod
(JNIEnv *env, jobject thisObj, jlong parAddr, jint initMethodValue)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    ((Parameter *)parAddr)->initMethod = (InitializationMethod)initMethodValue;
}

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cGetInitializationMethod
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL
Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cGetInitializationMethod
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    return(int)(((Parameter *)parAddr)->initMethod);
}

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cSetAlpha
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cSetAlpha
(JNIEnv *env, jobject thisObj, jlong parAddr, jdouble alpha)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    ((Parameter *)parAddr)->alpha = alpha;
}

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cGetAlpha
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cGetAlpha
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    return((Parameter *)parAddr)->alpha;
}

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cSetToleranceToConverge
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL
Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cSetToleranceToConverge
(JNIEnv *env, jobject thisObj, jlong parAddr, jdouble tol)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    ((Parameter *)parAddr)->toleranceToConverge = tol;
}

/*
 * Class:     com_intel_daal_algorithms_bacon_outlier_detection_Parameter
 * Method:    cGetToleranceToConverge
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL
Java_com_intel_daal_algorithms_bacon_1outlier_1detection_Parameter_cGetToleranceToConverge
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    using namespace daal;
    using namespace daal::algorithms::bacon_outlier_detection;
    return((Parameter *)parAddr)->toleranceToConverge;
}
