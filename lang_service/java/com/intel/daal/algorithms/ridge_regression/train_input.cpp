/* file: train_input.cpp */
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

#include "JComputeMode.h"
#include "JComputeStep.h"
#include "ridge_regression/training/JInput.h"
#include "ridge_regression/training/JDistributedStep2MasterInput.h"
#include "ridge_regression/training/JTrainingMethod.h"
#include "ridge_regression/training/JTrainingInputId.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::ridge_regression;

/*
 * Class:     com_intel_daal_algorithms_ridge_regression_training_Input
 * Method:    cSetInput
 * Signature:(JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_ridge_1regression_training_Input_cSetInput
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id, jlong ntAddr)
{
    jniInput<ridge_regression::training::Input>::set<ridge_regression::training::InputId, NumericTable>(inputAddr, id, ntAddr);
}

/*
 * Class:     com_intel_daal_algorithms_ridge_regression_training_Input
 * Method:    cGetInput
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_ridge_1regression_training_Input_cGetInput
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id)
{
    return jniInput<ridge_regression::training::Input>::get<ridge_regression::training::InputId, NumericTable>(inputAddr, id);
}

/*
 * Class:     com_intel_daal_algorithms_ridge_regression_training_DistributedStep2MasterInput
 * Method:    cAddInput
 * Signature:(JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_ridge_1regression_training_DistributedStep2MasterInput_cAddInput
(JNIEnv *env, jobject thisObj, jlong inputAddr, jint id, jlong presAddr)
{
    jniInput<ridge_regression::training::DistributedInput<step2Master> >::
        add<ridge_regression::training::Step2MasterInputId, training::PartialResult>(inputAddr, id, presAddr);
}
