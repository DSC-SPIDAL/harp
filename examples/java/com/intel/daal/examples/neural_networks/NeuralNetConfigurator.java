/* file: NeuralNetConfigurator.java */
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
 //     Java example of neural network configurator
 ////////////////////////////////////////////////////////////////////////////////
 */

package com.intel.daal.examples.neural_networks;

import com.intel.daal.algorithms.neural_networks.*;
import com.intel.daal.algorithms.neural_networks.initializers.uniform.*;
import com.intel.daal.algorithms.neural_networks.training.TrainingTopology;
import com.intel.daal.algorithms.neural_networks.layers.fullyconnected.*;
import com.intel.daal.algorithms.neural_networks.layers.softmax_cross.*;
import com.intel.daal.algorithms.neural_networks.layers.LayerDescriptor;
import com.intel.daal.algorithms.neural_networks.layers.NextLayers;
import com.intel.daal.algorithms.neural_networks.layers.ForwardLayer;
import com.intel.daal.algorithms.neural_networks.layers.BackwardLayer;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-EXAMPLE-JAVA-NEURALNETWORKCONFIGURATOR">
 * @example NeuralNetConfigurator.java
 */
class NeuralNetConfigurator {
    public static TrainingTopology configureNet(DaalContext context) {
        /* Create layers of the neural network */
        /* Create fully-connected layer and initialize layer parameters */
        FullyConnectedBatch fullyconnectedLayer1 = new FullyConnectedBatch(context, Float.class, FullyConnectedMethod.defaultDense, 5);

        fullyconnectedLayer1.parameter.setWeightsInitializer(new UniformBatch(context, Float.class, UniformMethod.defaultDense, -0.001, 0.001));

        fullyconnectedLayer1.parameter.setBiasesInitializer(new UniformBatch(context, Float.class, UniformMethod.defaultDense, 0, 0.5));

        /* Create fully-connected layer and initialize layer parameters */
        FullyConnectedBatch fullyconnectedLayer2 = new FullyConnectedBatch(context, Float.class, FullyConnectedMethod.defaultDense, 2);

        fullyconnectedLayer2.parameter.setWeightsInitializer(new UniformBatch(context, Float.class, UniformMethod.defaultDense, 0.5, 1));

        fullyconnectedLayer2.parameter.setBiasesInitializer(new UniformBatch(context, Float.class, UniformMethod.defaultDense, 0.5, 1));

        /* Create softmax cross-entropy loss layer and initialize layer parameters */
        SoftmaxCrossBatch softmaxCrossEntropyLayer = new SoftmaxCrossBatch(context, Float.class, SoftmaxCrossMethod.defaultDense);

        /* Create topology of the neural network */
        TrainingTopology topology = new TrainingTopology(context);

        /* Add layers to the topology of the neural network */
        long fc1 = topology.add(fullyconnectedLayer1);
        long fc2 = topology.add(fullyconnectedLayer2);
        long sm = topology.add(softmaxCrossEntropyLayer);
        topology.addNext(fc1, fc2);
        topology.addNext(fc2, sm);
        return topology;
    }
}
