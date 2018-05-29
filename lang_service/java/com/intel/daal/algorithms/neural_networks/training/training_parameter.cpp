/* file: training_parameter.cpp */
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
#include "neural_networks/training/JTrainingParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_training_TrainingParameter
 * Method:    cInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_training_TrainingParameter_cInit
(JNIEnv *env, jobject thisObj, jlong optAddr)
{
    services::SharedPtr<optimization_solver::iterative_solver::Batch > opt =
        *((services::SharedPtr<optimization_solver::iterative_solver::Batch > *)optAddr);
    return (jlong)(new training::Parameter(opt));
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_training_TrainingParameter
 * Method:    cSetOptimizationSolver
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_training_TrainingParameter_cSetOptimizationSolver
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong optAddr)
{
    services::SharedPtr<optimization_solver::iterative_solver::Batch > opt =
        *((services::SharedPtr<optimization_solver::iterative_solver::Batch > *)optAddr);
    (((training::Parameter *)cParameter))->optimizationSolver = opt;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_training_TrainingParameter
 * Method:    cGetOptimizationSolver
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_neural_1networks_training_TrainingParameter_cGetOptimizationSolver
(JNIEnv *env, jobject thisObj, jlong cParameter)
{
    SharedPtr<optimization_solver::iterative_solver::Batch > *opt =
        new SharedPtr<optimization_solver::iterative_solver::Batch >((((training::Parameter *)cParameter))->optimizationSolver);
    return (jlong)opt;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_training_TrainingParameter
 * Method:    cSetEngine
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_training_TrainingParameter_cSetEngine
(JNIEnv *env, jobject thisObj, jlong cParameter, jlong engineAddr)
{
    (((training::Parameter *)cParameter))->engine = staticPointerCast<engines::BatchBase, AlgorithmIface> (*(SharedPtr<AlgorithmIface> *)engineAddr);
}
