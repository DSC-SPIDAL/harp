/* file: TrainParameter.java */
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
 * @ingroup ridge_regression
 * @{
 */
/**
 * \brief Contains classes for computing the result of the ridge regression algorithm
 */
package com.intel.daal.algorithms.ridge_regression;

import com.intel.daal.services.DaalContext;

import com.intel.daal.algorithms.ridge_regression.Parameter;

import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__RIDGE_REGRESSION__TRAINPARAMETER"></a>
 * @brief Ridge regression algorithm parameters
 */
public class TrainParameter extends com.intel.daal.algorithms.ridge_regression.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public TrainParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     * Sets the numeric table that represents ridge parameters. If no ridge parameters are provided,
     * the implementation will generate ridge parameters equal to 1.
     * @param ridgeParameters The numeric table that represents ridge parameters
     */
    public void setRidgeParameters(NumericTable ridgeParameters) {
        cSetRidgeParameters(this.cObject, ridgeParameters.getCObject());
    }

    /**
     * Retrieves the numeric table that represents ridge parameters. If no ridge parameters are provided,
     * the implementation will generate ridge parameters equal to 1.
     * @return The numeric table that represents ridge parameters.
     */
    public NumericTable getRidgeParameters() {
        return (NumericTable)Factory.instance().createObject(getContext(), cGetRidgeParameters(this.cObject));
    }

    private native void cSetRidgeParameters(long parAddr, long ridgeParameters);
    private native long cGetRidgeParameters(long parAddr);
}
/** @} */
