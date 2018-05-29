/* file: Input.java */
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
 * @ingroup sum_of_functions
 * @{
 */
package com.intel.daal.algorithms.optimization_solver.sum_of_functions;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__SUM_OF_FUNCTIONS__INPUT"></a>
 * @brief %Input objects for the Sum of functions algorithm
 */
public class Input extends com.intel.daal.algorithms.optimization_solver.objective_function.Input {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    private long cCreatedInput; /*!< Pointer to C++ interface implementation of the input */

    /**
     * Constructs the input for the sum of functions algorithm
     * @param context       Context to manage the sum of functions algorithm
     * @param cInput        Pointer to C++ implementation of the input
     */
    public Input(DaalContext context, long cInput) {
        super(context, cInput);
    }

    /**
     * Constructs the input for the sum of functions algorithm
     * @param context       Context to manage the input for the sum of functions algorithm
     */
    public Input(DaalContext context) {
        super(context);
        this.cCreatedInput = cCreateInput();
        this.cObject = this.cCreatedInput;
    }

    /**
     * Sets an input object for the Sum of functions algorithm
     * @param id    Identifier of the input object
     * @param val   The input object
     */
    public void set(InputId id, NumericTable val) {
        if (id != InputId.argument) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetInput(cObject, id.getValue(), val.getCObject());
    }

    /**
     * Returns an input object for the Sum of functions algorithm
     * @param id Identifier of the input object
     * @return   %Input object that corresponds to the given identifier
     */
    public NumericTable get(InputId id) {
        if (id != InputId.argument) {
            throw new IllegalArgumentException("id unsupported");
        }
        return (NumericTable)Factory.instance().createObject(getContext(), cGetInput(cObject, id.getValue()));
    }

    /**
     * Sets input pointer for algorithm in native side
     * @param cInput     The address of the native input object
     * @param cAlgorithm The address of the native algorithm object
     */
    public void setCInput(long cInput, long cAlgorithm) {
        this.cObject = cInput;
        cSetCInput(this.cObject, cAlgorithm);
    }

    /**
    * Releases memory allocated for the native parameter object
    */
    @Override
    public void dispose() {
        if(this.cCreatedInput != 0) {
            cInputDispose(this.cCreatedInput);
            this.cCreatedInput = 0;
        }
    }

    private native void cSetInput(long cInput, int id, long ntAddr);
    private native long cGetInput(long cInput, int id);
    private native void cSetCInput(long cObject, long cAlgorithm);
    private native long cCreateInput();
    private native void cInputDispose(long cCreatedInput);
}
/** @} */
