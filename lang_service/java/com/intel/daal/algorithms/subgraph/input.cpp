/* file: input.cpp */
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
#include "subgraph/JInput.h"
#include "subgraph/JMethod.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::subgraph;

/*
 * Class:     com_intel_daal_algorithms_subgraph_Input
 * Method:    cSetInputTable
 * Signature:(JIJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSetInputTable
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id, jlong ntAddr)
{
    jniInput<subgraph::Input>::set<subgraph::InputId, NumericTable>(inputAddr, id, ntAddr);
}

/**
 * @brief read in graph data from HDFS 
 *
 * @param env
 * @param thisObj
 * @param inputAddr
 *
 * @return 
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cReadGraph
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->readGraph();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitGraph
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->init_Graph();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cReadTemplate
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->readTemplate();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitTemplate
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->init_Template();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitNumTable
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->create_tables();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitDTTable
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->init_DTTable();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitComm
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint mapper_num, jint local_mapper_id, jlong send_array_limit, jboolean rotation_pipeline)
{
	((subgraph::Input*)inputAddr)->init_comm(mapper_num, local_mapper_id, send_array_limit, rotation_pipeline);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitCommPrepare
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint mapperid)
{
	((subgraph::Input*)inputAddr)->init_comm_prepare(mapperid);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitCommFinal
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->init_comm_final();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSetSendVertexSize
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint size)
{
	((subgraph::Input*)inputAddr)->setSendVertexSize(size);
}


JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSetSendVertexArray
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint dstID)
{
	((subgraph::Input*)inputAddr)->setSendVertexArray(dstID);
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSendCommParcelInit
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint sub_id, jint send_id)
{
	return (jint)(((subgraph::Input*)inputAddr)->sendCommParcelInit(sub_id, send_id));
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSendCommParcelPrep
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint parcel_id)
{
	((subgraph::Input*)inputAddr)->sendCommParcelPrep(parcel_id);
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetCommParcelPrepVNum
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->cur_parcel_v_num);
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetCommParcelPrepCountLen
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->cur_parcel_count_num);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSendCommParcelLoad
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->sendCommParcelLoad();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cUploadCommPrepare
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->upload_prep_comm();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cUpdateRecvParcelInit
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint comm_id)
{
	((subgraph::Input*)inputAddr)->updateRecvParcelInit(comm_id);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cUpdateRecvParcel
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->updateRecvParcel();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cFreeRecvParcel
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->freeRecvParcel();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cFreeRecvParcelPip
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint pipId)
{
	((subgraph::Input*)inputAddr)->freeRecvParcelPip(pipId);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cCalculateUpdateIds
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint sub_id)
{
	((subgraph::Input*)inputAddr)->calculate_update_ids(sub_id);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cReleaseUpdateIds
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->release_update_ids();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cClearTaskUpdateList
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->clear_task_update_list();
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetDaalTableSize
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jlong)(((subgraph::Input*)inputAddr)->daal_table_size);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitDTSub
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint s)
{
	((subgraph::Input*)inputAddr)->initDtSub(s);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cClearDTSub
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint s)
{
	((subgraph::Input*)inputAddr)->clearDtSub(s);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSetToTable
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint src, jint dst)
{
	((subgraph::Input*)inputAddr)->setToTable(src, dst);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSampleColors
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->sampleGraph();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cInitPartitioner
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->init_Partitioner();
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cFreeInput
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->free_input();
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetReadInThd
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getReadInThd());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetLocalVNum
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getLocalVNum());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetSubVertN
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint sub_itr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getSubVertN(sub_itr));
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetSubtemplateCount
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getSubtemplateCount());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetMorphism
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->computeMorphism());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetTVNum
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getTVNum());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetTENum
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getTENum());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetLocalMaxV
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getLocalMaxV());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetTotalDeg
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getTotalDeg());
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetMaxDeg
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getMaxDeg());
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cSetGlobalMaxV
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
	((subgraph::Input*)inputAddr)->setGlobalMaxV(id);
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetCombLen
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
	return (jint)(((subgraph::Input*)inputAddr)->getCombLen(id));
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetCombCur
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
	return (jint)(((subgraph::Input*)inputAddr)->getCombCur(id));
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetCombActiveCur
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
	return (jint)(((subgraph::Input*)inputAddr)->getCombActiveCur(id));
}

JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetPeakMem
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jdouble)(((subgraph::Input*)inputAddr)->getPeakMem());
}

JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetThdWorkAvg
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jdouble)(((subgraph::Input*)inputAddr)->thdwork_avg);
}

JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetThdWorkStdev
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jdouble)(((subgraph::Input*)inputAddr)->thdwork_stdev);
}

JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cResetPeakMem
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	((subgraph::Input*)inputAddr)->resetPeakMem();
}

JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetLocalADJLen
(JNIEnv *env, jobject thisObj, jlong inputAddr)
{
	return (jint)(((subgraph::Input*)inputAddr)->getLocalADJLen());
}
/*
 * Class:     com_intel_daal_algorithms_subgraph_Input
 * Method:    cGetInputTable
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_subgraph_Input_cGetInputTable
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
    return jniInput<subgraph::Input>::get<subgraph::InputId, NumericTable>(inputAddr, id);
}

