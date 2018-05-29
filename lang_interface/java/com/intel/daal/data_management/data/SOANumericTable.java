/* file: SOANumericTable.java */
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
 * @ingroup numeric_tables
 * @{
 */
package com.intel.daal.data_management.data;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Vector;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__SOANUMERICTABLE"></a>
 * @brief Class that provides methods to access data that is stored as a Structure
 *        Of Arrays(SOA), where each contiguous array represents values
 *        corresponding to a specific feature
 */
public class SOANumericTable extends NumericTable {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public SOANumericTable(DaalContext context, SOANumericTableImpl impl) {
        super(context);
        tableImpl = impl;
    }

    /**
     * Constructs a Structure Of Arrays(SOA) numeric table
     *
     * @param context   Context to manage created numeric table
     * @param nFeatures Number of features in numeric table
     * @param nVectors  Number of feature vectors in numeric table
     */
    public SOANumericTable(DaalContext context, long nFeatures, long nVectors) {
        super(context);
        tableImpl = new SOANumericTableImpl(context, nFeatures, nVectors);
    }

    /**
     * Sets array of doubles of the feature to the table
     *
     * @param arr Array of values of the feature
     * @param idx Index of the feature
     */
    public void setArray(double[] arr, long idx) {
        ((SOANumericTableImpl)tableImpl).setArray(arr, idx);
    }

    /**
     * Sets array of floats of the feature to the table
     *
     * @param arr Array of values of the feature
     * @param idx Index of the feature
     */
    public void setArray(float[] arr, long idx) {
        ((SOANumericTableImpl)tableImpl).setArray(arr, idx);
    }

    /**
     * Sets array of longs of the feature to the table
     *
     * @param arr Array of values of the feature
     * @param idx Index of the feature
     */
    public void setArray(long[] arr, long idx) {
        ((SOANumericTableImpl)tableImpl).setArray(arr, idx);
    }

    /**
     * Sets array of integers of the feature to the table
     *
     * @param arr Array of values of the feature
     * @param idx Index of the feature
     */
    public void setArray(int[] arr, long idx) {
        ((SOANumericTableImpl)tableImpl).setArray(arr, idx);
    }
}
/** @} */
