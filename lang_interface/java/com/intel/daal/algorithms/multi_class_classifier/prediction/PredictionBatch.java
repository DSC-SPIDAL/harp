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
 * @defgroup multi_class_classifier_prediction_batch Batch
 * @ingroup multi_class_classifier_prediction
 * @{
 */
/**
 * @brief Contains classes for making prediction based on the Multi-class classifier models
 */
package com.intel.daal.algorithms.multi_class_classifier.prediction;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.algorithms.multi_class_classifier.Parameter;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTI_CLASS_CLASSIFIER__PREDICTION__PREDICTIONBATCH"></a>
 * @brief Runs multi-class classifier model based prediction algorithm
 * <!-- \n<a href="DAAL-REF-MULTICLASSCLASSIFIER-ALGORITHM">Multi-class classifier algorithm description and usage models</a> -->
 *
 * @par References
 *      - com.intel.daal.algorithms.classifier.prediction.NumericTableInputId class
 *      - com.intel.daal.algorithms.classifier.prediction.ModelInputId class
 *      - com.intel.daal.algorithms.classifier.prediction.PredictionResultId class
 *      - com.intel.daal.algorithms.classifier.prediction.PredictionResult class
 */
public class PredictionBatch extends com.intel.daal.algorithms.classifier.prediction.PredictionBatch {
    public PredictionInput  input;     /*!< %Input data */
    public Parameter        parameter; /*!< Parameters of the algorithm */
    public PredictionMethod method;    /*!< %Prediction method for the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs multi-class classifier prediction algorithm by copying input objects and parameters
     * of another multi-class classifier prediction algorithm
     * @param context   Context to manage the multi-class classifier prediction algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public PredictionBatch(DaalContext context, PredictionBatch other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), this.method.getValue());

        input = new PredictionInput(getContext(), cObject, ComputeMode.batch);
        parameter = new Parameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()));
    }

    private void constructBatch(Class<? extends Number> cls, PredictionMethod method, long nClasses) {
        this.method = method;
        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (this.method != PredictionMethod.multiClassClassifierWu && this.method != PredictionMethod.voteBased) {
            throw new IllegalArgumentException("method unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        } else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cInit(prec.getValue(), this.method.getValue(), nClasses);

        input = new PredictionInput(getContext(), cObject, ComputeMode.batch);
        parameter = new Parameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Constructs multi-class classifier prediction algorithm
     * @DAAL_DEPRECATED
     * @param context   Context to manage the multi-class classifier prediction algorithm
     * @param cls       Data type to use in intermediate computations of the multi-class classifier prediction algorithm,
     *                  Double.class or Float.class
     * @param method    Multi-class classifier prediction method, @ref PredictionMethod
     */
    @Deprecated
    public PredictionBatch(DaalContext context, Class<? extends Number> cls, PredictionMethod method) {
        super(context);
        constructBatch(cls, method, 0);
    }

    /**
     * Constructs multi-class classifier prediction algorithm
     * @param context   Context to manage the multi-class classifier prediction algorithm
     * @param cls       Data type to use in intermediate computations of the multi-class classifier prediction algorithm,
     *                  Double.class or Float.class
     * @param method    Multi-class classifier prediction method, @ref PredictionMethod
     * @param nClasses  Number of classes
     */
    public PredictionBatch(DaalContext context, Class<? extends Number> cls, PredictionMethod method, long nClasses) {
        super(context);
        constructBatch(cls, method, nClasses);
    }

    /**
     * Returns the newly allocated multi-class classifier prediction algorithm
     * with a copy of input objects and parameters of this multi-class classifier prediction algorithm
     * @param context   Context to manage the multi-class classifier prediction algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public PredictionBatch clone(DaalContext context) {
        return new PredictionBatch(context, this);
    }

    private native long cInit(int prec, int method, long nClasses);

    private native long cInitParameter(long algAddr, int prec, int method);

    private native long cClone(long algAddr, int prec, int method);

    /**
     * Releases memory allocated for the native algorithm object
     */
    //public void dispose() {
    /*
    if(this.cObject != 0) {
        cDispose(this.cObject);
    }
    */
    //}
}
/** @} */
