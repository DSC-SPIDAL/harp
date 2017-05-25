/* file: mf_sgd_distri.java */
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
//     Java example of computing mf_sgd factorization in the distributed processing mode
////////////////////////////////////////////////////////////////////////////////
*/

/**
 * <a name="DAAL-EXAMPLE-JAVA-MF_SGD_DISTRI">
 * @example mf_sgd_distri.java
 */

package com.intel.daal.examples.mf_sgd;

import java.lang.System.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.lang.Long;
import java.util.ArrayList;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import com.intel.daal.algorithms.mf_sgd.*;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.AOSNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

class mf_sgd_distri{

    /* Input data set parameters */
    private static final String trainDataFile = "../data/distributed/movielens-train.mm";
    private static final String testDataFile = "../data/distributed/movielens-test.mm";

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
        Distri sgdAlgorithm = new Distri(context, Float.class, Method.defaultSGD);

        /* load data from CSV files */
        ArrayList<VPoint> table_train = new ArrayList<>();
        int num_Train = sgdAlgorithm.input.loadData(trainDataFile, table_train);

        /* convert training data into mf_sgd format */
        VPoint[] points_Train = new VPoint[num_Train];
        for (int j=0; j<num_Train; j++) 
            points_Train[j] = new VPoint(0,0,0);

        int[] row_col_num = sgdAlgorithm.input.convert_format_distri(points_Train, num_Train, table_train);

        /* get size of model W, H */
        int row_num_w = row_col_num[0];
        int col_num_h = row_col_num[1];

        int[] wPos = new int[num_Train];
        int[] hPos = new int[num_Train];
        float[] val = new float[num_Train];

        for(int p=0;p<num_Train;p++)
        {
            wPos[p] = points_Train[p]._wPos;
            hPos[p] = points_Train[p]._hPos;
            val[p] = points_Train[p]._val;
        }

        /* create HomogenNumericTable to store training data */
        NumericTable trainWPos = new HomogenNumericTable(context, Integer.class, 1, num_Train, NumericTable.AllocationFlag.DoAllocate);
        NumericTable trainHPos = new HomogenNumericTable(context, Integer.class, 1, num_Train, NumericTable.AllocationFlag.DoAllocate);
        NumericTable trainVal = new HomogenNumericTable(context, Float.class, 1, num_Train, NumericTable.AllocationFlag.DoAllocate);

        IntBuffer wPos_array_buf = IntBuffer.wrap(wPos);
        trainWPos.releaseBlockOfColumnValues(0, 0, num_Train, wPos_array_buf);

        IntBuffer hPos_array_buf = IntBuffer.wrap(hPos);
        trainHPos.releaseBlockOfColumnValues(0, 0, num_Train, hPos_array_buf);

        FloatBuffer val_array_buf = FloatBuffer.wrap(val);
        trainVal.releaseBlockOfColumnValues(0, 0, num_Train, val_array_buf);

        /* load training table into sgdAlgorithm */
        sgdAlgorithm.input.set(InputId.dataWPos, trainWPos);
        sgdAlgorithm.input.set(InputId.dataHPos, trainHPos);
        sgdAlgorithm.input.set(InputId.dataVal, trainVal);

        NumericTable matrixW = new HomogenNumericTable(context, Float.class, r_dim, row_num_w, NumericTable.AllocationFlag.DoAllocate, 0.5);
        NumericTable matrixH = new HomogenNumericTable(context, Float.class, r_dim, col_num_h, NumericTable.AllocationFlag.DoAllocate, 0.5);

        /* load model W, H into sgdAlgorithm */
        PartialResult pres = new PartialResult(context);
        pres.set(PartialResultId.presWMat, matrixW);
        pres.set(PartialResultId.presHMat, matrixH);
        sgdAlgorithm.setPartialResult(pres);

        /* set up parameters */
        sgdAlgorithm.parameter.set(learningRate,lambda, r_dim, row_num_w, col_num_h, iteration, threads, tbb_grainsize, Avx_explicit);

        sgdAlgorithm.compute();

        /* printout result model W, H, first 10 rows and columns */
        Service.printNumericTable("Model W:", matrixW, 10,10);
        Service.printNumericTable("Model H:", matrixH, 10,10);

        context.dispose();

    }

}
