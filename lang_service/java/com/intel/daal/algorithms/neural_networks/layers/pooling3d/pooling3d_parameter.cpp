/* file: pooling3d_parameter.cpp */
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
#include "neural_networks/layers/pooling3d/JPooling3dParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks::layers;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cSetKernelSize
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cSetKernelSizes
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong first, jlong second, jlong third)
{
    (((pooling3d::Parameter *)cParameter))->kernelSizes.size[0] = first;
    (((pooling3d::Parameter *)cParameter))->kernelSizes.size[1] = second;
    (((pooling3d::Parameter *)cParameter))->kernelSizes.size[2] = third;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cSetStrides
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cSetStrides
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong first, jlong second, jlong third)
{
    (((pooling3d::Parameter *)cParameter))->strides.size[0] = first;
    (((pooling3d::Parameter *)cParameter))->strides.size[1] = second;
    (((pooling3d::Parameter *)cParameter))->strides.size[2] = third;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cSetSD
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cSetSD
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong first, jlong second, jlong third)
{
    (((pooling3d::Parameter *)cParameter))->indices.size[0] = first;
    (((pooling3d::Parameter *)cParameter))->indices.size[1] = second;
    (((pooling3d::Parameter *)cParameter))->indices.size[2] = third;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cSetPaddings
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cSetPaddings
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong first, jlong second, jlong third)
{
    (((pooling3d::Parameter *)cParameter))->paddings.size[0] = first;
    (((pooling3d::Parameter *)cParameter))->paddings.size[1] = second;
    (((pooling3d::Parameter *)cParameter))->paddings.size[2] = third;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cGetKernelSizes
 * Signature: (J)J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cGetKernelSizes
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    jlongArray sizeArray;
    sizeArray = env->NewLongArray(3);
    jlong tmp[3];
    tmp[0] = (jlong)((((pooling3d::Parameter *)cParameter))->kernelSizes.size[0]);
    tmp[1] = (jlong)((((pooling3d::Parameter *)cParameter))->kernelSizes.size[1]);
    tmp[2] = (jlong)((((pooling3d::Parameter *)cParameter))->kernelSizes.size[2]);
    env->SetLongArrayRegion(sizeArray, 0, 3, tmp);

    return sizeArray;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cGetStrides
 * Signature: (J)J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cGetStrides
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    jlongArray sizeArray;
    sizeArray = env->NewLongArray(3);
    jlong tmp[3];
    tmp[0] = (jlong)((((pooling3d::Parameter *)cParameter))->strides.size[0]);
    tmp[1] = (jlong)((((pooling3d::Parameter *)cParameter))->strides.size[1]);
    tmp[2] = (jlong)((((pooling3d::Parameter *)cParameter))->strides.size[2]);
    env->SetLongArrayRegion(sizeArray, 0, 3, tmp);

    return sizeArray;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cGetPaddings
 * Signature: (J)J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cGetPaddings
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    jlongArray sizeArray;
    sizeArray = env->NewLongArray(3);
    jlong tmp[3];
    tmp[0] = (jlong)((((pooling3d::Parameter *)cParameter))->paddings.size[0]);
    tmp[1] = (jlong)((((pooling3d::Parameter *)cParameter))->paddings.size[1]);
    tmp[2] = (jlong)((((pooling3d::Parameter *)cParameter))->paddings.size[2]);
    env->SetLongArrayRegion(sizeArray, 0, 3, tmp);

    return sizeArray;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_pooling3d_Pooling3dParameter
 * Method:    cGetSD
 * Signature: (J)J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_pooling3d_Pooling3dParameter_cGetSD
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    jlongArray sizeArray;
    sizeArray = env->NewLongArray(3);
    jlong tmp[3];
    tmp[0] = (jlong)((((pooling3d::Parameter *)cParameter))->indices.size[0]);
    tmp[1] = (jlong)((((pooling3d::Parameter *)cParameter))->indices.size[1]);
    tmp[2] = (jlong)((((pooling3d::Parameter *)cParameter))->indices.size[2]);
    env->SetLongArrayRegion(sizeArray, 0, 3, tmp);

    return sizeArray;
}
