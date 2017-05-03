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

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.ShortArray;

class RowData {
  int height;
  int width;
  int row;
  int rowOffset;
  String distPath;
  String weightPath;
  String vPath;
  ShortArray distArray;
  DoubleArray weightArray;
  IntArray vArray;
}
