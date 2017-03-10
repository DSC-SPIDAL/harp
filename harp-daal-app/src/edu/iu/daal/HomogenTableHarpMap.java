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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

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
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.NumericTable;

/**
 * @brief A class to convert data structure between DAAL's HomogenNumericTable
 * and Harp's Map 
 */
public class HomogenTableHarpMap<E extends Object> {

  private static final Logger LOG = Logger.getLogger(HomogenTableHarpMap.class);

  private Int2ObjectOpenHashMap<E> harp_map; 
  private Int2ObjectOpenHashMap<Integer> harp_index_map;
  private NumericTable daal_table;

  private int map_size;                   //dimension of map
  private int vec_size;                     //dimension of vector in map 
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
   * @param harp_map
   * @param daal_table
   * @param map_size
   * @param vec_size
   * @param num_threads
   *
   * @return 
   */
  public HomogenTableHarpMap(Int2ObjectOpenHashMap<E> harp_map, 
                             Int2ObjectOpenHashMap<Integer> harp_index_map,
                             NumericTable daal_table, 
                             int map_size, 
                             int vec_size, 
                             int num_threads) {

      this.harp_map = harp_map;
      this.harp_index_map = harp_index_map;
      this.daal_table = daal_table;
      this.map_size = map_size;
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
   * @brief Accessor to the harp map
   *
   * @return 
   */
  public Int2ObjectOpenHashMap<E> harp_map() {
      return this.harp_map;
  }

  /**
   * @brief Accessor to the harp index map
   *
   * @return 
   */
  public Int2ObjectOpenHashMap<Integer> harp_index_map() {
      return this.harp_index_map;
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
   * @brief Convert harp map to daal HomogenNumericTable
   * double precision
   *
   * @return 
   */
  public void HarpToDaalDouble() {//{{{
        
      if (buffer_array_double == null)
        buffer_array_double = new double[map_size*vec_size];
      
      if (obj_list_double == null)
      {
          this.obj_list_double = new CopyObjDouble[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              double[] data = (double[])entry.getValue();
              this.obj_list_double[map_entry] = new CopyObjDouble(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferDouble(q, num_threads, map_size, this.obj_list_double, buffer_array_double, vec_size));
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
      // daal_table.releaseBlockOfRows(0, map_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, map_size, buffer_array_double);

  }//}}}

  /**
   * @brief Convert daal HomogenNumericTable table to daal map
   * double precision
   *
   * @return 
   */
  public void DaalToHarpDouble() {//{{{

      if (buffer_array_double == null)
        buffer_array_double = new double[map_size*vec_size];
      
      if (obj_list_double == null)
      {
          this.obj_list_double = new CopyObjDouble[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              double[] data = (double[])entry.getValue();
              this.obj_list_double[map_entry] = new CopyObjDouble(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // DoubleBuffer buffer_daal = DoubleBuffer.allocate(vec_size*map_size);
      // buffer_daal = daal_table.getBlockOfRows(0, map_size, buffer_daal);
      // buffer_daal.get(buffer_array_double, 0, vec_size*map_size);

      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, map_size, buffer_array_double);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableDouble(q, num_threads, map_size, this.obj_list_double, buffer_array_double, vec_size));
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
   * @brief Convert harp map to daal HomogenNumericTable
   * float precision
   *
   * @return 
   */
  public void HarpToDaalFloat() {//{{{
        
      if (buffer_array_float == null)
        buffer_array_float = new float[map_size*vec_size];
      
      if (obj_list_float == null)
      {
          this.obj_list_float = new CopyObjFloat[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              float[] data = (float[])entry.getValue();
              this.obj_list_float[map_entry] = new CopyObjFloat(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferFloat(q, num_threads, map_size, this.obj_list_float, buffer_array_float, vec_size));
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
      // daal_table.releaseBlockOfRows(0, map_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, map_size, buffer_array_float);

  }//}}}

  /**
   * @brief Convert daal HomogenNumericTable table to daal map
   * float precision
   *
   * @return 
   */
  public void DaalToHarpFloat() {//{{{

      if (buffer_array_float == null)
        buffer_array_float = new float[map_size*vec_size];
      
      if (obj_list_float == null)
      {
          this.obj_list_float = new CopyObjFloat[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              float[] data = (float[])entry.getValue();
              this.obj_list_float[map_entry] = new CopyObjFloat(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // FloatBuffer buffer_daal = FloatBuffer.allocate(vec_size*map_size);
      // buffer_daal = daal_table.getBlockOfRows(0, map_size, buffer_daal);
      // buffer_daal.get(buffer_array_float, 0, vec_size*map_size);

      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, map_size, buffer_array_float);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableFloat(q, num_threads, map_size, this.obj_list_float, buffer_array_float, vec_size));
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
   * @brief Convert harp map to daal HomogenNumericTable
   * int value
   *
   * @return 
   */
  public void HarpToDaalInt() {//{{{
        
      if (buffer_array_int == null)
        buffer_array_int = new int[map_size*vec_size];
      
      if (obj_list_int == null)
      {
          this.obj_list_int = new CopyObjInt[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              int[] data = (int[])entry.getValue();
              this.obj_list_int[map_entry] = new CopyObjInt(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskTableToBufferInt(q, num_threads, map_size, this.obj_list_int, buffer_array_int, vec_size));
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
      // daal_table.releaseBlockOfRows(0, map_size, buffer_daal);
      ((HomogenBMNumericTable)daal_table).releaseBlockOfRowsByte(0, map_size, buffer_array_int);

  }//}}}

  /**
   * @brief Convert daal HomogenNumericTable table to daal map
   * int value
   *
   * @return 
   */
  public void DaalToHarpInt() {//{{{

      if (buffer_array_int == null)
        buffer_array_int = new int[map_size*vec_size];
      
      if (obj_list_int == null)
      {
          this.obj_list_int = new CopyObjInt[map_size];
          ObjectIterator<Int2ObjectMap.Entry<E>> harp_map_itr = harp_map.int2ObjectEntrySet().fastIterator();

          int map_entry = 0;
          while(harp_map_itr.hasNext())
          {
              Int2ObjectMap.Entry<E> entry = harp_map_itr.next();
              int[] data = (int[])entry.getValue();
              this.obj_list_int[map_entry] = new CopyObjInt(data, map_entry);
              this.harp_index_map.put(entry.getIntKey(),new Integer(map_entry)); 
              map_entry++;
          }

      }

      //retrieve data from daal table to buffer
      // IntBuffer buffer_daal = IntBuffer.allocate(vec_size*map_size);
      // buffer_daal = daal_table.getBlockOfRows(0, map_size, buffer_daal);
      // buffer_daal.get(buffer_array_int, 0, vec_size*map_size);
      ((HomogenBMNumericTable)daal_table).getBlockOfRowsByte(0, map_size, buffer_array_int);

      //multi-threading copy
      Thread[] threads = new Thread[num_threads];

      for (int q = 0; q<num_threads; q++) 
      {
          threads[q] = new Thread(new TaskBufferToTableInt(q, num_threads, map_size, this.obj_list_int, buffer_array_int, vec_size));
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

