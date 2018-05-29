/* file: ConcatLayerDenseBatch.java */
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
 //     Java example of concat layer in the batch processing mode
 ////////////////////////////////////////////////////////////////////////////////
 */

package com.intel.daal.examples.neural_networks;

import com.intel.daal.algorithms.neural_networks.layers.concat.*;
import com.intel.daal.algorithms.neural_networks.layers.ForwardResultId;
import com.intel.daal.algorithms.neural_networks.layers.ForwardResultLayerDataId;
import com.intel.daal.algorithms.neural_networks.layers.ForwardInputLayerDataId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardResultLayerDataId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardInputId;
import com.intel.daal.algorithms.neural_networks.layers.BackwardInputLayerDataId;
import com.intel.daal.data_management.data.Tensor;
import com.intel.daal.data_management.data.HomogenTensor;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-EXAMPLE-JAVA-CONCATLAYERBATCH">
 * @example ConcatLayerDenseBatch.java
 */
class ConcatLayerDenseBatch {
    private static final String datasetFileName = "../data/batch/layer.csv";
    private static DaalContext context = new DaalContext();
    private static final long concatDimension = 1;

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Read datasetFileName from a file and create a tensor to store forward input data */
        Tensor data = Service.readTensorFromCSV(context, datasetFileName);
        KeyValueDataCollection dataCollection = new KeyValueDataCollection(context);

        for (int i = 0; i < 3; i++) {
            dataCollection.set(i, data);
        }

        /* Create an algorithm to compute forward concat layer results using default method */
        ConcatForwardBatch forwardLayer = new ConcatForwardBatch(context, Float.class, ConcatMethod.defaultDense);
        forwardLayer.parameter.setConcatDimension(concatDimension);

        /* Set input objects for the forward concat layer */
        forwardLayer.input.set(ForwardInputLayerDataId.inputLayerData, dataCollection);

        /* Compute forward concat layer results */
        ConcatForwardResult forwardResult = forwardLayer.compute();

        /* Print the results of the forward concat layer */
        Service.printTensor("Forward concatenation layer result value (first 5 rows):", forwardResult.get(ForwardResultId.value), 5, 0);

        /* Create an algorithm to compute backward concat layer results using default method */
        ConcatBackwardBatch backwardLayer = new ConcatBackwardBatch(context, Float.class, ConcatMethod.defaultDense);
        backwardLayer.parameter.setConcatDimension(concatDimension);

        /* Set input objects for the backward concat layer */
        backwardLayer.input.set(BackwardInputId.inputGradient, forwardResult.get(ForwardResultId.value));
        backwardLayer.input.set(BackwardInputLayerDataId.inputFromForward, forwardResult.get(ForwardResultLayerDataId.resultForBackward));

        Service.printNumericTable("auxInputDimensions ", forwardResult.get(ConcatLayerDataId.auxInputDimensions), 5, 0);

        /* Compute backward concat layer results */
        ConcatBackwardResult backwardResult = backwardLayer.compute();

        /* Print the results of the backward concat layer */
        for (int i = 0; i < dataCollection.size(); i++) {
            Service.printTensor("Backward concatenation layer backward result (first 5 rows):",
                                backwardResult.get(BackwardResultLayerDataId.resultLayerData, i), 5, 0);
        }
        context.dispose();
    }
}
