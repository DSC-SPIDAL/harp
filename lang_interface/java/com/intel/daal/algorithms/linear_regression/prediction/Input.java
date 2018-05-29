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
 * @defgroup linear_regression_prediction Prediction
 * @brief Contains a class for making linear regression model-based prediction
 * @ingroup linear_regression
 * @{
 */
/**
 * \brief Contains classes for linear regression model-based prediction
 */
package com.intel.daal.algorithms.linear_regression.prediction;

import com.intel.daal.algorithms.Precision;
import com.intel.daal.algorithms.linear_regression.Model;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LINEAR_REGRESSION__PREDICTION__INPUT"></a>
 * @brief %Input object for making linear regression model-based prediction
 */
public final class Input extends com.intel.daal.algorithms.Input {

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public Input(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Sets an input object for linear regression model-based prediction
     * @param id      Identifier of the input object
     * @param val     Serializable object
     */
    public void set(PredictionInputId id, SerializableBase val) {
        if (id != PredictionInputId.data && id != PredictionInputId.model) {
            throw new IllegalArgumentException("Incorrect PredictionInputId");
        }

        long addr = 0;

        if (id == PredictionInputId.data) {
            addr = ((NumericTable) val).getCObject();
        } else if (id == PredictionInputId.model) {
            addr = ((Model) val).getCObject();
        }
        cSetInput(this.cObject, id.getValue(), addr);
    }

    /**
     * Returns an input object for linear regression model-based prediction
     * @param id      Identifier of the input object
     * @return      Serializable object that corresponds to the given identifier
     */
    public SerializableBase get(PredictionInputId id) {
        if (id != PredictionInputId.data && id != PredictionInputId.model) {
            throw new IllegalArgumentException("id unsupported"); // error processing
        }

        if (id == PredictionInputId.data) {
            return (NumericTable)Factory.instance().createObject(getContext(), cGetInput(cObject, id.getValue()));
        } else if (id == PredictionInputId.model) {
            return new Model(getContext(), cGetInput(cObject, id.getValue()));
        }
        return null;
    }

    private native void cSetInput(long cObject, int id, long resAddr);

    private native long cGetInput(long cObject, int id);
}
/** @} */
