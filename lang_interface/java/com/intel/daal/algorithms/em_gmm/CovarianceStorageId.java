/* file: CovarianceStorageId.java */
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
 * @ingroup em_gmm
 * @{
 */
package com.intel.daal.algorithms.em_gmm;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__EM_GMM__COVARIANCESTORAGEID"></a>
 * @brief Available identifiers of covariance types in the EM for GMM algorithm
 */
public final class CovarianceStorageId {
    private int _value;

    /**
     * Constructs the covariance type object identifier using the provided value
     * @param value     Value corresponding to the covariance type object identifier
     */
    public CovarianceStorageId(int value) {
        _value = value;
    }

    /**
     * Returns the value corresponding to the covariance type object identifier
     * @return Value corresponding to the covariance type object identifier
     */
    public int getValue() {
        return _value;
    }

    private static final int fullValue      = 0;
    private static final int diagonalValue  = 1;

    public static final CovarianceStorageId full     = new CovarianceStorageId(fullValue);      /*!< Full */
    public static final CovarianceStorageId diagonal = new CovarianceStorageId(diagonalValue);  /*!< Diagonal */
}
/** @} */
