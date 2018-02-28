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

package edu.iu.wdamds;

import edu.iu.harp.schdynamic.Task;
import org.apache.hadoop.mapreduce.Mapper;

public class DataLoadTask
  implements Task<RowData, Object> {

  private final Mapper<String, String, Object, Object>.Context context;

  public DataLoadTask(
    Mapper<String, String, Object, Object>.Context context) {
    this.context = context;
  }

  @Override
  public Object run(RowData rowData)
    throws Exception {
    DataFileUtil.loadRowData(rowData, context);
    return null;
  }
}
