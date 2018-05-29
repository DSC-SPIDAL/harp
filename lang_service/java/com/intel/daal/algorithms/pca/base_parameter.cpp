/* file: base_parameter.cpp */
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
#include "pca/JOnline.h"
#include "pca/JMethod.h"
#include "pca/JBaseParameter.h"
#include "JComputeMode.h"
#include "JComputeStep.h"

using namespace daal;
using namespace daal::algorithms;
using namespace daal::services;
using namespace daal::data_management;

#define CorrelationDenseValue com_intel_daal_algorithms_pca_Method_correlationDenseValue
#define SVDDenseValue         com_intel_daal_algorithms_pca_Method_svdDenseValue

#define batchValue com_intel_daal_algorithms_ComputeMode_batchValue
#define onlineValue com_intel_daal_algorithms_ComputeMode_onlineValue
#define distributedValue com_intel_daal_algorithms_ComputeMode_distributedValue

#define step1Value com_intel_daal_algorithms_ComputeStep_step1LocalValue
#define step2Value com_intel_daal_algorithms_ComputeStep_step2MasterValue
#define step3Value com_intel_daal_algorithms_ComputeStep_step3LocalValue

/*
 * Class:     com_intel_daal_algorithms_pca_BaseParameter
 * Method:    cSetCovariance
 * Signature: (JJJIIII)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_pca_BaseParameter_cSetCovariance
(JNIEnv *env, jobject thisObj, jlong parAddr, jlong covarianceAddr, jint method, jint cmode, jint computeStep, jint prec)
{
    using namespace daal::algorithms::pca;

    if(method == CorrelationDenseValue)
    {
        if(cmode == batchValue)
        {
            if(prec == 0) //double
            {
                BatchParameter<double, correlationDense> *parameterAddr = (BatchParameter<double, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::BatchImpl> *)covarianceAddr);
            }
            else
            {
                BatchParameter<float, correlationDense> *parameterAddr = (BatchParameter<float, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::BatchImpl> *)covarianceAddr);
            }
        }
        else if(cmode == onlineValue || (cmode == distributedValue && computeStep == step1Value))
        {
            if(prec == 0) //double
            {
                OnlineParameter<double, correlationDense> *parameterAddr = (OnlineParameter<double, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::OnlineImpl> *)covarianceAddr);
            }
            else
            {
                OnlineParameter<float, correlationDense> *parameterAddr = (OnlineParameter<float, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::OnlineImpl> *)covarianceAddr);
            }
        }
        else if(cmode == distributedValue)
        {
            if(prec == 0) //double
            {
                DistributedParameter<step2Master, double, correlationDense> *parameterAddr =
                    (DistributedParameter<step2Master, double, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::DistributedIface<step2Master> > *)covarianceAddr);
            }
            else
            {
                DistributedParameter<step2Master, float, correlationDense> *parameterAddr =
                    (DistributedParameter<step2Master, float, correlationDense> *)parAddr;
                parameterAddr->covariance = *((SharedPtr<daal::algorithms::covariance::DistributedIface<step2Master> > *)covarianceAddr);
            }
        }
    }
}
