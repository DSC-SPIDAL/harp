/* file: ResultCollection.java */
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
 * @ingroup linear_regression_quality_metric_set
 * @{
 */
package com.intel.daal.algorithms.linear_regression.quality_metric_set;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.quality_metric.QualityMetricResult;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.linear_regression.quality_metric.SingleBetaResult;
import com.intel.daal.algorithms.linear_regression.quality_metric.GroupOfBetasResult;

/**
 *  <a name="DAAL-CLASS-ALGORITHMS__LINEAR_REGRESSION__QUALITY_METRIC_SET__RESULTCOLLECTION"></a>
 *  @brief Class that implements functionality of the collection of result objects of the quality metrics algorithm
 */
public class ResultCollection extends com.intel.daal.algorithms.quality_metric_set.ResultCollection {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public ResultCollection(DaalContext context, long cAlgorithm, ComputeMode cmode) {
        super(context, cAlgorithm, cmode);
    }

    /**
     * Returns the element that matches the identifier
     * @param  id     Identifier of the quality metric
     * @return Result object
     */
    public QualityMetricResult getResult(QualityMetricId id) {
        if (id == QualityMetricId.singleBeta)
            return new SingleBetaResult(getContext(), cGetResult(getCObject(), id.getValue()));
        if (id == QualityMetricId.groupOfBetas)
            return new GroupOfBetasResult(getContext(), cGetResult(getCObject(), id.getValue()));
        throw new IllegalArgumentException("id unsupported");
    }
}
/** @} */
