/* file: InputId.java */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.intel.daal.algorithms.mf_sgd;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__INPUTID"></a>
 * @brief Available identifiers of input objects for the mf_sgd algorithm
 */
public final class InputId {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    private int _value;

    public InputId(int value) {
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    private static final int dataTrainId = 0;   /* training data in AOSNumericTable */
    private static final int dataTestId = 1;    /* test data in AOSNumericTable */
    private static final int dataWPosId = 2;    /* Row id of training points in Model W stored in homogenNumericTable */
    private static final int dataHPosId = 3;    /* Col id of training points in Model H stored in homogenNumericTable */
    private static final int dataValId = 4;     /* Value of training points stored in homogenNumericTable */
    private static final int testWPosId = 5;    /* Row id of training points in Model W stored in homogenNumericTable */
    private static final int testHPosId = 6;    /* Col id of training points in Model H stored in homogenNumericTable */
    private static final int testValId = 7;     /* Value of training points stored in homogenNumericTable */

    /** %Input data table */
    public static final InputId dataTrain = new InputId(dataTrainId);
    public static final InputId dataTest = new InputId(dataTestId);
    public static final InputId dataWPos = new InputId(dataWPosId);
    public static final InputId dataHPos = new InputId(dataHPosId);
    public static final InputId dataVal = new InputId(dataValId);
    public static final InputId testWPos = new InputId(testWPosId);
    public static final InputId testHPos = new InputId(testHPosId);
    public static final InputId testVal = new InputId(testValId);

}
