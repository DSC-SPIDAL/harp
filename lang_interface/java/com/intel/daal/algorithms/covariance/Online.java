/* file: Online.java */
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
 * @defgroup covariance_online Online
 * @ingroup covariance
 * @{
 */
package com.intel.daal.algorithms.covariance;

import com.intel.daal.algorithms.AnalysisOnline;
import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__COVARIANCE__ONLINE"></a>
 * @brief Computes the correlation or variance-covariance matrix in the online processing mode
 * <!-- \n<a href="DAAL-REF-COVARIANCE-ALGORITHM">Correlation or variance-covariance matrix algorithm description and usage models</a> -->
 *
 * @par References
 *      - InputId class. Identifiers of input objects
 *      - PartialResultId class. Identifiers of partial results
 *      - ResultId class. Identifiers of the results
 *      - Input class
 *      - OnlineParameter class
 *      - PartialResult class
 *      - Result class
 */
public class Online extends OnlineImpl {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the correlation or variance-covariance matrix algorithm in the online processing mode
     * by copying input objects and parameters of another algorithm for correlation or variance-covariance
     * matrix computation
     * @param context   Context to manage the correlation or variance-covariance matrix algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public Online(DaalContext context, Online other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), this.method.getValue());

        input = new Input(getContext(), cObject, prec, method, ComputeMode.online);
        parameter = new OnlineParameter(getContext(),  cInitParameter(cObject, prec.getValue(), method.getValue()), cObject);
    }

    /**
     * Constructs the correlation or variance-covariance matrix algorithm in the online processing mode
     * @param context   Context to manage the correlation or variance-covariance matrix algorithm
     * @param cls       Data type to use in intermediate computations of the correlation or variance-covariance matrix,
     *                  Double.class or Float.class
     * @param method    Computation method, @ref Method
     */
    public Online(DaalContext context, Class<? extends Number> cls, Method method) {
        super(context);
        this.method = method;
        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (this.method != Method.defaultDense && this.method != Method.singlePassDense
            && this.method != Method.sumDense && this.method != Method.fastCSR
            && this.method != Method.singlePassCSR && this.method != Method.sumCSR) {
            throw new IllegalArgumentException("method unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        }
        else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cInit(prec.getValue(), this.method.getValue());

        input = new Input(getContext(), cObject, prec, method, ComputeMode.online);
        parameter = new OnlineParameter(getContext(),  cInitParameter(cObject, prec.getValue(), method.getValue()), cObject);
    }

    /**
     * Returns the newly allocated correlation or variance-covariance matrix algorithm in the online processing mode
     * with a copy of input objects and parameters of this correlation or variance-covariance matrix algorithm
     * @param context   Context to manage the correlation or variance-covariance matrix algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public Online clone(DaalContext context) {
        return new Online(context, this);
    }

    private native long cInit(int prec, int method);

    private native long cClone(long cAlgorithm, int prec, int method);

    private native long cInitParameter(long cAlgorithm, int prec, int method);

}
/** @} */
