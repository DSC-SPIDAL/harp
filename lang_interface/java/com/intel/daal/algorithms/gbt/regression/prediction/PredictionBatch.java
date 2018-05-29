/* file: PredictionBatch.java */
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
 * @defgroup gbt_regression_prediction_batch Batch
 * @ingroup gbt_regression_prediction
 * @{
 */
/**
 * @brief Contains classes for predictions based on gradient boosted trees regression models
 */
package com.intel.daal.algorithms.gbt.regression.prediction;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.gbt.regression.prediction.Parameter;
import com.intel.daal.algorithms.gbt.regression.prediction.PredictionInput;
import com.intel.daal.algorithms.gbt.regression.prediction.PredictionResult;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__GBT__REGRESSION__PREDICTION__PREDICTIONBATCH"></a>
 * @brief Predicts gradient boosted trees regression regression results
 *
 * \par References
 *      - com.intel.daal.algorithms.gbt.regression.Model class
 *      - com.intel.daal.algorithms.gbt.regression.prediction.PredictionResult class
 */
public class PredictionBatch extends com.intel.daal.algorithms.Prediction {
    protected Precision    prec;
    public PredictionInput input;   /*!< %Input data */
    public Parameter parameter;     /*!< Parameters of the algorithm */
    public PredictionMethod method; /*!< %Prediction method for the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the gradient boosted trees regression prediction algorithm by copying input objects and parameters
     * of another gradient boosted trees regression prediction algorithm
     * @param context   Context to manage gradient boosted trees regression prediction
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public PredictionBatch(DaalContext context, PredictionBatch other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), this.method.getValue());
        input = new PredictionInput(getContext(), cObject, prec, method);
        parameter = new Parameter(getContext(),
                cInitParameter(this.cObject, prec.getValue(), method.getValue(), ComputeMode.batch.getValue()));
    }

    /**
     * Constructs the gradient boosted trees regression prediction algorithm
     * @param context   Context to manage gradient boosted trees regression prediction
     * @param cls       Data type to use in intermediate computations for gradient boosted trees regression prediction,
     *                  Double.class or Float.class
     * @param method    gradient boosted trees regression prediction method, @ref PredictionMethod
     */
    public PredictionBatch(DaalContext context, Class<? extends Number> cls, PredictionMethod method) {
        super(context);

        this.method = method;

        if (this.method != PredictionMethod.defaultDense) {
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

        this.cObject = cInit(prec.getValue(), this.method.getValue());
        input = new PredictionInput(getContext(), cObject, prec, method);
        parameter = new Parameter(getContext(),
                cInitParameter(this.cObject, prec.getValue(), method.getValue(), ComputeMode.batch.getValue()));
    }

    /**
     * Computes the results of gradient boosted trees regression prediction in the batch processing mode
     * @return Results of igradient boosted trees regression prediction in the batch processing mode
     */
    @Override
    public PredictionResult compute() {
        super.compute();
        PredictionResult result = new PredictionResult(getContext(), cObject, prec, method);
        return result;
    }

    /**
     * Returns the newly allocated gradient boosted trees regression prediction algorithm with a copy of input objects
     * and parameters of this gradient boosted trees regression prediction algorithm
     * @param context   Context to manage gradient boosted trees regression prediction
     *
     * @return The newly allocated algorithm
     */
    @Override
    public PredictionBatch clone(DaalContext context) {
        return new PredictionBatch(context, this);
    }

    private native long cInit(int prec, int method);

    private native long cInitParameter(long algAddr, int prec, int method, int cmode);

    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
