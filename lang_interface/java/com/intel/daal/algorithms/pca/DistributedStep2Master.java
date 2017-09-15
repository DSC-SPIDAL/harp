/* file: DistributedStep2Master.java */
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
 * @ingroup pca_distributed
 * @{
 */
package com.intel.daal.algorithms.pca;

import com.intel.daal.algorithms.AnalysisDistributed;
import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__PCA__DISTRIBUTEDSTEP2MASTER"></a>
 * @brief Runs the PCA algorithm in the the second step of the distributed processing mode
 * <!-- \n<a href="DAAL-REF-PCA-ALGORITHM">PCA algorithm description and usage models</a> -->
 *
 * @par References
 *      - ComputeStep class
 *      - InputId class
 *      - PartialCorrelationResultID class
 *      - PartialSVDTableResultID class
 *      - ResultId class
 *      - Input class
 *      - PartialCorrelationResult class
 *      - PartialSVDResult class
 */
public class DistributedStep2Master extends AnalysisDistributed {
    public DistributedStep2MasterInput     input;        /*!< %Input data */
    public Method                               method;  /*!< Computation method for the algorithm */
    private PartialResult                  partialResult; /*!< Partial result of the algorithm */
    private Precision                 prec; /*!< Precision of intermediate computations */
    public DistributedStep2MasterParameter parameter;    /*!< %Parameter of the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the PCA algorithm in the second step of the distributed processing mode
     * by copying input objects and parameters of another PCA algorithm
     * @param context   Context to manage the PCA algorithm
     * @param other     An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    public DistributedStep2Master(DaalContext context, DistributedStep2Master other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), this.method.getValue());

        partialResult = null;
        parameter = new DistributedStep2MasterParameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()),
            cObject, prec, method);
        input = new DistributedStep2MasterInput(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()), method);
    }

    /**
     * Constructs the PCA algorithm in the the second step of the distributed processing mode
     * @param context   Context to manage the PCA algorithm
     * @param cls       Data type to use in intermediate computations for the PCA algorithm,
     *                  Double.class or Float.class
     * @param method    PCA computation method, @ref Method
     */
    public DistributedStep2Master(DaalContext context, Class<? extends Number> cls, Method method) {
        super(context);

        this.method = method;
        if (method != Method.correlationDense && method != Method.svdDense) {
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

        this.cObject = cInit(prec.getValue(), this.method.getValue());

        partialResult = null;
        parameter = new DistributedStep2MasterParameter(getContext(), cInitParameter(this.cObject, prec.getValue(), method.getValue()),
            cObject, prec, method);
        input = new DistributedStep2MasterInput(getContext(), cGetInput(cObject, prec.getValue(), method.getValue()), method);
    }

    /**
     * Runs the PCA algorithm in the the second step of the distributed processing mode
     * @return  Partial results of the PCA algorithm in the the second step of the distributed processing mode
     */
    @Override
    public PartialResult compute() {
        super.compute();
        if (method == Method.correlationDense) {
            partialResult = new PartialCorrelationResult(getContext(), cGetPartialResult(cObject, prec.getValue(), method.getValue()));
        }
        else if (method == Method.svdDense) {
            partialResult = new PartialSVDResult(getContext(), cGetPartialResult(cObject, prec.getValue(), method.getValue()));
        }
        else {
            partialResult = null;
        }

        return partialResult;
    }

    /**
     * Computes the results of the PCA algorithm in the the second step of the distributed processing mode
     * @return  Results of the PCA algorithm in the the second step of the distributed processing mode
     */
    @Override
    public Result finalizeCompute() {
        super.finalizeCompute();
        Result result = new Result(getContext(), cGetResult(cObject, prec.getValue(), method.getValue()));
        return result;
    }

    /**
     * Registers user-allocated memory to store partial results of the PCA algorithm
     * in the the second step of the distributed processing mode
     * @param partialResult         Structure for storing partial results of the PCA algorithm
     * @param initializationFlag    Flag that specifies whether partial results are initialized
     */
    public void setPartialResult(PartialResult partialResult, boolean initializationFlag) {
        this.partialResult = partialResult;
        cSetPartialResult(cObject, prec.getValue(), method.getValue(), partialResult.getCObject(),
                          initializationFlag);
    }

    /**
     * Registers user-allocated memory to store partial results of the PCA algorithm
     * in the the second step of the distributed processing mode
     * @param partialResult         Structure to store partial results of the PCA algorithm
     */
    public void setPartialResult(PartialResult partialResult) {
        setPartialResult(partialResult, false);
    }

    /**
     * Registers user-allocated memory to store the results of the PCA algorithm
     * in the the second step of the distributed processing mode
     * @param result    Structure for storing the results of the PCA algorithm
     */
    public void setResult(Result result) {
        cSetResult(cObject, prec.getValue(), method.getValue(), result.getCObject());
    }

    /**
     * Returns the newly allocated PCA algorithm in the second step of the distributed processing mode
     * with a copy of input objects and parameters of this PCA algorithm
     * @param context   Context to manage the PCA algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public DistributedStep2Master clone(DaalContext context) {
        return new DistributedStep2Master(context, this);
    }

    private native long cInit(int prec, int method);
    private native long cInitParameter(long algAddr, int prec, int method);
    private native long cGetInput(long algAddr, int prec, int method);
    private native long cGetResult(long algAddr, int prec, int method);
    private native long cGetPartialResult(long algAddr, int prec, int method);
    private native void cSetResult(long cObject, int prec, int method, long cResult);
    private native void cSetPartialResult(long cObject, int prec, int method, long cPartialResult, boolean initializationFlag);
    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
