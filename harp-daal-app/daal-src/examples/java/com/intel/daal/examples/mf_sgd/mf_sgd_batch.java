/* file: mf_sgd_batch.java */
/*******************************************************************************
 * Copyright 2014-2016 Intel Corporation
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
 *******************************************************************************/

/*
//  Content:
//     Java example of computing mf_sgd factorization in the batch processing mode
////////////////////////////////////////////////////////////////////////////////
*/

/**
 * <a name="DAAL-EXAMPLE-JAVA-MF_SGD">
 * @example mf_sgd_batch.java
 */

package com.intel.daal.examples.mf_sgd;

import java.lang.System.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.lang.Long;
import java.util.ArrayList;

import com.intel.daal.algorithms.mf_sgd.*;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.AOSNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class mf_sgd_batch{

    /* Input data set parameters */
    private static final String trainDataFile = "../data/batch/movielens-train.mm";
    private static final String testDataFile = "../data/batch/movielens-test.mm";

    private static double learningRate = 0.005;
    private static double lambda = 0.002;
    private static int iteration = 10;		         /* num of iterations in SGD training */

    private static int threads = 0;			         /* specify the num of threads used by TBB, when 0 TBB chooses automatic threads num  */
    private static int tbb_grainsize = 0;        /* 0 by auto partitioner of TBB or user specified tbb grainsize   */
    private static int Avx_explicit = 0;             /* 0 to use compiler generated vectorization codes, 1 to use explicit intel intrinsic codes */

    private static int r_dim = 128;                  /* dimension of vectors in model W and model H */
    private static final int field_v = 3;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {

        /* Create an algorithm to compute mf_sgd_batch  */
        Batch sgdAlgorithm = new Batch(context, Float.class, Method.defaultSGD);

        /* load data from CSV files */
        ArrayList<VPoint> table_train = new ArrayList<>();
        ArrayList<VPoint> table_test = new ArrayList<>();

        int num_Train = sgdAlgorithm.input.loadData(trainDataFile, table_train);
        int num_Test = sgdAlgorithm.input.loadData(testDataFile, table_test);

        VPoint[] points_Train = new VPoint[num_Train];
        VPoint[] points_Test = new VPoint[num_Test];

        for (int j=0; j<num_Train; j++) 
            points_Train[j] = new VPoint(0,0,0);

        for (int j=0; j<num_Test; j++) 
            points_Test[j] = new VPoint(0,0,0);

        /* convert points to mf_sgd format */
        int[] row_col_num = sgdAlgorithm.input.convert_format(points_Train, num_Train, points_Test, num_Test, table_train, table_test);

        /* get the size of model W and H */
        int row_num_w = row_col_num[0];
        int col_num_h = row_col_num[1];

        /* create the AOSNumericTable for Training dataset and test dataset */
        AOSNumericTable dataTable_Train = new AOSNumericTable(context, points_Train);
        AOSNumericTable dataTable_Test = new AOSNumericTable(context, points_Test);

        sgdAlgorithm.input.set(InputId.dataTrain, dataTable_Train);
        sgdAlgorithm.input.set(InputId.dataTest, dataTable_Test);

        sgdAlgorithm.parameter.set(learningRate,lambda, r_dim, row_num_w, col_num_h, iteration, threads, tbb_grainsize, Avx_explicit);
        sgdAlgorithm.parameter.setRatio(1.0);   /* ratio 1.0 means we train all the points */

        Result res = sgdAlgorithm.compute();

        NumericTable matrixW = res.get(ResultId.resWMat);
        NumericTable matrixH = res.get(ResultId.resHMat);

        /* printout result model W, H, first 10 rows and columns */
        Service.printNumericTable("Model W:", matrixW, 10,10);
        Service.printNumericTable("Model H:", matrixH, 10,10);

        context.dispose();
    }
}
