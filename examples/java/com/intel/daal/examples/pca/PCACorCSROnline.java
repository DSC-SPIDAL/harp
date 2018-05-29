/* file: PCACorCSROnline.java */
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
 //     method in the online processing mode for sparse data
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-PCACORRELATIONCSRONLINE">
 * @example PCACorCSROnline.java
 */

package com.intel.daal.examples.pca;

import com.intel.daal.algorithms.pca.InputId;
import com.intel.daal.algorithms.pca.Method;
import com.intel.daal.algorithms.pca.Online;
import com.intel.daal.algorithms.pca.Result;
import com.intel.daal.algorithms.pca.ResultId;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class PCACorCSROnline {
    /* Input data set parameters */
    private static DaalContext  context = new DaalContext();

    /* Input data set parameters */
    private static final String datasetFileNames[] = new String[] { "../data/online/covcormoments_csr_1.csv",
                                                                    "../data/online/covcormoments_csr_2.csv",
                                                                    "../data/online/covcormoments_csr_3.csv",
                                                                    "../data/online/covcormoments_csr_4.csv"
                                                                  };
    private static final int nBlocks = 4;

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Create an algorithm to compute PCA decomposition using the correlation method */
        Online pcaAlgorithm = new Online(context, Float.class, Method.correlationDense);

        com.intel.daal.algorithms.covariance.Online covarianceSparse
            = new com.intel.daal.algorithms.covariance.Online(context, Float.class, com.intel.daal.algorithms.covariance.Method.fastCSR);
        pcaAlgorithm.parameter.setCovariance(covarianceSparse);

        for (int i = 0; i < nBlocks; i++) {
            /* Read the input data from a file */
            CSRNumericTable data = Service.createSparseTable(context, datasetFileNames[i]);

            /* Set the input data */
            pcaAlgorithm.input.set(InputId.data, data);

            /* Compute partial estimates */
            pcaAlgorithm.compute();
        }

        /* Finalize computations and retrieve the results */
        Result res = pcaAlgorithm.finalizeCompute();

        NumericTable eigenValues = res.get(ResultId.eigenValues);
        NumericTable eigenVectors = res.get(ResultId.eigenVectors);
        Service.printNumericTable("Eigenvalues:", eigenValues);
        Service.printNumericTable("Eigenvectors:", eigenVectors);

        context.dispose();
    }
}
