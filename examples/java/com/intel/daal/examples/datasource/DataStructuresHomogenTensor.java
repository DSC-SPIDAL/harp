/* file: DataStructuresHomogenTensor.java */
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
 //     Java example of using homogeneous data structures
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-DATASTRUCTURESHOMOGENTENSOR">
 * @example DataStructuresHomogenTensor.java
 */

package com.intel.daal.examples.datasource;

import java.nio.FloatBuffer;
import com.intel.daal.data_management.data.HomogenTensor;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;


class DataStructuresHomogenTensor {
    private static DaalContext context = new DaalContext();

    public static void main(String[] args) {
        int readFeatureIdx;

        float[] data = {1,2,3,4,5,6,7,8,9,11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29};

        long[] dims = {3,3,3};

        System.out.println("Initial data:");
        for(int i= 0;i<dims[0]*dims[1]*dims[2];i++)
        {
            System.out.format("% 5.1f ", data[i]);
        }
        System.out.println("");

        HomogenTensor dataTensor = new HomogenTensor(context, dims, data);

        long fDims[] = {0,1};

        FloatBuffer dataFloatSubtensor = FloatBuffer.allocate(2);

        dataTensor.getSubtensor(fDims, 1, 2, dataFloatSubtensor);

        int sub_demension = dataFloatSubtensor.arrayOffset() + 1;
        int sub_size = dataFloatSubtensor.capacity();

        System.out.format("Subtensor dimensions: % 5.1f\n", (float)sub_demension);
        System.out.format("Subtensor size: % 5.1f\n", (float)sub_size);
        System.out.println("Subtensor data:");
        for(int i= 0;i<sub_size;i++)
        {
            System.out.format("% 5.1f ", dataFloatSubtensor.get(i));
        }
        System.out.println("");

        dataFloatSubtensor.put(-1);

        dataTensor.releaseSubtensor(fDims, 1, 2, dataFloatSubtensor);

        System.out.println("Data after modification:");
        for(int i= 0;i<dims[0]*dims[1]*dims[2];i++)
        {
            System.out.format("% 5.1f ", data[i]);
        }
        System.out.println("");

        context.dispose();
    }

    private static void printFloatBuffer(FloatBuffer buf, long nColumns, long nRows, String message) {
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

    private static void printFloatArray(float[] buf, long nColumns, long nRows, String message) {
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
