/* file: RatingsResult.java */
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
 * @ingroup implicit_als_prediction_batch
 * @{
 */
package com.intel.daal.algorithms.implicit_als.prediction.ratings;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__IMPLICIT_ALS__PREDICTION__RATINGS__RATINGSRESULT"></a>
 * @brief Provides methods to access the results obtained with the compute() method
 *        of the implicit ALS ratings prediction algorithm in the batch processing mode
 */
public final class RatingsResult extends com.intel.daal.algorithms.Result {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the result of the implicit ALS ratings prediction algorithm and attaches it to the context
     * @param context Context to manage the memory in the native part of the result object
     */
    public RatingsResult(DaalContext context) {
        super(context);
        this.cObject = cNewResult();
    }

    /** @private */
    public RatingsResult(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Returns the result of the implicit ALS ratings prediction algorithm
     * @param id    Identifier of the result, @ref RatingsResultId
     * @return      Result that corresponds to the given identifier
     */
    public NumericTable get(RatingsResultId id) {
        if (id != RatingsResultId.prediction) {
            throw new IllegalArgumentException("RatingsResultId unsupported");
        }

        return (NumericTable)Factory.instance().createObject(getContext(), cGetNumericTable(getCObject(), id.getValue()));
    }

    /**
     * Sets the result of the implicit ALS ratings prediction algorithm
     * @param id    Identifier of the result, @ref RatingsResultId
     * @param value Prediction result
     */
    public void set(RatingsResultId id, NumericTable value) {
        if (id != RatingsResultId.prediction) {
            throw new IllegalArgumentException("RatingsResultId unsupported");
        }
        cSetNumericTable(getCObject(), id.getValue(), value.getCObject());
    }

    private native long cNewResult();

    private native long cGetNumericTable(long resAddr, int id);
    private native void cSetNumericTable(long resAddr, int id, long cNumericTable);
}
/** @} */
