/* file: cholesky_kernel.h */
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

//++
//  Declaration of template function that calculate choleskys.
//--


#ifndef __CHOLESKY_KERNEL_H__
#define __CHOLESKY_KERNEL_H__

#include "cholesky.h"
#include "kernel.h"
#include "numeric_table.h"
#include "service_lapack.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace cholesky
{
namespace internal
{
/**
 *  \brief Kernel for cholesky calculation
 *  in case floating point type of intermediate calculations
 *  and method of calculations are different
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class CholeskyKernel : public Kernel
{
public:
    services::Status compute(NumericTable *a, NumericTable *r, const daal::algorithms::Parameter *par);

private:
    services::Status copyMatrix(NumericTableIface::StorageLayout iLayout, const algorithmFPType *pA,
                    NumericTableIface::StorageLayout rLayout, algorithmFPType *pL, DAAL_INT dim) const;
    services::Status performCholesky(NumericTableIface::StorageLayout rLayout, algorithmFPType *pL, DAAL_INT dim);
    bool copyToFullMatrix(NumericTableIface::StorageLayout iLayout, const algorithmFPType *pA, algorithmFPType *pL,
                          DAAL_INT dim) const;
    bool copyToLowerTrianglePacked(NumericTableIface::StorageLayout iLayout, const algorithmFPType *pA, algorithmFPType *pL,
                                   DAAL_INT dim) const;
};

} // namespace daal::internal
}
}
} // namespace daal

#endif
