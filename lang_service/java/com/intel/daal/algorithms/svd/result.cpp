/* file: result.cpp */
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

#include "JComputeMode.h"
#include "JComputeStep.h"
#include "svd/JMethod.h"
#include "svd/JResult.h"
#include "svd/JResultId.h"

#include "common_helpers.h"

#define singularValuesId com_intel_daal_algorithms_svd_ResultId_singularValuesId
#define leftSingularMatrixId com_intel_daal_algorithms_svd_ResultId_leftSingularMatrixId
#define rightSingularMatrixId com_intel_daal_algorithms_svd_ResultId_rightSingularMatrixId

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::svd;

/*
 * Class:     Java_com_intel_daal_algorithms_qr_Result
 * Method:    cNewResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svd_Result_cNewResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<svd::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_svd_Result
 * Method:    cGetFactor
 * Signature:(JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svd_Result_cGetFactor
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    if ( id == singularValuesId )
    {
        return jniArgument<svd::Result>::get<svd::ResultId, NumericTable>(resAddr, svd::singularValues);
    }
    else if(id == leftSingularMatrixId)
    {
        return jniArgument<svd::Result>::get<svd::ResultId, NumericTable>(resAddr, svd::leftSingularMatrix);
    }
    else if(id == rightSingularMatrixId)
    {
        return jniArgument<svd::Result>::get<svd::ResultId, NumericTable>(resAddr, svd::rightSingularMatrix);
    }

    return (jlong)0;
}
/*
 * Class:     com_intel_daal_algorithms_svd_Result
 * Method:    cGetResultTable
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_svd_Result_cGetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    return jniArgument<svd::Result>::get<svd::ResultId, NumericTable>(resAddr, id);
}

/*
 * Class:     com_intel_daal_algorithms_svd_Result
 * Method:    cSetResultTable
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_svd_Result_cSetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    jniArgument<svd::Result>::set<svd::ResultId, NumericTable>(resAddr, id, ntAddr);
}
