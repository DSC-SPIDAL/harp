/* file: parameter.cpp */
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

#include <jni.h>
#include "daal.h"
#include "em_gmm/JMethod.h"
#include "em_gmm/JParameter.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::em_gmm;

#define DefaultDense com_intel_daal_algorithms_em_gmm_Method_defaultDenseValue

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetNComponents
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cGetNComponents
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((em_gmm::Parameter *)parameterAddress)->nComponents;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetMaxIterations
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cGetMaxIterations
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((em_gmm::Parameter *)parameterAddress)->maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetAccuracyThreshold
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cGetAccuracyThreshold
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((em_gmm::Parameter *)parameterAddress)->accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cSetNComponents
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cSetNComponents
(JNIEnv *, jobject, jlong parameterAddress, jlong nComponents)
{
    ((em_gmm::Parameter *)parameterAddress)->nComponents = nComponents;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cSetMaxIterations
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cSetMaxIterations
(JNIEnv *, jobject, jlong parameterAddress, jlong maxIterations)
{
    ((em_gmm::Parameter *)parameterAddress)->maxIterations = maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cSetAccuracyThreshold
 * Signature:(JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cSetAccuracyThreshold
(JNIEnv *, jobject, jlong parameterAddress, jdouble accuracyThreshold)
{
    ((em_gmm::Parameter *)parameterAddress)->accuracyThreshold = accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetRegularizationFactor
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cGetRegularizationFactor
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((em_gmm::Parameter *)parameterAddress)->regularizationFactor;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cSetRegularizationFactor
 * Signature:(JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cSetRegularizationFactor
(JNIEnv *, jobject, jlong parameterAddress, jdouble regularizationFactor)
{
    ((em_gmm::Parameter *)parameterAddress)->regularizationFactor = regularizationFactor;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetCovarianceStorage
 * Signature:(J)D
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cGetCovarianceStorage
(JNIEnv *, jobject, jlong parameterAddress)
{
    return (jint)((em_gmm::Parameter *)parameterAddress)->covarianceStorage;
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_Parameter
 * Method:    cGetCovarianceStorage
 * Signature: (J)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_Parameter_cSetCovarianceStorage
(JNIEnv *, jobject, jlong parameterAddress, jint covarianceStorage)
{
    ((em_gmm::Parameter *)parameterAddress)->covarianceStorage = (em_gmm::CovarianceStorageId)covarianceStorage;
}
