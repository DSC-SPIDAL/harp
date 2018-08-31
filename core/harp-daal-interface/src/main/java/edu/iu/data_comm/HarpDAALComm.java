/*
 * Copyright 2013-2018 Indiana University
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

package edu.iu.data_comm;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStreamReader;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.ListIterator;

import edu.iu.dymoro.*;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Array;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.schstatic.StaticScheduler;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.data_aux.*;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.hadoop.conf.Configuration;

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

public class HarpDAALComm {
  private int self_id;
  private int master_id;
  private int num_workers;
  private DaalContext context;
  private CollectiveMapper mapper;
  private Table<ByteArray> comm_table;

  public HarpDAALComm(int self_id, int master_id, int num_workers, DaalContext context, CollectiveMapper mapper) {
    this.self_id = self_id;
    this.master_id = master_id;
    this.num_workers = num_workers;
    this.context = context;
    this.mapper = mapper;
    this.comm_table = new Table<>(0, new ByteArrPlus());
  }

  public SerializableBase harpdaal_braodcast(SerializableBase input, String contextName, String operationName, boolean useSpanTree) {//{{{
    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    if (this.self_id == this.master_id) {

      try {
        this.comm_table.addPartition(new Partition<>(this.master_id, serializeInput(input)));
      } catch (Exception e) {
        System.out.println("Fail to serialize" + e.toString());
        e.printStackTrace();
      }

    }

    // broadcast via harp
    this.mapper.barrier("barrier", "broadcast");
    boolean bcastStatus = false;
    bcastStatus = this.mapper.broadcast(contextName, operationName, this.comm_table, this.master_id, useSpanTree);
    this.mapper.barrier("barrier", "broadcast");

    if (!bcastStatus) {
      System.out.println("bcast not successful");
    } else {
      System.out.println("bcast successful");
    }

    // deserialization
    try {
      return deserializeOutput(this.comm_table.getPartition(this.master_id).get());

    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  public SerializableBase harpdaal_braodcast(SerializableBase input, int root_id, String contextName, String operationName, boolean useSpanTree) {//{{{
    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    if (this.self_id == root_id) {

      try {
        this.comm_table.addPartition(new Partition<>(root_id, serializeInput(input)));
      } catch (Exception e) {
        System.out.println("Fail to serialize" + e.toString());
        e.printStackTrace();
      }

    }

    // broadcast via harp
    this.mapper.barrier("barrier", "broadcast");
    boolean bcastStatus = false;
    bcastStatus = this.mapper.broadcast(contextName, operationName, this.comm_table, root_id, useSpanTree);
    this.mapper.barrier("barrier", "broadcast");

    if (!bcastStatus) {
      System.out.println("bcast not successful");
    } else {
      System.out.println("bcast successful");
    }

    // deserialization
    try {
      return deserializeOutput(this.comm_table.getPartition(root_id).get());

    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  public SerializableBase[] harpdaal_gather(SerializableBase input, String contextName, String operationName) {//{{{
    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    try {
      this.comm_table.addPartition(new Partition<>(this.self_id, serializeInput(input)));
    } catch (Exception e) {
      System.out.println("Fail to serialize" + e.toString());
      e.printStackTrace();
    }

    // reduce via harp
    this.mapper.barrier("barrier", "reduce");
    boolean reduceStatus = false;
    reduceStatus = this.mapper.reduce(contextName, operationName, this.comm_table, this.master_id);
    this.mapper.barrier("barrier", "reduce");

    if (!reduceStatus) {
      System.out.println("reduce not successful");
    } else {
      System.out.println("reducebcast successful");
    }

    // deserialization
    SerializableBase[] output = new SerializableBase[this.num_workers];
    try {

      if (this.self_id == this.master_id) {
        for (int i = 0; i < this.num_workers; i++) {
          output[i] = (SerializableBase) deserializeOutput(comm_table.getPartition(i).get());
        }

        return output;
      } else
        return null;

    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  public SerializableBase[] harpdaal_gather(SerializableBase input, int root_id, String contextName, String operationName) {//{{{
    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    try {
      this.comm_table.addPartition(new Partition<>(this.self_id, serializeInput(input)));
    } catch (Exception e) {
      System.out.println("Fail to serialize" + e.toString());
      e.printStackTrace();
    }

    // reduce via harp
    this.mapper.barrier("barrier", "reduce");
    boolean reduceStatus = false;
    reduceStatus = this.mapper.reduce(contextName, operationName, this.comm_table, root_id);
    this.mapper.barrier("barrier", "reduce");

    if (!reduceStatus) {
      System.out.println("reduce not successful");
    } else {
      System.out.println("reducebcast successful");
    }

    // deserialization
    SerializableBase[] output = new SerializableBase[this.num_workers];
    try {

      if (this.self_id == root_id) {
        for (int i = 0; i < this.num_workers; i++) {
          output[i] = (SerializableBase) deserializeOutput(comm_table.getPartition(i).get());
        }

        return output;
      } else
        return null;

    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  public SerializableBase[] harpdaal_gather(SerializableBase input, int local_id, int root_id, String contextName, String operationName) {//{{{

    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    try {
      this.comm_table.addPartition(new Partition<>(local_id, serializeInput(input)));
    } catch (Exception e) {
      System.out.println("Fail to serialize" + e.toString());
      e.printStackTrace();
    }

    // reduce via harp
    this.mapper.barrier("barrier", "reduce");
    boolean reduceStatus = false;
    reduceStatus = this.mapper.reduce(contextName, operationName, this.comm_table, root_id);
    this.mapper.barrier("barrier", "reduce");

    if (!reduceStatus) {
      System.out.println("reduce not successful");
    } else {
      System.out.println("reducebcast successful");
    }

    // deserialization
    SerializableBase[] output = new SerializableBase[this.num_workers];
    try {

      if (this.self_id == root_id) {

        int[] pid = comm_table.getPartitionIDs().toIntArray();
        for (int i = 0; i < pid.length; i++) {
          output[i] = (SerializableBase) deserializeOutput(comm_table.getPartition(pid[i]).get());
        }

        return output;
      } else
        return null;

    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  public SerializableBase[] harpdaal_allgather(SerializableBase input, String contextName, String operationName) {//{{{
    // clear the previous table content
    this.comm_table.free();

    // serialize the input obj
    try {
      this.comm_table.addPartition(new Partition<>(this.self_id, serializeInput(input)));
    } catch (Exception e) {
      System.out.println("Fail to serialize" + e.toString());
      e.printStackTrace();
    }

    // reduce via harp
    this.mapper.barrier("barrier", "allreduce");
    boolean allreduceStatus = false;
    allreduceStatus = this.mapper.allreduce(contextName, operationName, this.comm_table);
    this.mapper.barrier("barrier", "allreduce");

    if (!allreduceStatus) {
      System.out.println("allreduce not successful");
    } else {
      System.out.println("allreduce successful");
    }

    // deserialization
    SerializableBase[] output = new SerializableBase[this.num_workers];
    try {

      for (int i = 0; i < this.num_workers; i++) {
        output[i] = (SerializableBase) deserializeOutput(comm_table.getPartition(i).get());
      }

      return output;


    } catch (Exception e) {
      System.out.println("Fail to deserilize" + e.toString());
      e.printStackTrace();
      return null;
    }

  }//}}}

  private ByteArray serializeInput(SerializableBase input) throws IOException {//{{{
    /* Create an output stream to serialize the numeric table */
    ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

    /* Serialize the numeric table into the output stream */
    input.pack();
    outputStream.writeObject(input);

    /* Store the serialized data in an array */
    byte[] serializedInput = outputByteStream.toByteArray();

    return new ByteArray(serializedInput, 0, serializedInput.length);
  }//}}}

  private SerializableBase deserializeOutput(ByteArray byteArray) throws IOException, ClassNotFoundException {//{{{
    /* Create an input stream to deserialize the numeric table from the array */
    byte[] buffer = byteArray.get();
    ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
    ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

    /* Create a numeric table object */
    SerializableBase output = (SerializableBase) inputStream.readObject();
    output.unpack(this.context);

    return output;
  }//}}}

}

