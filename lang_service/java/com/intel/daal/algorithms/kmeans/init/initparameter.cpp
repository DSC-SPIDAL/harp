/* file: initparameter.cpp */
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
#include "kmeans/init/JInitParameter.h"
#include "kmeans/init/JInitDistributedStep2LocalPlusPlusParameter.h"

using namespace daal::algorithms;

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    initEuclidean
 * Signature:(JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_init
(JNIEnv *, jobject, jlong nClusters, jlong startingIndex)
{
    return(jlong)(new kmeans::init::Parameter(nClusters, startingIndex));
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetNClusters
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cGetNClusters
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::Parameter *)parameterAddress)->nClusters;
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cGetNRowsTotal
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::Parameter *)parameterAddress)->nRowsTotal;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cGetMaxIterations
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cGetOffset
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::Parameter *)parameterAddress)->offset;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitParameter
* Method:    cGetOversamplingFactor
* Signature: (J)D
*/
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cGetOversamplingFactor
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::Parameter *)parameterAddress)->oversamplingFactor;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitParameter
* Method:    cGetNRounds
* Signature: (J)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cGetNRounds
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::Parameter *)parameterAddress)->nRounds;
}


/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetNClusters
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cSetNRowsTotal
(JNIEnv *, jobject, jlong parameterAddress, jlong nRowsTotal)
{
    ((kmeans::init::Parameter *)parameterAddress)->nRowsTotal = nRowsTotal;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cSetNClusters
(JNIEnv *, jobject, jlong parameterAddress, jlong nClusters)
{
    ((kmeans::init::Parameter *)parameterAddress)->nClusters = nClusters;
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_Parameter
 * Method:    cSetMaxIterations
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cSetOffset
(JNIEnv *, jobject, jlong parameterAddress, jlong offset)
{
    ((kmeans::init::Parameter *)parameterAddress)->offset = offset;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitParameter
* Method:    cSetOversamplingFactor
* Signature: (JD)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cSetOversamplingFactor
(JNIEnv *, jobject, jlong parameterAddress, jdouble oversamplingFactor)
{
    ((kmeans::init::Parameter *)parameterAddress)->oversamplingFactor = oversamplingFactor;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitParameter
* Method:    cSetNRounds
* Signature: (JJ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitParameter_cSetNRounds
(JNIEnv *, jobject, jlong parameterAddress, jlong nRounds)
{
    ((kmeans::init::Parameter *)parameterAddress)->nRounds = nRounds;
}

/////////////////////////////////////// plusPlus methods ///////////////////////////////////////////////////////
///////////////////////////////////////   step2Local     ///////////////////////////////////////////////////////
/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter
* Method:    init
* Signature: (JZ)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter_init
(JNIEnv *, jobject, jlong nClusters, jboolean bFirstIteration)
{
    return(jlong)(new kmeans::init::DistributedStep2LocalPlusPlusParameter(nClusters, bFirstIteration));
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter
* Method:    cGetIsFirstIteration
* Signature: (J)Z
*/
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter_cGetIsFirstIteration
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::DistributedStep2LocalPlusPlusParameter *)parameterAddress)->firstIteration;
}


/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter
* Method:    cGetOutputForStep5Required
* Signature: (J)Z
*/
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter_cGetOutputForStep5Required
(JNIEnv *, jobject, jlong parameterAddress)
{
    return((kmeans::init::DistributedStep2LocalPlusPlusParameter *)parameterAddress)->outputForStep5Required;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter
* Method:    cSetIsFirstIteration
* Signature: (JZ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter_cSetIsFirstIteration
(JNIEnv *, jobject, jlong parameterAddress, jboolean val)
{
    ((kmeans::init::DistributedStep2LocalPlusPlusParameter *)parameterAddress)->firstIteration = val;
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter
* Method:    cSetOutputForStep5Required
* Signature: (JZ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2LocalPlusPlusParameter_cSetOutputForStep5Required
(JNIEnv *, jobject, jlong parameterAddress, jboolean val)
{
    ((kmeans::init::DistributedStep2LocalPlusPlusParameter *)parameterAddress)->outputForStep5Required = val;
}
