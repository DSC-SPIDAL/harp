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
 * The types of the Data
 ******************************************************/
public class DataType {
  // Data Type
  public static final byte UNKNOWN_DATA_TYPE = 0;
  public static final byte BYTE_ARRAY = 1;
  public static final byte SHORT_ARRAY = 2;
  public static final byte INT_ARRAY = 3;
  public static final byte FLOAT_ARRAY = 4;
  public static final byte LONG_ARRAY = 5;
  public static final byte DOUBLE_ARRAY = 6;
  public static final byte WRITABLE = 7;
  public static final byte SIMPLE_LIST = 8;
  public static final byte PARTITION_LIST = 9;
}
