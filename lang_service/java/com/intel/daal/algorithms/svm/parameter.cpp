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
#include "svm/JParameter.h"
#include "daal.h"

using namespace daal::services;
using namespace daal::algorithms;

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cInitDefault
 * Signature:(JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cInitDefault(JNIEnv *, jobject)
{
    return(jlong)(new svm::Parameter());
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cInitWithKernel
 * Signature:(JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cInitWithKernel(JNIEnv *, jobject, jlong kernelAddr)
{
    kernel_function::KernelIfacePtr kernel =
        staticPointerCast<kernel_function::KernelIface, AlgorithmIface>(*(SharedPtr<AlgorithmIface> *)kernelAddr);
    return(jlong)(new svm::Parameter(kernel));
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetC
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetC
(JNIEnv *env, jobject obj, jlong parAddr, jdouble val)
{
    ((svm::Parameter *)parAddr)->C = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetC
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetC
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jdouble)((svm::Parameter *)parAddr)->C;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetAccuracyThreshold
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetAccuracyThreshold
(JNIEnv *env, jobject obj, jlong parAddr, jdouble val)
{
    ((svm::Parameter *)parAddr)->accuracyThreshold = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetAccuracyThreshold
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetAccuracyThreshold
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jdouble)((svm::Parameter *)parAddr)->accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetTau
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetTau
(JNIEnv *env, jobject obj, jlong parAddr, jdouble val)
{
    ((svm::Parameter *)parAddr)->tau = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetTau
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetTau
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jdouble)((svm::Parameter *)parAddr)->tau;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetMaxIterations
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetMaxIterations
(JNIEnv *env, jobject obj, jlong parAddr, jlong val)
{
    ((svm::Parameter *)parAddr)->maxIterations = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetMaxIterations
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetMaxIterations
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jlong)((svm::Parameter *)parAddr)->maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetCacheSize
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetCacheSize
(JNIEnv *env, jobject obj, jlong parAddr, jlong val)
{
    ((svm::Parameter *)parAddr)->cacheSize = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetCacheSize
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetCacheSize
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jlong)((svm::Parameter *)parAddr)->cacheSize;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetDoShrinking
 * Signature:(JZ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetDoShrinking
(JNIEnv *env, jobject obj, jlong parAddr, jboolean val)
{
    ((svm::Parameter *)parAddr)->doShrinking = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetDoShrinking
 * Signature:(J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetDoShrinking
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jboolean)((svm::Parameter *)parAddr)->doShrinking;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetShrinkingStep
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetShrinkingStep
(JNIEnv *env, jobject obj, jlong parAddr, jlong val)
{
    ((svm::Parameter *)parAddr)->shrinkingStep = val;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cGetShrinkingStep
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cGetShrinkingStep
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return(jlong)((svm::Parameter *)parAddr)->shrinkingStep;
}

/*
 * Class:     com_intel_daal_algorithms_svm_Parameter
 * Method:    cSetKernel
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svm_Parameter_cSetKernel
(JNIEnv *env, jobject obj, jlong parAddr, jlong kernelAddr)
{
    kernel_function::KernelIfacePtr kernel =
        staticPointerCast<kernel_function::KernelIface, AlgorithmIface>(*(SharedPtr<AlgorithmIface> *)kernelAddr);
    ((svm::Parameter *)parAddr)->kernel = kernel;
}
