/* file: DataStructuresCSR.java */
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

/**
 * <a name="DAAL-EXAMPLE-JAVA-DATASTRUCTURESCSR">
 * @example DataStructuresCSR.java
 */

package com.intel.daal.examples.datasource;

import java.nio.DoubleBuffer;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

/*
// Compressed spares rows (CSR) data structures example
*/

class DataStructuresCSR {
    private static final long nObservations = 5;
    private static final long nFeatures     = 5;
    private static final long firstReadRow  = 1;
    private static final long nRead         = 3;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) {
        System.out.println("Compressed spares rows (CSR) numeric table example");
        int readFeatureIdx;

        double values[]   = {1, -1, -3, -2,  5,  4,  6,  4, -4,  2,  7,  8, -5};
        long colIndices[] = {1,  2,  4,  1,  2,  3,  4,  5,  1,  3,  4,  2,  5};
        long rowOffsets[] = {1,          4,      6,          9,         12,     14};

        CSRNumericTable dataTable = new CSRNumericTable(context, values, colIndices, rowOffsets, nFeatures, nObservations);

        /* Read block of rows in dense format */
        DoubleBuffer dataDouble = DoubleBuffer.allocate((int)(nRead * nFeatures));
        dataDouble = dataTable.getBlockOfRows(firstReadRow, nRead, dataDouble);

        printDoubleBuffer(dataDouble, nFeatures, nRead,
                          "Print 3 rows from CSR data array as dense double array:");
        dataTable.releaseBlockOfRows(firstReadRow, nRead, dataDouble);

        context.dispose();
    }

    private static void printDoubleBuffer(DoubleBuffer buf, long nColumns, long nRows, String message) {
        int step = (int) nColumns;
        System.out.println(message);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nColumns; j++) {
                System.out.format("%6.3f   ", buf.get(i * step + j));
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
