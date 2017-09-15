/* file: QRDenseOnline.java */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
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
 //     Java example of computing QR decomposition in the online processing mode
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-QRONLINE">
 * @example QRDenseOnline.java
 */

package com.intel.daal.examples.qr;

import com.intel.daal.algorithms.qr.InputId;
import com.intel.daal.algorithms.qr.Method;
import com.intel.daal.algorithms.qr.Online;
import com.intel.daal.algorithms.qr.Result;
import com.intel.daal.algorithms.qr.ResultId;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class QRDenseOnline {
    /* Input data set parameters */
    private static final String dataset         = "../data/online/qr.csv";
    private static final int    nVectorsInBlock = 4000;
    private static Result       res;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Initialize FileDataSource to retrieve the input data from a .csv file */
        FileDataSource dataSource = new FileDataSource(context, dataset,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);

        /* Create an algorithm to compute QR decomposition in the online processing mode */
        Online qrAlgorithm = new Online(context, Float.class, Method.defaultDense);

        NumericTable input = dataSource.getNumericTable();
        qrAlgorithm.input.set(InputId.data, input);

        while (dataSource.loadDataBlock(nVectorsInBlock) == nVectorsInBlock) {
            /* Compute QR decomposition */
            qrAlgorithm.compute();
        }

        /* Finalize computations and retrieve the results */
        res = qrAlgorithm.finalizeCompute();

        /* Print the results */
        printResults();

        context.dispose();
    }

    private static void printResults() {
        HomogenNumericTable Q = (HomogenNumericTable) res.get(ResultId.matrixQ);
        HomogenNumericTable R = (HomogenNumericTable) res.get(ResultId.matrixR);

        Service.printNumericTable("Orthogonal matrix Q (10 first vectors):", Q, 10);
        Service.printNumericTable("Triangular matrix R:", R);
    }
}
