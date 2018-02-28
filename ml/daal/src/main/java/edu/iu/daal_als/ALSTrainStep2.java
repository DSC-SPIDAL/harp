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
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data.KeyValueDataCollection;

import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ALSTrainStep2 {

    private DistributedStep2Master algo;
    private long nFactor;
    private long numThreads;
    private Table<ByteArray> inputData;
    private static DaalContext daal_Context = new DaalContext();
    private ALSDaalCollectiveMapper collect_mapper;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep2.class);

    public ALSTrainStep2(long nFactor, long numThreads, 
            Table<ByteArray> inputData, ALSDaalCollectiveMapper collect_mapper)
    {
        this.algo = new DistributedStep2Master(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
        this.inputData = inputData;
        this.collect_mapper = collect_mapper;

        this.algo.parameter.setNFactors(this.nFactor);
        // this.algo.parameter.setNumThreads(this.numThreads);
    }

    public NumericTable compute()
    {

        try {

            for (int j = 0; j < this.collect_mapper.getNumWorkers(); j++)
            {
                DistributedPartialResultStep1 step1LocalResultNew =  deserializeStep1Result(inputData.getPartition(j).get().get());
                this.algo.input.add(MasterInputId.inputOfStep2FromStep1, step1LocalResultNew);
            }

            return this.algo.compute().get(DistributedPartialResultStep2Id.outputOfStep2ForStep4);

        } catch (Exception e) 
        {
            LOG.error("Fail to serilization.", e);
            return null;
        }

    }

    public NumericTable communicate(NumericTable input_res) throws IOException
    {

        try {

            Table<ByteArray> step2Result_table = new Table<>(0, new ByteArrPlus());

            if (this.collect_mapper.getSelfID() == 0)
            {
                byte[] serialStep2MasterResult = serializeStep2MResult((HomogenNumericTable)input_res);
                ByteArray step2MasterResult_harp = new ByteArray(serialStep2MasterResult, 0, serialStep2MasterResult.length);
                step2Result_table.addPartition(new Partition<>(this.collect_mapper.getSelfID(), step2MasterResult_harp));
            }
            else
            {
                byte[] dummyRes = new byte[1];
                ByteArray step2LocalResult_harp = new ByteArray(dummyRes, 0, 1);
                step2Result_table.addPartition(new Partition<>(this.collect_mapper.getSelfID(), step2LocalResult_harp));
            }

            this.collect_mapper.allgather("als", "sync-step2-master", step2Result_table);

            return deserializeStep2MResult(step2Result_table.getPartition(0).get().get());


        } catch (Exception e) 
        {
            LOG.error("Fail to serilization.", e);
            return null;
        }

    }

    private DistributedPartialResultStep1 deserializeStep1Result(byte[] buffer) throws IOException, ClassNotFoundException 
    {
        // Create an input stream to deserialize the numeric table from the array 
        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        // Create a numeric table object 
        DistributedPartialResultStep1 restoredRes = (DistributedPartialResultStep1)inputStream.readObject();
        restoredRes.unpack(daal_Context);

        return restoredRes;
    }

    private byte[] serializeStep2MResult(HomogenNumericTable res) throws IOException {
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

    private HomogenNumericTable deserializeStep2MResult(byte[] buffer) throws IOException, ClassNotFoundException 
    {

        // Create an input stream to deserialize the numeric table from the array 
        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        // Create a numeric table object 
        HomogenNumericTable restoredRes = (HomogenNumericTable)inputStream.readObject();
        restoredRes.unpack(daal_Context);

        return restoredRes;
    }
}

