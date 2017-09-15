/* file: PredictionBatch.java */
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
 * @defgroup classifier_prediction_batch Batch
 * @ingroup prediction
 * @{
 */
package com.intel.daal.algorithms.classifier.prediction;

import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__CLASSIFIER__PREDICTION__PREDICTIONBATCH"></a>
 * @brief Base class for classifier model-based prediction in the batch processing mode
 *
 * @par References
 *      - NumericTableInputId class
 *      - ModelInputId class
 *      - PredictionResultId class
 */
public abstract class PredictionBatch extends com.intel.daal.algorithms.Prediction {
    protected Precision    prec;

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the classifier model-based prediction algorithm by copying input objects and parameters
     * of another classifier model-based prediction algorithm
     * @param context   Context to manage classifier algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public PredictionBatch(DaalContext context, PredictionBatch other) {
        super(context);
        prec = other.prec;
    }

    /**
     * Constructs the classifier model-based prediction algorithm
     * @param context   Context to manage classifier algorithm
     */
    public PredictionBatch(DaalContext context) {
        super(context);
    }

    /**
     * Computes prediction results based on the model of the classification algorithm
     * @return %Prediction results
     */
    @Override
    public PredictionResult compute() {
        super.compute();
        PredictionResult result = new PredictionResult(getContext(), cObject);
        return result;
    }

    /**
     * Registers user-allocated memory for storing results
     * @param result Structure for storing results
     */
    public void setResult(PredictionResult result) {
        cSetResult(cObject, result.getCObject());
    }

    /**
     * Returns the newly allocated classifier model-based prediction algorithm with a copy of input objects
     * and parameters of this algorithm
     * @param context   Context to manage classifier algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public abstract PredictionBatch clone(DaalContext context);

    private native void cSetResult(long cAlgorithm, long cResult);
}
/** @} */
