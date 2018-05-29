/* file: kernel.h */
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
//  Defines used for kernel allocation, deallocation and calling kernel methods
//--
*/

#ifndef __KERNEL_H__
#define __KERNEL_H__

#include "daal_defines.h"
#include "service_defines.h"
#include "services/daal_kernel_defines.h"

#undef __DAAL_INITIALIZE_KERNELS
#define __DAAL_INITIALIZE_KERNELS(KernelClass, ...)        \
    {                                                      \
        _kernel = (new KernelClass<__VA_ARGS__, cpu>);     \
    }

#undef __DAAL_DEINITIALIZE_KERNELS
#define __DAAL_DEINITIALIZE_KERNELS()    \
    {                                    \
        if(_kernel) delete _kernel;      \
    }

#undef __DAAL_KERNEL_ARGUMENTS
#define __DAAL_KERNEL_ARGUMENTS(...) __VA_ARGS__

#undef __DAAL_CALL_KERNEL
#define __DAAL_CALL_KERNEL(env, KernelClass, templateArguments, method, ...)            \
    {                                                                                   \
        return ((KernelClass<templateArguments, cpu> *)(_kernel))->method(__VA_ARGS__); \
    }

#undef __DAAL_CALL_KERNEL_STATUS
#define __DAAL_CALL_KERNEL_STATUS(env, KernelClass, templateArguments, method, ...) \
        ((KernelClass<templateArguments, cpu> *)(_kernel))->method(__VA_ARGS__);

#define __DAAL_INSTANTIATE_DISPATCH_IMPL(ContainerTemplate, Mode, ClassName, BaseClassName, ...) \
template<>                                                                                       \
    ClassName< Mode,                                                                             \
    ContainerTemplate<__VA_ARGS__, sse2>                                                         \
    DAAL_KERNEL_SSSE3_CONTAINER(ContainerTemplate, __VA_ARGS__)                                  \
    DAAL_KERNEL_SSE42_CONTAINER(ContainerTemplate, __VA_ARGS__)                                  \
    DAAL_KERNEL_AVX_CONTAINER(ContainerTemplate, __VA_ARGS__)                                    \
    DAAL_KERNEL_AVX2_CONTAINER(ContainerTemplate, __VA_ARGS__)                                   \
    DAAL_KERNEL_AVX512_mic_CONTAINER(ContainerTemplate, __VA_ARGS__)                             \
    DAAL_KERNEL_AVX512_CONTAINER(ContainerTemplate, __VA_ARGS__) >::                             \
    ClassName(daal::services::Environment::env *daalEnv) : BaseClassName(daalEnv), _cntr(NULL)   \
{                                                                                                \
    switch (daalEnv->cpuid)                                                                      \
    {                                                                                            \
        DAAL_KERNEL_SSSE3_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                         \
        DAAL_KERNEL_SSE42_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                         \
        DAAL_KERNEL_AVX_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                           \
        DAAL_KERNEL_AVX2_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                          \
        DAAL_KERNEL_AVX512_mic_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                    \
        DAAL_KERNEL_AVX512_CONTAINER_CASE(ContainerTemplate, __VA_ARGS__)                        \
        default: _cntr = (new ContainerTemplate<__VA_ARGS__, sse2> (daalEnv)); break;            \
    }                                                                                            \
}                                                                                                \
                                                                                                 \
template                                                                                         \
class ClassName< Mode,                                                                           \
    ContainerTemplate<__VA_ARGS__, sse2>                                                         \
    DAAL_KERNEL_SSSE3_CONTAINER(ContainerTemplate, __VA_ARGS__)                                  \
    DAAL_KERNEL_SSE42_CONTAINER(ContainerTemplate, __VA_ARGS__)                                  \
    DAAL_KERNEL_AVX_CONTAINER(ContainerTemplate, __VA_ARGS__)                                    \
    DAAL_KERNEL_AVX2_CONTAINER(ContainerTemplate, __VA_ARGS__)                                   \
    DAAL_KERNEL_AVX512_mic_CONTAINER(ContainerTemplate, __VA_ARGS__)                             \
    DAAL_KERNEL_AVX512_CONTAINER(ContainerTemplate, __VA_ARGS__)>;

#define __DAAL_INSTANTIATE_DISPATCH_LAYER_CONTAINER(ContainerTemplate,  ...) \
        __DAAL_INSTANTIATE_DISPATCH_IMPL(ContainerTemplate, batch, AlgorithmDispatchLayerContainer, LayerContainerIfaceImpl, __VA_ARGS__)

#define __DAAL_INSTANTIATE_DISPATCH_CONTAINER(ContainerTemplate, Mode, ...) \
        __DAAL_INSTANTIATE_DISPATCH_IMPL(ContainerTemplate, Mode, AlgorithmDispatchContainer, AlgorithmContainerImpl<Mode>, __VA_ARGS__)

#endif
