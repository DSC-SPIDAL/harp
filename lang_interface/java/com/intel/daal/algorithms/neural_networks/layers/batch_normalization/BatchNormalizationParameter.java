/* file: BatchNormalizationParameter.java */
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
 * @ingroup batch_normalization
 * @{
 */
package com.intel.daal.algorithms.neural_networks.layers.batch_normalization;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__BATCHNORMALIZATIONBATCH_NORMALIZATION__BATCHNORMALIZATIONPARAMETER"></a>
 * \brief Class that specifies parameters of the batch normalization layer
 */
public class BatchNormalizationParameter extends com.intel.daal.algorithms.neural_networks.layers.Parameter {

    /**
     *  Constructs the parameters for the batch normalization layer
     */
    public BatchNormalizationParameter(DaalContext context) {
        super(context);
        cObject = cInit();
    }

    public BatchNormalizationParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     *  Gets the smoothing factor of the batch normalization layer
     */
    public double getAlpha() {
        return cGetAlpha(cObject);
    }

    /**
     *  Sets the smoothing factor of the batch normalization layer
     *  @param alpha Smoothing factor that is used in population mean and population variance computations
     */
    public void setAlpha(double alpha) {
        cSetAlpha(cObject, alpha);
    }

    /**
     *  Gets the constant added to the mini-batch variance for numerical stability
     */
    public double getEpsilon() {
        return cGetEpsilon(cObject);
    }

    /**
     *  Sets the constant added to the mini-batch variance for numerical stability
     *  @param epsilon A constant added to the mini-batch variance for numerical stability
     */
    public void setEpsilon(double epsilon) {
       cSetEpsilon(cObject, epsilon);
    }

    /**
     *  Gets the index of the dimension for which the normalization is performed
     */
    public long getDimension() {
        return cGetDimension(cObject);
    }

    /**
     *  Sets the index of the dimension for which the normalization is performed
     *  @param dimension BatchNormalizationIndex of the dimension for which the normalization is performed
     */
    public void setDimension(long dimension) {
       cSetDimension(cObject, dimension);
    }

    /**
     *  Gets the flag that specifies whether the layer is used for the prediction stage or not
     */
    public boolean getPredictionStage() {
        return cGetPredictionStage(cObject);
    }

    /**
     *  Sets the flag that specifies whether the layer is used for the prediction stage or not
     *  @param predictionStage Flag that specifies whether the layer is used for the prediction stage or not
     */
    public void setPredictionStage(boolean predictionStage) {
       cSetPredictionStage(cObject, predictionStage);
    }

    private native long    cInit();
    private native double  cGetAlpha(long cParameter);
    private native void    cSetAlpha(long cParameter, double alpha);
    private native double  cGetEpsilon(long cParameter);
    private native void    cSetEpsilon(long cParameter, double epsilon);
    private native long    cGetDimension(long cParameter);
    private native void    cSetDimension(long cParameter, long dimension);
    private native boolean cGetPredictionStage(long cParameter);
    private native void    cSetPredictionStage(long cParameter, boolean predictionStage);
}
/** @} */
