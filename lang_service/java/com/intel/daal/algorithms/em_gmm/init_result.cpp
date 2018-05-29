/* file: init_result.cpp */
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
#include "em_gmm/init/JInitResult.h"
#include "em_gmm/init/JInitResultId.h"
#include "em_gmm/init/JInitResultCovariancesId.h"
#include "common_defines.i"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::em_gmm::init;

#define WeightsValue     com_intel_daal_algorithms_em_gmm_init_InitResultId_weightsValue
#define MeansValue       com_intel_daal_algorithms_em_gmm_init_InitResultId_meansValue
#define CovariancesValue com_intel_daal_algorithms_em_gmm_init_InitResultCovariancesId_covariancesValue

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cNewResult
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cNewResult
(JNIEnv *env, jobject thisObj)
{
    return jniArgument<em_gmm::init::Result>::newObj();
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cGetResultTable
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cGetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    return jniArgument<em_gmm::init::Result>::get<em_gmm::init::ResultId, NumericTable>(resAddr, id);
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cGetCovariancesDataCollection
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cGetCovariancesDataCollection
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id)
{
    return jniArgument<em_gmm::init::Result>::get<em_gmm::init::ResultCovariancesId, DataCollection>(resAddr, id);
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cGetResultCovarianceTable
 * Signature: (JII)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cGetResultCovarianceTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jint index)
{
    return jniArgument<em_gmm::init::Result>::get<em_gmm::init::ResultCovariancesId, NumericTable>(resAddr, covariances, index);
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cSetResultTable
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cSetResultTable
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong ntAddr)
{
    ResultId cid;
    if(id == WeightsValue) { cid = weights; }
    else if(id == MeansValue) { cid = means; }
    else { return; }

    jniArgument<em_gmm::init::Result>::set<em_gmm::init::ResultId, NumericTable>(resAddr, cid, ntAddr);
}

/*
 * Class:     com_intel_daal_algorithms_em_gmm_init_InitResult
 * Method:    cSetCovarianceCollection
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_em_1gmm_init_InitResult_cSetCovarianceCollection
(JNIEnv *env, jobject thisObj, jlong resAddr, jint id, jlong dcAddr)
{
    ResultCovariancesId cid;
    if(id == CovariancesValue) { cid = covariances; }
    else { return; }

    jniArgument<em_gmm::init::Result>::set<em_gmm::init::ResultCovariancesId, DataCollection>(resAddr, cid, dcAddr);
}
