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

package org.apache.hadoop.mapred;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;

import java.io.IOException;
import java.util.List;

/**
 * This class is modified from YARNRunner for
 * submitting map-collective jobs. In
 * createApplicationSubmissionContext, launch
 * MapCollectiveAppMaster
 */
public class MapCollectiveRunner
  extends YARNRunner {

  public MapCollectiveRunner(Configuration conf) {
    super(conf);
  }

  public ApplicationSubmissionContext
    createApplicationSubmissionContext(
      Configuration jobConf, String jobSubmitDir,
      Credentials ts) throws IOException {
    ApplicationSubmissionContext appContext =
      super.createApplicationSubmissionContext(
        jobConf, jobSubmitDir, ts);
    List<String> commands = appContext
      .getAMContainerSpec().getCommands();
    // There should be only one string in the
    // commands
    String command = commands.get(0);
    if (command.contains(
      MRJobConfig.APPLICATION_MASTER_CLASS)) {
      command = command.replace(
        MRJobConfig.APPLICATION_MASTER_CLASS,
        "org.apache.hadoop.mapreduce.v2.app.MapCollectiveAppMaster");
      // Set command back
      System.out.println("command: " + command);
      commands.set(0, command);
    } else {
      throw new IOException(
        "Cannot find MRJobConfig.APPLICATION_MASTER_CLASS");
    }
    return appContext;
  }
}
