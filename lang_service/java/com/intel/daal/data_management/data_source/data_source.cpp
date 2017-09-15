/* file: data_source.cpp */
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

#include "JDataSource.h"

#include "numeric_table.h"
#include "data_source.h"

using namespace daal;
using namespace daal::data_management;

#define NO_PARAMS_FUNCTION_MAP_0ARG(jType,jFunc,cFunc)                                         \
    JNIEXPORT jType JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_##jFunc \
    (JNIEnv *env, jobject obj, jlong ptr)                                                      \
    {                                                                                          \
        return(jType)((DataSource *)ptr)->cFunc();                                             \
    }                                                                                          \

#define NO_PARAMS_FUNCTION_MAP_1ARG(jType,jFunc,cFunc,arg1,jArg1type,cArg1type)                \
    JNIEXPORT jType JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_##jFunc \
    (JNIEnv *env, jobject obj, jlong ptr, jArg1type arg1)                                      \
    {                                                                                          \
        return(jType)((DataSource *)ptr)->cFunc((cArg1type)arg1);                              \
    }                                                                                          \

#define NO_PARAMS_FUNCTION_MAP_2ARG(jType,jFunc,cFunc,arg1,jArg1type,cArg1type,arg2,jArg2type,cArg2type)   \
    JNIEXPORT jType JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_##jFunc             \
    (JNIEnv *env, jobject obj, jlong ptr, jArg1type arg1, jArg2type arg2)                                  \
    {                                                                                                      \
        return(jType)((DataSource *)ptr)->cFunc((cArg1type)arg1, (cArg2type)arg2);                         \
    }                                                                                                      \

NO_PARAMS_FUNCTION_MAP_0ARG(void, cCreateDictionaryFromContext, createDictionaryFromContext);
NO_PARAMS_FUNCTION_MAP_0ARG(jlong, cGetNumberOfColumns,          getNumberOfColumns         );
NO_PARAMS_FUNCTION_MAP_0ARG(jlong, cGetNumberOfAvailableRows,    getNumberOfAvailableRows   );
NO_PARAMS_FUNCTION_MAP_0ARG(void, cAllocateNumericTable,        allocateNumericTable       );
//NO_PARAMS_FUNCTION_MAP_0ARG(jlong,cGetNumericTable,             getNumericTable            );
NO_PARAMS_FUNCTION_MAP_0ARG(void, cFreeNumericTable,            freeNumericTable           );

NO_PARAMS_FUNCTION_MAP_0ARG(jlong, cLoadDataBlock0Inputs,      loadDataBlock);
NO_PARAMS_FUNCTION_MAP_1ARG(jlong, cLoadDataBlock,      loadDataBlock,    maxRows, jlong, size_t);

/*
 * Class:     com_intel_daal_data_1management_data_1source_DataSource
 * Method:    cDispose
 * Signature:(J)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_cDispose
(JNIEnv *env, jobject obj, jlong ptr)
{
    delete(DataSource *)ptr;
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_cGetNumericTable(JNIEnv *env, jobject obj, jlong ptr)
{
    NumericTablePtr *spnt = new NumericTablePtr();
    *spnt = ((DataSource *)ptr)->getNumericTable();

    return (jlong)((SerializationIfacePtr *)spnt);
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_cLoadDataBlock3Inputs
(JNIEnv *env, jobject obj, jlong ptr, jlong maxRows, jlong offset, jlong fullRows)
{
    return(jlong)((DataSource *)ptr)->loadDataBlock((size_t)maxRows, (size_t)offset, (size_t)fullRows);
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_cLoadDataBlockNt
(JNIEnv *env, jobject obj, jlong ptr, jlong maxRows, jlong ntAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)ntAddr)->get();
    return(jlong)((DataSource *)ptr)->loadDataBlock((size_t)maxRows, tbl);
}

JNIEXPORT jlong JNICALL Java_com_intel_daal_data_1management_data_1source_DataSource_cLoadDataBlockNt1Input
(JNIEnv *env, jobject obj, jlong ptr, jlong ntAddr)
{
    NumericTable *tbl = ((NumericTablePtr *)ntAddr)->get();
    return(jlong)((DataSource *)ptr)->loadDataBlock(tbl);
}
