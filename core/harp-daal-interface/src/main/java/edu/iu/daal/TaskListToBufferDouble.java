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

package edu.iu.daal;

public class TaskListToBufferDouble implements Runnable {

    private int task_id;
    private int th_num;
    private int task_num;
    private long[] startP;
    private double[][] data;
    private double[] buffer_array;

    //constructor
    public TaskListToBufferDouble(
            int task_id, 
            int th_num,
            int task_num, 
            long[] startP,
            double[][] data,
            double[] buffer_array
    )
    {
        this.task_id = task_id;
        this.th_num = th_num;
        this.task_num = task_num;
        this.startP = startP;
        this.data = data;
        this.buffer_array = buffer_array;
    }

    @Override
    public void run() {

        while(task_id < task_num)
        {
            System.arraycopy(data[task_id], 0, buffer_array, (int)startP[task_id], data[task_id].length); 
            task_id += th_num;
        }

    }

}
