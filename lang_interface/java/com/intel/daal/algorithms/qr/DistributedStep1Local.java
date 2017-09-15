/* file: DistributedStep1Local.java */
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
 * @ingroup qr_distributed
 * @{
 */
package com.intel.daal.algorithms.qr;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__QR__DISTRIBUTEDSTEP1LOCAL"></a>
 * @brief Computes the results of the QR decomposition algorithm on the first step in the distributed processing mode
 * <!-- \n <a href="DAAL-REF-QR-ALGORITHM">QR decomposition algorithm description and usage models</a> -->
 *
 * @par References
 *      - InputId class. Identifiers of input objects for the QR decomposition algorithm
 *      - PartialResultId class. Identifiers of partial results of the QR decomposition algorithm
 *      - ResultId class. Identifiers of the results of the QR decomposition algorithm
 */
public class DistributedStep1Local extends Online {

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the QR decomposition algorithm by copying input objects and parameters
     * of another QR decomposition algorithm
     * @param context   Context to manage the QR decomposition algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public DistributedStep1Local(DaalContext context, DistributedStep1Local other) {
        super(context, other);
    }

    /**
     * Constructs the QR decomposition algorithm
     * @param context   Context to manage the QR decomposition algorithm
     * @param cls       Data type to use in intermediate computations of the QR decomposition algorithm,
     *                  Double.class or Float.class
     * @param method    Computation method, @ref Method
     */
    public DistributedStep1Local(DaalContext context, Class<? extends Number> cls, Method method) {
        super(context, cls, method);
    }

    /**
     * Runs the QR decomposition algorithm
     * @return  Partial results of the first step of the QR decomposition algorithm in the distributed processing mode
     */
    @Override
    public DistributedStep1LocalPartialResult compute() {
        super.compute();
        return new DistributedStep1LocalPartialResult(getContext(), cGetPartialResult(cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Computes final results of the QR decomposition algorithm
     * @return  Final results of the QR decomposition algorithm
     */
    @Override
    public Result finalizeCompute() {
        return super.finalizeCompute();
    }

    /**
     * Returns the newly allocated QR decomposition algorithm
     * with a copy of input objects and parameters of this QR decomposition algorithm
     * @param context   Context to manage the QR decomposition algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public DistributedStep1Local clone(DaalContext context) {
        return new DistributedStep1Local(context, this);
    }
}
/** @} */
