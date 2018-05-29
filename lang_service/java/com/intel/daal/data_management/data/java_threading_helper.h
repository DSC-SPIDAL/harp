/* file: java_threading_helper.h */
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

#ifndef __JAVA_THREADING_HELPER_H__
#define __JAVA_THREADING_HELPER_H__

#include <jni.h>
#include <tbb/tbb.h>

#include "services/error_handling.h"

namespace daal
{

namespace internal
{

struct _java_tls
{
    JNIEnv *jenv;    // JNI interface poiner
    jobject jbuf;
    jclass jcls;     // Java class associated with this C++ object
    bool is_main_thread;
    bool is_attached;
    /* Default constructor */
    _java_tls()
    {
        jenv = NULL;
        jbuf = NULL;
        jcls = NULL;
        is_main_thread = false;
        is_attached = false;
    }
};

static services::Status attachCurrentThread(JavaVM *jvm, _java_tls &local_tls)
{
    if (!local_tls.is_attached)
    {
        jint status = jvm->AttachCurrentThread((void **)(&(local_tls.jenv)), NULL);
        if(status == JNI_OK)
        {
            local_tls.is_attached = true;
        }
        else
        {
            return services::Status(services::ErrorCouldntAttachCurrentThreadToJavaVM);
        }
    }
    return services::Status();
}

static services::Status detachCurrentThread(JavaVM *jvm, _java_tls &local_tls, bool detach_main_thread = false)
{
    if (local_tls.is_attached)
    {
        if(!local_tls.is_main_thread || detach_main_thread)
        {
            jint status = jvm->DetachCurrentThread();
            if (status == JNI_OK)
            {
                local_tls.is_attached = false;
            }
        }
    }
    return services::Status();
}

} // namespace internal

} // namespace daal

#endif
