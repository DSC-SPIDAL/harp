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

package edu.iu.daal_sgd;

public class Constants {
  public static final String R = "r";
  public static final String LAMBDA = "lambda";
  public static final String EPSILON = "epsilon";
  public static final String NUM_ITERATIONS =
    "num_iterations";
  public static final String NUM_THREADS =
    "num_threads";
  public static final String MODEL_DIR =
    "model_dir";
  public static final String Time = "mini_batch";
  public static final String NUM_MODEL_SLICES =
    "num_model_slices";
  public static final String TEST_FILE_PATH =
    "test_file_path";
  public static final int ARR_LEN = 16;
  public static final String ENABLE_TUNING =
    "enable_tuning";
  public static final double COMM_VS_COMPUTE =
    10.0;
  public static final double MAX_RATIO = 0.9;
  public static final double MIN_RATIO = 0.618;
  public static final double GOLDEN_RATIO = (Math
    .sqrt(5.0) - 1.0) / 2.0;
}
