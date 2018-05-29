/* file: service_memory.cpp */
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
//  Implementation of memory service functions
//--
*/

#include "service_memory.h"
#include "service_service.h"

void *daal::services::daal_malloc(size_t size, size_t alignment)
{
    return daal::internal::Service<>::serv_malloc(size, alignment);
}

void daal::services::daal_free(void *ptr)
{
    daal::internal::Service<>::serv_free(ptr);
}

namespace daal
{
namespace services
{
void daal_free_buffers()
{
    daal::internal::Service<>::serv_free_buffers();
}
}
}

void daal::services::daal_memcpy_s(void *dest, size_t destSize, const void *src, size_t srcSize)
{
    size_t copySize = srcSize;
    if(destSize < srcSize) {copySize = destSize;}

    size_t BLOCKSIZE = 200000000; // approx 200MB
    size_t nBlocks = copySize / BLOCKSIZE;
    size_t sizeOfLastBlock = 0;
    if(nBlocks * BLOCKSIZE != copySize) {sizeOfLastBlock = copySize - (nBlocks * BLOCKSIZE);}

    char* dstChar = (char*)dest;
    char* srcChar = (char*)src;
    for(size_t i = 0; i < nBlocks; i++)
    {
        daal::internal::Service<>::serv_memcpy_s(&dstChar[i * BLOCKSIZE], BLOCKSIZE, &srcChar[i * BLOCKSIZE], BLOCKSIZE);
    }
    if(sizeOfLastBlock != 0)
    {
        daal::internal::Service<>::serv_memcpy_s(&dstChar[nBlocks * BLOCKSIZE], sizeOfLastBlock, &srcChar[nBlocks * BLOCKSIZE], sizeOfLastBlock);
    }
}
