/* file: RowMergedNumericTable.java */
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

/**
 * @ingroup numeric_tables
 * @{
 */
package com.intel.daal.data_management.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Vector;

import com.intel.daal.data_management.data.DataCollection;
import com.intel.daal.services.DaalContext;

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__DATA__ROWMERGEDNUMERICTABLE"></a>
 *  @brief Class that provides methods to access a collection of numeric tables as if they are joined by rows
 */
public class RowMergedNumericTable extends NumericTable {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public RowMergedNumericTable(DaalContext context, RowMergedNumericTableImpl impl) {
        super(context);
        tableImpl = impl;
    }

    /**
     * Constructs empty row merged numeric table
     *
     * @param context   Context to manage created row merged numeric table
     */
    public RowMergedNumericTable(DaalContext context) {
        super(context);
        tableImpl = new RowMergedNumericTableImpl(context);
    }

    /**
     * Constructs row merged numeric table from C++ row merged numeric table
     *
     * @param context   Context to manage created row merged numeric table
     * @param cTable    Pointer to C++ numeric table
     */
    public RowMergedNumericTable(DaalContext context, long cTable) {
        super(context);
        tableImpl = new RowMergedNumericTableImpl(context, cTable);
    }

    /**
     * Constructs row merged numeric table consisting of one table
     *
     * @param context   Context to manage created row merged numeric table
     * @param table     Pointer to the Numeric Table
     */
    public RowMergedNumericTable(DaalContext context, NumericTable table) {
        super(context);
        tableImpl = new RowMergedNumericTableImpl(context, table);
    }

    /**
     *  Adds the table to the right of the rowmerged numeric table
     *  \param table    Pointer to the Numeric Table
     */
    public void addNumericTable(NumericTable table) {
        ((RowMergedNumericTableImpl)tableImpl).addNumericTable(table);
    }
}
/** @} */
