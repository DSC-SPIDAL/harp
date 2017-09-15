/* file: SoftmaxCrossParameter.java */
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
 * @ingroup softmax_cross
 * @{
 */
package com.intel.daal.algorithms.neural_networks.layers.softmax_cross;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__SOFTMAX_CROSS__SOFTMAXCROSSPARAMETER"></a>
 * \brief Class that specifies parameters of the softmax cross-entropy layer
 */
public class SoftmaxCrossParameter extends com.intel.daal.algorithms.neural_networks.layers.loss.LossParameter {

    /**
     *  Constructs the parameters for the softmax cross-entropy layer
     */
    public SoftmaxCrossParameter(DaalContext context) {
        super(context);
        cObject = cInit();
    }

    public SoftmaxCrossParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     *  Gets the value needed to avoid degenerate cases in logarithm computing
     */
    public double getAccuracyThreshold() {
        return cGetAccuracyThreshold(cObject);
    }

    /**
     *  Sets the value needed to avoid degenerate cases in logarithm computing
     *  @param accuracyThreshold Value needed to avoid degenerate cases in logarithm computing
     */
    public void setAccuracyThreshold(double accuracyThreshold) {
        cSetAccuracyThreshold(cObject, accuracyThreshold);
    }

    /**
     *  Gets the dimension index used to calculate softmax cross-entropy
     */
    public double getDimension() {
        return cGetDimension(cObject);
    }

    /**
     *  Sets the dimension index used to calculate softmax cross-entropy
     *  @param dimension Dimension index used to calculate softmax cross-entropy
     */
    public void setDimension(double dimension) {
        cSetDimension(cObject, dimension);
    }

    private native long   cInit();
    private native double cGetAccuracyThreshold(long cParameter);
    private native void   cSetAccuracyThreshold(long cParameter, double accuracyThreshold);
    private native double cGetDimension(long cParameter);
    private native void   cSetDimension(long cParameter, double dimension);
}
/** @} */
