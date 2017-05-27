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

import edu.iu.daal.*;

// packages from Daal 
import com.intel.daal.algorithms.implicit_als.PartialModel;
import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;

import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data.KeyValueDataCollection;

import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ALSTrainStep3 {

    private DistributedStep3Local algo;
    private long nFactor;
    private long numThreads;
    private static DaalContext daal_Context = new DaalContext();
    private ALSDaalCollectiveMapper collect_mapper;

    private DistributedPartialResultStep4 inputData;
    private long[] inputOffset;
    private KeyValueDataCollection inputDataOut;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep3.class);

    public ALSTrainStep3(long nFactor, long numThreads, long[] inputOffset,
                         KeyValueDataCollection inputDataOut, 
                         DistributedPartialResultStep4 inputData, 
                         ALSDaalCollectiveMapper collect_mapper)
    {
        this.algo = new DistributedStep3Local(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
        this.inputOffset = inputOffset;
        this.inputDataOut = inputDataOut;
        this.inputData = inputData;
        this.collect_mapper = collect_mapper;

        this.algo.parameter.setNFactors(this.nFactor);
        this.algo.parameter.setNumThreads(this.numThreads);

        long[] offsetArray = new long[1];
        offsetArray[0] = this.inputOffset[this.collect_mapper.getSelfID()];
        HomogenNumericTable offsetTable = new HomogenNumericTable(daal_Context, offsetArray, 1, 1);

        this.algo.input.set(PartialModelInputId.partialModel,
                        this.inputData.get(DistributedPartialResultStep4Id.outputOfStep4ForStep3));

        this.algo.input.set(Step3LocalCollectionInputId.partialModelBlocksToNode, inputDataOut);
        this.algo.input.set(Step3LocalNumericTableInputId.offset, offsetTable);

    }

    public DistributedPartialResultStep3 compute()
    {
        return this.algo.compute(); 
    }

    public KeyValueDataCollection communicate(KeyValueDataCollection step3_res, KeyValueDataCollection step4_input, 
            Table<ByteArray> step3LocalResult_table) throws IOException
    {

        try {

            byte[] serialStep3LocalResult = serializeStep3Result(step3_res);
            ByteArray step3LocalResult_harp = new ByteArray(serialStep3LocalResult, 0, serialStep3LocalResult.length);
            // Table<ByteArray> step3LocalResult_table = new Table<>(0, new ByteArrPlus());
            step3LocalResult_table.addPartition(new Partition<>(this.collect_mapper.getSelfID(), step3LocalResult_harp));
            this.collect_mapper.allgather("als", "sync-partial-step3-res", step3LocalResult_table);

            for(int j=0;j<this.collect_mapper.getNumWorkers();j++)
            {
                step4_input.set(j, (deserializeStep3Result(step3LocalResult_table.getPartition(j).get().get())).get(this.collect_mapper.getSelfID()));
            }

            return step4_input; 

        } catch (Exception e) 
        {
            LOG.error("Fail to serilization.", e);
            return null;
        }

    }

    private byte[] serializeStep3Result(KeyValueDataCollection res) throws IOException {
        // Create an output stream to serialize the numeric table 
        ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

        // Serialize the partialResult table into the output stream 
        res.pack();
        outputStream.writeObject(res);

        // Store the serialized data in an array 
        byte[] buffer = outputByteStream.toByteArray();
        return buffer;
    }

    private KeyValueDataCollection deserializeStep3Result(byte[] buffer) throws IOException, ClassNotFoundException 
    {

        // Create an input stream to deserialize the numeric table from the array 
        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        // Create a numeric table object 
        KeyValueDataCollection restoredRes = (KeyValueDataCollection)inputStream.readObject();
        restoredRes.unpack(daal_Context);

        return restoredRes;
    }

}

