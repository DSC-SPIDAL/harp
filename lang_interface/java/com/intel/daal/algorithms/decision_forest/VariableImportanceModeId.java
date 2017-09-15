/* file: VariableImportanceModeId.java */
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
* @defgroup decision_forest Decision forest
 * @ingroup training_and_prediction
 * @{
 */

package com.intel.daal.algorithms.decision_forest;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DECISION_FOREST__TRAINING__VARIABLEIMPORTANCEMODEID"></a>
 * @brief Variable importance computation mode
 */
public final class VariableImportanceModeId {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    private int _value;

    /**
     * Constructs the variable importance computation mode object identifier using the provided value
     * @param value     Value corresponding to the variable importance computation mode object identifier
     */
    public VariableImportanceModeId(int value) {
        _value = value;
    }

    /**
     * Returns the value corresponding to the variable importance computation mode object identifier
     * @return Value corresponding to the variable importance computation mode object identifier
     */
    public int getValue() {
        return _value;
    }

    private static final int noneId = 0;
    private static final int MDIId = 1;
    private static final int MDA_RawId = 2;
    private static final int MDA_ScaledId = 3;

    public static final VariableImportanceModeId none = new VariableImportanceModeId(noneId);
        /*!< Do not compute */
    public static final VariableImportanceModeId MDI = new VariableImportanceModeId(MDIId);
        /*!< Mean Decrease Impurity. Computed as the sum of weighted impurity decreases for all nodes where the variable is used,
             averaged over all trees in the forest */
    public static final VariableImportanceModeId MDA_Raw = new VariableImportanceModeId(MDA_RawId);
        /*!< Mean Decrease Accuracy (permutation importance).
             For each tree, the prediction error on the out-of-bag portion of the data is computed
             (error rate for classification, MSE for regression).
             The same is done after permuting each predictor variable.
             The difference between the two are then averaged over all trees. */
    public static final VariableImportanceModeId MDA_Scaled = new VariableImportanceModeId(MDA_ScaledId);
        /*!< Mean Decrease Accuracy (permutation importance).
             This is MDA_Raw value normalized by its standard deviation. */
}
/** @} */
