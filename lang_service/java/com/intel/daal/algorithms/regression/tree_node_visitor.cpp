/* file: tree_node_visitor.cpp */
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
#include "common_helpers_functions.h"
#include "tree_node_visitor.h"

namespace daal
{
namespace regression
{

bool JavaTreeNodeVisitor::onLeafNode(size_t level, double response)
{
    ThreadLocalStorage tls = _tls.local();
    jint status = jvm->AttachCurrentThread((void **)(&tls.jniEnv), NULL);
    JNIEnv *env = tls.jniEnv;

    /* Get current context */
    jclass javaObjectClass = env->GetObjectClass(javaObject);
    if(javaObjectClass == NULL)
        throwError(env, "Couldn't find class of this java object");

    jmethodID methodID = env->GetMethodID(javaObjectClass, "onLeafNode", "(JD)Z");
    if(methodID == NULL)
        throwError(env, "Couldn't find onLeafNode method");

    jboolean val = env->CallBooleanMethod(javaObject, methodID, (jlong)level, (jdouble)response);

    if(!tls.is_main_thread)
        status = jvm->DetachCurrentThread();
    _tls.local() = tls;
    return val != 0;
}

bool JavaTreeNodeVisitor::onSplitNode(size_t level, size_t featureIndex, double featureValue)
{
    ThreadLocalStorage tls = _tls.local();
    jint status = jvm->AttachCurrentThread((void **)(&tls.jniEnv), NULL);
    JNIEnv *env = tls.jniEnv;

    /* Get current context */
    jclass javaObjectClass = env->GetObjectClass(javaObject);
    if(javaObjectClass == NULL)
        throwError(env, "Couldn't find class of this java object");

    jmethodID methodID = env->GetMethodID(javaObjectClass, "onSplitNode", "(JJD)Z");
    if(methodID == NULL)
        throwError(env, "Couldn't find onSplitNode method");
    jboolean val = env->CallBooleanMethod(javaObject, methodID, (jlong)level, (jlong)featureIndex, (jdouble)featureValue);

    if(!tls.is_main_thread)
        status = jvm->DetachCurrentThread();
    _tls.local() = tls;
    return val != 0;
}

}//namespace
}//namespace
