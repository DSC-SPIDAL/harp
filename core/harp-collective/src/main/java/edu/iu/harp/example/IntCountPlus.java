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

package edu.iu.harp.example;

import edu.iu.harp.keyval.ValCombiner;
import edu.iu.harp.keyval.ValStatus;

/*******************************************************
 * Combiner for IntCount
 ******************************************************/
public class IntCountPlus
  extends ValCombiner<IntCount> {

  /**
   * Combine two IntCountss
   */
  @Override
  public ValStatus combine(IntCount curVal,
    IntCount newVal) {
    curVal
      .setInt(curVal.getInt() + newVal.getInt());
    curVal.setCount(
      curVal.getCount() + newVal.getCount());
    return ValStatus.COMBINED;
  }
}
