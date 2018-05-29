/* file: parameter.cpp */
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
#include "daal.h"
#include "kdtree_knn_classification/JParameter.h"
#include "common_helpers.h"

USING_COMMON_NAMESPACES();

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cSetK
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cSetK
(JNIEnv *env, jobject thisObj, jlong parAddr, jlong k)
{
    (*(kdtree_knn_classification::Parameter *)parAddr).k = k;
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cGetK
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cGetK
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    return(*(kdtree_knn_classification::Parameter *)parAddr).k;
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cSetSeed
 * Signature:(JI)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cSetSeed
(JNIEnv *env, jobject thisObj, jlong parAddr, jint seed)
{
    (*(kdtree_knn_classification::Parameter *)parAddr).seed = seed;
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cGetSeed
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cGetSeed
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    return(*(kdtree_knn_classification::Parameter *)parAddr).seed;
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cSetEngine
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cSetEngine
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong engineAddr)
{
    (((kdtree_knn_classification::Parameter *)cParameter))->engine = staticPointerCast<engines::BatchBase, AlgorithmIface> (*(SharedPtr<AlgorithmIface> *)engineAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cSetDataUseInModel
 * Signature:(JI)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cSetDataUseInModel
(JNIEnv *env, jobject thisObj, jlong parAddr, jint flag)
{
    (*(kdtree_knn_classification::Parameter *)parAddr).dataUseInModel = (kdtree_knn_classification::DataUseInModel)flag;
}

/*
 * Class:     com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter
 * Method:    cGetDataUseInModel
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_kdtree_1knn_1classification_Parameter_cGetDataUseInModel
(JNIEnv *env, jobject thisObj, jlong parAddr)
{
    return (jint)((*(kdtree_knn_classification::Parameter *)parAddr).dataUseInModel);
}
