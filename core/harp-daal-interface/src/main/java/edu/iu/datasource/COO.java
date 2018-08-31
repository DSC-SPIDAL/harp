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

package edu.iu.datasource;

/**
 * @brief this class to store COO sparse format
 */
public class COO {
  private long rowId;
  private long colId;
  private double val;


  public COO(long rowId, long colId, double val) {
    this.rowId = rowId;
    this.colId = colId;
    this.val = val;
  }

  public COO() {
    this.rowId = -1;
    this.colId = -1;
    this.val = 0;
  }

  public long getRowId() {
    return this.rowId;
  }

  public long getColId() {
    return this.colId;
  }

  public double getVal() {
    return this.val;
  }

  public void setRowId(long id) {
    this.rowId = id;
  }

  public void setColId(long id) {
    this.colId = id;
  }

  public void setVal(double val) {
    this.val = val;
  }

  public void reset() {
    this.rowId = -1;
    this.colId = -1;
    this.val = 0;
  }
}

