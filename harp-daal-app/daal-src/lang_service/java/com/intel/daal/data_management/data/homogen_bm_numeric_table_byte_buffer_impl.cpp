/* file: homogen_numeric_table_byte_buffer_impl.cpp */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

#include <jni.h>
#include <cstring>

#include "JHomogenBMNumericTableByteBufferImpl.h"
#include "numeric_table.h"
#include "homogen_bm_numeric_table.h"

using namespace daal;
using namespace daal::data_management;

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getIndexType
 * Signature:(J)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getIndexType
(JNIEnv *env, jobject thisobj, jlong numTableAddr)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    NumericTableDictionary *dict = nt->getDictionary();
    return(jint)((*dict)[0].indexType);
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    dInit
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_dInit
(JNIEnv *env, jobject thisobj, jlong nColumns)
{
    HomogenBMNumericTable<double> *tbl = new HomogenBMNumericTable<double>(NULL, nColumns, 0);
    SerializationIfacePtr *sPtr = new SerializationIfacePtr(tbl);

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
    return (jlong)sPtr;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    sInit
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_sInit
(JNIEnv *env, jobject thisobj, jlong nColumns)
{
    HomogenBMNumericTable<float> *tbl = new HomogenBMNumericTable<float>(NULL, nColumns, 0);
    SerializationIfacePtr *sPtr = new SerializationIfacePtr(tbl);
    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
    return (jlong)sPtr;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    lInit
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_lInit
(JNIEnv *env, jobject thisobj, jlong nColumns)
{
    HomogenBMNumericTable<__int64> *tbl = new HomogenBMNumericTable<__int64>(NULL, nColumns, 0);
    SerializationIfacePtr *sPtr = new SerializationIfacePtr(tbl);
    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
    return (jlong)sPtr;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    iInit
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_iInit
(JNIEnv *env, jobject thisobj, jlong nColumns)
{
    HomogenBMNumericTable<int> *tbl = new HomogenBMNumericTable<int>(NULL, nColumns, 0);
    SerializationIfacePtr *sPtr = new SerializationIfacePtr(tbl);
    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
    return (jlong)sPtr;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getDoubleBuffer
 * Signature:(J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getDoubleBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    HomogenBMNumericTable<double> *nt = static_cast<HomogenBMNumericTable<double> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();
    size_t nCols = nt->getNumberOfColumns();
    double *data = nt->getArray();

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    jobject byteBuffer = env->NewDirectByteBuffer(data, (jlong)(nRows * nCols * sizeof(double)));
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getFloatBuffer
 * Signature:(J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getFloatBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    HomogenBMNumericTable<float> *nt = static_cast<HomogenBMNumericTable<float> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();
    size_t nCols = nt->getNumberOfColumns();
    float *data = nt->getArray();

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    jobject byteBuffer = env->NewDirectByteBuffer(data, (jlong)(nRows * nCols * sizeof(float)));
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getLongBuffer
 * Signature:(J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getLongBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    HomogenBMNumericTable<__int64> *nt = static_cast<HomogenBMNumericTable<__int64> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();
    size_t nCols = nt->getNumberOfColumns();
    __int64 *data = nt->getArray();

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    jobject byteBuffer = env->NewDirectByteBuffer(data, (jlong)(nRows * nCols * sizeof(__int64)));
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getIntBuffer
 * Signature:(J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getIntBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    HomogenBMNumericTable<int> *nt = static_cast<HomogenBMNumericTable<int> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());

    size_t nRows = nt->getNumberOfRows();
    size_t nCols = nt->getNumberOfColumns();
    int *data = nt->getArray();

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    jobject byteBuffer = env->NewDirectByteBuffer(data, (jlong)(nRows * nCols * sizeof(int)));
    return byteBuffer;
}



/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseDoubleBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseDoubleBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, writeOnly, block);
    double *data = block.getBlockPtr();

    double *src = (double *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*nCols*sizeof(double));

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseFloatBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseFloatBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<float> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, writeOnly, block);
    float* data = block.getBlockPtr();

    float *src = (float *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*nCols*sizeof(float));

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseIntBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseIntBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, writeOnly, block);
    int* data = block.getBlockPtr();

    int *src = (int *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*nCols*sizeof(int));

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getDoubleBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getDoubleBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, readOnly, block);
    double *data = block.getBlockPtr();

    double *dst = (double *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*nCols*sizeof(double));

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfRows(block);
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getFloatBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getFloatBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<float> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, readOnly, block);
    float *data = block.getBlockPtr();

    float *dst = (float *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*nCols*sizeof(float));

    // for(size_t i = 0; i < vectorNum * nCols; i++)
    // {
    //     dst[i] = data[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfRows(block);
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getIntBlockBuffer
 * Signature:(JJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getIntBlockBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, readOnly, block);
    int *data = block.getBlockPtr();

    int *dst = (int *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*nCols*sizeof(int));

    // for(size_t i = 0; i < vectorNum * nCols; i++)
    // {
    //     dst[i] = data[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfRows(block);

    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getDoubleColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getDoubleColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, readOnly, block);
    double *data = block.getBlockPtr();

    double *dst = (double *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*sizeof(double));
    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     dst[i] = data[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getFloatColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getFloatColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<float> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, readOnly, block);
    float *data = block.getBlockPtr();

    float *dst = (float *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*sizeof(float));

    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     dst[i] = data[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getIntColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getIntColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, readOnly, block);
    int *data = block.getBlockPtr();

    int *dst = (int *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(dst, data, vectorNum*sizeof(int));

    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     dst[i] = data[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
    return byteBuffer;
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseFloatColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseFloatColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<float> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, writeOnly, block);
    float* data = block.getBlockPtr();

    float *src = (float *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*sizeof(float));

    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     data[i] = src[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseDoubleColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseDoubleColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, writeOnly, block);
    double *data = block.getBlockPtr();

    double *src = (double *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*sizeof(double));

    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     data[i] = src[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
}

/*
 * Class:     com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    releaseIntColumnBuffer
 * Signature:(JJJJLjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_releaseIntColumnBuffer
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong featureIndex, jlong vectorIndex, jlong vectorNum, jobject byteBuffer)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfColumnValues(featureIndex, vectorIndex, vectorNum, writeOnly, block);
    int* data = block.getBlockPtr();

    int *src = (int *)(env->GetDirectBufferAddress(byteBuffer));

    std::memcpy(data, src, vectorNum*sizeof(int));

    // for(size_t i = 0; i < vectorNum; i++)
    // {
    //     data[i] = src[i];
    // }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    // nt->releaseBlockOfColumnValues(block);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    assignLong
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_assignLong
(JNIEnv *env, jobject, jlong numTableAddr, jlong constValue)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    size_t nRows = nt->getNumberOfRows();

    nt->getBlockOfRows(0, nRows, readWrite, block);
    int *data = block.getBlockPtr();

    for(size_t i = 0; i < nRows * nCols; i++)
    {
        data[i] = (int)constValue;
    }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    assignInt
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_assignInt
(JNIEnv *env, jobject, jlong numTableAddr, jint constValue)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<int> block;

    size_t nCols = nt->getNumberOfColumns();
    size_t nRows = nt->getNumberOfRows();

    nt->getBlockOfRows(0, nRows, readWrite, block);
    int *data = block.getBlockPtr();

    for(size_t i = 0; i < nRows * nCols; i++)
    {
        data[i] = (int)constValue;
    }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    assignDouble
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_assignDouble
(JNIEnv *env, jobject, jlong numTableAddr, jdouble constValue)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    size_t nRows = nt->getNumberOfRows();

    nt->getBlockOfRows(0, nRows, readWrite, block);
    double *data = block.getBlockPtr();

    for(size_t i = 0; i < nRows * nCols; i++)
    {
        data[i] = (double)constValue;
    }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    assignFloat
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_assignFloat
(JNIEnv *env, jobject, jlong numTableAddr, jfloat constValue)
{
    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<float> block;

    size_t nCols = nt->getNumberOfColumns();
    size_t nRows = nt->getNumberOfRows();

    nt->getBlockOfRows(0, nRows, readWrite, block);
    float *data = block.getBlockPtr();

    for(size_t i = 0; i < nRows * nCols; i++)
    {
        data[i] = (float)constValue;
    }

    if(nt->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), nt->getErrors()->getDescription());
    }

    nt->releaseBlockOfRows(block);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    set
 * Signature: (JJD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_set
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column, jdouble value)
{
    HomogenBMNumericTable<double> *nt = static_cast<HomogenBMNumericTable<double> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    (*nt)[row][column] = (double)value;
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    set
 * Signature: (JJF)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_set
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column, jfloat value)
{
    HomogenBMNumericTable<float> *nt = static_cast<HomogenBMNumericTable<float> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    (*nt)[row][column] = (float)value;
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    set
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_set
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column, jlong value)
{
    HomogenBMNumericTable<long> *nt = static_cast<HomogenBMNumericTable<long> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    (*nt)[row][column] = (long)value;
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    set
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_set
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column, jint value)
{
    HomogenBMNumericTable<int> *nt = static_cast<HomogenBMNumericTable<int> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    (*nt)[row][column] = (int)value;
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getDouble
 * Signature: (JJ)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getDouble
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column)
{
    HomogenBMNumericTable<double> *nt = static_cast<HomogenBMNumericTable<double> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    return (jdouble)((*nt)[row][column]);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getFloat
 * Signature: (JJ)F
 */
JNIEXPORT jfloat JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getFloat
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column)
{
    HomogenBMNumericTable<float> *nt = static_cast<HomogenBMNumericTable<float> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    return (jfloat)((*nt)[row][column]);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getLong
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getLong
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column, jlong value)
{
    HomogenBMNumericTable<long> *nt = static_cast<HomogenBMNumericTable<long> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    return (jlong)((*nt)[row][column]);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    getInt
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getInt
(JNIEnv *env, jobject, jlong numTableAddr, jlong row, jlong column)
{
    HomogenBMNumericTable<int> *nt = static_cast<HomogenBMNumericTable<int> *>(
            ((SerializationIfacePtr *)numTableAddr)->get());
    return (jint)((*nt)[row][column]);
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    cAllocateDataMemoryDouble
 * Signature:(J)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_cAllocateDataMemoryDouble
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)numTableAddr)->get();

    ((HomogenBMNumericTable<double> *)tbl)->allocateDataMemory();

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    cAllocateDataMemoryFloat
 * Signature:(J)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_cAllocateDataMemoryFloat
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)numTableAddr)->get();

    ((HomogenBMNumericTable<float> *)tbl)->allocateDataMemory();

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    cAllocateDataMemoryLong
 * Signature:(J)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_cAllocateDataMemoryLong
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)numTableAddr)->get();

    ((HomogenBMNumericTable<long> *)tbl)->allocateDataMemory();

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
}

/*
 * Class:     com_intel_daal_data_management_data_HomogenBMNumericTableByteBufferImpl
 * Method:    cAllocateDataMemoryInt
 * Signature:(J)J
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_cAllocateDataMemoryInt
(JNIEnv *env, jobject thisObj, jlong numTableAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)numTableAddr)->get();

    ((HomogenBMNumericTable<int> *)tbl)->allocateDataMemory();

    if(tbl->getErrors()->size() > 0)
    {
        env->ThrowNew(env->FindClass("java/lang/Exception"), tbl->getErrors()->getDescription());
    }
}

//langshi added not being tested
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_HomogenBMNumericTableByteBufferImpl_getNumericTableAddr
(JNIEnv *env, jobject thisObj, jlong numTableAddr, jlong vectorIndex, jlong vectorNum)
{

    NumericTable *nt = static_cast<NumericTable *>(((SerializationIfacePtr *)numTableAddr)->get());
    BlockDescriptor<double> block;

    size_t nCols = nt->getNumberOfColumns();
    nt->getBlockOfRows(vectorIndex, vectorNum, writeOnly, block);
    double *data = block.getBlockPtr();

    return reinterpret_cast<jlong>(&(data[0]));
}
