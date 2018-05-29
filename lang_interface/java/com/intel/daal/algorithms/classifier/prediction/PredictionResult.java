/* file: PredictionResult.java */
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
 * @ingroup prediction
 * @{
 */
package com.intel.daal.algorithms.classifier.prediction;

import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__CLASSIFIER__PREDICTION__PREDICTIONRESULT"></a>
 * @brief Provides methods to access final results obtained with the compute() method of the classifier model-based prediction algorithm in the batch processing mode
 */

public final class PredictionResult extends com.intel.daal.algorithms.Result {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the result of the classifier model-based prediction algorithm
     * @param context   Context to manage the result of the classifier model-based prediction algorithm
     */
    public PredictionResult(DaalContext context) {
        super(context);
        cObject = cNewResult();
    }

    public PredictionResult(DaalContext context, long cAlgorithm) {
        super(context);
        cObject = cGetResult(cAlgorithm);
    }

    /**
     * Returns the final result of the classification algorithm
     * @param id   Identifier of the result, @ref PredictionResultId
     * @return     Result that corresponds to the given identifier
     */
    public NumericTable get(PredictionResultId id) {
        if (id == PredictionResultId.prediction) {
            return (NumericTable)Factory.instance().createObject(getContext(),
                    cGetResultTable(cObject, PredictionResultId.prediction.getValue()));
        } else {
            return null;
        }
    }

    /**
     * Sets the final result of the algorithm
     * @param id    Identifier of the final result
     * @param value Object for storing the final result
     */
    public void set(PredictionResultId id, NumericTable value) {
        if (id != PredictionResultId.prediction) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetResultTable(cObject, id.getValue(), value.getCObject());
    }

    private native long cNewResult();

    private native long cGetResult(long algAddress);

    private native long cGetResultTable(long resAddr, int id);

    private native void cSetResultTable(long cResult, int id, long cNumericTable);
}
/** @} */
