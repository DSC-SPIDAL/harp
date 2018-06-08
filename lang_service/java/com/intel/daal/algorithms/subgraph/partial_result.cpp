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
#include "subgraph/JPartialResult.h"
#include "subgraph/JPartialResultId.h"

#include "common_helpers.h"

#define presWMatId com_intel_daal_algorithms_subgraph_PartialResultId_presWMatId
#define presHMatId com_intel_daal_algorithms_subgraph_PartialResultId_presHMatId
#define presRMSEId com_intel_daal_algorithms_subgraph_PartialResultId_presRMSEId

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::subgraph;

/*
 * Class:     Java_com_intel_daal_algorithms_subgraph_PartialResult
 * Method:    cNewPartialResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_PartialResult_cNewPartialResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<subgraph::DistributedPartialResult>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_subgraph_PartialResult
 * Method:    cGetPartialResultTable
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_PartialResult_cGetPartialResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if ( id == presWMatId )
    {
        return jniArgument<subgraph::DistributedPartialResult>::get<subgraph::DistributedPartialResultId, NumericTable>(resAddr, subgraph::presWMat);
    }
    else if(id == presHMatId)
    {
        return jniArgument<subgraph::DistributedPartialResult>::get<subgraph::DistributedPartialResultId, NumericTable>(resAddr, subgraph::presHMat);
    }
    else if(id == presRMSEId)
    {
        return jniArgument<subgraph::DistributedPartialResult>::get<subgraph::DistributedPartialResultId, NumericTable>(resAddr, subgraph::presRMSE);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_subgraph_PartialResult
 * Method:    cSetPartialResultTable
 * Signature:(JI)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_PartialResult_cSetPartialResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    jniArgument<subgraph::DistributedPartialResult>::set<subgraph::DistributedPartialResultId, NumericTable>(resAddr, id, ntAddr);
}
