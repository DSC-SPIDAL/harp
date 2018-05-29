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

//++
//  JNI layer for multi_class_classifier_Parameter
//--


#include <jni.h>
#include "multi_class_classifier/JParameter.h"
#include "daal.h"

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cSetNClasses
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cSetNClasses
(JNIEnv *env, jobject obj, jlong parAddr, jlong val)
{
    ((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->nClasses = val;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cGetNClasses
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cGetNClasses
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->nClasses;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cSetMaxIterations
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cSetMaxIterations
(JNIEnv *env, jobject obj, jlong parAddr, jlong val)
{
    ((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->maxIterations = val;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cGetMaxIterations
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cGetMaxIterations
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->maxIterations;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cSetAccuracyThreshold
 * Signature:(JD)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cSetAccuracyThreshold
(JNIEnv *env, jobject obj, jlong parAddr, jdouble val)
{
    ((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->accuracyThreshold = val;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cGetAccuracyThreshold
 * Signature:(J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cGetAccuracyThreshold
(JNIEnv *env, jobject obj, jlong parAddr)
{
    return((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cSetTraining
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cSetTraining
(JNIEnv *env, jobject obj, jlong parAddr, jlong trainingAddr)
{
    using namespace daal::algorithms;
    daal::services::SharedPtr<classifier::training::Batch> training =
        daal::services::staticPointerCast<classifier::training::Batch, AlgorithmIface>
            (*(daal::services::SharedPtr<AlgorithmIface> *)trainingAddr);
    ((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->training = training;
}

/*
 * Class:     com_intel_daal_algorithms_multi_class_classifier_Parameter
 * Method:    cSetPrediction
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_multi_1class_1classifier_Parameter_cSetPrediction
(JNIEnv *env, jobject obj, jlong parAddr, jlong predictionAddr)
{
    using namespace daal::algorithms;
    daal::services::SharedPtr<classifier::prediction::Batch> prediction =
        daal::services::staticPointerCast<classifier::prediction::Batch, AlgorithmIface>
            (*(daal::services::SharedPtr<AlgorithmIface> *)predictionAddr);
    ((daal::algorithms::multi_class_classifier::Parameter *)parAddr)->prediction = prediction;
}
