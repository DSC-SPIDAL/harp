/* file: concat_layer_dense_batch.cpp */
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
!    C++ example of forward and backward concatenation (concat) layer usage
!
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-CONCAT_LAYER_BATCH"></a>
 * \example concat_layer_dense_batch.cpp
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
const size_t concatDimension = 1;
const size_t nInputs = 3;

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &datasetName);

    /* Retrieve the input data */
    TensorPtr tensorData = readTensorFromCSV(datasetName);
    LayerDataPtr tensorDataCollection = LayerDataPtr(new LayerData());

    for(int i = 0; i < nInputs; i++)
    {
        (*tensorDataCollection)[i] = tensorData;
    }

    /* Create an algorithm to compute forward concatenation layer results using default method */
    concat::forward::Batch<> concatLayerForward(concatDimension);

    /* Set input objects for the forward concatenation layer */
    concatLayerForward.input.set(forward::inputLayerData, tensorDataCollection);

    /* Compute forward concatenation layer results */
    concatLayerForward.compute();

    /* Print the results of the forward concatenation layer */
    concat::forward::ResultPtr forwardResult = concatLayerForward.getResult();

    printTensor(forwardResult->get(forward::value), "Forward concatenation layer result value (first 5 rows):", 5);

    /* Create an algorithm to compute backward concatenation layer results using default method */
    concat::backward::Batch<> concatLayerBackward(concatDimension);

    /* Set inputs for the backward concatenation layer */
    concatLayerBackward.input.set(backward::inputGradient, forwardResult->get(forward::value));
    concatLayerBackward.input.set(backward::inputFromForward, forwardResult->get(forward::resultForBackward));

    printNumericTable(forwardResult->get(concat::auxInputDimensions), "auxInputDimensions ");

    /* Compute backward concatenation layer results */
    concatLayerBackward.compute();

    /* Print the results of the backward concatenation layer */
    concat::backward::ResultPtr backwardResult = concatLayerBackward.getResult();

    for(size_t i = 0; i < tensorDataCollection->size(); i++)
    {
        printTensor(backwardResult->get(backward::resultLayerData, i), "Backward concatenation layer backward result (first 5 rows):", 5);
    }

    return 0;
}
