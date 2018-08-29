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

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.LongArray;

import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;

// packages from Daal 
import com.intel.daal.algorithms.implicit_als.PartialModel;
import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

