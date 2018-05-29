/* file: Batch.java */
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
 * @defgroup engines_mcg59_batch Batch
 * @ingroup engines_mcg59
 * @{
 */
/**
 * @brief Contains classes for the mcg59 engine
 */
package com.intel.daal.algorithms.engines.mcg59;

import com.intel.daal.algorithms.engines.Input;
import com.intel.daal.algorithms.engines.Result;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__ENGINES__MCG59__BATCH"></a>
 * \brief Provides methods for mcg59 engine computations in the batch processing mode
 *
 * \par References
 *      - @ref com.intel.daal.algorithms.engines.Input class
 */
public class Batch extends com.intel.daal.algorithms.engines.BatchBase {
    public  Method       method;    /*!< Computation method for the engine */
    private Precision    prec;      /*!< Data type to use in intermediate computations for the engine */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs mcg59 engine by copying input objects and parameters of another mcg59 engine
     * @param context Context to manage the mcg59 engine
     * @param other   A engines to be used as the source to initialize the input objects
     *                and parameters of this engine
     */
    public Batch(DaalContext context, Batch other) {
        super(context);
        this.method = other.method;
        prec = other.prec;

        this.cObject = cClone(other.cObject, prec.getValue(), method.getValue());
    }

    /**
     * Constructs the mcg59 engine
     * @param context    Context to manage the engine
     * @param cls        Data type to use in intermediate computations for the engine, Double.class or Float.class
     * @param method     The engine computation method, @ref Method
     * @param seed       Initial condition
     */
    public Batch(DaalContext context, Class<? extends Number> cls, Method method, int seed) {
        super(context);
        constructBatch(context, cls, method, seed);
    }

    private void constructBatch(DaalContext context, Class<? extends Number> cls, Method method, int seed) {
        this.method = method;

        if (method != Method.defaultDense) {
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

        this.cObject = cInit(prec.getValue(), method.getValue(), seed);
    }

    /**
     * Computes the result of the mcg59 engine
     * @return  Mcg59 engine result
     */
    @Override
    public Result compute() {
        super.compute();
        return new Result(getContext(), cGetResult(cObject, prec.getValue(), method.getValue()));
    }

    /**
     * Returns the newly allocated mcg59 engine
     * with a copy of input objects and parameters of this mcg59 engine
     * @param context    Context to manage the engine
     * @return The newly allocated mcg59 engine
     */
    @Override
    public Batch clone(DaalContext context) {
        return new Batch(context, this);
    }

    private native long cInit(int prec, int method, int seed);
    private native long cGetResult(long cAlgorithm, int prec, int method);
    private native long cClone(long algAddr, int prec, int method);
}
/** @} */
