/* file: QualityMetricSetBatch.java */
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
 * @defgroup logitboost_quality_metric_set_batch Batch
 * @ingroup logitboost_quality_metric_set
 * @{
 */
/**
 * @brief Contains classes to check the quality of the model trained with the LogitBoost algorithm
 */
package com.intel.daal.algorithms.logitboost.quality_metric_set;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOGITBOOST__QUALITY_METRIC_SET__QUALITYMETRICSETBATCH"></a>
 * @brief Class that represents a quality metric set to check the model trained with the LogitBoost algorithm
 *
 * @par Enumerations
 *      - @ref QualityMetricId  Identifiers of quality metrics provided by the library
 */

public class QualityMetricSetBatch extends com.intel.daal.algorithms.quality_metric_set.QualityMetricSetBatch {
    public QualityMetricSetParameter parameter;
    private InputDataCollection      inputData;

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public QualityMetricSetBatch(DaalContext context, long nClasses) {
        super(context);
        this.cObject = cInit(nClasses);
        inputData = new InputDataCollection(getContext(), cObject, ComputeMode.batch);
        parameter = new QualityMetricSetParameter(getContext(), cInitParameter(cObject), nClasses);
    }

    /**
     * Returns the collection of input objects of quality metrics algorithms
     * @return Collection of input objects of quality metrics algorithms
     */
    public InputDataCollection getInputDataCollection() {
        return inputData;
    }

    /**
     * Computes the results for the quality metric set in the batch processing mode
     * @return Structure that contains a computed quality metric set
     */
    @Override
    public ResultCollection compute() {
        super.compute();
        return new ResultCollection(getContext(), cObject, ComputeMode.batch);
    }

    private native long cInit(long nClasses);

    private native long cInitParameter(long algAddr);
}
/** @} */
