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
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;
import edu.iu.data_comm.HarpDAALComm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

// packages from Daal

public class ALSTrainStep1 {

    private DistributedStep1Local algo;
    private long nFactor;
    private long numThreads;
    private int num_mappers;
    private DistributedPartialResultStep4 inputData;
    private static DaalContext daal_Context = new DaalContext();
    private HarpDAALComm harpcomm;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep1.class);

    public ALSTrainStep1(long nFactor, long numThreads, int num_mappers, 
            DistributedPartialResultStep4 inputData, HarpDAALComm harpcomm)
    {
        this.algo = new DistributedStep1Local(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
	this.num_mappers = num_mappers;
        this.inputData = inputData;
        this.harpcomm = harpcomm;

        this.algo.parameter.setNFactors(this.nFactor);

        // Set input objects for the algorithm 
        this.algo.input.set(PartialModelInputId.partialModel,
                inputData.get(DistributedPartialResultStep4Id.outputOfStep4ForStep1));

    }

    public DistributedPartialResultStep1 compute()
    {
        return this.algo.compute();
    }

    public DistributedPartialResultStep1[] communicate(DistributedPartialResultStep1 res) throws IOException
    {
	    SerializableBase[] des_ouput = this.harpcomm.harpdaal_allgather(res, "als", "comm_train_step1");
	    DistributedPartialResultStep1[] res_out = new DistributedPartialResultStep1[this.num_mappers];
	    for(int i=0;i<this.num_mappers;i++)
		    res_out[i] = (DistributedPartialResultStep1)(des_ouput[i]);

	    return res_out;
    }

}

