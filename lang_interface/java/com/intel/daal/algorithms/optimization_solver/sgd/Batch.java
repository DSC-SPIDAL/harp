/* file: Batch.java */
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
 * @defgroup sgd Stochastic Gradient Descent Algorithm
 * @brief Contains classes for computing the Stochastic gradient descent
 * @ingroup optimization_solver
 * @{
 */
/**
 * @defgroup sgd_batch Batch
 * @ingroup sgd
 * @{
 */
package com.intel.daal.algorithms.optimization_solver.sgd;

import com.intel.daal.algorithms.AnalysisBatch;
import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.optimization_solver.sgd.*;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.Input;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.Result;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__SGD__BATCH"></a>
 * @brief %Base interface for the SGD algorithm in the batch processing mode
 * <!-- \n<a href="DAAL-REF-SGD-ALGORITHM">SGD algorithm description and usage models</a> -->
 *
 * @par References
 *      - Parameter class
 *      - com.intel.daal.algorithms.optimization_solver.iterative_solver.InputId class
 *      - com.intel.daal.algorithms.optimization_solver.iterative_solver.ResultId class
 *      - com.intel.daal.algorithms.optimization_solver.iterative_solver.Input class
 *
 */
public class Batch extends com.intel.daal.algorithms.optimization_solver.iterative_solver.Batch {

    public Method method; /*!< Computation method for the algorithm */
    private Precision prec; /*!< Precision of intermediate computations */
    public BaseParameter parameter; /*!< Parameters of the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the SGD algorithm by copying input objects and parameters of another SGD algorithm
     * @param context    Context to manage the SGD algorithm
     * @param other      An algorithm to be used as the source to initialize the input objects
     *                   and parameters of the algorithm
     */
    public Batch(DaalContext context, Batch other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), method.getValue());
        input = new Input(context, cGetInput(cObject, prec.getValue(), method.getValue()));
        if(method == Method.defaultDense) {
            parameter = new ParameterDefaultDense(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.miniBatch) {
            parameter = new ParameterMiniBatch(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.momentum) {
            parameter = new ParameterMomentum(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        super.parameter = parameter;
    }

    /**
     * <a name="DAAL-METHOD-ALGORITHMS__OPTIMIZATION_SOLVER__SGD__BATCH__BATCH"></a>
     * Constructs the SGD algorithm
     *
     * @param context      Context to manage the SGD algorithm
     * @param cls          Data type to use in intermediate computations for the SGD algorithm, Double.class or Float.class
     * @param method       SGD computation method, @ref Method
     */
    public Batch(DaalContext context, Class<? extends Number> cls, Method method) {
        super(context);

        this.method = method;

        if (method != Method.defaultDense && method != Method.miniBatch && method != Method.momentum) {
            throw new IllegalArgumentException("method unsupported");
        }
        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        }
        else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cInit(prec.getValue(), method.getValue());
        input = new Input(context, cGetInput(cObject, prec.getValue(), method.getValue()));
        if(method == Method.defaultDense) {
            parameter = new ParameterDefaultDense(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.miniBatch) {
            parameter = new ParameterMiniBatch(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.momentum) {
            parameter = new ParameterMomentum(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        super.parameter = parameter;
    }

    public Batch(DaalContext context, Class<? extends Number> cls, Method method, long cAlgorithm) {
        super(context);

        this.method = method;

        if (method != Method.defaultDense && method != Method.miniBatch && method != Method.momentum) {
            throw new IllegalArgumentException("method unsupported");
        }
        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        }
        else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cAlgorithm;
        input = new Input(context, cGetInput(cObject, prec.getValue(), method.getValue()));
        if(method == Method.defaultDense) {
            parameter = new ParameterDefaultDense(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.miniBatch) {
            parameter = new ParameterMiniBatch(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        else if(method == Method.momentum) {
            parameter = new ParameterMomentum(getContext(), cGetParameter(this.cObject, prec.getValue(), method.getValue()));
        }
        super.parameter = parameter;
    }

    /**
     * Computes the SGD in the batch processing mode
     * @return  Results of the computation
     */
    @Override
    public Result compute() {
        super.compute();
        Result result = new Result(getContext(), cGetResult(cObject));
        return result;
    }

    /**
     * Returns the newly allocated SGD algorithm
     * with a copy of input objects and parameters of this SGD algorithm
     * @param context    Context to manage the SGD algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public Batch clone(DaalContext context) {
        return new Batch(context, this);
    }

    private native long cInit(int prec, int method);
    private native long cClone(long algAddr, int prec, int method);
    private native long cGetInput(long cAlgorithm, int prec, int method);
    private native long cGetParameter(long cAlgorithm, int prec, int method);
}
/** @} */
/** @} */
