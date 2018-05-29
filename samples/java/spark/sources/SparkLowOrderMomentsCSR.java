/* file: SparkLowOrderMomentsCSR.java */
/*******************************************************************************
* Copyright 2017-2018 Intel Corporation
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
//      Java sample of computing low order moments in the distributed
//      processing mode.
//
//      Input matrix is stored in the compressed sparse row (CSR) format.
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

import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.*;

public class SparkLowOrderMomentsCSR {
    /* Class containing the algorithm results */
    static class MomentsResult {
        public HomogenNumericTable minimum;
        public HomogenNumericTable maximum;
        public HomogenNumericTable sum;
        public HomogenNumericTable sumSquares;
        public HomogenNumericTable sumSquaresCentered;
        public HomogenNumericTable mean;
        public HomogenNumericTable secondOrderRawMoment;
        public HomogenNumericTable variance;
        public HomogenNumericTable standardDeviation;
        public HomogenNumericTable variation;
    }

    static JavaPairRDD<Integer, PartialResult> partsRDD;

    public static MomentsResult runMoments(DaalContext context, JavaRDD<CSRNumericTable> dataRDD) {
        JavaRDD<PartialResult> partsRDD = computestep1Local(dataRDD);

        PartialResult finalPartRes = reducePartialResults(partsRDD);

        MomentsResult result = finalizeMergeOnMasterNode(context, finalPartRes);

        return result;
    }

    private static JavaRDD<PartialResult> computestep1Local(JavaRDD<CSRNumericTable> dataRDD) {
        return dataRDD.map(new Function<CSRNumericTable, PartialResult>() {
            public PartialResult call(CSRNumericTable table) {
                DaalContext context = new DaalContext();

                /* Create an algorithm to compute low order moments on local nodes */
                DistributedStep1Local momentsLocal = new DistributedStep1Local(context, Double.class, Method.fastCSR);

                /* Set the input data on local nodes */
                table.unpack(context);
                momentsLocal.input.set( InputId.data, table );

                /* Compute low order moments on local nodes */
                PartialResult pres = momentsLocal.compute();
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
                DistributedStep2Master momentsMaster = new DistributedStep2Master(context, Double.class, Method.fastCSR);

                /* Set the partial results recieved from the local nodes */
                p1.unpack(context);
                p2.unpack(context);
                momentsMaster.input.add(DistributedStep2MasterInputId.partialResults, p1);
                momentsMaster.input.add(DistributedStep2MasterInputId.partialResults, p2);

                /* Compute a new partial result from two partial results */
                PartialResult pres = momentsMaster.compute();
                pres.pack();

                context.dispose();
                return pres;
            }
        });
    }

    private static MomentsResult finalizeMergeOnMasterNode(DaalContext context, PartialResult partRes) {

        /* Create an algorithm to compute low order moments on the master node */
        DistributedStep2Master momentsMaster = new DistributedStep2Master(context, Double.class, Method.fastCSR);

        /* Add partial results computed on local nodes to the algorithm on the master node */
        partRes.unpack(context);
        momentsMaster.input.add(DistributedStep2MasterInputId.partialResults, partRes);

        /* Compute low order moments on the master node */
        momentsMaster.compute();

        /* Finalize computations and retrieve the results */
        Result res = momentsMaster.finalizeCompute();

        MomentsResult result = new MomentsResult();
        result.minimum              = (HomogenNumericTable)res.get(ResultId.minimum);
        result.maximum              = (HomogenNumericTable)res.get(ResultId.maximum);
        result.sum                  = (HomogenNumericTable)res.get(ResultId.sum);
        result.sumSquares           = (HomogenNumericTable)res.get(ResultId.sumSquares);
        result.sumSquaresCentered   = (HomogenNumericTable)res.get(ResultId.sumSquaresCentered);
        result.mean                 = (HomogenNumericTable)res.get(ResultId.mean);
        result.secondOrderRawMoment = (HomogenNumericTable)res.get(ResultId.secondOrderRawMoment);
        result.variance             = (HomogenNumericTable)res.get(ResultId.variance);
        result.standardDeviation    = (HomogenNumericTable)res.get(ResultId.standardDeviation);
        result.variation            = (HomogenNumericTable)res.get(ResultId.variation);
        return result;
    }
}