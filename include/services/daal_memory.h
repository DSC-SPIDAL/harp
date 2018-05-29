/* file: daal_memory.h */
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
//  Common definitions.
//--
*/

#ifndef __DAAL_MEMORY_H__
#define __DAAL_MEMORY_H__

#include "services/daal_defines.h"

namespace daal
{
/**
 * \brief Contains classes that implement service functionality, including error handling,
 * memory allocation, and library version information
 */
namespace services
{
/**
 * @ingroup memory
 * @{
 */
/**
 * Allocates an aligned block of memory
 * \param[in] size      Size of the block of memory in bytes
 * \param[in] alignment Alignment constraint. Must be a power of two
 * \return Pointer to the beginning of a newly allocated block of memory
 */
DAAL_EXPORT void *daal_malloc(size_t size, size_t alignment = DAAL_MALLOC_DEFAULT_ALIGNMENT);

/**
 * Deallocates the space previously allocated by daal_malloc
 * \param[in] ptr   Pointer to the beginning of a block of memory to deallocate
 */
DAAL_EXPORT void  daal_free(void *ptr);

/**
 * Copies bytes between buffers
 * \param[out] dest               Pointer to new buffer
 * \param[in]  numberOfElements   Size of new buffer
 * \param[in]  src                Pointer to source buffer
 * \param[in]  count              Number of bytes to copy.
 */
DAAL_EXPORT void  daal_memcpy_s(void *dest, size_t numberOfElements, const void *src, size_t count);
/** @} */

DAAL_EXPORT float daal_string_to_float(const char * nptr, char ** endptr);

DAAL_EXPORT double daal_string_to_double(const char * nptr, char ** endptr);
}
} // namespace daal

#endif
