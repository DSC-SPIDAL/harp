/* file: distributed_iface.cpp */
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
#include "covariance/JDistributedIface.h"
#include "common_defines.i"
#include "covariance_types.i"
#include "java_distributed.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::covariance;

extern "C"
{

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cGetResult
     * Signature: (JII)J
     */
    JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cGetResult
    (JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
    {
        return jniDistributed<step2Master, covariance::Method, Distributed, defaultDense, singlePassDense, sumDense,
            fastCSR, singlePassCSR, sumCSR>::getResult(prec, method, algAddr);
    }

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cSetResult
     * Signature: (JIIJ)V
     */
    JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cSetResult
    (JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong resultAddr)
    {
        jniDistributed<step2Master, covariance::Method, Distributed, defaultDense, singlePassDense, sumDense,
            fastCSR, singlePassCSR, sumCSR>::setResult<covariance::Result>(prec, method, algAddr, resultAddr);
    }

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cGetPartialResult
     * Signature: (JII)J
     */
    JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cGetPartialResult
    (JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
    {
        return jniDistributed<step2Master, covariance::Method, Distributed, defaultDense, singlePassDense, sumDense,
            fastCSR, singlePassCSR, sumCSR>::getPartialResult(prec, method, algAddr);
    }

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cSetPartialResult
     * Signature: (JIIJZ)V
     */
    JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cSetPartialResult
    (JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method, jlong partialResultAddr, jboolean initFlag)
    {
        jniDistributed<step2Master, covariance::Method, Distributed, defaultDense, singlePassDense, sumDense,
            fastCSR, singlePassCSR, sumCSR>::setPartialResult<covariance::PartialResult>(prec, method, algAddr, partialResultAddr, initFlag);
    }

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cInitDistributedIface
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cInitDistributedIface
    (JNIEnv *env, jobject thisObj)
    {
        JavaVM *jvm;
        // Get pointer to the Java VM interface function table
        jint status = env->GetJavaVM(&jvm);
        if(status != 0)
        {
            env->ThrowNew(env->FindClass("java/lang/Exception"), "Unable to get pointer to the Java VM interface function table");
            return 0;
        }
        SharedPtr<JavaDistributed> *covDistributed = new SharedPtr<JavaDistributed>(new JavaDistributed(jvm, thisObj));
        return (jlong)covDistributed;
    }

    /*
     * Class:     com_intel_daal_algorithms_covariance_DistributedIface
     * Method:    cDispose
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_covariance_DistributedIface_cDispose
    (JNIEnv *env, jobject thisObj, jlong initAddr)
    {
        SharedPtr<DistributedIface<step2Master> > *covDistributed = (SharedPtr<DistributedIface<step2Master> > *)initAddr;
        if(initAddr) { delete covDistributed; }
    }
}
