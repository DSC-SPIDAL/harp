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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

/**
 * The base class for Examples
 */
public abstract class AbstractExampleMapper extends CollectiveMapper<String, String, Object, Object> {
  protected String cmd;
  protected int elements;
  protected int numPartitions;
  protected int numMappers;
  protected int numIterations;
  protected String dataType;
  // weather we are going to verify the results
  protected boolean verify;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context) {
    Configuration configuration =
        context.getConfiguration();
    cmd = configuration
        .get(Constants.ARGS_OPERATION, "allreduce");
    numMappers = configuration
        .getInt(Constants.ARGS_MAPPERS, 1);
    numPartitions = configuration
        .getInt(Constants.ARGS_PARTITIONS, 1);
    elements = configuration
        .getInt(Constants.ARGS_ELEMENTS, 1);
    numIterations = configuration
        .getInt(Constants.ARGS_ITERATIONS, 1);
    dataType = configuration
        .get(Constants.ARGS_DATA_TYPE, "double");
    verify = configuration
        .getBoolean(Constants.ARGS_VERIFY , false);
    LOG.info("Example operation " + cmd);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Partitions " + numPartitions);
    LOG.info(
        "Bytes per Partition " + elements);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Data type " + dataType);
    LOG.info("Verify " + verify);
  }
}
