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
#include "low_order_moments/JParameter.h"
#include "low_order_moments/JEstimatesToCompute.h"
#include "common_helpers.h"

#define EstimatesAll          com_intel_daal_algorithms_low_order_moments_EstimatesToCompute_EstimatesAll
#define EstimatesMinMax       com_intel_daal_algorithms_low_order_moments_EstimatesToCompute_EstimatesMinMax
#define EstimatesMeanVariance com_intel_daal_algorithms_low_order_moments_EstimatesToCompute_EstimatesMeanVariance

USING_COMMON_NAMESPACES()
using namespace daal;

/*
 * Class:     com_intel_daal_algorithms_low_order_moments_Parameter
 * Method:    cSetEstimatesToCompute
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_low_1order_1moments_Parameter_cSetEstimatesToCompute
  (JNIEnv *env, jobject thisObj, jlong parAddr, jint estComp)
{
    using namespace daal::algorithms;
    low_order_moments::Parameter *parameterAddr = (low_order_moments::Parameter *)parAddr;

    if(estComp == EstimatesAll)
    {
        parameterAddr->estimatesToCompute = low_order_moments::estimatesAll;
    }
    else if(estComp == EstimatesMinMax)
    {
        parameterAddr->estimatesToCompute = low_order_moments::estimatesMinMax;
    }
    else if(estComp == EstimatesMeanVariance)
    {
        parameterAddr->estimatesToCompute = low_order_moments::estimatesMeanVariance;
    }
}

/*
 * Class:     com_intel_daal_algorithms_low_order_moments_Parameter
 * Method:    cGetEstimatesToCompute
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_algorithms_low_1order_1moments_Parameter_cGetEstimatesToCompute
  (JNIEnv *env, jobject thisObj, jlong parAddr)
{
    using namespace daal::algorithms;
    low_order_moments::Parameter *parameterAddr = (low_order_moments::Parameter *)parAddr;

    return (jint)(parameterAddr->estimatesToCompute);

}
