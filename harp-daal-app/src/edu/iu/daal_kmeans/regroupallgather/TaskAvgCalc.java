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

package edu.iu.daal_kmeans.regroupallgather;

import java.lang.System;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import java.util.Arrays;

public class TaskAvgCalc implements Runnable {


    private int cenVecSize;
    private DoubleArray array;

    public TaskAvgCalc(int cenVecSize, DoubleArray array)
    {
        this.cenVecSize = cenVecSize;
        this.array = array;
    }


    /**
     * @brief average each entrie of centroid vectors 
     * by the total counts stored in the first element
     *
     * @return 
     */
    @Override
    public void run() {

        double[] doubles = array.get();
        int size = array.size();

        for (int j = 0; j < size; j += cenVecSize) 
        {
            if (doubles[j] != 0) 
            {
                for (int k = 1; k < cenVecSize; k++) {
                    doubles[j + k] /= doubles[j];
                }
            }

        }

    }


}

