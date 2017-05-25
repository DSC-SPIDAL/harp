/* file: ResultId.java */
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
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__RESULTID"></a>
 * @brief Available types of the results of the mf_sgd algorithm
 */
public final class ResultId {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    private int _value;

    public ResultId(int value) {
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    private static final int resWMatId = 0;
    private static final int resHMatId = 1;

    public static final ResultId resWMat = new ResultId(resWMatId); /*!< model matrix W */
    public static final ResultId resHMat = new ResultId(resHMatId); /*!< model matrix H */
}
