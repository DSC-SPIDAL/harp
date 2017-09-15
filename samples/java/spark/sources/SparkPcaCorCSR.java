/* file: SparkPcaCorCSR.java */
/*******************************************************************************
* Copyright 2017 Intel Corporation
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
//      Java sample of principal component analysis (PCA) using the correlation
//      method in the distributed processing mode
////////////////////////////////////////////////////////////////////////////////
*/

package DAAL;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.SparkConf;

import scala.Tuple2;
import com.intel.daal.algorithms.pca.*;
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.*;


public class SparkPcaCorCSR {
    /* Class containing the algorithm results */
    static class PCAResult {
        public HomogenNumericTable eigenVectors;
        public HomogenNumericTable eigenValues;
    }

    public static PCAResult runPCA(DaalContext context, JavaRDD<CSRNumericTable> dataRDD) {
        JavaRDD<PartialResult> partsRDD = computestep1Local(dataRDD);

        PartialResult finalPartRes = reducePartialResults(partsRDD);

        PCAResult result = finalizeMergeOnMasterNode(context, finalPartRes);

        return result;
    }

    private static JavaRDD<PartialResult> computestep1Local(JavaRDD<CSRNumericTable> dataRDD) {
        return dataRDD.map(new Function<CSRNumericTable, PartialResult>() {
            public PartialResult call(CSRNumericTable table) {
                DaalContext context = new DaalContext();

                /* Create an algorithm to compute PCA decomposition using the correlation method on local nodes */
                DistributedStep1Local pcaLocal = new DistributedStep1Local(context, Double.class, Method.correlationDense);

                com.intel.daal.algorithms.covariance.DistributedStep1Local covarianceSparse
                    = new com.intel.daal.algorithms.covariance.DistributedStep1Local(
                        context, Double.class, com.intel.daal.algorithms.covariance.Method.fastCSR);

                pcaLocal.parameter.setCovariance(covarianceSparse);

                /* Set the input data on local nodes */
                table.unpack(context);
                pcaLocal.input.set( InputId.data, table );

                /* Compute PCA decomposition on local nodes */
                PartialResult pres = pcaLocal.compute();
                pres.pack();

                context.dispose();
                return pres;
            }
        });
    }

    private static PartialResult reducePartialResults(JavaRDD<PartialResult> partsRDD) {
        return partsRDD.reduce(new Function2<PartialResult, PartialResult, PartialResult>() {
            public PartialResult call(PartialResult p1, PartialResult p2) {
                DaalContext context = new DaalContext();

                /* Create an algorithm to compute new partial result from two partial results */
                DistributedStep2Master pcaMaster = new DistributedStep2Master(context, Double.class, Method.correlationDense);

                com.intel.daal.algorithms.covariance.DistributedStep2Master covarianceSparse
                    = new com.intel.daal.algorithms.covariance.DistributedStep2Master(
                        context, Double.class, com.intel.daal.algorithms.covariance.Method.fastCSR);

                pcaMaster.parameter.setCovariance(covarianceSparse);

                /* Set the partial results recieved from the local nodes */
                p1.unpack(context);
                p2.unpack(context);
                pcaMaster.input.add(MasterInputId.partialResults, p1);
                pcaMaster.input.add(MasterInputId.partialResults, p2);

                /* Compute a new partial result from two partial results */
                PartialResult pres = pcaMaster.compute();
                pres.pack();

                context.dispose();
                return pres;
            }
        });
    }

    private static PCAResult finalizeMergeOnMasterNode(DaalContext context, PartialResult partRest) {

        /* Create an algorithm to compute PCA decomposition using the correlation method on the master node */
        DistributedStep2Master pcaMaster = new DistributedStep2Master(context, Double.class, Method.correlationDense);

        com.intel.daal.algorithms.covariance.DistributedStep2Master covarianceSparse
            = new com.intel.daal.algorithms.covariance.DistributedStep2Master(
                context, Double.class, com.intel.daal.algorithms.covariance.Method.fastCSR);

        pcaMaster.parameter.setCovariance(covarianceSparse);

        /* Add partial results computed on local nodes to the algorithm on the master node */
        partRest.unpack(context);
        pcaMaster.input.add(MasterInputId.partialResults, partRest);

        /* Compute PCA decomposition on the master node */
        pcaMaster.compute();

        /* Finalize computations and retrieve the results */
        Result res = pcaMaster.finalizeCompute();

        PCAResult result = new PCAResult();
        result.eigenVectors = (HomogenNumericTable)res.get(ResultId.eigenVectors);
        result.eigenValues  = (HomogenNumericTable)res.get(ResultId.eigenValues);
        return result;
    }
}
