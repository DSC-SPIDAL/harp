/* file: implicit_als_predict_ratings_dense_default_impl.i */
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
//  Implementation of impicit ALS prediction algorithm
//--
*/

#ifndef __IMPLICIT_ALS_PREDICT_RATINGS_DENSE_DEFAULT_IMPL_I__
#define __IMPLICIT_ALS_PREDICT_RATINGS_DENSE_DEFAULT_IMPL_I__

#include "implicit_als_predict_ratings_dense_default_kernel.h"
#include "service_numeric_table.h"
#include "service_blas.h"

using namespace daal::data_management;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace implicit_als
{
namespace prediction
{
namespace ratings
{
namespace internal
{

template <typename algorithmFPType, CpuType cpu>
services::Status ImplicitALSPredictKernel<algorithmFPType, cpu>::compute(
            const NumericTable *usersFactorsTable, const NumericTable *itemsFactorsTable,
            NumericTable *ratingsTable, const Parameter *parameter)
{
    const size_t nUsers = usersFactorsTable->getNumberOfRows();
    const size_t nItems = itemsFactorsTable->getNumberOfRows();

    ReadRows<algorithmFPType, cpu> mtUsersFactors(*const_cast<NumericTable*>(usersFactorsTable), 0, nUsers);
    DAAL_CHECK_BLOCK_STATUS(mtUsersFactors);
    ReadRows<algorithmFPType, cpu> mtItemsFactors(*const_cast<NumericTable*>(itemsFactorsTable), 0, nItems);
    DAAL_CHECK_BLOCK_STATUS(mtItemsFactors);
    WriteOnlyRows<algorithmFPType, cpu> mtRatings(*ratingsTable, 0, nUsers);
    DAAL_CHECK_BLOCK_STATUS(mtRatings);


    const algorithmFPType *usersFactors = mtUsersFactors.get();
    const algorithmFPType *itemsFactors = mtItemsFactors.get();
    algorithmFPType *ratings = mtRatings.get();
    const size_t nFactors = parameter->nFactors;

    /* GEMM parameters */
    const char trans   = 'T';
    const char notrans = 'N';
    const algorithmFPType one(1.0);
    const algorithmFPType zero(0.0);

    Blas<algorithmFPType, cpu>::xgemm(&trans, &notrans, (DAAL_INT *)&nItems, (DAAL_INT *)&nUsers, (DAAL_INT *)&nFactors,
                       &one, itemsFactors, (DAAL_INT *)&nFactors, usersFactors, (DAAL_INT *)&nFactors, &zero,
                       ratings, (DAAL_INT *)&nItems);
    return services::Status();
}

}
}
}
}
}
}

#endif
