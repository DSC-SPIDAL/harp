/* file: BinaryConfusionMatrixResult.java */
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
 * @ingroup quality_metric_binary
 * @{
 */
package com.intel.daal.algorithms.classifier.quality_metric.binary_confusion_matrix;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__CLASSIFIER__QUALITY_METRIC__BINARY_CONFUSION_MATRIX__BINARYCONFUSIONMATRIXRESULT"></a>
 * @brief  Class for the results of the binary confusion matrix algorithm
 */
public class BinaryConfusionMatrixResult extends com.intel.daal.algorithms.quality_metric.QualityMetricResult {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public BinaryConfusionMatrixResult(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Constructs the result of the classification algorithms
     * @param context   Context to manage the result of the classification algorithms
     */
    public BinaryConfusionMatrixResult(DaalContext context) {
        super(context);
        this.cObject = cNewResult();
    }

    /**
     * Sets the result of the binary confusion matrix algorithm
     * @param id    Identifier of the result
     * @param val   Value that corresponds to the given identifier
     */
    public void set(BinaryConfusionMatrixResultId id, NumericTable val) {
        if (id != BinaryConfusionMatrixResultId.confusionMatrix || id != BinaryConfusionMatrixResultId.binaryMetrics) {
            throw new IllegalArgumentException("id unsupported");
        }

        cSetResultTable(cObject, id.getValue(), val.getCObject());
    }

    /**
     * Returns the quality metric of the classification algorithm
     * @param id Identifier of the result
     * @return   Result that corresponds to the given identifier
     */
    public NumericTable get(BinaryConfusionMatrixResultId id) {
        if (id == BinaryConfusionMatrixResultId.confusionMatrix || id == BinaryConfusionMatrixResultId.binaryMetrics) {
            return (NumericTable)Factory.instance().createObject(getContext(), cGetResultTable(cObject, id.getValue()));
        } else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    private native void cSetResultTable(long inputAddr, int id, long ntAddr);

    private native long cGetResultTable(long cResult, int id);

    private native long cNewResult();

}
/** @} */
