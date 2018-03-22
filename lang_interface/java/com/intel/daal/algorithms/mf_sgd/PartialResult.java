/* file: PartialResult.java */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.intel.daal.algorithms.mf_sgd;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__RESULT"></a>
 * @brief Provides methods to access final results obtained with the compute() method of mf_sgd algorithm
 *        in the batch processing mode or finalizeCompute() method in the online processing mode  for the algorithm on the second or third
 *        steps in the distributed processing mode
 */
public class PartialResult extends com.intel.daal.algorithms.PartialResult {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public PartialResult(DaalContext context) {
        super(context);
        cObject = cNewPartialResult();
    }

    public PartialResult(DaalContext context, long cResult) {
        super(context);
        this.cObject = cResult;
    }

    /**
     * Returns the result of the mf_sgd algorithm
     * @param id    Identifier of the result
     * @return      Result that corresponds to the given identifier
     */
    public NumericTable get(PartialResultId id) {
        if (id != PartialResultId.presWMat 
            && id != PartialResultId.presHMat 
            && id != PartialResultId.presRMSE) {
            throw new IllegalArgumentException("id unsupported");
        }
        return new HomogenNumericTable(getContext(), cGetPartialResultTable(cObject, id.getValue()));
    }

    /**
     * Sets the result of the mf_sgd algorithm
     * @param id    Identifier of the result
     * @param value NumericTable to store result
     */
    public void set(PartialResultId id, NumericTable value) {
        if (id != PartialResultId.presWMat 
            && id != PartialResultId.presHMat
            && id != PartialResultId.presRMSE) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetPartialResultTable(cObject, id.getValue(), value.getCObject());
    }

    private native long cNewPartialResult();

    private native long cGetPartialResultTable(long cResult, int id);

    private native void cSetPartialResultTable(long cResult, int id, long cNumericTable);
}
