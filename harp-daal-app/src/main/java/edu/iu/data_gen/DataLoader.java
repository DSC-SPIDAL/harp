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

package edu.iu.data_gen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public final class DataLoader
{

    private DataLoader() {
    }

    /**
     * Load data points from a file.
     * 
     * @param file
     * @param conf
     * @return
     * @throws IOException
     */
    public static double[] loadPoints(String file,
            int pointsPerFile, int cenVecSize,
            Configuration conf) throws Exception {
        double[] points =
            new double[pointsPerFile * cenVecSize];
        Path pointFilePath = new Path(file);
        FileSystem fs =
            pointFilePath.getFileSystem(conf);
        FSDataInputStream in = fs.open(pointFilePath);
        try {
            for (int i = 0; i < points.length;) {
                for (int j = 0; j < cenVecSize; j++) {
                    points[i++] = in.readDouble();
                }
            }
        } finally {
            in.close();
        }
        return points;
    }

    /**
     * @brief load points from a ASCII Matrix Market format 
     * Dense file
     *
     * @param file
     * @param pointsPerFile
     * @param cenVecSize
     * @param conf
     *
     * @return 
     */
    public static double[] loadPointsMMDense(String file,
            int pointsPerFile, int cenVecSize,
            Configuration conf) throws Exception {

        FSDataInputStream in = null;
        BufferedReader reader = null;
        String line = null;
        double[] points = new double[pointsPerFile*cenVecSize];

        try {

            Path pointFilePath = new Path(file);
            FileSystem fs = pointFilePath.getFileSystem(conf);

            in = fs.open(pointFilePath);
            reader = new BufferedReader(new InputStreamReader(in), 1048576);

            for (int i = 0; i < points.length;) {
                for (int j = 0; j < cenVecSize; j++) {
                    if ((line = reader.readLine())!= null)
                        points[i++] = Double.parseDouble(line);
                }
            }

        } finally {

            in.close();
            reader.close();

        }

        return points;
    }
}
