/* file: train.cpp */
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

/*
//++
//  JNI layer for multi_class_classifier_Train
//--
*/

#include <jni.h>
#include "JComputeMode.h"
#include "classifier/training/JTrainingResultId.h"
#include "multi_class_classifier/training/JTrainingMethod.h"
#include "classifier/training/JInputId.h"
#include "multi_class_classifier/training/JTrainingBatch.h"
#include "multi_class_classifier/training/JTrainingResult.h"
#include "daal.h"

#include "common_helpers.h"

const int innerModelID = com_intel_daal_algorithms_classifier_training_TrainingResultId_Model;

USING_COMMON_NAMESPACES()
using namespace daal::algorithms::multi_class_classifier::training;

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_training_TrainingBatch
 * Method:    cInit
 * Signature:(II)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_training_TrainingBatch_cInit
(JNIEnv *env, jobject obj, jint prec, jint method, jlong nClasses)
{
    return jniBatch<multi_class_classifier::training::Method, Batch, oneAgainstOne>::newObj(prec, method, nClasses);
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_training_TrainingBatch
 * Method:    cInitParameter
 * Signature:(JIII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_training_TrainingBatch_cInitParameter
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<multi_class_classifier::training::Method, Batch, oneAgainstOne>::getParameter(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_training_TrainingBatch
 * Method:    cGetResult
 * Signature:(JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_training_TrainingBatch_cGetResult
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<multi_class_classifier::training::Method, Batch, oneAgainstOne>::getResult(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_training_TrainingBatch
 * Method:    cClone
 * Signature:(JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_training_TrainingBatch_cClone
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<multi_class_classifier::training::Method, Batch, oneAgainstOne>::getClone(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_training_TrainingResult
 * Method:    cGetModel
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_training_TrainingResult_cGetModel
(JNIEnv *env, jobject obj, jlong resAddr, jint id)
{
    if (id == innerModelID)
    {
        return jniArgument<multi_class_classifier::training::Result>::
            get<classifier::training::ResultId, classifier::Model>(resAddr, classifier::training::model);
    }

    return (jlong)0;
}
