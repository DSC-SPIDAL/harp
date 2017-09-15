/* file: FullyconLayerDenseBatch.java */
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

/*
 //  Content:
 //     Java example of fully-connected layer in the batch processing mode
 ////////////////////////////////////////////////////////////////////////////////
 */

package com.intel.daal.examples.neural_networks;

import com.intel.daal.algorithms.neural_networks.layers.fullyconnected.*;
import com.intel.daal.algorithms.neural_networks.layers.ForwardResultId;
import com.intel.daal.algorithms.neural_networks.layers.ForwardResultLayerDataId;
import com.intel.daal.algorithms.neural_networks.layers.ForwardInputId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardResultId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardInputId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardInputLayerDataId;
import com.intel.daal.data_management.data.Tensor;
import com.intel.daal.data_management.data.HomogenTensor;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-EXAMPLE-JAVA-FULLYCONNECTEDLAYERBATCH">
 * @example FullyconLayerDenseBatch.java
 */
class FullyconLayerDenseBatch {
    private static final String datasetFileName = "../data/batch/layer.csv";
    private static DaalContext context = new DaalContext();
    private static final long m = 5;

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Read datasetFileName from a file and create a tensor to store forward input data */
        Tensor data = Service.readTensorFromCSV(context, datasetFileName);

        /* Create an algorithm to compute forward fully-connected layer results using default method */
        FullyConnectedForwardBatch forwardLayer = new FullyConnectedForwardBatch(context, Float.class, FullyConnectedMethod.defaultDense, m);

        /* Set input objects for the forward fully-connected layer */
        forwardLayer.input.set(ForwardInputId.data, data);

        /* Compute forward fully-connected layer results */
        FullyConnectedForwardResult forwardResult = forwardLayer.compute();

        /* Print the results of the forward fully-connected layer */
        Service.printTensor("Forward fully-connected layer result (first 5 rows):", forwardResult.get(ForwardResultId.value), 5, 0);

        /* Create input gradient tensor for backward fully-connected layer */
        long[] dims = forwardResult.get(ForwardResultId.value).getDimensions();
        double[] inputGradientData = new double[(int)forwardResult.get(ForwardResultId.value).getSize()];
        Tensor inputGradient = new HomogenTensor(context, dims, inputGradientData, 0.01);

        /* Create an algorithm to compute backward fully-connected layer results using default method */
        FullyConnectedBackwardBatch backwardLayer = new FullyConnectedBackwardBatch(context, Float.class, FullyConnectedMethod.defaultDense, m);

        /* Set input objects for the backward fully-connected layer */
        backwardLayer.input.set(BackwardInputId.inputGradient, inputGradient);
        backwardLayer.input.set(BackwardInputLayerDataId.inputFromForward, forwardResult.get(ForwardResultLayerDataId.resultForBackward));

        /* Compute backward fully-connected layer results */
        FullyConnectedBackwardResult backwardResult = backwardLayer.compute();

        /* Print the results of the backward fully-connected layer */
        Service.printTensor("Backward fully-connected gradient result (first 5 rows):",
                            backwardResult.get(BackwardResultId.gradient), 5, 0);
        Service.printTensor("Backward fully-connected weightDerivatives result (first 5 rows):",
                            backwardResult.get(BackwardResultId.weightDerivatives), 5, 0);
        Service.printTensor("Backward fully-connected biasDerivatives result (first 5 rows):",
                            backwardResult.get(BackwardResultId.biasDerivatives), 5, 0);

        context.dispose();
    }
}
