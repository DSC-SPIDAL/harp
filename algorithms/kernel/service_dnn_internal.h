/* file: service_dnn_internal.h */
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

/*
//++
//  DNN service functions
//--
*/

#ifndef __SERVICE_DNN_INTERNAL_H__
#define __SERVICE_DNN_INTERNAL_H__

#include "service_defines.h"
#include "service_dnn.h"

#define ON_ERR(err)                                                                        \
{                                                                                          \
    if ((err) != E_SUCCESS)                                                                \
    {                                                                                      \
        if ((err) == E_MEMORY_ERROR)                                                       \
        { return services::Status(services::ErrorMemoryAllocationFailed);  }               \
        return  services::Status(services::ErrorConvolutionInternal);                      \
    }                                                                                      \
}

namespace daal
{
namespace internal
{

template<typename algorithmFPType, CpuType cpu>
class DnnLayout
{
public:
    typedef Dnn<algorithmFPType, cpu> dnn;

    DnnLayout() : layout(NULL), err(E_SUCCESS) {}

    DnnLayout(size_t dim, size_t *size, size_t *strides) : layout(NULL), err(E_SUCCESS)
    {
        err = dnn::xLayoutCreate(&layout, dim, size, strides);
    }

    DnnLayout(dnnPrimitive_t primitive, dnnResourceType_t resource) : layout(NULL), err(E_SUCCESS)
    {
        err = dnn::xLayoutCreateFromPrimitive(&layout, primitive, resource);
    }

    DnnLayout& operator=(DnnLayout&& source)
    {
        free();
        layout = source.layout;
        source.layout = NULL;
        return *this;
    }

    ~DnnLayout()
    {
        free();
    }

    void free()
    {
        if( layout != NULL )
        {
            dnn::xLayoutDelete(layout);
        }
    }

    dnnLayout_t& get() { return layout; }

    dnnError_t err;

protected:
    dnnLayout_t layout;
};

template<typename algorithmFPType, CpuType cpu>
class DnnBuffer
{
public:
    typedef Dnn<algorithmFPType, cpu> dnn;

    DnnBuffer(): buffer(0)
    {
    }

    DnnBuffer(dnnLayout_t layout)
    {
        err = dnn::xAllocateBuffer((void**)&buffer, layout);
    }

    ~DnnBuffer()
    {
        if (buffer)
        {
            dnn::xReleaseBuffer(buffer);
        }
    }

    algorithmFPType* get() { return buffer; }

    algorithmFPType* allocate(dnnLayout_t layout)
    {
        err = dnn::xAllocateBuffer((void**)&buffer, layout);
        return buffer;
    }

    dnnError_t err;
protected:
    algorithmFPType *buffer;
};

}
}

#endif
