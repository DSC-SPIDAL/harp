/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.daal_subgraph;

public class SCConstants { 
  public static final String NUM_MAPPERS = "num_mappers"; 
  public static final String NUM_COLORS = "num_color";
  public static final String NUM_ISOM = "num_isom";
  public static final String TEMPLATE_PATH = "template_path";
  public static final String OUTPUT_PATH = "output_path";
  public static final String USE_LOCAL_MULTITHREAD = "use_local_multithread";
  public static final String NUM_THREADS_PER_NODE="num_threads_per_node";
  public static final String CORE_NUM = "core_num";
  public static final String THREAD_NUM = "num_threads";
  public static final String THD_AFFINITY = "compact or scatter";
  public static final String OMPSCHEDULE = "static dynamic or guided";
  public static final String TPC = "threads per core";
  public static final String SENDLIMIT = "mem per regroup array";
  public static final String NBRTASKLEN = "len per nbr split task";
  public static final String ROTATION_PIPELINE = "use rotation-pipelined regroup";
  public static final String NUM_ITERATION="num_itr";
  public static final int ARR_LEN = 16;

  public static final int NULL_VAL = Integer.MAX_VALUE;
  public static final int CREATE_SIZE = 100;

}
