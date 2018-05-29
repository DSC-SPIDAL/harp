/* file: batch_input.cpp */
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
#include "pca/transform/JTransformInput.h"
#include "pca/transform/JTransformInputId.h"
#include "pca/transform/JTransformDataInputId.h"
#include "pca/transform/JTransformComponentId.h"
#include "pca/transform/JTransformMethod.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::pca::transform;

#define InputDataId  com_intel_daal_algorithms_pca_transform_TransformInputId_InputDataId
#define InputEigenvectorsId  com_intel_daal_algorithms_pca_transform_TransformInputId_InputEigenvectorsId
#define DataForTransformId  com_intel_daal_algorithms_pca_transform_TransformDataInputId_DataForTransformId
#define TransformComponentMeansId  com_intel_daal_algorithms_pca_transform_TransformComponentId_TransformComponentMeansId
#define TransformComponentVariancesId  com_intel_daal_algorithms_pca_transform_TransformComponentId_TransformComponentVariancesId
#define TransformComponentEigenvaluesId  com_intel_daal_algorithms_pca_transform_TransformComponentId_TransformComponentEigenvaluesId

/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cSetInput
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cSetInputTable
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint id, jlong ntAddr)
{
    if(id == InputDataId || id == InputEigenvectorsId)
    {
        jniInput<pca::transform::Input>::
            set<pca::transform::InputId, NumericTable>(inputAddr, id, ntAddr);
    }
}

/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cSetInputTransformData
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cSetInputTransformData
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint id, jlong ntAddr)
{
    if(id == DataForTransformId)
    {
        jniInput<pca::transform::Input>::
            set<pca::transform::TransformDataInputId, KeyValueDataCollection>(inputAddr, id, ntAddr);
    }
}

/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cSetInputTransformComponent
 * Signature: (JIIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cSetInputTransformComponent
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint wid, jint id, jlong ntAddr)
{
    if(wid == DataForTransformId && (id == TransformComponentMeansId || id == TransformComponentVariancesId || id == TransformComponentEigenvaluesId))
    {
        jniInput<pca::transform::Input>::
            setex<pca::transform::TransformDataInputId, pca::transform::TransformComponentId, NumericTable>(inputAddr, wid, id, ntAddr);
    }
}



/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cGetInputTable
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cGetInputTable
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint id)
{
    if(id == InputDataId || id == InputEigenvectorsId)
    {
        return jniInput<pca::transform::Input>::
            get<pca::transform::InputId, NumericTable>(inputAddr, id);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cGetInputTransformData
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cGetInputTransformData
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint id)
{
    if(id == DataForTransformId)
    {
        return jniInput<pca::transform::Input>::
            get<pca::transform::TransformDataInputId, KeyValueDataCollection>(inputAddr, id);
    }

    return (jlong)0;
}

/*
 * Class:     com_intel_daal_algorithms_pca_transform_TransformInput
 * Method:    cGetInputTransformComponent
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_pca_transform_TransformInput_cGetInputTransformComponent
(JNIEnv *jenv, jobject thisObj, jlong inputAddr, jint wid, jint id)
{
    if(wid == DataForTransformId && (id == TransformComponentMeansId || id == TransformComponentVariancesId || id == TransformComponentEigenvaluesId))
    {
        return jniInput<pca::transform::Input>::
            getex<pca::transform::TransformDataInputId, pca::transform::TransformComponentId, NumericTable>(inputAddr, wid, id);
    }

    return (jlong)0;
}
