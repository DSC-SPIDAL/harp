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

import edu.iu.harp.schdynamic.Task;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SortTask
  implements Task<LongArrayList, Object> {

  protected static final Log LOG =
    LogFactory.getLog(SortTask.class);

  private final long[] array;

  public SortTask(int numTopics) {
    array = new long[numTopics];
  }

  @Override
  public Object run(LongArrayList list)
    throws Exception {
    int size = list.size();
    list.toArray(array);
    LongArrays.quickSort(array, 0, size,
      new LongComparator() {

        @Override
        public int compare(Long o1, Long o2) {
          return Long.compare(o2, o1);
        }

        @Override
        public int compare(long k1, long k2) {
          return Long.compare(k2, k1);
        }
      });
    list.clear();
    list.addElements(0, array, 0, size);
    return null;
  }
}
