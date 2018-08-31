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

package edu.iu.data_aux;

public class HarpDAALConstants {

  // system specific 
  public static final String NUM_MAPPERS =
      "num_mappers";
  public static final String NUM_THREADS =
      "num_threads";
  public static final String NUM_ITERATIONS =
      "num_iterations";
  public static final String WORK_DIR =
      "work_dir";

  // application specific
  public static final String FEATURE_DIM =
      "vector_size";
  public static final String FILE_DIM =
      "file_dimension";
  public static final String NUM_FACTOR =
      "nFactors";
  public static final String NUM_CLASS =
      "nClass";
  public static final String MAX_ITERATIONS =
      "max_num_iterations";
  public static final String ACC_THRESHOLD =
      "accuracy_threshold";
  public static final String LEARNING_RATE =
      "learning_rate";
  public static final String NUM_CENTROIDS =
      "centroids";
  public static final String CEN_DIR =
      "centroids_dir";
  public static final String CENTROID_FILE_NAME =
      "centroids_file";

  public static final String TEST_FILE_PATH =
      "test_file_path";
  public static final String TRAIN_PRUNE_PATH =
      "train_prune_path";
  public static final String TRAIN_TRUTH_PATH =
      "train_truth_path";
  public static final String TEST_TRUTH_PATH =
      "ground_truth_file_path";
  public static final String TEST_PRUNE_PATH =
      "ground_truth_file_path";
  public static final String BATCH_SIZE =
      "batch size";
  public static final String STEP_LENGTH =
      "step_length";
  public static final String NUM_DEPVAR =
      "nDependentVariables";

  public static final int ARR_LEN = 1000;
}
