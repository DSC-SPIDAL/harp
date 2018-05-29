/* file: PCACorCSRBatch.java */
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
 //     Java example of principal component analysis (PCA) using the correlation
 //     method in the batch processing mode for sparse data
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-PCACORRELATIONCSRBATCH">
 * @example PCACorCSRBatch.java
 */

package com.intel.daal.examples.pca;

import com.intel.daal.algorithms.pca.Batch;
import com.intel.daal.algorithms.pca.InputId;
import com.intel.daal.algorithms.pca.Method;
import com.intel.daal.algorithms.pca.Result;
import com.intel.daal.algorithms.pca.ResultId;
import com.intel.daal.algorithms.pca.ResultsToComputeId;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

class PCACorCSRBatch {
    /* Input data set parameters */
    private static DaalContext  context = new DaalContext();
    private static final String datasetFileName = "../data/batch/covcormoments_csr.csv";

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Read a data set from a file and create a numeric table for storing the input data */
        CSRNumericTable data = Service.createSparseTable(context, datasetFileName);

        /* Create an algorithm to compute PCA decomposition using the correlation method */
        Batch pcaAlgorithm = new Batch(context, Float.class, Method.correlationDense);

        com.intel.daal.algorithms.covariance.Batch covarianceSparse
            = new com.intel.daal.algorithms.covariance.Batch(context, Float.class, com.intel.daal.algorithms.covariance.Method.fastCSR);
        pcaAlgorithm.parameter.setCovariance(covarianceSparse);
        pcaAlgorithm.parameter.setResultsToCompute(ResultsToComputeId.mean | ResultsToComputeId.variance | ResultsToComputeId.eigenvalue);
        pcaAlgorithm.parameter.setIsDeterministic(true);
        /* Set the input data */
        pcaAlgorithm.input.set(InputId.data, data);

        /* Compute PCA decomposition */
        Result res = pcaAlgorithm.compute();

        NumericTable eigenValues = res.get(ResultId.eigenValues);
        NumericTable eigenVectors = res.get(ResultId.eigenVectors);
        NumericTable means = res.get(ResultId.means);
        NumericTable variances = res.get(ResultId.variances);

        Service.printNumericTable("Eigenvalues:", eigenValues);
        Service.printNumericTable("Eigenvectors:", eigenVectors);
        Service.printNumericTable("Means:", means);
        Service.printNumericTable("Variances:", variances);

        context.dispose();
    }
}
