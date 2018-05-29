/* file: dropout_layer_dense_batch.cpp */
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
!  Content:
!    C++ example of forward and backward dropout layer usage
!
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-DROPOUT_LAYER_BATCH"></a>
 * \example dropout_layer_dense_batch.cpp
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

    /* Create an algorithm to compute forward dropout layer results using default method */
    dropout::forward::Batch<> dropoutLayerForward;

    /* Set input objects for the forward dropout layer */
    dropoutLayerForward.input.set(forward::data, tensorData);

    /* Compute forward dropout layer results */
    dropoutLayerForward.compute();

    /* Print the results of the forward dropout layer */
    dropout::forward::ResultPtr forwardResult = dropoutLayerForward.getResult();
    printTensor(forwardResult->get(forward::value), "Forward dropout layer result (first 5 rows):", 5);
    printTensor(forwardResult->get(dropout::auxRetainMask), "Dropout layer retain mask (first 5 rows):", 5);

    /* Get the size of forward dropout layer output */
    const Collection<size_t> &gDims = forwardResult->get(forward::value)->getDimensions();
    TensorPtr tensorDataBack = TensorPtr(new HomogenTensor<>(gDims, Tensor::doAllocate, 0.01f));

    /* Create an algorithm to compute backward dropout layer results using default method */
    dropout::backward::Batch<> dropoutLayerBackward;

    /* Set input objects for the backward dropout layer */
    dropoutLayerBackward.input.set(backward::inputGradient, tensorDataBack);
    dropoutLayerBackward.input.set(backward::inputFromForward, forwardResult->get(forward::resultForBackward));

    /* Compute backward dropout layer results */
    dropoutLayerBackward.compute();

    /* Print the results of the backward dropout layer */
    backward::ResultPtr backwardResult = dropoutLayerBackward.getResult();
    printTensor(backwardResult->get(backward::gradient), "Backward dropout layer result (first 5 rows):", 5);

    return 0;
}
