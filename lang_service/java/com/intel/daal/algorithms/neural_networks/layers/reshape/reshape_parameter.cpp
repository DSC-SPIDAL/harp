/* file: reshape_parameter.cpp */
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

#include <jni.h>
#include "neural_networks/layers/reshape/JReshapeParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();

using namespace daal::algorithms::neural_networks::layers;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_reshape_ReshapeParameter
 * Method:    cInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_reshape_ReshapeParameter_cInit
(JNIEnv *env, jobject thisObj)
{
    return (jlong)(new reshape::Parameter);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_reshape_ReshapeParameter
 * Method:    cSetReshapeDimensions
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_reshape_ReshapeParameter_cSetReshapeDimensions
(JNIEnv *env, jobject thisObj, jlong cParameter, jlongArray jArray)
{
    jint size = env->GetArrayLength(jArray);
    jlong* jarr;
    jboolean jniNoCopy = JNI_FALSE;
    jarr = env->GetLongArrayElements(jArray, &jniNoCopy);

    (((reshape::Parameter *)cParameter))->reshapeDimensions.clear();
    for(int i=0; i<size; i++)
    {
        (((reshape::Parameter *)cParameter))->reshapeDimensions.push_back( (size_t)jarr[i] );
    }

    env->ReleaseLongArrayElements(jArray,jarr,0);
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_layers_reshape_ReshapeParameter
 * Method:    cGetReshapeDimensions
 * Signature: (J)J
 */
JNIEXPORT jlongArray JNICALL Java_com_intel_daal_algorithms_neural_1networks_layers_reshape_ReshapeParameter_cGetReshapeDimensions
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    size_t size = (((reshape::Parameter *)cParameter))->reshapeDimensions.size();
    jlongArray sizeArray = env->NewLongArray( size );

    jlong* tmp = (jlong*)daal_malloc(size*sizeof(jlong));

    if(!tmp)
    {
        Error e(services::ErrorMemoryAllocationFailed);
        const char *description = e.description();
        env->ThrowNew(env->FindClass("java/lang/Exception"),description);
        return sizeArray;
    }

    for(int i=0; i<size; i++)
    {
        tmp[i] = (((reshape::Parameter *)cParameter))->reshapeDimensions[i];
    }

    env->SetLongArrayRegion(sizeArray, 0, size, tmp);

    daal_free(tmp);

    return sizeArray;
}
