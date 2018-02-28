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

import com.intel.daal.algorithms.implicit_als.*;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.services.DaalContext;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.ByteArray;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

// packages from Daal
// import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
// import com.intel.daal.algorithms.implicit_als.training.*;
// import com.intel.daal.algorithms.implicit_als.training.init.*;

public class ALSInitResult {

    private KeyValueDataCollection initStep1LocalResult = null;
    private KeyValueDataCollection initStep2LocalInput = null;
    private int self_id = 0;
    private Table<ByteArray> comm_table = null;
    private static DaalContext daal_Context = new DaalContext();
    private ALSDaalCollectiveMapper collect_mapper;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep1.class);

    public ALSInitResult(KeyValueDataCollection initStep1LocalResult,
                         KeyValueDataCollection initStep2LocalInput,
                         int self_id, ALSDaalCollectiveMapper collect_mapper)
    {
        this.initStep1LocalResult = initStep1LocalResult;
        this.initStep2LocalInput = initStep2LocalInput;
        this.self_id = self_id;
        this.collect_mapper = collect_mapper;
        this.comm_table = new Table<>(0, new ByteArrPlus());
    }
    
    /**
     * @brief communicate initStep1LocalResult among mappers 
     *
     * @return 
     */
    public void communicate() throws IOException
    {
        try {

            byte[] serial_initStep1LocalRes = serialinitStep1LocalRes(this.initStep1LocalResult);
            ByteArray serial_initStep1LocalRes_harp= new ByteArray(serial_initStep1LocalRes, 0, serial_initStep1LocalRes.length);

            comm_table.addPartition(new Partition<>(this.collect_mapper.getSelfID(), serial_initStep1LocalRes_harp));

            //reduce to master node with id 0
            this.collect_mapper.allgather("als", "sync-init-res", comm_table);

        } catch (Exception e) 
        {
            LOG.error("Fail to serilization.", e);
        }

    }

    /**
     * @brief assign initStep1LocalResult to initStep2LocalInput 
     *
     * @return 
     */
    public void compute()
    {
        for (int j=0; j< this.collect_mapper.getNumWorkers(); j++ ) 
        {
            //deserialization 
            try {

                KeyValueDataCollection initStep1LocalResult_des = deserialinitStep1LocalRes(this.comm_table.getPartition(j).get().get()); 
                this.initStep2LocalInput.set(j, initStep1LocalResult_des.get(this.collect_mapper.getSelfID()));

            }catch (Exception e) 
            {
                LOG.error("Fail to deserialization.", e);
            }
        
        }
    }

    /**
     * @brief serialization helper function 
     *
     * @param res
     *
     * @return 
     */
    private byte[] serialinitStep1LocalRes(KeyValueDataCollection res) throws IOException {
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

    /**
     * @brief deserialization helper function
     *
     * @param buffer
     *
     * @return 
     */
    private KeyValueDataCollection deserialinitStep1LocalRes(byte[] buffer) throws IOException, ClassNotFoundException
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

