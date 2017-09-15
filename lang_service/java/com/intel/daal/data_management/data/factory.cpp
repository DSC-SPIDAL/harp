/* file: factory.cpp */
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

#include "JFactory.h"

#include "java_numeric_table.h"
#include "java_tensor.h"

using namespace daal;
using namespace daal::data_management;
using namespace daal::services;

/*
 * Class:     com_intel_daal_data_management_data_Factory
 * Method:    cGetSerializationTag
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_Factory_cGetSerializationTag
  (JNIEnv *env, jobject thisObj, jlong serializableAddr)
{
    SerializationIfacePtr *object = (SerializationIfacePtr *)serializableAddr;
    int tag = (*object)->getSerializationTag();
    return (jint)tag;
}

/*
 * Class:     com_intel_daal_data_management_data_Factory
 * Method:    cGetJavaNumericTable
 * Signature: (OJ)O
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_Factory_cGetJavaNumericTable
  (JNIEnv *env, jobject thisObj, jlong cObject)
{
    SerializationIfacePtr *object = (SerializationIfacePtr *)cObject;
    JavaNumericTableBase *nt = dynamic_cast<JavaNumericTableBase*>(object->get());

    if (nt != 0) { return nt->getJavaObject(); }

    return 0;
}

/*
 * Class:     com_intel_daal_data_management_data_Factory
 * Method:    cGetJavaTensor
 * Signature: (OJ)O
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_Factory_cGetJavaTensor
  (JNIEnv *env, jobject thisObj, jlong cObject)
{
    SerializationIfacePtr *object = (SerializationIfacePtr *)cObject;
    JavaTensorBase *nt = dynamic_cast<JavaTensorBase*>(object->get());

    if (nt != 0) { return nt->getJavaObject(); }

    return 0;
}
