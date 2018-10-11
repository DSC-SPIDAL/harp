/*
 * Copyright 2013-2016 Indiana University
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
 */

package edu.iu.daal_als;

import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;
import edu.iu.data_comm.HarpDAALComm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

// packages from Daal

public class ALSTrainStep3 {

    private DistributedStep3Local algo;
    private long nFactor;
    private long numThreads;
    private int self_id;
    private int num_mappers;
    private static DaalContext daal_Context = new DaalContext();
    private HarpDAALComm harpcomm;

    private DistributedPartialResultStep4 inputData;
    private KeyValueDataCollection inputDataOut;
    private NumericTable inputOffset;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep3.class);

    public ALSTrainStep3(long nFactor, long numThreads, int self_id, int num_mappers, 
                         NumericTable inputOffset,
                         DistributedPartialResultStep4 inputData, 
                         KeyValueDataCollection inputDataOut, 
                         HarpDAALComm harpcomm)
    {
        this.algo = new DistributedStep3Local(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
	this.self_id = self_id;
	this.num_mappers = num_mappers;
        this.inputOffset = inputOffset;
        this.inputData = inputData;
        this.inputDataOut = inputDataOut;
        this.harpcomm = harpcomm;

        this.algo.parameter.setNFactors(this.nFactor);

        this.algo.input.set(PartialModelInputId.partialModel,
                        this.inputData.get(DistributedPartialResultStep4Id.outputOfStep4ForStep3));

        this.algo.input.set(Step3LocalCollectionInputId.partialModelBlocksToNode, inputDataOut);
        this.algo.input.set(Step3LocalNumericTableInputId.offset, inputOffset);

    }

    public DistributedPartialResultStep3 compute()
    {
        return this.algo.compute(); 
    }

    public KeyValueDataCollection communicate(KeyValueDataCollection step3_res, KeyValueDataCollection step4_input) throws IOException
    {
	SerializableBase[] des_ouput = this.harpcomm.harpdaal_allgather(step3_res, "als", "broadcast_step3");
	for(int i=0;i<this.num_mappers;i++)
	{
	   KeyValueDataCollection res_out = (KeyValueDataCollection)(des_ouput[i]);
	   step4_input.set(i, res_out.get(this.self_id));
	}

	return step4_input;

    }

}

