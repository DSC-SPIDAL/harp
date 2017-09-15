/* file: Online.java */
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
 * @defgroup svd_online Online
 * @ingroup svd
 * @{
 */
package com.intel.daal.algorithms.svd;

import com.intel.daal.algorithms.AnalysisOnline;
import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__SVD__ONLINE"></a>
 * @brief Runs the SVD algorithm in the online processing mode
 * <!-- \n<a href="DAAL-REF-SVD-ALGORITHM">SVD algorithm description and usage models</a> -->
 *
 * @par References
 *      - InputId class. Identifiers of SVD input objects
 *      - PartialResultId class. Identifiers of partial SVD results
 *      - ResultId class. Identifiers of SVD results
 *      - ResultFormat class. Options to return SVD output matrices
 */
public class Online extends AnalysisOnline {
    public Input                  input;        /*!< %Input data */
    public Parameter  parameter;     /*!< Parameters of the algorithm */
    public Method     method;  /*!< Computation method for the algorithm */
    protected OnlinePartialResult partialResult;    /*!< Partial result of the algorithm */
    protected Precision                 prec; /*!< Precision of intermediate computations */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the SVD algorithm by copying input objects and parameters
     * of another SVD algorithm
     * @param context   Context to manage created SVD algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public Online(DaalContext context, Online other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), method.getValue());
        partialResult = null;
        input = new Input(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()));
        parameter = new Parameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Constructs the SVD algorithm
     * @param context   Context to manage created SVD algorithm
     * @param cls       Data type to use in intermediate computations for the SVD algorithm,
     *                  Double.class or Float.class
     * @param method    SVD computation method, @ref Method
     */
    public Online(DaalContext context, Class<? extends Number> cls, Method method) {
        super(context);

        this.method = method;
        if (this.method != Method.defaultDense) {
            throw new IllegalArgumentException("method unsupported");
        }

        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        } else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cInitOnline(prec.getValue(), method.getValue());
        partialResult = null;
        input = new Input(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()));
        parameter = new Parameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Runs the SVD algorithm
     * @return  Partial results of the SVD algorithm obtained in the online processing mode
     */
    @Override
    public OnlinePartialResult compute() {
        super.compute();
        partialResult = new OnlinePartialResult(getContext(), cGetPartialResult(cObject, prec.getValue(), method.getValue()));
        return partialResult;
    }

    /**
     * Computes final results of the SVD algorithm
     * @return  Final results of the SVD algorithm
     */
    @Override
    public Result finalizeCompute() {
        super.finalizeCompute();
        Result result = new Result(getContext(), cGetResult(cObject, prec.getValue(), method.getValue()));
        return result;
    }

    /**
     * Registers user-allocated memory to store partial results of the SVD algorithm
     * @param partialResult         Structure to store partial results of the SVD algorithm
     * @param initializationFlag    Flag that specifies whether partial results are initialized
     */
    public void setPartialResult(OnlinePartialResult partialResult, boolean initializationFlag) {
        this.partialResult = partialResult;
        cSetPartialResult(cObject, prec.getValue(), method.getValue(), partialResult.getCObject(),
                initializationFlag);
    }

    /**
     * Registers user-allocated memory to store partial results of the SVD algorithm
     * @param partialResult         Structure to store partial results of the SVD algorithm
     */
    public void setPartialResult(OnlinePartialResult partialResult) {
        setPartialResult(partialResult, false);
    }

    /**
     * Registers user-allocated memory to store results of the SVD algorithm
     * @param result    Structure to store results of the SVD algorithm
     */
    public void setResult(Result result) {
        cSetResult(cObject, prec.getValue(), method.getValue(), result.getCObject());
    }

    /**
     * Returns the newly allocated SVD algorithm
     * with a copy of input objects and parameters of this SVD algorithm
     * @param context   Context to manage created SVD algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public Online clone(DaalContext context) {
        return new Online(context, this);
    }

    private native long cInitOnline(int prec, int method);

    private native long cInitParameter(long addr, int prec, int method);

    private native long cGetInput(long addr, int prec, int method);

    private native long cGetResult(long addr, int prec, int method);

    protected native long cGetPartialResult(long addr, int prec, int method);

    private native void cSetResult(long cObject, int prec, int method, long cResult);

    private native void cSetPartialResult(long cObject, int prec, int method, long cPartialResult,
            boolean initializationFlag);

    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
