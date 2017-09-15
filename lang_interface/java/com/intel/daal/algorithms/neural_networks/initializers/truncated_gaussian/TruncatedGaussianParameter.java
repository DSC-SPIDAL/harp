/* file: TruncatedGaussianParameter.java */
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
 * @ingroup initializers_truncated_gaussian
 * @{
 */
package com.intel.daal.algorithms.neural_networks.initializers.truncated_gaussian;

import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.Precision;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__INITIALIZERS__TRUNCATED_GAUSSIAN__TRUNCATEDGAUSSIANPARAMETER"></a>
 * @brief Class that specifies parameters of the neural network weights and biases truncated gaussian initializer
 */
public class TruncatedGaussianParameter extends com.intel.daal.algorithms.neural_networks.initializers.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    private Precision _prec;

    /** @private */
    public TruncatedGaussianParameter(DaalContext context, long cObject, Precision prec) {
        super(context, cObject);
        _prec = prec;
    }
    /**
     * Returns the distribution mean
     * @return  The distribution mean
     */
    public double getMean() {
        return cGetMean(cObject, _prec.getValue());
    }

    /**
     * Sets the distribution mean
     * @param mean   The distribution mean
     */
    public void setMean(double mean) {
        cSetMean(cObject, mean, _prec.getValue());
    }

    /**
     * Returns the standard deviation of the distribution
     * @return  The standard deviation of the distribution
     */
    public double getSigma() {
        return cGetSigma(cObject, _prec.getValue());
    }

    /**
     * Sets the standard deviation of the distribution
     * @param sigma  The standard deviation of the distribution
     */
    public void setSigma(double sigma) {
        cSetSigma(cObject, sigma, _prec.getValue());
    }

    /**
     * Returns the left bound a of the truncation range from which the random values are selected
     * @return  Left bound of the truncation range
     */
    public double getA() {
        return cGetA(cObject, _prec.getValue());
    }

    /**
     * Sets the left bound a of the truncation range from which the random values are selected
     * @param a Left bound of the truncation range
     */
    public void setA(double a) {
        cSetA(cObject, a, _prec.getValue());
    }

    /**
     * Returns the right bound b of the truncation range from which the random values are selected
     * @return  Right bound of the truncation range
     */
    public double getB() {
        return cGetB(cObject, _prec.getValue());
    }

    /**
     * Sets the right bound b of the truncation range from which the random values are selected
     * @param b Right bound of the truncation range
     */
    public void setB(double b) {
        cSetB(cObject, b, _prec.getValue());
    }

    private native void cSetMean(long cObject, double mean, int prec);
    private native void cSetSigma(long cObject, double sigma, int prec);
    private native void cSetA(long cObject, double a, int prec);
    private native void cSetB(long cObject, double b, int prec);
    private native double cGetMean(long cObject, int prec);
    private native double cGetSigma(long cObject, int prec);
    private native double cGetA(long cObject, int prec);
    private native double cGetB(long cObject, int prec);
}
/** @} */
