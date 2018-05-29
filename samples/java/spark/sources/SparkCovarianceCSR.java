/* file: SparkCovarianceCSR.java */
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
//      Java sample of sparse variance-covariance matrix computation in the
//      distributed processing mode
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

import com.intel.daal.algorithms.covariance.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.*;

public class SparkCovarianceCSR {
    /* Class containing the algorithm results */
    static class CovarianceResult {
        public HomogenNumericTable covariance;
        public HomogenNumericTable mean;
    }

    public static CovarianceResult runCovariance(DaalContext context, JavaRDD<CSRNumericTable> dataRDD) {
        JavaRDD<PartialResult> partsRDD = computeStep1Local(context, dataRDD);

        PartialResult reducedPres = reducePartialResults(context, partsRDD);

        CovarianceResult result = finalizeMergeOnMasterNode(context, reducedPres);

        return result;
    }

    private static JavaRDD<PartialResult> computeStep1Local(DaalContext context, JavaRDD<CSRNumericTable> dataRDD) {
        return dataRDD.map(new Function<CSRNumericTable, PartialResult>() {
            public PartialResult call(CSRNumericTable table) {
                DaalContext context = new DaalContext();

                /* Create an algorithm to compute a sparse variance-covariance matrix on local nodes*/
                DistributedStep1Local covarianceLocal = new DistributedStep1Local(context, Double.class, Method.fastCSR);

                /* Set the input data on local nodes */
                table.unpack(context);
                covarianceLocal.input.set(InputId.data, table);

                /* Compute a sparse variance-covariance matrix on local nodes */
                PartialResult pres = covarianceLocal.compute();
                pres.pack();

                context.dispose();
                return pres;
            }
        });
    }

    private static PartialResult reducePartialResults(DaalContext context, JavaRDD<PartialResult> partsRDD) {
        return partsRDD.reduce(new Function2<PartialResult, PartialResult, PartialResult>() {
            public PartialResult call(PartialResult pr1, PartialResult pr2) {
                DaalContext localContext = new DaalContext();

                /* Create an algorithm to compute new partial result from two partial results */
                DistributedStep2Master covarianceMaster = new DistributedStep2Master(localContext, Double.class, Method.fastCSR);

                /* Set the input data recieved from the local nodes */
                pr1.unpack(localContext);
                pr2.unpack(localContext);
                covarianceMaster.input.add(DistributedStep2MasterInputId.partialResults, pr1);
                covarianceMaster.input.add(DistributedStep2MasterInputId.partialResults, pr2);

                /* Compute a new partial result from two partial results */
                PartialResult reducedPresLocal = (PartialResult)covarianceMaster.compute();
                reducedPresLocal.pack();

                localContext.dispose();
                return reducedPresLocal;
            }
        });
    }

    private static CovarianceResult finalizeMergeOnMasterNode(DaalContext context, PartialResult reducedPres) {

        /* Create an algorithm to compute a dense variance-covariance matrix on the master node */
        DistributedStep2Master covarianceMaster = new DistributedStep2Master(context, Double.class, Method.fastCSR);

        /* Set the reduced partial result to the master algorithm to compute the final result */
        reducedPres.unpack(context);
        covarianceMaster.input.add(DistributedStep2MasterInputId.partialResults, reducedPres);

        /* Compute a dense variance-covariance matrix on the master node */
        covarianceMaster.compute();

        /* Finalize computations and retrieve the results */
        Result res = covarianceMaster.finalizeCompute();

        CovarianceResult covResult = new CovarianceResult();
        covResult.covariance = (HomogenNumericTable)res.get(ResultId.covariance);
        covResult.mean = (HomogenNumericTable)res.get(ResultId.mean);
        return covResult;
    }
}
