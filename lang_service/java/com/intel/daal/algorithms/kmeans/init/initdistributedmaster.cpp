/* file: initdistributedmaster.cpp */
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
#include "kmeans/init/JInitDistributedStep2Master.h"
#include "kmeans/init/JInitDistributedStep3Master.h"
#include "kmeans/init/JInitDistributedStep5Master.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::kmeans::init;

#define AllMethodsList\
    kmeans::init::Method, Distributed, deterministicDense, randomDense, deterministicCSR, randomCSR

#define PlusPlusMethodsList\
    kmeans::init::Method, Distributed, plusPlusDense, parallelPlusDense, plusPlusCSR, parallelPlusCSR

#define ParallelPlusMethodsList\
    kmeans::init::Method, Distributed, parallelPlusDense, parallelPlusCSR

/*
 * Class:     com_intel_daal_algorithms_kmeans_Distributed
 * Method:    cInit
 * Signature:(II)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cInit
(JNIEnv *env, jobject thisObj, jint prec, jint method, jlong nClusters)
{
    return jniDistributed<step2Master, AllMethodsList>::newObj(prec, method, nClusters);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cSetResult
 * Signature: (JIIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cSetResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong resultAddr)
{
    jniDistributed<step2Master, AllMethodsList>::setResult<kmeans::init::Result>(prec, method, algAddr, resultAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cGetResult
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cGetResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step2Master, AllMethodsList>::getResult(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cSetPartialResult
 * Signature: (JIIJZ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cSetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong partialResultAddr, jboolean initFlag)
{
    jniDistributed<step2Master, AllMethodsList>::
        setPartialResult<kmeans::init::PartialResult>(prec, method, algAddr, partialResultAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cGetPartialResult
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cGetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step2Master, AllMethodsList>::getPartialResult(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cInitParameter
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cInitParameter
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step2Master, AllMethodsList>::getParameter(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cGetInput
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cGetInput
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step2Master, AllMethodsList>::getInput(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kmeans_DistributedStep2Master
 * Method:    cClone
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep2Master_cClone
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step2Master, AllMethodsList>::getClone(prec, method, algAddr);
}

/////////////////////////////////////// plusPlus methods ///////////////////////////////////////////////////////
///////////////////////////////////////   step3Master     ///////////////////////////////////////////////////////
/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cInit
* Signature: (IIJ)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cInit
(JNIEnv *env, jobject thisObj, jint prec, jint method, jlong nClusters)
{
    return jniDistributed<step3Master, PlusPlusMethodsList>::newObj(prec, method, nClusters);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cInitParameter
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cInitParameter
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step3Master, PlusPlusMethodsList>::getParameter(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cGetInput
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cGetInput
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step3Master, PlusPlusMethodsList>::getInput(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cSetPartialResult
* Signature: (JIIJZ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cSetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong partialResultAddr, jboolean initFlag)
{
    jniDistributed<step3Master, PlusPlusMethodsList>::setPartialResult<
        kmeans::init::DistributedStep3MasterPlusPlusPartialResult>(prec, method, algAddr, partialResultAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cGetPartialResult
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cGetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step3Master, PlusPlusMethodsList>::getPartialResult(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master
* Method:    cClone
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep3Master_cClone
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step3Master, PlusPlusMethodsList>::getClone(prec, method, algAddr);
}

///////////////////////////////////////   step5Master     ///////////////////////////////////////////////////////
/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cInit
* Signature: (IIJ)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cInit
(JNIEnv *env, jobject thisObj, jint prec, jint method, jlong nClusters)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::newObj(prec, method, nClusters);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cInitParameter
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cInitParameter
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::getParameter(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cGetInput
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cGetInput
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::getInput(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cGetResult
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cGetResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::getResult(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cSetResult
* Signature: (JIIJ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cSetResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong resultAddr)
{
    jniDistributed<step5Master, ParallelPlusMethodsList>::setResult<kmeans::init::Result>(prec, method, algAddr, resultAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cGetPartialResult
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cGetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::getPartialResult(prec, method, algAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cSetPartialResult
* Signature: (JIIJZ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cSetPartialResult
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong partialResultAddr, jboolean initFlag)
{
    jniDistributed<step5Master, ParallelPlusMethodsList>::setPartialResult<
        kmeans::init::DistributedStep5MasterPlusPlusPartialResult>(prec, method, algAddr, partialResultAddr);
}

/*
* Class:     com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master
* Method:    cClone
* Signature: (JII)J
*/
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kmeans_init_InitDistributedStep5Master_cClone
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniDistributed<step5Master, ParallelPlusMethodsList>::getClone(prec, method, algAddr);
}
