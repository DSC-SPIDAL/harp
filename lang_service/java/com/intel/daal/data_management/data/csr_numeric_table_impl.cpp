/* file: csr_numeric_table_impl.cpp */
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

/*
//++
//  Implementation of the JNI layer for Java CSR Numeric Table
//--
*/

#include <jni.h>

#include "JCSRNumericTableImpl.h"
#include "java_csr_numeric_table.h"
#include "daal.h"
#include "common_helpers_functions.h"

using namespace daal;
using namespace daal::data_management;

/*
 * Class:     com_intel_daal_data_1management_data_CSRNumericTableImpl
 * Method:    initCSRNumericTable
 * Signature:(JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_initCSRNumericTable
(JNIEnv *env, jobject thisObj, jlong nFeatures, jlong nVectors)
{
    JavaVM *jvm;
    // Get pointer to the Java VM interface function table
    jint status = env->GetJavaVM(&jvm);
    if(status != 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Error: Couldn't get Java VM");
        return 0;
    }

    // Create C++ object of the class JavaNumericTable
    JavaCSRNumericTable *tbl = new JavaCSRNumericTable((size_t)nFeatures, (size_t)nVectors, jvm, thisObj);

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }

    return (jlong)new SerializationIfacePtr(tbl);
}

/*
 * Class:     com_intel_daal_data_1management_data_CSRNumericTableImpl
 * Method:    cGetNumberOfRows
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_cGetNumberOfRows
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    return(jint)(nt->getNumberOfRows());
}

/*
 * Class:     com_intel_daal_data_1management_data_CSRNumericTableImpl
 * Method:    cGetDataSize
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_cGetDataSize
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    return(jint)(nt->getDataSize());
}

/*
 * Class:     com_intel_daal_data_1management_data_CSRNumericTableImpl
 * Method:    getIndexType
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getIndexType
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    NumericTableDictionary *dict = nt->getDictionary();
    return(jint)((*dict)[0].indexType);
}

/*
 * Class:     com_intel_daal_data_management_data_CSRNumericTableImpl
 * Method:    getColIndicesBuffer
 * Signature: (JLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getColIndicesBuffer
(JNIEnv *env, jobject thisobj, jlong numTableAddr, jobject byteBuffer)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    size_t dataSize = nt->getDataSize();
    double *ptr;
    size_t *colIndices;
    size_t *rowOffsets;
    nt->getArrays<double>(&ptr, &colIndices, &rowOffsets); //template parameter doesn't matter

    __int64 *dest = (__int64 *)(env->GetDirectBufferAddress(byteBuffer));

    for(size_t i = 0; i < dataSize; i++)
    {
        dest[i] = colIndices[i];
    }
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_management_data_CSRNumericTableImpl
 * Method:    getRowOffsetsBuffer
 * Signature: (JLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getRowOffsetsBuffer
(JNIEnv *env, jobject thisobj, jlong numTableAddr, jobject byteBuffer)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();
    double *ptr;
    size_t *colIndices;
    size_t *rowOffsets;
    nt->getArrays<double>(&ptr, &colIndices, &rowOffsets);//template parameter doesn't matter

    __int64 *dest = (__int64 *)(env->GetDirectBufferAddress(byteBuffer));

    for(size_t i = 0; i < nRows; i++)
    {
        dest[i] = rowOffsets[i];
    }
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_management_data_CSRNumericTableImpl
 * Method:    getDoubleBuffer
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getDoubleBuffer
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();

    double *ptr;
    size_t *column_indices;
    size_t *row_offsets;

    nt->getArrays<double>(&ptr, &column_indices, &row_offsets);

    size_t dataSize = nt->getDataSize();
    jobject byteBuffer = env->NewDirectByteBuffer(ptr, (jlong)(dataSize * sizeof(double)));
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_management_data_CSRNumericTableImpl
 * Method:    getFloatBuffer
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getFloatBuffer
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();

    float *ptr;
    size_t *column_indices;
    size_t *row_offsets;

    nt->getArrays<float>(&ptr, &column_indices, &row_offsets);

    size_t dataSize = nt->getDataSize();
    jobject byteBuffer = env->NewDirectByteBuffer(ptr, (jlong)(dataSize * sizeof(float)));
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_management_data_CSRNumericTableImpl
 * Method:    getLongBuffer
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_CSRNumericTableImpl_getLongBuffer
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    CSRNumericTable *nt = static_cast<CSRNumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();

    int *ptr;
    size_t *column_indices;
    size_t *row_offsets;

    nt->getArrays<int>(&ptr, &column_indices, &row_offsets);

    size_t dataSize = nt->getDataSize();
    jobject byteBuffer = env->NewDirectByteBuffer(ptr, (jlong)(dataSize * sizeof(int)));
    return byteBuffer;
}
