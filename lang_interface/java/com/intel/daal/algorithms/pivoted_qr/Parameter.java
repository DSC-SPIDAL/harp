/* file: Parameter.java */
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
 * @ingroup pivoted_qr
 * @{
 */
package com.intel.daal.algorithms.pivoted_qr;

import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__PIVOTED_QR__PARAMETER"></a>
 * @brief Pivoted QR algorithm parameters
 */
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the parameter of the Pivoted QR algorithm
     * @param context   Context to manage the parameter of the Pivoted QR algorithm
     */
    public Parameter(DaalContext context) {
        super(context);
    }

    public Parameter(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Sets parameter of the pivoted QR algorithm
     * @param permutedColumns  On entry, if i-th element of permutedColumns != 0,
     *                          the i-th column of input matrix is moved  to the beginning of Data * P before
     *                          the computation, and fixed in place during the computation.
     *                          If i-th element of permutedColumns = 0, the i-th column of input data
     *                          is a free column (that is, it may be interchanged during the
     *                          computation with any other free column).
     */
    public void setPermutedColumns(NumericTable permutedColumns) {
        cSetPermutedColumns(this.cObject, permutedColumns.getCObject());
    }

    /**
     * Gets parameter of  the pivoted QR algorithm
     * @return    Identifier of the parameter
     */
    public NumericTable getPermutedColumns() {
        return (NumericTable)Factory.instance().createObject(getContext(), cGetPermutedColumns(this.cObject));
    }

    private native void cSetPermutedColumns(long parAddr, long permutedColumnsAddr);

    private native long cGetPermutedColumns(long parAddr);
}
/** @} */
