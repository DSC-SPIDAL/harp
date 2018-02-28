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

import java.lang.System;

public class TaskTableToBufferFloat implements Runnable {

    private int th_id;
    private int th_num;
    private int task_num;
    private CopyObjFloat[] queue;
    private float[] buffer_array;
    private int vecsize;

    //constructor
    TaskTableToBufferFloat(
            int th_id, 
            int th_num,
            int task_num, 
            CopyObjFloat[] queue,
            float[] buffer_array,
            int vecsize 
    )
    {
        this.th_id = th_id;
        this.th_num = th_num;
        this.task_num = task_num;
        this.queue = queue;
        this.buffer_array = buffer_array;
        this.vecsize = vecsize;
    }

    @Override
    public void run() {

        while(th_id < task_num)
        {
            CopyObjFloat obj = queue[th_id];
            System.arraycopy(obj.data(), 0, buffer_array, obj.index()*vecsize, vecsize); 
            th_id += th_num;
        }

    }


}
