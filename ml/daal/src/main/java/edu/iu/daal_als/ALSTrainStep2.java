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
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;
import edu.iu.data_comm.HarpDAALComm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

// packages from Daal

public class ALSTrainStep2 {

    private DistributedStep2Master algo;
    private long nFactor;
    private long numThreads;
    private int num_mappers;
    private DistributedPartialResultStep1[] inputData;
    private static DaalContext daal_Context = new DaalContext();
    private HarpDAALComm harpcomm;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep2.class);

    public ALSTrainStep2(long nFactor, long numThreads, int num_mappers, 
            DistributedPartialResultStep1[] inputData, HarpDAALComm harpcomm)
    {
        this.algo = new DistributedStep2Master(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
	this.num_mappers = num_mappers;
        this.inputData = inputData;
        this.harpcomm = harpcomm;

        this.algo.parameter.setNFactors(this.nFactor);
    }

    public NumericTable compute()
    {

	    for (int j = 0; j < this.num_mappers; j++)
                this.algo.input.add(MasterInputId.inputOfStep2FromStep1, this.inputData[j]);

            return this.algo.compute().get(DistributedPartialResultStep2Id.outputOfStep2ForStep4);

    }

    public NumericTable communicate(NumericTable input_res) throws IOException
    {
	//broadcast input_res from master_mapper
	SerializableBase res_out = this.harpcomm.harpdaal_braodcast(input_res, "als", "broadcast_step2", true);
	return (NumericTable)res_out;
    }
    
}

