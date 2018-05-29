/* file: ResultId.java */
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
 * @ingroup low_order_moments
 * @{
 */
package com.intel.daal.algorithms.low_order_moments;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOW_ORDER_MOMENTS__RESULTID"></a>
 * @brief Available types of results of the low order %moments algorithm
 */
public final class ResultId {
    private int _value;

    /**
     * Constructs the result object identifier using the provided value
     * @param value     Value corresponding to the result object identifier
     */
    public ResultId(int value) {
        _value = value;
    }

    /**
     * Returns the value corresponding to the result object identifier
     * @return Value corresponding to the result object identifier
     */
    public int getValue() {
        return _value;
    }

    private static final int Minimum              = 0;
    private static final int Maximum              = 1;
    private static final int Sum                  = 2;
    private static final int SumSquares           = 3;
    private static final int SumSquaresCentered   = 4;
    private static final int Mean                 = 5;
    private static final int SecondOrderRawMoment = 6;
    private static final int Variance             = 7;
    private static final int StandardDeviation    = 8;
    private static final int Variation            = 9;

    public static final ResultId minimum              = new ResultId(Minimum);           /*!< Minimum */
    public static final ResultId maximum              = new ResultId(Maximum);           /*!< Maximum */
    public static final ResultId sum                  = new ResultId(Sum);               /*!< Sum */
    public static final ResultId sumSquares           = new ResultId(SumSquares);        /*!< Sum of squares */
    public static final ResultId sumSquaresCentered   = new ResultId(
            SumSquaresCentered);                                                         /*!< Sum of squared difference from the means */
    public static final ResultId mean                 = new ResultId(Mean);              /*!< Mean */
    public static final ResultId secondOrderRawMoment = new ResultId(
            SecondOrderRawMoment);                                                       /*!< Second raw order moment */
    public static final ResultId variance             = new ResultId(Variance);          /*!< Variance */
    public static final ResultId standardDeviation    = new ResultId(StandardDeviation); /*!< Standard deviation */
    public static final ResultId variation            = new ResultId(Variation);         /*!< Variation */
}
/** @} */
