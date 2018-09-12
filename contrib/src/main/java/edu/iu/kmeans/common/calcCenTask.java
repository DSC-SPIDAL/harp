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

package edu.iu.kmeans.common;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// calculate Euclidean distance.
public class calcCenTask
    implements Task<double[], Object> {

    protected static final Log LOG =
        LogFactory.getLog(calcCenTask.class);

    //TODO: add necessary private data members
    
    // constructor
    public calcCenTask(Table<DoubleArray> cenTable, int vectorSize) 
    {
        //TODO: add constructor
        
    }

    @Override
    public Object run(double[] aPoint) throws Exception 
    {
        //TODO: add codes

        return null;
        
    }


    private double calcEucDistSquare(double[] aPoint, double[] centroid, int length)
    {
        double dist = 0;
        for (int i = 0; i < length; i++) 
            dist += Math.pow(aPoint[i] - centroid[i], 2);
        
        return Math.sqrt(dist);
    }

}

