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
import com.intel.daal.algorithms.implicit_als.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ALSInitResult {

    private KeyValueDataCollection initStep1LocalResult = null;
    private KeyValueDataCollection initStep2LocalInput = null;
    private int num_mappers = 0;
    private int self_id = 0;
    private static DaalContext daal_Context = new DaalContext();
    private HarpDAALComm harpcomm;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep1.class);

    public ALSInitResult(KeyValueDataCollection initStep1LocalResult,
                         KeyValueDataCollection initStep2LocalInput,
                         int self_id, int num_mappers, HarpDAALComm harpcomm)
    {
        this.initStep1LocalResult = initStep1LocalResult;
        this.initStep2LocalInput = initStep2LocalInput;
        this.self_id = self_id;
	this.num_mappers = num_mappers;
	this.harpcomm = harpcomm;
    }
    
    /**
     * @brief communicate initStep1LocalResult among mappers 
     *
     * @return 
     */
    public void communicate() throws IOException
    {
	SerializableBase[] des_ouput = this.harpcomm.harpdaal_allgather(this.initStep1LocalResult, "als", "init_result"); 
	for(int i=0;i<this.num_mappers;i++)
	{
		KeyValueDataCollection initStep1LocalResult_des = (KeyValueDataCollection)(des_ouput[i]);
		this.initStep2LocalInput.set(i, initStep1LocalResult_des.get(this.self_id));
	}

    }


}

