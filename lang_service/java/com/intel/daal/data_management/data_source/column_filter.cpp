/* file: column_filter.cpp */
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
#include "JColumnFilter.h"

#include "csv_feature_manager.h"
#include "data_collection.h"

using namespace daal;
using namespace daal::data_management;

/*
 * Class:     com_intel_daal_data_1management_data_1source_ColumnFilter
 * Method:    cInit
 * Signature:(J)J
 */
JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_1source_ColumnFilter_cInit
(JNIEnv *env, jobject obj)
{
    services::SharedPtr<ModifierIface>* ptr = new services::SharedPtr<ModifierIface>(new ColumnFilter());
    return (jlong)ptr;
}

/*
 * Class:     com_intel_daal_data_1management_data_1source_ColumnFilter
 * Method:    cOdd
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_1source_ColumnFilter_cOdd
(JNIEnv *env, jobject obj, jlong ptr)
{
    services::SharedPtr<ColumnFilter> columnFilter =
        services::staticPointerCast<ColumnFilter, ModifierIface>(
            (*(services::SharedPtr<ModifierIface> *)ptr));
    columnFilter->odd();
}

/*
 * Class:     com_intel_daal_data_1management_data_1source_ColumnFilter
 * Method:    cEven
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_1source_ColumnFilter_cEven
(JNIEnv *env, jobject obj, jlong ptr)
{
    services::SharedPtr<ColumnFilter> columnFilter =
        services::staticPointerCast<ColumnFilter, ModifierIface>(
            (*(services::SharedPtr<ModifierIface> *)ptr));
    columnFilter->even();
}

/*
 * Class:     com_intel_daal_data_1management_data_1source_ColumnFilter
 * Method:    cNone
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_1source_ColumnFilter_cNone
(JNIEnv *env, jobject obj, jlong ptr)
{
    services::SharedPtr<ColumnFilter> columnFilter =
        services::staticPointerCast<ColumnFilter, ModifierIface>(
            (*(services::SharedPtr<ModifierIface> *)ptr));
    columnFilter->none();
}

/*
 * Class:     com_intel_daal_data_1management_data_1source_ColumnFilter
 * Method:    cList
 * Signature:(JJ)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_1source_ColumnFilter_cList
(JNIEnv *env, jobject obj, jlong ptr, jlongArray valid)
{
    services::SharedPtr<ColumnFilter> columnFilter =
        services::staticPointerCast<ColumnFilter, ModifierIface>(
            (*(services::SharedPtr<ModifierIface> *)ptr));
    size_t n = env->GetArrayLength(valid);
    jlong* arr = env->GetLongArrayElements(valid, 0);
    services::Collection<size_t> collection(n);
    for (int i = 0; i < n; i++)
    {
        collection[i] = (size_t)arr[i];
    }
    columnFilter->list(collection);
    env->ReleaseLongArrayElements(valid, arr, JNI_ABORT);
}
