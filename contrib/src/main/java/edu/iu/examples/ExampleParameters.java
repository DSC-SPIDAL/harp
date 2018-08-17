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

import java.util.logging.Logger;

public class ExampleParameters {
  private static final Logger LOG = Logger.getLogger(ExampleParameters.class.getName());
  private int size;

  private int iterations;

  private String operation;

  private String fileName;

  private int printInterval = 0;

  private int initIterations = 0;

  private int tasks = 0;

  public ExampleParameters(int tasks, int size, int iterations, String operation, String fileName,
                           int printInterval, int initIterations) {
    this.size = size;
    this.iterations = iterations;
    this.operation = operation;
    this.fileName = fileName;
    this.printInterval = printInterval;
    this.initIterations = initIterations;
    this.tasks = tasks;
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

  public String getFileName() {
    return fileName;
  }

  public int getPrintInterval() {
    return printInterval;
  }

  public int getInitIterations() {
    return initIterations;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setPrintInterval(int printInterval) {
    this.printInterval = printInterval;
  }

  public void setInitIterations(int initIterations) {
    this.initIterations = initIterations;
  }

  public int getTasks() {
    return tasks;
  }

  public void setTasks(int tasks) {
    this.tasks = tasks;
  }
}
