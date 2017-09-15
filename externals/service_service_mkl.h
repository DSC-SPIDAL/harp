/* file: service_service_mkl.h */
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
//  Declaration of math service functions
//--
*/

#ifndef __SERVICE_SERVICE_MKL_H__
#define __SERVICE_SERVICE_MKL_H__


#include "mkl_daal.h"
#include "istrconv_daal.h"


namespace daal
{
namespace internal
{
namespace mkl
{

struct MklService
{
    static void * serv_malloc(size_t size, size_t alignment)
    {
        return fpk_serv_malloc(size, alignment);
    }

    static void serv_free(void *ptr)
    {
        fpk_serv_free(ptr);
    }

    static int serv_memcpy_s(void *dest, size_t destSize, const void *src, size_t srcSize)
    {
        return fpk_serv_memcpy_s(dest, destSize, src, srcSize);
    }

    static int serv_get_ht()
    {
        return fpk_serv_get_ht();
    }

    static int serv_get_ncpus()
    {
        return fpk_serv_get_ncpus();
    }

    static int serv_get_ncorespercpu()
    {
        return fpk_serv_get_ncorespercpu();
    }

    static int serv_set_memory_limit(int type, size_t limit)
    {
        return fpk_serv_set_memory_limit(type, limit);
    }

    static int serv_strncpy_s(char *dest, size_t dmax, const char *src, size_t slen)
    {
        return fpk_serv_strncpy_s(dest, dmax, src, slen);
    }

    static int serv_strncat_s(char *dest, size_t dmax, const char *src, size_t slen)
    {
        return fpk_serv_strncat_s(dest, dmax, src, slen);
    }

    static float serv_string_to_float(const char * nptr, char ** endptr) {
        return __FPK_string_to_float(nptr, endptr);
    }

    static double serv_string_to_double(const char * nptr, char ** endptr) {
        return __FPK_string_to_double(nptr, endptr);
    }
};

}
}
}

#endif
