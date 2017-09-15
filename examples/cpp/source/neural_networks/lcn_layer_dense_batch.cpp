/* file: lcn_layer_dense_batch.cpp */
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
!    C++ example of forward and backward local contrast normalization layer usage
!
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-LCN_LAYER_BATCH"></a>
 * \example lcn_layer_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::neural_networks::layers;
using namespace daal::data_management;
using namespace daal::services;

int main()
{
    /* Create collection of dimension sizes of the input data tensor */
    Collection<size_t> inDims;
    inDims.push_back(2);
    inDims.push_back(1);
    inDims.push_back(3);
    inDims.push_back(4);
    TensorPtr tensorData = TensorPtr(new HomogenTensor<>(inDims, Tensor::doAllocate, 1.0f));

    /* Create an algorithm to compute forward local contrast normalization layer results using default method */
    lcn::forward::Batch<> lcnLayerForward;

    /* Set input objects for the forward local contrast normalization layer */
    lcnLayerForward.input.set(forward::data, tensorData);

    /* Compute forward local contrast normalization layer results */
    lcnLayerForward.compute();

    /* Print the results of the forward local contrast normalization layer */
    lcn::forward::ResultPtr forwardResult = lcnLayerForward.getResult();
    printTensor(forwardResult->get(forward::value),       "Forward local contrast normalization layer result:");
    printTensor(forwardResult->get(lcn::auxCenteredData), "Centered data tensor:");
    printTensor(forwardResult->get(lcn::auxSigma),        "Sigma tensor:");
    printTensor(forwardResult->get(lcn::auxC),            "C tensor:");
    printTensor(forwardResult->get(lcn::auxInvMax),       "Inverted max(sigma, C):");

    /* Create input gradient tensor for backward local contrast normalization layer */
    TensorPtr tensorDataBack = TensorPtr(new HomogenTensor<>(inDims, Tensor::doAllocate, 0.01f));

    /* Create an algorithm to compute backward local contrast normalization layer results using default method */
    lcn::backward::Batch<> lcnLayerBackward;
    lcnLayerBackward.input.set(backward::inputGradient, tensorDataBack);
    lcnLayerBackward.input.set(backward::inputFromForward, forwardResult->get(forward::resultForBackward));

    /* Compute backward local contrast normalization layer results */
    lcnLayerBackward.compute();

    /* Get the computed backward local contrast normalization layer results */
    backward::ResultPtr backwardResult = lcnLayerBackward.getResult();
    printTensor(backwardResult->get(backward::gradient), "Local contrast normalization layer backpropagation gradient result:");

    return 0;
}
