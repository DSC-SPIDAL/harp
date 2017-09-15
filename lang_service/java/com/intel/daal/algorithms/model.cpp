/* file: model.cpp */
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
#include "JModel.h"
#include "daal.h"

using namespace daal;
using namespace daal::services;
using namespace daal::data_management;

/*
 * Class:     com_intel_daal_algorithms_Model
 * Method:    cSerializeCObject
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_algorithms_Model_cSerializeCObject
(JNIEnv *env, jobject thisObj, jlong modelPtr)
{
    SerializationIface *model = ((SerializationIfacePtr *)modelPtr)->get();
    InputDataArchive dataArch;
    model->serialize(dataArch);

    size_t length = dataArch.getSizeOfArchive();

    byte *buffer = (byte *)daal_malloc(length);
    dataArch.copyArchiveToArray(buffer, length);

    return env->NewDirectByteBuffer(buffer, length);
}

/*
 * Class:     com_intel_daal_algorithms_Model
 * Method:    cFreeByteBuffer
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_Model_cFreeByteBuffer
(JNIEnv *env, jobject thisObj, jobject byteBuffer)
{
    byte *buffer = (byte *)(env->GetDirectBufferAddress(byteBuffer));
    daal_free(buffer);
}

/*
 * Class:     com_intel_dal_algorithms_Model
 * Method:    cDispose
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_Model_cDispose
(JNIEnv *env, jobject thisObj, jlong modelAddr)
{
    algorithms::ModelPtr *modelShPtr = (algorithms::ModelPtr *)modelAddr;
    delete modelShPtr;
}
