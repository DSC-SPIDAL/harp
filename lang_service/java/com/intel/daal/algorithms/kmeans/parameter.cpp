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
#include "kmeans/JParameter.h"

using namespace daal::algorithms::kmeans;

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    initEuclidean
 * Signature:(JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_initEuclidean
(JNIEnv *, jobject, jlong nClusters, jlong maxIterations)
{
    return(jlong)(new Parameter(nClusters, maxIterations));
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetNClusters
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cGetNClusters
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((Parameter *)parameterAddress)->nClusters;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetMaxIterations
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cGetMaxIterations
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((Parameter *)parameterAddress)->maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetAccuracyThreshold
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cGetAccuracyThreshold
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((Parameter *)parameterAddress)->accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetGamma
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cGetGamma
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((Parameter *)parameterAddress)->gamma;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetAssignFlag
 * Signature:(J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cGetAssignFlag
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((Parameter *)parameterAddress)->assignFlag;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetNClusters
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cSetNClusters
(JNIEnv *, jobject, jlong parameterAddress, jlong nClusters)
{
    ((Parameter *)parameterAddress)->nClusters = nClusters;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetMaxIterations
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cSetMaxIterations
(JNIEnv *, jobject, jlong parameterAddress, jlong maxIterations)
{
    ((Parameter *)parameterAddress)->maxIterations = maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetAccuracyThreshold
 * Signature:(JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cSetAccuracyThreshold
(JNIEnv *, jobject, jlong parameterAddress, jdouble accuracyThreshold)
{
    ((Parameter *)parameterAddress)->accuracyThreshold = accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetGamma
 * Signature:(JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cSetGamma
(JNIEnv *, jobject, jlong parameterAddress, jdouble gamma)
{
    ((Parameter *)parameterAddress)->gamma = gamma;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetAssignFlag
 * Signature:(JZ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_Parameter_cSetAssignFlag
(JNIEnv *, jobject, jlong parameterAddress, jboolean assignFlag)
{
    ((Parameter *)parameterAddress)->assignFlag = assignFlag;
}
