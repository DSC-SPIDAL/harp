/* file: parameter.cpp */
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

#include "daal.h"

#include "optimization_solver/iterative_solver/JParameter.h"

#include "common_defines.i"

using namespace daal;
using namespace daal::algorithms;
using namespace daal::services;
using namespace daal::data_management;
using namespace daal::algorithms::optimization_solver;

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
 * Method:    cSetFunction
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cSetFunction
(JNIEnv *, jobject, jlong parAddr, jlong cFunction)
{
    iterative_solver::Parameter *parameterAddr = (iterative_solver::Parameter *)parAddr;
    SharedPtr<optimization_solver::sum_of_functions::Batch> objectiveFunction =
        staticPointerCast<optimization_solver::sum_of_functions::Batch, AlgorithmIface>
        (*(SharedPtr<AlgorithmIface> *)cFunction);
    parameterAddr->function = objectiveFunction;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
 * Method:    cSetNIterations
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cSetNIterations
(JNIEnv *, jobject, jlong parAddr, jlong nIterations)
{
    ((iterative_solver::Parameter *)parAddr)->nIterations = nIterations;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
 * Method:    cGetNIterations
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cGetNIterations
(JNIEnv *, jobject, jlong parAddr)
{
    return ((iterative_solver::Parameter *)parAddr)->nIterations;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
 * Method:    cSetAccuracyThreshold
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cSetAccuracyThreshold
(JNIEnv *, jobject, jlong parAddr, jdouble accuracyThreshold)
{
    ((iterative_solver::Parameter *)parAddr)->accuracyThreshold = accuracyThreshold;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
 * Method:    cGetAccuracyThreshold
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cGetAccuracyThreshold
(JNIEnv *, jobject, jlong parAddr)
{
    return ((iterative_solver::Parameter *)parAddr)->accuracyThreshold;
}

/*
* Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
* Method:    cSetOptionalResultRequired
* Signature: (JZ)V
*/
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cSetOptionalResultRequired
(JNIEnv *, jobject, jlong parAddr, jboolean flag)
{
    ((iterative_solver::Parameter *)parAddr)->optionalResultRequired = flag;
}

/*
* Class:     com_intel_daal_algorithms_optimization_solver_iterative_solver_Parameter
* Method:    cGetOptionalResultRequired
* Signature: (J)Z
*/
JNIEXPORT jboolean JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cGetOptionalResultRequired
(JNIEnv *, jobject, jlong parAddr)
{
    return ((iterative_solver::Parameter *)parAddr)->optionalResultRequired;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_adagrad_Parameter
 * Method:    cSetBatchSize
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cSetBatchSize
(JNIEnv *, jobject, jlong parAddr, jlong batchSize)
{
    ((iterative_solver::Parameter *)parAddr)->batchSize = batchSize;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_adagrad_Parameter
 * Method:    cGetBatchSize
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_optimization_1solver_iterative_1solver_Parameter_cGetBatchSize
(JNIEnv *, jobject, jlong parAddr)
{
    return ((iterative_solver::Parameter *)parAddr)->batchSize;
}
