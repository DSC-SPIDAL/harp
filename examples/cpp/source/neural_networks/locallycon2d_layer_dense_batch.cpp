/* file: locallycon2d_layer_dense_batch.cpp */
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
!    C++ example of forward and backward 2D locally connected layer usage
!
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-LOCALLYCON2D_LAYER_BATCH"></a>
 * \example locallycon2d_layer_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::neural_networks::layers;
using namespace daal::data_management;
using namespace daal::services;

int main(int argc, char *argv[])
{
    /* Create collection of dimension sizes of the input data tensor */
    Collection<size_t> inDims;
    inDims << 2 << 2 << 6 << 8;

    TensorPtr dataTensor = TensorPtr(new HomogenTensor<>(inDims, Tensor::doAllocate, 1.0f));

    /* Create an algorithm to compute forward 2D locally connected layer results using default method */
    locallyconnected2d::forward::Batch<> locallyconnected2dLayerForward;
    locallyconnected2dLayerForward.input.set(forward::data, dataTensor);

    /* Compute forward 2D locally connected layer results */
    locallyconnected2dLayerForward.compute();

    /* Get the computed forward 2D locally connected layer results */
    locallyconnected2d::forward::ResultPtr forwardResult = locallyconnected2dLayerForward.getResult();
    printTensor(forwardResult->get(forward::value), "Forward 2D locally connected layer result (first 5 rows):", 5, 15);
    printTensor(forwardResult->get(locallyconnected2d::auxWeights), "2D locally connected layer weights (first 5 rows):", 5, 15);

    const Collection<size_t> &gDims = forwardResult->get(forward::value)->getDimensions();
    /* Create input gradient tensor for backward 2D locally connected layer */
    TensorPtr tensorDataBack = TensorPtr(new HomogenTensor<>(gDims, Tensor::doAllocate, 0.01f));

    /* Create an algorithm to compute backward 2D locally connected layer results using default method */
    locallyconnected2d::backward::Batch<> locallyconnected2dLayerBackward;
    locallyconnected2dLayerBackward.input.set(backward::inputGradient, tensorDataBack);
    locallyconnected2dLayerBackward.input.set(backward::inputFromForward, forwardResult->get(forward::resultForBackward));

    /* Compute backward 2D locally connected layer results */
    locallyconnected2dLayerBackward.compute();

    /* Get the computed backward 2D locally connected layer results */
    backward::ResultPtr backwardResult = locallyconnected2dLayerBackward.getResult();
    printTensor(backwardResult->get(backward::gradient),
                "2D locally connected layer backpropagation gradient result (first 5 rows):", 5, 15);
    printTensor(backwardResult->get(backward::weightDerivatives),
                "2D locally connected layer backpropagation weightDerivative result (first 5 rows):", 5, 15);
    printTensor(backwardResult->get(backward::biasDerivatives),
                "2D locally connected layer backpropagation biasDerivative result (first 5 rows):", 5, 15);
    return 0;
}
