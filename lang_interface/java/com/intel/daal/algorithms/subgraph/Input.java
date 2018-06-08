/* file: Input.java */
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

package com.intel.daal.algorithms.subgraph;

import java.lang.System.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.lang.Long;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__subgraph__INPUT"></a>
 * @brief Input objects for the subgraph algorithm in the batch and distributed mode and for the  
 */
public final class Input extends com.intel.daal.algorithms.Input {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public Input(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Sets input object for the subgraph algorithm
     * @param id    Identifier of the %input object for the subgraph algorithm
     * @param val   Value of the input object
     */
    public void set(InputId id, NumericTable val) {
        cSetInputTable(cObject, id.getValue(), val.getCObject());
    }

    /**
     * Returns input object for the subgraph algorithm
     * @param id Identifier of the input object
     * @return   Input object that corresponds to the given identifier
     */
    public NumericTable get(InputId id) {
        return new HomogenNumericTable(getContext(), cGetInputTable(cObject, id.getValue()));
    }

    public void readGraph() {
        cReadGraph(cObject);
    }

    public void readTemplate() {
        cReadTemplate(cObject);
    }

    public void initGraph() {
        cInitGraph(cObject);
    }

    public void initTemplate() {
        cInitTemplate(cObject);
    }

    public void initPartitioner() {
        cInitPartitioner(cObject);
    }

    public void initNumTable() {
        cInitNumTable(cObject);
    }

    public void initDTTable() {
        cInitDTTable(cObject);
    }

    public void initDTSub(int s) {
        cInitDTSub(cObject, s);
    }

    public void clearDTSub(int s) {
        cClearDTSub(cObject, s);
    }

    public void setToTable(int src, int dst){
        cSetToTable(cObject, src, dst);
    }

    public void sampleColors() {
        cSampleColors(cObject);
    }

    public int getSubVertN(int sub_itr) {
        return cGetSubVertN(cObject, sub_itr);
    }

    public int getReadInThd() {
        return cGetReadInThd(cObject);
    }

    public int getLocalVNum() {
        return cGetLocalVNum(cObject);
    }
    
    public int getTVNum() {
        return cGetTVNum(cObject);
    }

    public int getTENum() {
        return cGetTENum(cObject);
    }

    public int getLocalMaxV() {
        return cGetLocalMaxV(cObject);
    }

    public int getLocalADJLen() {
        return cGetLocalADJLen(cObject);
    }

    public void setGlobalMaxV(int id) {
        cSetGlobalMaxV(cObject, id);
    }

	public int getTotalDeg(){
		return cGetTotalDeg(cObject);
	}

	public int getMaxDeg(){
		return cGetMaxDeg(cObject);
	}

    public int getSubtemplateCount(){
        return cGetSubtemplateCount(cObject);
    }

    public int getMorphism(){
        return cGetMorphism(cObject);
    }

	public int getCombLen(int subid)
	{
		return cGetCombLen(cObject, subid);
	}

	public int getCombCur(int subid)
	{
		return cGetCombCur(cObject, subid);
	}

	public int getCombActiveCur(int subid)
	{
		return cGetCombActiveCur(cObject, subid);
	}

	public double getPeakMem()
	{
		return cGetPeakMem(cObject);
	}

    public double getThdWorkAvg()
    {
        return cGetThdWorkAvg(cObject);
    }

    public double getThdWorkStdev()
    {
        return cGetThdWorkStdev(cObject);
    }

	public void resetPeakMem()
	{
		cResetPeakMem(cObject);
	}

    public void freeInput() 
	{
        cFreeInput(cObject);
    }

    // for comm
    public void initComm(int mapper_num, int local_mapper_id, 
            long send_array_limit, boolean rotation_pipeline) 
    {
        cInitComm(cObject, mapper_num, local_mapper_id, send_array_limit, rotation_pipeline);
    }

    public void initCommPrepare(int mapper_id)
    {
        cInitCommPrepare(cObject, mapper_id);
    }

    public void initCommFinal()
    {
        cInitCommFinal(cObject);
    }

    public int sendCommParcelInit(int sub_id, int send_id)
    {
        return cSendCommParcelInit(cObject, sub_id, send_id);
    }

    public void sendCommParcelPrep(int parcel_id)
    {
        cSendCommParcelPrep(cObject, parcel_id);
    }

    public int getCommParcelPrepVNum()
    {
        return cGetCommParcelPrepVNum(cObject);
    }

    public int getCommParcelPrepCountLen()
    {
        return cGetCommParcelPrepCountLen(cObject);
    }
    public void updateRecvParcelInit(int comm_id)
    {
        cUpdateRecvParcelInit(cObject, comm_id);
    }

    public void updateRecvParcel()
    {
        cUpdateRecvParcel(cObject);
    }

    public void freeRecvParcel()
    {
        cFreeRecvParcel(cObject);
    }

    public void freeRecvParcelPip(int pipId)
    {
        cFreeRecvParcelPip(cObject, pipId);
    }

    public void calculateUpdateIds(int sub_id)
    {
        cCalculateUpdateIds(cObject, sub_id);
    }
    public void releaseUpdateIds()
    {
        cReleaseUpdateIds(cObject);
    }

    public void clearTaskUpdateList()
    {
        cClearTaskUpdateList(cObject);
    }

    public void sendCommParcelLoad()
    {
        cSendCommParcelLoad(cObject);
    }

    public long getDaalTableSize() {
        return cGetDaalTableSize(cObject);
    }

    public void uploadCommPrepare()
    {
        cUploadCommPrepare(cObject);
    }

    public void setSendVertexSize(int size)
    {
        cSetSendVertexSize(cObject, size);
    }

    public void setSendVertexArray(int dstId)
    {
        cSetSendVertexArray(cObject, dstId);
    }

    private native void cSetInputTable(long cInput, int id, long ntAddr);
    private native long cGetInputTable(long cInput, int id);
    private native void cReadGraph(long cInput);
    private native void cInitGraph(long cInput);
    private native void cReadTemplate(long cInput);
    private native void cInitTemplate(long cInput);
    private native void cInitPartitioner(long cInput);
    private native void cInitNumTable(long cInput);
    private native void cInitDTTable(long cInput);
    private native void cInitDTSub(long cInput, int s);
    private native void cClearDTSub(long cInput, int s);

    // for comm
    private native void cInitComm(long cInput, int mapper_num, int local_mapper_id, long send_array_limit, boolean rotation_pipeline);
    private native void cInitCommPrepare(long cInput, int mapper_id);
    private native void cInitCommFinal(long cInput);

    private native void cUploadCommPrepare(long cInput);
    private native long cGetDaalTableSize(long cInput);
    private native void cSetSendVertexSize(long cInput, int size);
    private native void cSetSendVertexArray(long cInput, int dstID);
	private native int cGetTotalDeg(long cInput);
	private native int cGetMaxDeg(long cInput);
	private native double cGetPeakMem(long cInput);
    private native double cGetThdWorkAvg(long cInput);
    private native double cGetThdWorkStdev(long cInput);

	private native void cResetPeakMem(long cInput);

    private native int cSendCommParcelInit(long cInput, int sub_id, int send_id);
    private native void cSendCommParcelPrep(long cInput, int parcel_id);
    private native int cGetCommParcelPrepVNum(long cInput);
    private native int cGetCommParcelPrepCountLen(long cInput);
    private native void cSendCommParcelLoad(long cInput);
    private native void cUpdateRecvParcelInit(long cInput, int comm_id);
    private native void cUpdateRecvParcel(long cInput);
    private native void cFreeRecvParcel(long cInput);
    private native void cFreeRecvParcelPip(long cInput, int pipId);
    private native void cCalculateUpdateIds(long cInput, int sub_id);
    private native void cReleaseUpdateIds(long cInput);
    private native void cClearTaskUpdateList(long cInput);

    private native void cSetToTable(long cInput, int src, int dst);
    private native void cSampleColors(long cInput);

    private native int cGetSubVertN(long cInput, int sub_itr);
    private native void cFreeInput(long cInput);
    private native int cGetReadInThd(long cInput);
    private native int cGetLocalVNum(long cInput);
    private native int cGetTVNum(long cInput);
    private native int cGetTENum(long cInput);
    private native int cGetLocalMaxV(long cInput);
    private native int cGetLocalADJLen(long cInput);
    private native int cGetSubtemplateCount(long cInput);
    private native int cGetMorphism(long cInput);
    private native void cSetGlobalMaxV(long cInput, int id);
    private native int cGetCombLen(long cInput, int id);
    private native int cGetCombCur(long cInput, int id);
    private native int cGetCombActiveCur(long cInput, int id);

}
