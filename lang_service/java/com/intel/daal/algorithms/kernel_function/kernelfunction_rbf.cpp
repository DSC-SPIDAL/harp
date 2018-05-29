/* file: kernelfunction_rbf.cpp */
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
#include "rbf/JBatch.h"
#include "rbf/JResult.h"
#include "rbf/JMethod.h"
#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::kernel_function::rbf;

#define DefaultDense com_intel_daal_algorithms_kernel_function_rbf_Method_defaultDenseValue
#define FastCSR      com_intel_daal_algorithms_kernel_function_rbf_Method_fastCSRValue

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch
 * Method:    cInit
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cInit
(JNIEnv *env, jobject thisObj, jint prec, jint method)
{
    return jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::newObj(prec, method);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_linear_Batch
 * Method:    cGetParameter
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cGetParameter
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::getParameter(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch
 * Method:    cSetResult
 * Signature:(JIIJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cSetResult
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method, jlong resultAddr)
{
    jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::setResult<kernel_function::Result>(prec, method, algAddr, resultAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch
 * Method:    cGetResult
 * Signature:(JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cGetResult
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::getResult(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch
 * Method:    cGetInput
 * Signature:(JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cGetInput
(JNIEnv *env, jobject obj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::getInput(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch
 * Method:    cClone
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Batch_cClone
(JNIEnv *env, jobject thisObj, jlong algAddr, jint prec, jint method)
{
    return jniBatch<kernel_function::rbf::Method, Batch, defaultDense, fastCSR>::getClone(prec, method, algAddr);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch_Result
 * Method:    cGetResult
 * Signature:()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Result_cNewResult
(JNIEnv *env, jobject obj)
{
    return jniArgument<kernel_function::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch_Result
 * Method:    cGetResultTable
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Result_cGetResultTable
(JNIEnv *env, jobject obj, jlong resAddr, jint id)
{
    return jniArgument<kernel_function::Result>::get<kernel_function::ResultId, NumericTable>(resAddr, id);
}

/*
 * Class:     com_intel_daal_algorithms_kernel_function_rbf_Batch_Result
 * Method:    cSetResultTable
 * Signature:(JJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_kernel_1function_rbf_Result_cSetResultTable
(JNIEnv *env, jobject obj, jlong resAddr, jint id, jlong ntAddr)
{
    jniArgument<kernel_function::Result>::set<kernel_function::ResultId, NumericTable>(resAddr, id, ntAddr);
}
