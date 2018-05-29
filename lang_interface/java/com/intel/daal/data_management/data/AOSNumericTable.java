/* file: AOSNumericTable.java */
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

import java.lang.reflect.Field;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__AOSNUMERICTABLE"></a>
 * @brief Class that provides methods to access data that is stored as a contiguous array
 *         of heterogeneous feature vectors,  and each feature vector is represented
 *         with a data structure.
 *         Therefore, the data is represented as an Array Of Structures(AOS).
 */
public class AOSNumericTable extends NumericTable {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public AOSNumericTable(DaalContext context, AOSNumericTableImpl impl) {
        super(context);
        tableImpl = impl;
    }

    /**
     * Constructor for empty Numeric Table with predefined class for the feature vectors and given number of feature vectors
     *
     * @param context   Context to manage created AOS numeric table
     * @param cls       Class containing expected array elements
     * @param nVectors  The number of rows in the table
     */
    public AOSNumericTable(DaalContext context, Class<?> cls, long nVectors) {
        super(context);
        tableImpl = new AOSNumericTableImpl(context, cls, nVectors);
    }

    /**
     * Constructs Numeric Table from the array of objects representing feature vectors
     *
     * @param context Context to manage created AOS numeric table
     * @param ptr     Array of objects to associate with the Numeric Table
     */
    public AOSNumericTable(DaalContext context, Object[] ptr) {
        super(context);
        tableImpl = new AOSNumericTableImpl(context, ptr);
    }

    /**
     * Array of objects associated with the table
     */
    public void setArray(Object[] arr) {
        ((AOSNumericTableImpl)tableImpl).setArray(arr);
    }

    /**
     * Returns the array of objects associated with the table
     *
     * @return Array of objects
     */
    public Object[] getArray() {
        return ((AOSNumericTableImpl)tableImpl).getArray();
    }
}
/** @} */
