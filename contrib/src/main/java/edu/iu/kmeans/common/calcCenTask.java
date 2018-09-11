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
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// calculate Euclidean distance.
public class calcCenTask
    implements Task<double[], Object> {

    protected static final Log LOG =
        LogFactory.getLog(calcCenTask.class);

    // the centroids data synchronized
    private double[][] centroids;
    // the sum of local pts assigned to each centroids
    private double[][] pts_assign_sum;

    private final int vectorSize;
    private final int cenVecSize;
    private double error;
    private double tempDist;
    private double minDist;

    // constructor
    public calcCenTask(Table<DoubleArray> cenTable, int vectorSize) 
    {
        this.centroids = new double[cenTable.getNumPartitions()][];
        this.pts_assign_sum = new double[centroids.length][];

        for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
        {
            int partitionID = partition.id();
            DoubleArray array = partition.get();
            this.centroids[partitionID] = array.get();
            this.pts_assign_sum[partitionID] = new double[array.size()];
        }

        // the last number is the accumulated number of pts for this 
        // centroid
        this.vectorSize = vectorSize;
        this.cenVecSize = vectorSize+1 ;
        this.error = 0;
        this.tempDist = 0;
        this.minDist = -1;
    }

    @Override
    public Object run(double[] aPoint) throws Exception 
    {
        this.minDist = -1;
        this.tempDist = 0;
        int nearestPartitionID = -1;

        for(int i=0; i<this.centroids.length;i++)
        {
            this.tempDist = calcEucDistSquare(aPoint, this.centroids[i], this.vectorSize);
            if (this.minDist == -1 || this.tempDist < this.minDist)
            {
                this.minDist = tempDist;
                nearestPartitionID = i; 
            }
        }

        this.error += this.minDist;

        // update the pts_assign_sum values
        for(int j=0;j<this.vectorSize;j++)
            this.pts_assign_sum[nearestPartitionID][j] += aPoint[j]; 

        // sum up the number of added pts
        this.pts_assign_sum[nearestPartitionID][this.vectorSize] += 1;
        return null;
        
    }

    public double getError() {return this.error;}
    public double[][] getPtsAssignSum() { return this.pts_assign_sum; }

    private double calcEucDistSquare(double[] aPoint, double[] centroid, int length)
    {
        double dist = 0;
        for (int i = 0; i < length; i++) 
            dist += Math.pow(aPoint[i] - centroid[i], 2);
        
        return Math.sqrt(dist);
    }

}

