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

package edu.iu.harp.io;

/*******************************************************
 * This class defines the constants used in Harp
 * 
 ******************************************************/
public class Constant {
  // Code
  public static final byte UNKNOWN_CMD = -1;
  public static final byte SERVER_QUIT = 0;
  public static final byte SEND = 1;
  public static final byte SEND_DECODE = 2;
  public static final byte CHAIN_BCAST = 3;
  public static final byte CHAIN_BCAST_DECODE = 4;
  public static final byte MST_BCAST = 5;
  public static final byte MST_BCAST_DECODE = 6;
  public static final byte CONNECTION_END = 7;

  public static final int DATA_MAX_WAIT_TIME =
    1800; // seconds
  public static final int CONNECT_MAX_WAIT_TIME =
    60000;

  public static final long TERMINATION_TIMEOUT =
    60L;
  public static final int PIPELINE_SIZE = 262144;
  // 256 KB
  public static final int BUFFER_SIZE = 262144;
  // 256 KB
  public static final int MAX_ARRAY_SIZE =
    Integer.MAX_VALUE - 5;

  public static final int SHORT_SLEEP = 100;
  public static final int LONG_SLEEP = 1000;
  public static final int SMALL_RETRY_COUNT = 100;
  public static final int LARGE_RETRY_COUNT =
    10000;

  public static final int NUM_THREADS =
    Runtime.getRuntime().availableProcessors();
  public static final int DEFAULT_WORKER_POART_BASE =
    12800;

  // This means the worker ID is not known,
  // usually used as default.
  public static final int UNKNOWN_WORKER_ID = -1;
  public static final int UNKNOWN_PORT = -1;
  public static final int UNKNOWN_PARTITION_ID =
    Integer.MIN_VALUE;
}