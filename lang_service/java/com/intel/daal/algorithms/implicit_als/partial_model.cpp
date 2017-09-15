/* file: partial_model.cpp */
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

#include "implicit_als/JPartialModel.h"

using namespace daal;
using namespace daal::algorithms::implicit_als;
using namespace daal::data_management;

/*
 * Class:     com_intel_daal_algorithms_implicit_als_PartialModel
 * Method:    cNewPartialModel
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_PartialModel_cNewPartialModel
  (JNIEnv *env, jobject thisObj, jlong factorsAddr, jlong indicesAddr)
{
    SerializationIfacePtr *factorsShPtr = (SerializationIfacePtr *)factorsAddr;
    SerializationIfacePtr *indicesShPtr = (SerializationIfacePtr *)indicesAddr;
    NumericTablePtr factors =
        services::staticPointerCast<NumericTable, SerializationIface>(*factorsShPtr);
    NumericTablePtr indices =
        services::staticPointerCast<NumericTable, SerializationIface>(*indicesShPtr);
    return (jlong)(new SerializationIfacePtr(new PartialModel(factors, indices)));
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_PartialModel
 * Method:    cGetFactors
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_PartialModel_cGetFactors
(JNIEnv *env, jobject thisObj, jlong modAddr)
{
    SerializationIfacePtr *factors = new SerializationIfacePtr();
    PartialModel *pModel = static_cast<PartialModel *>(((SerializationIfacePtr *)modAddr)->get());

    *factors = pModel->getFactors();

    return (jlong)factors;
}

/*
 * Class:     com_intel_daal_algorithms_implicit_als_PartialModel
 * Method:    cGetIndices
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_algorithms_implicit_1als_PartialModel_cGetIndices
(JNIEnv *env, jobject thisObj, jlong modAddr)
{
    SerializationIfacePtr *indices = new SerializationIfacePtr();
    PartialModel *pModel = static_cast<PartialModel *>(((SerializationIfacePtr *)modAddr)->get());

    *indices = pModel->getIndices();

    return (jlong)indices;
}
