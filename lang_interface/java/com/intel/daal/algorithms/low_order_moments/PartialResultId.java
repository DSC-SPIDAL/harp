/* file: PartialResultId.java */
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
 * @ingroup low_order_moments
 * @{
 */
package com.intel.daal.algorithms.low_order_moments;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOW_ORDER_MOMENTS__PARTIALRESULTID"></a>
 * @brief Available identifiers of partial results of the low order %moments algorithm
 */
public final class PartialResultId {
    private int _value;

    /**
     * Constructs the partial result object identifier using the provided value
     * @param value     Value corresponding to the partial result object identifier
     */
    public PartialResultId(int value) {
        _value = value;
    }

    /**
     * Returns the value corresponding to the partial result object identifier
     * @return Value corresponding to the partial result object identifier
     */
    public int getValue() {
        return _value;
    }

    private static final int NObservations             = 0;
    private static final int PartialMinimum            = 1;
    private static final int PartialMaximum            = 2;
    private static final int PartialSum                = 3;
    private static final int PartialSumSquares         = 4;
    private static final int PartialSumSquaresCentered = 5;

    /**< Number of rows processed so far */
    public static final PartialResultId nObservations = new PartialResultId(NObservations);

    /**< Partial minimum */
    public static final PartialResultId partialMinimum = new PartialResultId(PartialMinimum);

    /*!< Partial maximum */
    public static final PartialResultId partialMaximum = new PartialResultId(PartialMaximum);

    /*!< Partial sum */
    public static final PartialResultId partialSum = new PartialResultId(PartialSum);

    /*!< Partial sum of squares */
    public static final PartialResultId partialSumSquares = new PartialResultId(PartialSumSquares);

    /**< Partial sum of squared difference from the means */
    public static final PartialResultId partialSumSquaresCentered = new PartialResultId(PartialSumSquaresCentered);
}
/** @} */
