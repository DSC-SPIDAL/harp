/* file: softmax_cross_parameter.cpp */
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
#include "neural_networks/layers/softmax_cross/JSoftmaxCrossParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks::layers::loss;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_softmax_cross_SoftmaxCrossParameter
 * Method:    cInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_softmax_1cross_SoftmaxCrossParameter_cInit
  (JNIEnv *env, jobject thisObj)
{
    return (jlong)(new softmax_cross::Parameter);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_softmax_cross_SoftmaxCrossParameter
 * Method:    cGetAccuracyThreshold
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_softmax_1cross_SoftmaxCrossParameter_cGetAccuracyThreshold
  (JNIEnv *env, jobject thisObj, jlong cParameter)
{
    return (((softmax_cross::Parameter *)cParameter))->accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_softmax_cross_SoftmaxCrossParameter
 * Method:    cSetAccuracyThreshold
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_softmax_1cross_SoftmaxCrossParameter_cSetAccuracyThreshold
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble accuracyThreshold)
{
    (((softmax_cross::Parameter *)cParameter))->accuracyThreshold = accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_softmax_cross_SoftmaxCrossParameter
 * Method:    cGetDimension
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_softmax_1cross_SoftmaxCrossParameter_cGetDimension
  (JNIEnv *env, jobject thisObj, jlong cParameter)
{
    return (((softmax_cross::Parameter *)cParameter))->dimension;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_softmax_cross_SoftmaxCrossParameter
 * Method:    cSetDimension
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_softmax_1cross_SoftmaxCrossParameter_cSetDimension
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble dimension)
{
    (((softmax_cross::Parameter *)cParameter))->dimension = dimension;
}
