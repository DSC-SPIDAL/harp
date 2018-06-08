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
#include "subgraph_types.i"

#include "JComputeMode.h"
#include "JComputeStep.h"
#include "subgraph/JMethod.h"
#include "subgraph/JResult.h"
#include "subgraph/JResultId.h"

#include "common_helpers.h"

#define resWMatId com_intel_daal_algorithms_subgraph_ResultId_resWMatId
#define resHMatId com_intel_daal_algorithms_subgraph_ResultId_resHMatId

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::subgraph;

/*
 * Class:     Java_com_intel_daal_algorithms_subgraph_Result
 * Method:    cNewResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_Result_cNewResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<subgraph::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_subgraph_Result
 * Method:    cGetResultTable
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_Result_cGetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if ( id == resWMatId )
    {
        return jniArgument<subgraph::Result>::get<subgraph::ResultId, NumericTable>(resAddr, subgraph::resWMat);
    }
    else if(id == resHMatId)
    {
        return jniArgument<subgraph::Result>::get<subgraph::ResultId, NumericTable>(resAddr, subgraph::resHMat);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_subgraph_Result
 * Method:    cSetResultTable
 * Signature:(JI)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Result_cSetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    jniArgument<subgraph::Result>::set<subgraph::ResultId, NumericTable>(resAddr, id, ntAddr);
}
