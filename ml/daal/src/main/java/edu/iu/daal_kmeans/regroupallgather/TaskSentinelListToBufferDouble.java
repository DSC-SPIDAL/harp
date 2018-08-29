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
 *
 * */

package edu.iu.daal_kmeans.regroupallgather;

import java.lang.System;

public class TaskSentinelListToBufferDouble implements Runnable {

    private int task_id;
    private int th_num;
    private int vecSize;
    private int task_num;
    private long[] startP;
    private double[][] data;
    private double[] buffer_array;

    //constructor
    public TaskSentinelListToBufferDouble(
            int task_id, 
            int th_num,
            int vecSize,
            int task_num, 
            long[] startP,
            double[][] data,
            double[] buffer_array
    )
    {
        this.task_id = task_id;
        this.th_num = th_num;
        this.vecSize = vecSize;
        this.task_num = task_num;
        this.startP = startP;
        this.data = data;
        this.buffer_array = buffer_array;
    }

    @Override
    public void run() {

        while(task_id < task_num)
        {
            int local_cen_num = (data[task_id].length)/(vecSize + 1);
            int local_startP = (int)startP[task_id];

            for(int k=0;k<local_cen_num;k++)
                System.arraycopy(data[task_id], k*(vecSize+1) + 1, buffer_array, local_startP + k*vecSize, vecSize); 

            task_id += th_num;
        }

    }

}
