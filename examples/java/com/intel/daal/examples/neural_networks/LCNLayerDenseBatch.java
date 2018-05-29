/* file: LCNLayerDenseBatch.java */
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

/*
 //  Content:
 //     Java example of local contrast normalization layer in the batch processing mode
 ////////////////////////////////////////////////////////////////////////////////
 */

package com.intel.daal.examples.neural_networks;

import com.intel.daal.algorithms.neural_networks.layers.lcn.*;
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
 * <a name="DAAL-EXAMPLE-JAVA-LCNLAYERBATCH">
 * @example LCNLayerDenseBatch.java
 */
class LCNLayerDenseBatch {
    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Create collection of dimension sizes of the input data tensor */
        long[] dimensionSizes = new long[4];
        dimensionSizes[0] = 2;
        dimensionSizes[1] = 1;
        dimensionSizes[2] = 3;
        dimensionSizes[3] = 4;

        /* Create input daat tensor */
        float[] data = new float[24];
        Tensor dataTensor = new HomogenTensor(context, dimensionSizes, data, 1.0f);

        /* Create an algorithm to compute forward local contrast normalization layer results using default method */
        LcnForwardBatch lcnLayerForward = new LcnForwardBatch(context, Float.class, LcnMethod.defaultDense);

        /* Set input objects for the forward local contrast normalization layer */
        lcnLayerForward.input.set(ForwardInputId.data, dataTensor);

        /* Compute forward local contrast normalization layer results */
        LcnForwardResult forwardResult = lcnLayerForward.compute();

        /* Print the results of the forward local contrast normalization layer */
        Service.printTensor("Forward local contrast normalization layer result:", forwardResult.get(ForwardResultId.value),5,12);
        Service.printTensor("Centered data tensor:", forwardResult.get(LcnLayerDataId.auxCenteredData),5,12);
        Service.printTensor("Sigma tensor:", forwardResult.get(LcnLayerDataId.auxSigma),5,12);
        Service.printTensor("C tensor:", forwardResult.get(LcnLayerDataId.auxC),5,1);
        Service.printTensor("Inverted max(sigma, C):", forwardResult.get(LcnLayerDataId.auxInvMax),5,12);

        /* Create input gradient tensor for backward local contrast normalization layer */
        float[] backData = new float[24];
        Tensor tensorDataBack = new HomogenTensor(context, dimensionSizes, backData, 0.01f);

        /* Create an algorithm to compute backward local contrast normalization layer results using default method */
        LcnBackwardBatch lcnLayerBackward = new LcnBackwardBatch(context, Float.class, LcnMethod.defaultDense);

        /* Set input objects for the backward local contrast normalization layer */
        lcnLayerBackward.input.set(BackwardInputId.inputGradient, tensorDataBack);
        lcnLayerBackward.input.set(BackwardInputLayerDataId.inputFromForward,
                                             forwardResult.get(ForwardResultLayerDataId.resultForBackward));

        /* Compute backward local contrast normalization layer results */
        LcnBackwardResult backwardResult = lcnLayerBackward.compute();

        /* Get the computed backward local contrast normalization layer results */
        Service.printTensor("Local contrast normalization layer backpropagation gradient result:", backwardResult.get(BackwardResultId.gradient),5,12);

        context.dispose();
    }
}
