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

package edu.iu.lda;

public class Constants {
  public static final String NUM_TOPICS =
    "NUM_TOPICS";
  public static final String ALPHA = "alpha";
  public static final String BETA = "beta";
  public static final String NUM_ITERATIONS =
    "num_iterations";
  public static final String NUM_THREADS =
    "num_threads";
  public static final String SCHEDULE_RATIO =
    "schedule_ratio";
  public static final String MODEL_DIR =
    "model_dir";
  public static final String PRINT_MODEL =
    "print_model";
  public static final String NUM_MODEL_SLICES =
    "num_model_slices";
  public static final int ARR_LEN = 64;
  public static final String TIME = "time";
  public static final int TRAIN_MAX_THRESHOLD =
    50;
  public static final int TRAIN_MIN_THRESHOLD =
    50;
  public static final String MAX_BOUND =
    "max_bound";
  public static final String MIN_BOUND =
    "min_bound";
  public static long TOPIC_DELTA = 1L << 32;
}