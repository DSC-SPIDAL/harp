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

#include "optimization_solver/sum_of_functions/JParameter.h"

#include "common_defines.i"

using namespace daal;
using namespace daal::algorithms;
using namespace daal::services;
using namespace daal::data_management;
using namespace daal::algorithms::optimization_solver;

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cSetBatchIndices
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cSetBatchIndices
(JNIEnv *, jobject, jlong parAddr, jlong cBatchIndices)
{
    SerializationIfacePtr *ntShPtr = (SerializationIfacePtr *)cBatchIndices;
    ((sum_of_functions::Parameter *)parAddr)->batchIndices = staticPointerCast<NumericTable, SerializationIface>(*ntShPtr);
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cGetBatchIndices
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cGetBatchIndices
(JNIEnv *, jobject, jlong parAddr)
{
    NumericTablePtr *ntShPtr = new NumericTablePtr();
    *ntShPtr = ((sum_of_functions::Parameter *)parAddr)->batchIndices;
    return (jlong)ntShPtr;
}


/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cSetNumberOfTerms
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cSetNumberOfTerms
(JNIEnv *, jobject, jlong parAddr, jlong numberOfTerms)
{
    ((sum_of_functions::Parameter *)parAddr)->numberOfTerms = numberOfTerms;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cGetNumberOfTerms
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cGetNumberOfTerms
(JNIEnv *, jobject, jlong parAddr)
{
    return ((sum_of_functions::Parameter *)parAddr)->numberOfTerms;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cGetNumberOfTerms
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cCreateParameter
(JNIEnv *, jobject, jlong numberOfTerms)
{
    jlong addr = 0;
    addr = (jlong) (new sum_of_functions::Parameter(numberOfTerms));
    return addr;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cParameterDispose
 * Signature: (J)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cParameterDispose
(JNIEnv *, jobject, jlong createdParameter)
{
    sum_of_functions::Parameter* ptr = (sum_of_functions::Parameter *) createdParameter;
    delete ptr;
}

/*
 * Class:     com_intel_daal_algorithms_optimization_solver_sum_of_functions_Parameter
 * Method:    cSetCParameter
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_optimization_1solver_sum_1of_1functions_Parameter_cSetCParameter
(JNIEnv *, jobject, jlong parAddr, jlong algAddr)
{
    sum_of_functions::Parameter *parameterPtr = (sum_of_functions::Parameter *)parAddr;

    SharedPtr<sum_of_functions::Batch> alg =
        staticPointerCast<sum_of_functions::Batch, AlgorithmIface>
        (*(SharedPtr<AlgorithmIface> *)algAddr);
    alg->sumOfFunctionsParameter = parameterPtr;
}
