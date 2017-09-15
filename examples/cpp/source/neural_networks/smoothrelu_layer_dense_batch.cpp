/* file: smoothrelu_layer_dense_batch.cpp */
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
!  Content:
!    C++ example of forward and backward smooth rectified linear unit (smooth relu) layer usage
!
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-SMOOTHRELU_LAYER_BATCH"></a>
 * \example smoothrelu_layer_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::neural_networks::layers;
using namespace daal::data_management;
using namespace daal::services;

/* Input data set parameters */
string datasetName = "../data/batch/layer.csv";

int main()
{
    /* Read datasetFileName from a file and create a tensor to store input data */
    TensorPtr tensorData = readTensorFromCSV(datasetName);

    /* Create an algorithm to compute forward smooth relu layer results using default method */
    smoothrelu::forward::Batch<> smoothreluLayerForward;

    /* Set input objects for the forward smooth relu layer */
    smoothreluLayerForward.input.set(forward::data, tensorData);

    /* Compute forward smooth relu layer results */
    smoothreluLayerForward.compute();

    /* Print the results of the forward smooth relu layer */
    smoothrelu::forward::ResultPtr forwardResult = smoothreluLayerForward.getResult();
    printTensor(forwardResult->get(forward::value), "Forward smooth ReLU layer result (first 5 rows):", 5);

    /* Get the size of forward dropout smooth relu output */
    const Collection<size_t> &gDims = forwardResult->get(forward::value)->getDimensions();
    TensorPtr tensorDataBack = TensorPtr(new HomogenTensor<>(gDims, Tensor::doAllocate, 0.01f));

    /* Create an algorithm to compute backward smooth relu layer results using default method */
    smoothrelu::backward::Batch<> smoothreluLayerBackward;

    /* Set input objects for the backward smooth relu layer */
    smoothreluLayerBackward.input.set(backward::inputGradient, tensorDataBack);
    smoothreluLayerBackward.input.set(backward::inputFromForward, forwardResult->get(forward::resultForBackward));

    /* Compute backward smooth relu layer results */
    smoothreluLayerBackward.compute();

    /* Print the results of the backward smooth relu layer */
    backward::ResultPtr backwardResult = smoothreluLayerBackward.getResult();
    printTensor(backwardResult->get(backward::gradient), "Backward smooth ReLU layer result (first 5 rows):", 5);

    return 0;
}
