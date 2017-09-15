/* file: DataStructuresHomogenTensor.java */
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
 //     Java example of using homogeneous data structures
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-DATASTRUCTURESHOMOGENTENSOR">
 * @example DataStructuresHomogenTensor.java
 */

package com.intel.daal.examples.datasource;

import java.nio.DoubleBuffer;
import com.intel.daal.data_management.data.HomogenTensor;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class DataStructuresHomogenTensor {
    private static DaalContext context = new DaalContext();

    public static void main(String[] args) {
        System.out.println("Homogeneous numeric tensor example");
        int readFeatureIdx;

        double[] data = {
            0.1, 0.2, 0.3,
            0.4, 0.5, 0.6,
            0.7, 0.8, 0.9,
                           1.1, 1.2, 1.3,
                           1.4, 1.5, 1.6,
                           1.7, 1.8, 1.9,
                                          2.1, 2.2, 2.3,
                                          2.4, 2.5, 2.6,
                                          2.7, 2.8, 2.9
        };

        long[] dims = {3,3,3};

        printDoubleArray(data, 9, 3, "Tensor before modification");

        HomogenTensor dataTensor = new HomogenTensor(context, dims, data);

        long fDims[] = {0,1};
        DoubleBuffer dataDoubleSubtensor = DoubleBuffer.allocate((int) 2);

        dataTensor.getSubtensor(fDims, 1, 2, dataDoubleSubtensor);

        printDoubleBuffer(dataDoubleSubtensor, 2, 1, "Subtensor");

        dataDoubleSubtensor.put(-1);

        dataTensor.releaseSubtensor(fDims, 1, 2, dataDoubleSubtensor);

        printDoubleArray(data, 9, 3, "Tensor after modification");

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

    private static void printDoubleArray(double[] buf, long nColumns, long nRows, String message) {
        int step = (int) nColumns;
        System.out.println(message);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nColumns; j++) {
                System.out.format("%6.3f   ", buf[i * step + j]);
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
