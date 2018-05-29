/* file: ExplainedVarianceBatch.java */
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
 * @defgroup pca_quality_metric_explained_variance_batch Batch
 * @ingroup pca_quality_metric_explained_variance
 * @{
 */
package com.intel.daal.algorithms.pca.quality_metric;

import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__PCA__QUALITY_METRIC__EXPLAINEDVARIANCEBATCH"></a>
 * @brief Computes the PCA explained variance quality metrics in batch processing mode
 *
 * @par Enumerations
 *      - @ref ExplainedVarianceMethod                     Method for computing the explained variance quality metrics
 *      - @ref ExplainedVarianceInputId                    Identifiers of input objects for the explained variance quality metrics algorithm
 *      - @ref ExplainedVarianceResultId                   Result identifiers for the explained variance quality metrics
 *
 * @par References
 *      - ExplainedVarianceInput class
 *      - ExplainedVarianceParameter class
 */
public class ExplainedVarianceBatch extends com.intel.daal.algorithms.quality_metric.QualityMetricBatch {
    public ExplainedVarianceInput     input;
    public ExplainedVarianceParameter parameter;
    public ExplainedVarianceMethod    method;

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the explained variance quality metrics algorithm in the batch processing mode
     * by copying input objects and parameters of another explained variance quality metrics algorithm
     * @param context   Context to manage the explained variance quality metrics algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public ExplainedVarianceBatch(DaalContext context, ExplainedVarianceBatch other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), this.method.getValue());

        input = new ExplainedVarianceInput(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()));
        parameter = new ExplainedVarianceParameter(getContext(), cInitParameter(cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Constructs the explained variance quality metrics algorithm in the batch processing mode
     * @param context   Context to manage the explained variance quality metrics algorithm
     * @param cls       Data type to use in intermediate computations of the explained variance quality metrics,
     *                  Double.class or Float.class
     * @param method    Method for computing the explained variance quality metrics, @ref ExplainedVarianceMethod
     */
    public ExplainedVarianceBatch(DaalContext context, Class<? extends Number> cls,
            ExplainedVarianceMethod method) {
        super(context);
        this.method = method;
        if (cls != Double.class && cls != Float.class) {
            throw new IllegalArgumentException("type unsupported");
        }

        if (this.method != ExplainedVarianceMethod.defaultDense) {
            throw new IllegalArgumentException("method unsupported");
        }

        if (cls == Double.class) {
            prec = Precision.doublePrecision;
        } else {
            prec = Precision.singlePrecision;
        }

        this.cObject = cInit(prec.getValue(), this.method.getValue());

        input = new ExplainedVarianceInput(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()));
        parameter = new ExplainedVarianceParameter(getContext(), cInitParameter(cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Computes the explained variance quality metrics
     * @return Structure that contains results of the explained variance quality metrics algorithm
     */
    @Override
    public ExplainedVarianceResult compute() {
        super.compute();
        ExplainedVarianceResult result = new ExplainedVarianceResult(getContext(), cGetResult(cObject, prec.getValue(),
                method.getValue()));
        return result;
    }

    /**
     * Registers user-allocated memory for storing results of the explained variance quality metrics algorithm
     * @param result    Structure for storing results of the explained variance quality metrics algorithm
     */
    public void setResult(ExplainedVarianceResult result) {
        cSetResult(cObject, prec.getValue(), method.getValue(), result.getCObject());
    }

    /**
     * Returns the newly allocated explained variance quality metrics algorithm with a copy of input objects
     * and parameters of this explained variance quality metrics algorithm
     * @param context   Context to manage the explained variance quality metrics algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public ExplainedVarianceBatch clone(DaalContext context) {
        return new ExplainedVarianceBatch(context, this);
    }

    private native long cInit(int prec, int method);

    private native void cSetResult(long cAlgorithm, int prec, int method, long cObject);

    private native long cInitParameter(long algAddr, int prec, int method);

    private native long cGetInput(long algAddr, int prec, int method);

    private native long cGetResult(long algAddr, int prec, int method);

    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
