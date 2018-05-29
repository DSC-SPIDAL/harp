/* file: maximum_pooling3d_forward_result.cpp */
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
#include "neural_networks/layers/maximum_pooling3d/JMaximumPooling3dForwardResult.h"
#include "neural_networks/layers/maximum_pooling3d/JMaximumPooling3dLayerDataId.h"
#include "neural_networks/layers/maximum_pooling3d/JMaximumPooling3dLayerDataNumericTableId.h"

#include "daal.h"

#include "common_helpers.h"

#define auxSelectedIndicesId com_intel_daal_algorithms_neural_networks_layers_maximum_pooling3d_MaximumPooling3dLayerDataId_auxSelectedIndicesId
#define auxInputDimensionsId com_intel_daal_algorithms_neural_networks_layers_maximum_pooling3d_MaximumPooling3dLayerDataNumericTableId_auxInputDimensionsId

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks::layers::maximum_pooling3d;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult
 * Method:    cNewResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult_cNewResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<forward::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult
 * Method:    cGetValue
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult_cGetValue
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if (id == auxSelectedIndicesId)
    {
        return jniArgument<forward::Result>::get<LayerDataId, Tensor>(resAddr, id);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult
 * Method:    cSetValue
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult_cSetValue
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    if (id == auxSelectedIndicesId)
    {
        jniArgument<forward::Result>::set<LayerDataId, Tensor>(resAddr, auxSelectedIndices, id);
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_pooling3d_MaximumPooling3dForwardResult
 * Method:    cGetNumericTableValue
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult_cGetNumericTableValue
  (JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if (id == auxInputDimensionsId)
    {
        return jniArgument<forward::Result>::get<LayerDataNumericTableId, NumericTable>(resAddr, id);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_maximum_pooling3d_MaximumPooling3dForwardResult
 * Method:    cSetNumericTableValue
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_maximum_1pooling3d_MaximumPooling3dForwardResult_cSetNumericTableValue
  (JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    if (id == auxInputDimensionsId)
    {
        jniArgument<forward::Result>::set<LayerDataNumericTableId, NumericTable>(resAddr, auxInputDimensions, id);
    }
}
