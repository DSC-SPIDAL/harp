/* file: parameter.cpp */
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
#include <string>
#include <cstring>
#include "subgraph_types.i"

#include "JComputeMode.h"
#include "subgraph/JParameter.h"
#include "subgraph/JMethod.h"

#include "common_helpers.h"


USING_COMMON_NAMESPACES()
using namespace daal::algorithms::subgraph;

/*
 * Class:     com_intel_daal_algorithms_subgraph_Parameter
 * Method:    cSetInputTable
 * cSetParameters(this.cObject,learningRate, lambda, Dim_r, Dim_w,  Dim_h, iteration, thread_num, tbb_grainsize, Avx_explicit );
 * Signature:(JIJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetParameters
(JNIEnv *env, jobject thisObj, jlong parAddr, jint thread_num, jint core_num, jint tpc, jint affinity, jint nbrtasklen, jint verbose)
{
	((subgraph::Parameter*)parAddr)->_thread_num = thread_num;
	((subgraph::Parameter*)parAddr)->_core_num = core_num;
	((subgraph::Parameter*)parAddr)->_tpc = tpc;
	((subgraph::Parameter*)parAddr)->_affinity = affinity;
	((subgraph::Parameter*)parAddr)->_nbr_task_len = nbrtasklen;
	((subgraph::Parameter*)parAddr)->_verbose = verbose;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetStage
(JNIEnv *env, jobject thisObj, jlong parAddr, jint stage)
{
	((subgraph::Parameter*)parAddr)->_stage = stage;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetOmpSchedule
(JNIEnv *env, jobject thisObj, jlong parAddr, jstring opt)
{
    jboolean isCopy;
    const char* opt_c = env->GetStringUTFChars(opt, &isCopy); 
    std::string opt_s(opt_c);
	((subgraph::Parameter*)parAddr)->_omp_schedule = opt_s;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetNbrTaskLen
(JNIEnv *env, jobject thisObj, jlong parAddr, jint len)
{
	((subgraph::Parameter*)parAddr)->_nbr_task_len = len;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetSubItr
(JNIEnv *env, jobject thisObj, jlong parAddr, jint sub_itr)
{
	((subgraph::Parameter*)parAddr)->_sub_itr = sub_itr;
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cSetPipId
(JNIEnv *env, jobject thisObj, jlong parAddr, jint id)
{
	((subgraph::Parameter*)parAddr)->_pip_id = id;
}

JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cGetTotalCounts
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
	return (jdouble)(((subgraph::Parameter*)parAddr)->_total_counts);
}

JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_subgraph_Parameter_cGetUpdateCounts
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
	return (jdouble)(((subgraph::Parameter*)parAddr)->_update_counts);
}
