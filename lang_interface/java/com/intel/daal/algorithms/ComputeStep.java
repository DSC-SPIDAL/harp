/* file: ComputeStep.java */
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
 * @ingroup base_algorithms
 * @{
 */
package com.intel.daal.algorithms;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__COMPUTESTEP"></a>
 * Describes on which node the computation stage is done in distributed computations
 */
public final class ComputeStep {
    private int _value;

    /**
     * Constructs the compute step object using the provided value
     * @param value     Value corresponding to the compute step object
     */
    public ComputeStep(int value) {
        _value = value;
    }

    /**
     * Returns the value corresponding to the compute step object
     * @return Value corresponding to the compute step object
     */
    public int getValue() {
        return _value;
    }

    private static final int step1LocalValue  = 0;
    private static final int step2MasterValue = 1;
    private static final int step3LocalValue  = 2;

    /** Processing is done on local nodes */
    public static final ComputeStep step1Local  = new ComputeStep(step1LocalValue);
    /** Processing is done on master nodes */
    public static final ComputeStep step2Master = new ComputeStep(step2MasterValue);
    /** Finalization is done on local nodes */
    public static final ComputeStep step3Local  = new ComputeStep(step3LocalValue);
}
/** @} */
