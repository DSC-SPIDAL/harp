/* file: result.cpp */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

#include <jni.h>
// #include "mf_sgd_types.i"

#include "JComputeMode.h"
#include "JComputeStep.h"
#include "mf_sgd/JMethod.h"
#include "mf_sgd/JResult.h"
#include "mf_sgd/JResultId.h"

#include "common_helpers.h"

#define resWMatId com_intel_daal_algorithms_mf_sgd_ResultId_resWMatId
#define resHMatId com_intel_daal_algorithms_mf_sgd_ResultId_resHMatId

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::mf_sgd;

/*
 * Class:     Java_com_intel_daal_algorithms_mf_sgd_Result
 * Method:    cNewResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_mf_1sgd_Result_cNewResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<mf_sgd::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_mf_sgd_Result
 * Method:    cGetResultTable
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_mf_1sgd_Result_cGetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if ( id == resWMatId )
    {
        return jniArgument<mf_sgd::Result>::get<mf_sgd::ResultId, NumericTable>(resAddr, mf_sgd::resWMat);
    }
    else if(id == resHMatId)
    {
        return jniArgument<mf_sgd::Result>::get<mf_sgd::ResultId, NumericTable>(resAddr, mf_sgd::resHMat);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_mf_sgd_Result
 * Method:    cSetResultTable
 * Signature:(JI)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_mf_1sgd_Result_cSetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    jniArgument<mf_sgd::Result>::set<mf_sgd::ResultId, NumericTable>(resAddr, id, ntAddr);
}
