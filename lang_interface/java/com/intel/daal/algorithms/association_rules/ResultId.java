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
 * @ingroup association_rules
 * @{
 */
package com.intel.daal.algorithms.association_rules;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__ASSOCIATION_RULES__RESULTID"></a>
 * @brief Available identifiers of results for the association rules algorithm
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

    private static final int LargeItemsets        = 0;
    private static final int LargeItemsetsSupport = 1;
    private static final int AntecedentItemsets   = 2;
    private static final int ConsequentItemsets   = 3;
    private static final int Confidence           = 4;

    public static final ResultId largeItemsets        = new ResultId(LargeItemsets); /*!< Large itemsets            */
    public static final ResultId largeItemsetsSupport = new ResultId(
            LargeItemsetsSupport);                                                   /*!< Support of large itemsets */
    public static final ResultId antecedentItemsets   = new ResultId(
            AntecedentItemsets);                                                     /*!< Antecedent itemsets       */
    public static final ResultId consequentItemsets   = new ResultId(
            ConsequentItemsets);                                                     /*!< Consequent itemsets       */
    public static final ResultId confidence           = new ResultId(Confidence);    /*!< Confidence                */
}
/** @} */
