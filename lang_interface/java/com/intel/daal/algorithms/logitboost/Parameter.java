/* file: Parameter.java */
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
 * @ingroup logitboost
 */
/**
 * @brief Contains classes of the LogitBoost classification algorithm
 */
package com.intel.daal.algorithms.logitboost;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOGITBOOST__PARAMETER"></a>
 * @brief Base class for parameters of the LogitBoost training algorithm
 */
public class Parameter extends com.intel.daal.algorithms.boosting.Parameter {

    public Parameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     * Sets the accuracy of the LogitBoost training algorithm
     * @param accuracyThreshold Accuracy of the LogitBoost training algorithm
     */
    public void setAccuracyThreshold(double accuracyThreshold) {
        cSetAccuracyThreshold(this.cObject, accuracyThreshold);
    }

    /**
     * Retrieves the accuracy of the LogitBoost training algorithm
     * @return Accuracy of the LogitBoost training algorithm
     */
    public double getAccuracyThreshold() {
        return cGetAccuracyThreshold(this.cObject);
    }

    /**
     * Sets the threshold to avoid degenerate cases when calculating weights W
     * @param weightsDegenerateCasesThreshold The threshold
     */
    public void setWeightsDegenerateCasesThreshold(double weightsDegenerateCasesThreshold) {
        cSetWeightsThreshold(this.cObject, weightsDegenerateCasesThreshold);
    }

    /**
     * Retrieves the threshold needed to avoid degenerate cases when calculating weights W
     * @return The threshold
     */
    public double getWeightsDegenerateCasesThreshold() {
        return cGetWeightsThreshold(this.cObject);
    }

    /**
     * Sets the threshold to avoid degenerate cases when calculating responses Z
     * @param responsesDegenerateCasesThreshold The threshold     */
    public void setResponsesDegenerateCasesThreshold(double responsesDegenerateCasesThreshold) {
        cSetResponsesThreshold(this.cObject, responsesDegenerateCasesThreshold);
    }

    /**
     * Retrieves the threshold needed to avoid degenerate cases when calculating responses Z
     * @return The threshold
     */
    public double getResponsesDegenerateCasesThreshold() {
        return cGetResponsesThreshold(this.cObject);
    }

    /**
     * Sets the maximal number of iterations of the LogitBoost training algorithm
     * @param maxIterations Maximal number of iterations
     */
    public void setMaxIterations(long maxIterations) {
        cSetMaxIterations(this.cObject, maxIterations);
    }

    /**
     * Retrieves the maximal number of iterations of the LogitBoost training algorithm
     * @return Maximal number of iterations
     */
    public long getMaxIterations() {
        return cGetMaxIterations(this.cObject);
    }

    private native void cSetAccuracyThreshold(long parAddr, double acc);

    private native double cGetAccuracyThreshold(long parAddr);

    private native void cSetWeightsThreshold(long parAddr, double acc);

    private native double cGetWeightsThreshold(long parAddr);

    private native void cSetResponsesThreshold(long parAddr, double acc);

    private native double cGetResponsesThreshold(long parAddr);

    private native void cSetMaxIterations(long parAddr, long nIter);

    private native long cGetMaxIterations(long parAddr);
}
/** @} */
