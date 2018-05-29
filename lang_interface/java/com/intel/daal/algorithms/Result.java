/* file: Result.java */
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

import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;

/**
 *  <a name="DAAL-CLASS-ALGORITHMS__RESULT"></a>
 *  \brief %Base class to represent final results of the computation.
 *         Algorithm-specific final results are represented as derivative classes of the Result class.
 */
abstract public class Result extends SerializableBase {

    /**
     * Constructs the result of the algorithm
     * @param context   Context to manage the result of the algorithm
     */
    protected Result(DaalContext context) {
        super(context);
    }

    /**
     * Constructs parameter from C++ parameter
     * @param context Context to manage the result
     * @param cResult Address of C++ parameter
     */
    public Result(DaalContext context, long cResult) {
        super(context);
        this.cObject = cResult;
    }

    /**
     * Checks the correctness of the result
     * @param input         Input object
     * @param parameter     Parameters of the algorithm
     * @param method        Computation method
     */
    public void check(Input input, Parameter parameter, int method) {
        cCheckInput(this.cObject, input.getCObject(), parameter.getCObject(), method);
    }

    /**
     * Checks the correctness of the result
     * @param partialResult Partial result of the algorithm
     * @param parameter     Parameters of the algorithm
     * @param method        Computation method
     */
    public void check(PartialResult partialResult, Parameter parameter, int method) {
        cCheckPartRes(this.cObject, partialResult.getCObject(), parameter.getCObject(), method);
    }

    private native void cDispose(long parAddr);

    private native void cCheckInput(long resAddr, long inputAddr, long parAddr, int method);

    private native void cCheckPartRes(long resAddr, long partResAddr, long parAddr, int method);

}
/** @} */
