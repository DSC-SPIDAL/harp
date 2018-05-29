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
 * @ingroup layers
 * @{
 */
/**
 * @brief Contains classes for the neural network layers
 */
package com.intel.daal.algorithms.neural_networks.layers;

import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.neural_networks.initializers.InitializerIface;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__PARAMETER"></a>
 * @brief Class that specifies parameters of the neural network layer
 */
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the parameter of the neural network layer
     * @param context   Context to manage the parameter of the neural network layer
     */
    public Parameter(DaalContext context) {
        super(context);
    }

    public Parameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     * Sets the layer weights initializer
     * @param weightsInitializer Layer weights initializer
     */
    public void setWeightsInitializer(InitializerIface weightsInitializer) {
        cSetWeightsInitializer(cObject, weightsInitializer.cObject);
    }

    /**
     * Sets the layer biases initializer
     * @param biasesInitializer Layer biases initializer
     */
    public void setBiasesInitializer(InitializerIface biasesInitializer) {
        cSetBiasesInitializer(cObject, biasesInitializer.cObject);
    }

    /**
     *  Gets the flag that specifies whether the weights and biases are initialized or not.
     */
    public boolean getWeightsAndBiasesInitializationFlag() {
        return cGetWeightsAndBiasesInitializationFlag(cObject);
    }

    /**
     *  Sets the flag that specifies whether the weights and biases are initialized or not.
     *  @param weightsAndBiasesInitialized Flag that specifies whether the weights and biases are initialized or not.
     */
    public void setWeightsAndBiasesInitializationFlag(boolean weightsAndBiasesInitialized) {
       cSetWeightsAndBiasesInitializationFlag(cObject, weightsAndBiasesInitialized);
    }

    /**
     *  Gets the flag specifying whether the layer is used for the prediction stage or not
     */
    public boolean getPredictionStage() {
        return cGetPredictionStage(cObject);
    }

    /**
     *  Sets the flag specifying whether the layer is used for the prediction stage or not
     *  @param predictionStage Flag specifying whether the layer is used for the prediction stage or not
     */
    public void setPredictionStage(boolean predictionStage) {
       cSetPredictionStage(cObject, predictionStage);
    }


    private native void cSetWeightsInitializer(long cObject, long cInitializer);
    private native void cSetBiasesInitializer(long cObject, long cInitializer);
    private native boolean cGetWeightsAndBiasesInitializationFlag(long cObject);
    private native void cSetWeightsAndBiasesInitializationFlag(long cObject, boolean weightsAndBiasesInitialized);
    private native boolean cGetPredictionStage(long cObject);
    private native void cSetPredictionStage(long cObject, boolean predictionStage);

}
/** @} */
