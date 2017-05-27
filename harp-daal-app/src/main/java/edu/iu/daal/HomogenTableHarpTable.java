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

package edu.iu.daal;

import java.lang.System;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import edu.iu.harp.partition.Table;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.io.DataType;
import edu.iu.harp.resource.Array;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.NumericTable;

/**
 * @brief A class to convert data structure between DAAL's HomogenNumericTable
 * and Harp's Table 
 */
public class HomogenTableHarpTable<I, E extends Array<I>, T extends Table<E> > {

  private static final Logger LOG = Logger.getLogger(HomogenTableHarpTable.class);

  private T harp_table; 
  private NumericTable daal_table;

  private int table_size;                   //dimension of table
  private int vec_size;                     //dimension of vector in table 
  private int num_threads;                  //number of threads used in parallel copy

  private double[] buffer_array_double;     //double buffer array for parallel copy of data
  private float[] buffer_array_float;       //float buffer array for parallel copy of data 
  private int[] buffer_array_int;           //int buffer array for parallel copy of data

  private CopyObjDouble[] obj_list_double;  //obj list for copying double data
  private CopyObjFloat[] obj_list_float;    //obj list for copying float data
  private CopyObjInt[] obj_list_int;        //obj list for copying int data

  /**
   * @brief Constructor 
   *
   * @param harp_table
   * @param daal_table
   * @param table_size
   * @param vec_size
   * @param num_threads
   *
   * @return 
   */
  public HomogenTableHarpTable(T harp_table, 
                               NumericTable daal_table, 
                               int table_size, 
                               int vec_size, 
                               int num_threads) {

      this.harp_table = harp_table;
      this.daal_table = daal_table;
      this.table_size = table_size;
      this.vec_size = vec_size;
      this.num_threads = num_threads;

      this.buffer_array_double = null;
      this.buffer_array_float = null;
      this.buffer_array_int = null;

      this.obj_list_double = null;
      this.obj_list_float = null;
      this.obj_list_int = null;

  }

  /**
   * @brief Accessor to the harp table
   *
   * @return 
   */
  public T harp_table() {
      return this.harp_table;
  }

  /**
   * @brief Accessor to the daal table
   *
   * @return 
   */
  public NumericTable daal_table() {
      return this.daal_table;
  }

  /**
   * @brief Accessor to the obj_list_double
   *
   * @return 
   */
  public CopyObjDouble[] obj_list_double() {
      return this.obj_list_double;
  }

  /**
   * @brief Accessor to the buffer_array_double
   *
   * @return 
   */
  public double[] buffer_array_double() {
      return this.buffer_array_double;
  }

  /**
   * @brief Accessor to the obj_list_float
   *
   * @return 
   */
  public CopyObjFloat[] obj_list_float() {
      return this.obj_list_float;
  }

  /**
   * @brief Accessor to the buffer_array_float
   *
   * @return 
   */
  public float[] buffer_array_float() {
      return this.buffer_array_float;
  }

  /**
   * @brief Accessor to the obj_list_int
   *
   * @return 
   */
  public CopyObjInt[] obj_list_int() {
      return this.obj_list_int;
  }

  /**
   * @brief Accessor to the buffer_array_int
   *
   * @return 
   */
  public int[] buffer_array_int() {
      return this.buffer_array_int;
  }

  /**
   * @brief Convert harp table to daal HomogenNumericTable
   * double precision
   *
   * @return 
   */
  public void HarpToDaalDouble() {//{{{
        
      if (buffer_array_double == null)
        buffer_array_double = new double[table_size*vec_size];

      if (obj_list_double == null)
      {
          this.obj_list_double = new CopyObjDouble[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              double[] data = (double[])p.get().get(); 
              obj_list_double[table_entry] = new CopyObjDouble(data, table_entry);
              table_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferDouble(q, num_threads, table_size, this.obj_list_double, buffer_array_double, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }

      //load buffer into daal table
      // DoubleBuffer buffer_daal = DoubleBuffer.wrap(buffer_array_double);
      // daal_table.releaseBlockOfRows(0, table_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, table_size, buffer_array_double);

  }//}}}

  /**
   * @brief Convert daal HomogenNumericTable table to daal table
   * double precision
   *
   * @return 
   */
  public void DaalToHarpDouble() {//{{{

      if (buffer_array_double == null)
        buffer_array_double = new double[table_size*vec_size];

      if (obj_list_double == null)
      {
          this.obj_list_double = new CopyObjDouble[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              double[] data = (double[])p.get().get(); 
              obj_list_double[table_entry] = new CopyObjDouble(data, table_entry);
              table_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // DoubleBuffer buffer_daal = DoubleBuffer.allocate(vec_size*table_size);
      // buffer_daal = daal_table.getBlockOfRows(0, table_size, buffer_daal);
      // buffer_daal.get(buffer_array_double, 0, vec_size*table_size);
      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, table_size, buffer_array_double);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableDouble(q, num_threads, table_size, this.obj_list_double, buffer_array_double, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }


  }//}}}



  /**
   * @brief Convert harp table to daal HomogenNumericTable
   * float precision
   *
   * @return 
   */
  public void HarpToDaalFloat() {//{{{

      if (buffer_array_float == null)
        buffer_array_float = new float[table_size*vec_size];
      
      if (obj_list_float == null)
      {
          this.obj_list_float = new CopyObjFloat[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              float[] data = (float[])p.get().get(); 
              obj_list_float[table_entry] = new CopyObjFloat(data, table_entry);
              table_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferFloat(q, num_threads, table_size, this.obj_list_float, buffer_array_float, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }

    
      //load buffer into daal table
      // FloatBuffer buffer_daal = FloatBuffer.wrap(buffer_array_float);
      // daal_table.releaseBlockOfRows(0, table_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, table_size, buffer_array_float);

  }//}}}

  /**
   * @brief Convert daal HomogenNumericTable table to daal table
   * float precision
   *
   * @return 
   */
  public void DaalToHarpFloat() {//{{{

      if (buffer_array_float == null)
        buffer_array_float = new float[table_size*vec_size];
      
      if (obj_list_float == null)
      {
          this.obj_list_float = new CopyObjFloat[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              float[] data = (float[])p.get().get(); 
              obj_list_float[table_entry] = new CopyObjFloat(data, table_entry);
              table_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // FloatBuffer buffer_daal = FloatBuffer.allocate(vec_size*table_size);
      // buffer_daal = daal_table.getBlockOfRows(0, table_size, buffer_daal);
      // buffer_daal.get(buffer_array_float, 0, vec_size*table_size);
      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, table_size, buffer_array_float);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableFloat(q, num_threads, table_size, this.obj_list_float, buffer_array_float, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }

  }//}}}

  /**
   * @brief Convert harp table to daal HomogenNumericTable
   * int value 
   *
   * @return 
   */
  public void HarpToDaalInt() {//{{{

      if (buffer_array_int == null)
        buffer_array_int = new int[table_size*vec_size];
      
      if (obj_list_int == null)
      {
          this.obj_list_int = new CopyObjInt[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              int[] data = (int[])p.get().get(); 
              obj_list_int[table_entry] = new CopyObjInt(data, table_entry);
              table_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferInt(q, num_threads, table_size, this.obj_list_int, buffer_array_int, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }

      //load buffer into daal table
      // IntBuffer buffer_daal = IntBuffer.wrap(buffer_array_int);
      // daal_table.releaseBlockOfRows(0, table_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, table_size, (buffer_array_int));
  }//}}}


  /**
   * @brief Convert daal HomogenNumericTable table to daal table
   * int value
   *
   * @return 
   */
  public void DaalToHarpInt() {//{{{

      if (buffer_array_int == null)
        buffer_array_int = new int[table_size*vec_size];
      
      if (obj_list_int == null)
      {
          this.obj_list_int = new CopyObjInt[table_size];

          int table_entry = 0;
          for(Partition<E> p : harp_table.getPartitions())
          {
              int[] data = (int[])p.get().get(); 
              obj_list_int[table_entry] = new CopyObjInt(data, table_entry);
              table_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // IntBuffer buffer_daal = IntBuffer.allocate(vec_size*table_size);
      // buffer_daal = daal_table.getBlockOfRows(0, table_size, buffer_daal);
      // buffer_daal.get(buffer_array_int, 0, vec_size*table_size);
      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, table_size, buffer_array_int);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableInt(q, num_threads, table_size, this.obj_list_int, buffer_array_int, vec_size));
          threads[q].start();
      }

      for (int q = 0; q< num_threads; q++) {

          try
          {
              threads[q].join();

          }catch(InterruptedException e)
          {
              System.out.println("Thread interrupted.");
          }

      }

  }//}}}

}

