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
 * @defgroup decision_tree_classification_prediction_batch Batch
 * @ingroup decision_tree_classification_prediction
 * @{
 */
/**
 * @brief Contains classes for predictions based on decision tree classification models
 */
package com.intel.daal.algorithms.decision_tree.classification.prediction;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.algorithms.decision_tree.classification.Parameter;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DECISION_TREE__CLASSIFICATION__PREDICTION__PREDICTIONBATCH"></a>
 * @brief Predicts decision tree classification classification results
 *
 * \par References
 *      - com.intel.daal.algorithms.decision_tree.classification.Model class
 *      - com.intel.daal.algorithms.classifier.prediction.PredictionResult class
 */
public class PredictionBatch extends com.intel.daal.algorithms.classifier.prediction.PredictionBatch {
    public PredictionInput      input;     /*!< %Input data */
    public Parameter  parameter;     /*!< Parameters of the algorithm */
    public PredictionMethod method; /*!< %Prediction method for the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the decision tree classification prediction algorithm by copying input objects and parameters
     * of another decision tree classification prediction algorithm
     * @param context   Context to manage decision tree classification prediction
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public PredictionBatch(DaalContext context, PredictionBatch other) {
        super(context);
        method = other.method;
        prec = other.prec;

        cObject = cClone(other.cObject, prec.getValue(), method.getValue());
        input = new PredictionInput(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()));
        parameter = new Parameter(getContext(), cInitParameter(cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Constructs the decision tree classification prediction algorithm
     * @param context   Context to manage decision tree classification prediction
     * @param cls       Data type to use in intermediate computations for decision tree classification prediction,
     *                  Double.class or Float.class
     * @param method    decision tree classification prediction method, @ref PredictionMethod
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

        cObject = cInit(prec.getValue(), this.method.getValue());
        input = new PredictionInput(getContext(), cGetInput(cObject, prec.getValue(), this.method.getValue()));
        parameter = new Parameter(getContext(), cInitParameter(cObject, prec.getValue(), this.method.getValue()));
    }

    /**
     * Returns the newly allocated decision tree classification prediction algorithm with a copy of input objects
     * and parameters of this decision tree classification prediction algorithm
     * @param context   Context to manage decision tree classification prediction
     *
     * @return The newly allocated algorithm
     */
    @Override
    public PredictionBatch clone(DaalContext context) {
        return new PredictionBatch(context, this);
    }

    private native long cInit(int prec, int method);

    private native long cInitParameter(long algAddr, int prec, int method);

    private native long cGetInput(long algAddr, int prec, int method);

    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
