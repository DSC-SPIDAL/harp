/* file: maximum_pooling2d_batch.cpp */
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

#include <jni.h>
#include "neural_networks/layers/maximum_pooling2d/JMaximumPooling2dBatch.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks::layers;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling2d_MaximumPooling2dBatch
 * Method:    cInit
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling2d_MaximumPooling2dBatch_cInit
(JNIEnv *env, jobject thisObj, jint prec, jint method, jlong nDim)
{
    return jniBatchLayer<maximum_pooling2d::Method, maximum_pooling2d::Batch, maximum_pooling2d::defaultDense>::
           newObj(prec, method, nDim);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling2d_MaximumPooling2dBatch
 * Method:    cInitParameter
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling2d_MaximumPooling2dBatch_cInitParameter
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniBatchLayer<maximum_pooling2d::Method, maximum_pooling2d::Batch, maximum_pooling2d::defaultDense>::
           getParameter(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling2d_MaximumPooling2dBatch
 * Method:    cGetForwardLayer
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling2d_MaximumPooling2dBatch_cGetForwardLayer
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniBatchLayer<maximum_pooling2d::Method, maximum_pooling2d::Batch, maximum_pooling2d::defaultDense>::
           getForwardLayer(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling2d_MaximumPooling2dBatch
 * Method:    cGetBackwardLayer
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling2d_MaximumPooling2dBatch_cGetBackwardLayer
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniBatchLayer<maximum_pooling2d::Method, maximum_pooling2d::Batch, maximum_pooling2d::defaultDense>::
           getBackwardLayer(prec, method, algAddr);
}