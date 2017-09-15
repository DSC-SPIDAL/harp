/* file: service_spblas.h */
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
//  Template wrappers for Sparse BLAS functions.
//--
*/


#ifndef __SERVICE_SPBLAS_H__
#define __SERVICE_SPBLAS_H__

#include "daal_defines.h"
#include "service_memory.h"

#include "service_spblas_mkl.h"

namespace daal
{
namespace internal
{

/*
// Template functions definition
*/
template<typename fpType, CpuType cpu, template<typename, CpuType> class _impl=mkl::MklSpBlas>
struct SpBlas
{
    typedef typename _impl<fpType,cpu>::SizeType SizeType;

    static void xsyrk(char *uplo, char *trans, SizeType *p, SizeType *n, fpType *alpha, fpType *a, SizeType *lda,
               fpType *beta, fpType *ata, SizeType *ldata)
    {
        _impl<fpType,cpu>::xsyrk(uplo, trans, p, n, alpha, a, lda, beta, ata, ldata);
    }

    static void xcsrmultd(const char *transa, const SizeType *m,
                   const SizeType *n, const SizeType *k, fpType *a, SizeType *ja, SizeType *ia,
                   fpType *b, SizeType *jb, SizeType *ib, fpType *c, SizeType *ldc)
    {
        _impl<fpType,cpu>::xcsrmultd(transa, m, n, k, a, ja, ia, b, jb, ib, c, ldc);
    }

    static void xcsrmv(const char *transa, const SizeType *m,
                const SizeType *k, const fpType *alpha, const char *matdescra,
                const fpType *val, const SizeType *indx, const SizeType *pntrb,
                const SizeType *pntre, const fpType *x, const fpType *beta, fpType *y)
    {
        _impl<fpType,cpu>::xcsrmv(transa, m, k, alpha, matdescra, val, indx, pntrb, pntre, x, beta, y);
    }

    static void xcsrmm(const char *transa, const SizeType *m, const SizeType *n, const SizeType *k,
                const fpType *alpha, const char *matdescra, const fpType *val, const SizeType *indx,
                const SizeType *pntrb, const fpType *b, const SizeType *ldb, const fpType *beta, fpType *c, const SizeType *ldc)
    {
        _impl<fpType,cpu>::xcsrmm(transa, m, n, k, alpha, matdescra, val, indx, pntrb, b, ldb, beta, c, ldc);
    }

    static void xxcsrmm(const char *transa, const SizeType *m, const SizeType *n, const SizeType *k,
                 const fpType *alpha, const char *matdescra, const fpType *val, const SizeType *indx,
                 const SizeType *pntrb, const fpType *b, const SizeType *ldb, const fpType *beta, fpType *c, const SizeType *ldc)
    {
        _impl<fpType,cpu>::xxcsrmm(transa, m, n, k, alpha, matdescra, val, indx, pntrb, b, ldb, beta, c, ldc);
    }
};

} // namespace internal
} // namespace daal

#endif
