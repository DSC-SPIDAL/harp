/* file: batch_normalization_forward_input.cpp */
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
#include "neural_networks/layers/batch_normalization/JBatchNormalizationForwardInput.h"
#include "neural_networks/layers/batch_normalization/JBatchNormalizationForwardInputLayerDataId.h"

#include "daal.h"

#include "common_helpers.h"

#define populationMeanId com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInputLayerDataId_populationMeanId
#define populationVarianceId com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInputLayerDataId_populationVarianceId

USING_COMMON_NAMESPACES()

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInput
 * Method:    cSetInput
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_batch_1normalization_BatchNormalizationForwardInput_cSetInput
  (JNIEnv *env, jobject thisObj, jlong inputAddr, jint id, jlong ntAddr)
{
    using namespace daal::algorithms::neural_networks::layers::batch_normalization;
    if (id == populationMeanId || id == populationVarianceId)
    {
        jniInput<forward::Input>::set<forward::InputLayerDataId, Tensor>(inputAddr, id, ntAddr);
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInput
 * Method:    cGetInput
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_batch_1normalization_BatchNormalizationForwardInput_cGetInput
  (JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
    using namespace daal::algorithms::neural_networks::layers::batch_normalization;
    if (id == populationMeanId || id == populationVarianceId)
    {
        return jniInput<forward::Input>::get<forward::InputLayerDataId, Tensor>(inputAddr, id);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInput
 * Method:    cGetWeightsSizes
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_batch_1normalization_BatchNormalizationForwardInput_cGetWeightsSizes
  (JNIEnv *env, jobject thisObj, jlong inputAddr, jlong paramAddr)
{
    using namespace daal::algorithms::neural_networks::layers;
    Collection<size_t> dims = ((batch_normalization::forward::Input *)inputAddr)->getWeightsSizes((batch_normalization::Parameter *)paramAddr);
    return getJavaLongArrayFromSizeTCollection(env, dims);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_batch_normalization_BatchNormalizationForwardInput
 * Method:    cGetBiasesSizes
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_batch_1normalization_BatchNormalizationForwardInput_cGetBiasesSizes
  (JNIEnv *env, jobject thisObj, jlong inputAddr, jlong paramAddr)
{
    using namespace daal::algorithms::neural_networks::layers;
    Collection<size_t> dims = ((batch_normalization::forward::Input *)inputAddr)->getBiasesSizes((batch_normalization::Parameter *)paramAddr);
    return getJavaLongArrayFromSizeTCollection(env, dims);
}
