/* file: compression_stream.cpp */
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

#include "JCompressionStream.h"
#include "daal.h"

using namespace daal;
using namespace daal::data_management;


/*
 * Class:     com_intel_daal_data_1management_compression_Compression
 * Method:    cDispose
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_compression_CompressionStream_cDispose
(JNIEnv *env, jobject, jlong strAddr)
{
    delete(CompressionStream *)strAddr;
}

/*
 * Class:     com_intel_daal_data_1management_compression_Compression
 * Method:    cDispose
 * Signature:(J)V
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_compression_CompressionStream_cInit
(JNIEnv *env, jobject, jlong comprAddr, jlong minSize)
{
    jlong strmAddr = 0;
    strmAddr = (jlong)(new CompressionStream((CompressorImpl *)comprAddr, minSize));

    if(((CompressionStream *)strmAddr)->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"),
                      ((CompressionStream *)strmAddr)->getErrors()->getDescription());
    }

    return strmAddr;
}

/*
 * Class:     com_intel_daal_data_1management_compression_Compression
 * Method:    cSetInputDataBlock
 * Signature:(J[BJJ)I
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_compression_CompressionStream_cAdd
(JNIEnv *env, jobject, jlong strmAddr, jbyteArray inBlock, jlong size)
{
    jbyte *inBuffer = env->GetByteArrayElements(inBlock, 0);

    DataBlock tmp((byte *)inBuffer, (size_t)size);

    ((CompressionStream *)strmAddr)->push_back(&tmp);

    if(((CompressionStream *)strmAddr)->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"),
                      ((CompressionStream *)strmAddr)->getErrors()->getDescription());
    }
}

/*
 * Class:     com_intel_daal_data_1management_compression_Compression
 * Method:    cIsOutputDataBlockFull
 * Signature:(J)Z
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_compression_CompressionStream_cGetCompressedDataSize
(JNIEnv *env, jobject, jlong strmAddr)
{
    if(((CompressionStream *)strmAddr)->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"),
                      ((CompressionStream *)strmAddr)->getErrors()->getDescription());
    }

    return(jlong)((CompressionStream *)strmAddr)->getCompressedDataSize();
}

/*
 * Class:     com_intel_daal_data_1management_compression_Compression
 * Method:    cRun
 * Signature:(J[BJJ)I
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_compression_CompressionStream_cCopyCompressedArray
(JNIEnv *env, jobject, jlong strmAddr, jbyteArray outBlock, jlong chunkSize)
{
    jbyte *outBuffer = env->GetByteArrayElements(outBlock, 0);
    jlong result = (jlong)((CompressionStream *)strmAddr)->copyCompressedArray((byte *)outBuffer, (size_t)chunkSize);
    env->ReleaseByteArrayElements(outBlock, outBuffer, 0);

    if(((CompressionStream *)strmAddr)->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"),
                      ((CompressionStream *)strmAddr)->getErrors()->getDescription());
    }

    return result;
}
