/* file: Parameter.java */
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
 * @ingroup multivariate_outlier_detection_bacondense
 * @{
 */
package com.intel.daal.algorithms.multivariate_outlier_detection.bacondense;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTIVARIATE_OUTLIER_DETECTION__BACONDENSE__PARAMETER"></a>
 * @brief Parameters of the multivariate outlier detection compute() method used with the baconDense method \DAAL_DEPRECATED_USE{com.intel.daal.algorithms.bacon_outlier_detection.Parameter}
 */
@Deprecated
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public Parameter(DaalContext context, long cParameter) {
        super(context);
        this.cObject = cParameter;
    }

    /**
     * Sets initialization method for the BACON multivariate outlier detection algorithm
     * @param method Initialization method
     */
    public void setInitializationMethod(InitializationMethod method) {}

    /**
     * Returns initialization method of the BACON multivariate outlier detection algorithm
     * @return Initialization method
     */
    public InitializationMethod getInitializationMethod() {
        return InitializationMethod.baconMedian;
    }

    /**
     * Sets alpha parameter of the BACON method.
     * alpha is a one-tailed probability that defines the \f$(1 - \alpha)\f$ quantile
     * of the \f$\chi^2\f$ distribution with \f$p\f$ degrees of freedom.
     * Recommended value: \f$\alpha / n\f$, where n is the number of observations.
     * @param alpha Value of the parameter alpha
     */
    public void setAlpha(double alpha) {}

    /**
     * Returns the parameter alpha of the BACON method.
     * @return Parameter alpha of the BACON method.
     */
    public double getAlpha() {return 0.0;}

    /**
     * Sets the threshold for the stopping criterion of the algorithms.
     * Stopping criterion: the algorithm is terminated if the size of the basic subset
     * is changed by less than the threshold.
     * @param threshold     Threshold for the stopping criterion of the algorithm
     */
    public void setToleranceToConverge(double threshold) {}

    /**
     * Sets the threshold for the stopping criterion of the algorithms.
     * @return Threshold for the stopping criterion of the algorithm
     */
    public double getToleranceToConverge() {
        return 0.0;
    }
}
/** @} */
