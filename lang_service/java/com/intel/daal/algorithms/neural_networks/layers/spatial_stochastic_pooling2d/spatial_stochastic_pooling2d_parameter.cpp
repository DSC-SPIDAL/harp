/* file: spatial_stochastic_pooling2d_parameter.cpp */
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
#include "neural_networks/layers/spatial_stochastic_pooling2d/JSpatialStochasticPooling2dParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks::layers;


/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_stochastic_1pooling2d_SpatialStochasticPooling2dParameter
 * Method:    cGetSeed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_spatial_1stochastic_1pooling2d_SpatialStochasticPooling2dParameter_cGetSeed
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    return (jlong)((((spatial_stochastic_pooling2d::Parameter *)cParameter))->seed);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_stochastic_1pooling2d_SpatialStochasticPooling2dParameter
 * Method:    cSetSeed
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_spatial_1stochastic_1pooling2d_SpatialStochasticPooling2dParameter_cSetSeed
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong seed)
{
    (((spatial_stochastic_pooling2d::Parameter *)cParameter))->seed = (size_t)seed;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_stochastic_1pooling2d_SpatialStochasticPooling2dParameter
 * Method:    cSetEngine
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_spatial_1stochastic_1pooling2d_SpatialStochasticPooling2dParameter_cSetEngine
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong engineAddr)
{
    (((spatial_stochastic_pooling2d::Parameter *)cParameter))->engine = staticPointerCast<engines::BatchBase, AlgorithmIface> (*(SharedPtr<AlgorithmIface> *)engineAddr);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_spatial_stochastic_pooling2d_SpatialStochasticPooling2dParameter
 * Method:    cGetPredictionStage
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_spatial_1stochastic_1pooling2d_SpatialStochasticPooling2dParameter_cGetPredictionStage
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    return (jlong)((((spatial_stochastic_pooling2d::Parameter *)cParameter))->predictionStage);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_spatial_stochastic_pooling2d_SpatialStochasticPooling2dParameter
 * Method:    cSetPredictionStage
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_spatial_1stochastic_1pooling2d_SpatialStochasticPooling2dParameter_cSetPredictionStage
(JNIEnv *env, jobject thisObj, jlong cParameter, jboolean predictionStage)
{
    (((spatial_stochastic_pooling2d::Parameter *)cParameter))->predictionStage = predictionStage;
}
