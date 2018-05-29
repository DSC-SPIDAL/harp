/* file: LowOrderMomsDenseDistr.java */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

/*
 //  Content:
 //     Java example of computing low order moments in the distributed processing
 //     mode
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-LOWORDERMOMENTSDENSEDISTRIBUTED">
 * @example LowOrderMomsDenseDistr.java
 */

package com.intel.daal.examples.moments;

import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class LowOrderMomsDenseDistr {

    /* Input data set parameters */
    private static final String datasetFileNames[] = new String[] { "../data/distributed/covcormoments_dense_1.csv",
            "../data/distributed/covcormoments_dense_2.csv", "../data/distributed/covcormoments_dense_3.csv",
            "../data/distributed/covcormoments_dense_4.csv" };

    private static final int nBlocks         = 4;

    private static PartialResult[] partialResult = new PartialResult[nBlocks];
    private static Result          result;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {

        for (int i = 0; i < nBlocks; i++) {
            computeOnLocalNode(i);
        }

        computeOnMasterNode();

        printResults();

        context.dispose();
    }

    private static void computeOnLocalNode(int block) {
        /* Retrieve the input data from a .csv file */
        FileDataSource dataSource = new FileDataSource(context, datasetFileNames[block],
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        /* Retrieve the data from an input file */
        dataSource.loadDataBlock();

        /* Create algorithm objects to compute low order moments in the distributed processing mode using the default method */
        DistributedStep1Local algorithm = new DistributedStep1Local(context, Float.class, Method.defaultDense);

        /* Set input objects for the algorithm */
        NumericTable input = dataSource.getNumericTable();
        algorithm.input.set(InputId.data, input);

        /* Compute partial low order moments estimates on local nodes */
        partialResult[block] = algorithm.compute();
    }

    private static void computeOnMasterNode() {
        /* Create algorithm objects to compute low order moments in the distributed processing mode using the default method */
        DistributedStep2Master algorithm = new DistributedStep2Master(context, Float.class, Method.defaultDense);

        /* Set input objects for the algorithm */
        for (int i = 0; i < nBlocks; i++) {
            algorithm.input.add(DistributedStep2MasterInputId.partialResults, partialResult[i]);
        }

        /* Compute a partial low order moments estimate on the master node from the partial estimates on local nodes */
        algorithm.compute();

        /* Finalize the result in the distributed processing mode */
        result = algorithm.finalizeCompute();
    }

    private static void printResults() {
        NumericTable minimum = result.get(ResultId.minimum);
        NumericTable maximum = result.get(ResultId.maximum);
        NumericTable sum = result.get(ResultId.sum);
        NumericTable sumSquares = result.get(ResultId.sumSquares);
        NumericTable sumSquaresCentered = result.get(ResultId.sumSquaresCentered);
        NumericTable mean = result.get(ResultId.mean);
        NumericTable secondOrderRawMoment = result.get(ResultId.secondOrderRawMoment);
        NumericTable variance = result.get(ResultId.variance);
        NumericTable standardDeviation = result.get(ResultId.standardDeviation);
        NumericTable variation = result.get(ResultId.variation);

        Service.printNumericTable("Minimum:", minimum);
        Service.printNumericTable("Maximum:", maximum);
        Service.printNumericTable("Sum:", sum);
        Service.printNumericTable("Sum of squares:", sumSquares);
        Service.printNumericTable("Sum of squared difference from the means:", sumSquaresCentered);
        Service.printNumericTable("Mean:", mean);
        Service.printNumericTable("Second order raw moment:", secondOrderRawMoment);
        Service.printNumericTable("Variance:", variance);
        Service.printNumericTable("Standard deviation:", standardDeviation);
        Service.printNumericTable("Variation:", variation);
    }
}
