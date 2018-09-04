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
package edu.iu.examples;

/**
 * Captures parameters for running the examples
 */
public class ExampleParameters {
  /**
   * Number of elements in each partition
   */
  private int size;

  /**
   * Number of iterations
   */
  private int iterations;

  /**
   * The operation
   */
  private String operation;

  /**
   * Number of mappers
   */
  private int mappers = 0;

  /**
   * Number of partitions in each data table
   */
  private int partitions = 0;

  /**
   * Data type
   */
  private String dataType;

  /**
   * Verify the results
   */
  private boolean verify;

  public ExampleParameters(int mappers, int size, int iterations,
                           String operation, int partitions,
                           String dataType, boolean verify) {
    this.size = size;
    this.iterations = iterations;
    this.operation = operation;
    this.mappers = mappers;
    this.partitions = partitions;
    this.dataType = dataType;
    this.verify = verify;
  }

  public int getSize() {
    return size;
  }

  public int getIterations() {
    return iterations;
  }

  public String getOperation() {
    return operation;
  }

  public int getMappers() {
    return mappers;
  }

  public int getPartitions() {
    return partitions;
  }

  public String getDataType() {
    return dataType;
  }

  public boolean isVerify() {
    return verify;
  }
}
