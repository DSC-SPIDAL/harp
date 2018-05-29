/* file: LowOrderMomsCSRBatch.java */
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
//  Java example of computing low order moments in the batch processing mode.
//
//      Input matrix is stored in the compressed sparse row (CSR) format with
//      one-based indexing.
////////////////////////////////////////////////////////////////////////////////
*/

/**
 * <a name="DAAL-EXAMPLE-JAVA-LOWORDERMOMENTSCSRBATCH">
 * @example LowOrderMomsCSRBatch.java
 */

package com.intel.daal.examples.moments;

import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.examples.utils.Service;
/*
// Input data set is stored in the compressed sparse row format
*/
import com.intel.daal.services.DaalContext;

class LowOrderMomsCSRBatch {

    /* Input data set parameters */
    private static final String datasetFileName = "../data/batch/covcormoments_csr.csv";

    private static CSRNumericTable dataTable;
    private static Result          result;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Read the input data from a file */
        dataTable = Service.createSparseTable(context, datasetFileName);

        /* Create algorithm objects to compute low order moments using the default method */
        Batch algorithm = new Batch(context, Float.class, Method.fastCSR);

        /* Set input objects for the algorithm */
        algorithm.input.set(InputId.data, dataTable);

        /* Compute low order moments */
        result = algorithm.compute();

        printResults();

        context.dispose();
    }

    private static void printResults() {
        HomogenNumericTable minimum = (HomogenNumericTable) result.get(ResultId.minimum);
        HomogenNumericTable maximum = (HomogenNumericTable) result.get(ResultId.maximum);
        HomogenNumericTable sum = (HomogenNumericTable) result.get(ResultId.sum);
        HomogenNumericTable sumSquares = (HomogenNumericTable) result.get(ResultId.sumSquares);
        HomogenNumericTable sumSquaresCentered = (HomogenNumericTable) result.get(ResultId.sumSquaresCentered);
        HomogenNumericTable mean = (HomogenNumericTable) result.get(ResultId.mean);
        HomogenNumericTable secondOrderRawMoment = (HomogenNumericTable) result.get(ResultId.secondOrderRawMoment);
        HomogenNumericTable variance = (HomogenNumericTable) result.get(ResultId.variance);
        HomogenNumericTable standardDeviation = (HomogenNumericTable) result.get(ResultId.standardDeviation);
        HomogenNumericTable variation = (HomogenNumericTable) result.get(ResultId.variation);

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
