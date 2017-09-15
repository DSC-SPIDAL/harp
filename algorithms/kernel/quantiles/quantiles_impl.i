/* file: quantiles_impl.i */
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
//  Quantiles computation algorithm implementation
//--
*/

#ifndef __QUANTILES_IMPL__
#define __QUANTILES_IMPL__

#include "service_numeric_table.h"
#include "service_memory.h"
#include "service_math.h"
#include "service_stat.h"

using namespace daal::internal;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace quantiles
{
namespace internal
{
template<Method method, typename algorithmFPType, CpuType cpu>
services::Status QuantilesKernel<method, algorithmFPType, cpu>::compute(
    const NumericTable &dataTable,
    const NumericTable &quantileOrdersTable,
    NumericTable &quantilesTable)
{
    const size_t nFeatures = dataTable.getNumberOfColumns();
    const size_t nVectors = dataTable.getNumberOfRows();
    const size_t nQuantileOrders = quantilesTable.getNumberOfColumns();

    ReadRows<algorithmFPType, cpu> dataBlock(const_cast<NumericTable &>(dataTable), 0, nVectors);
    DAAL_CHECK_BLOCK_STATUS(dataBlock)
    const algorithmFPType *data = dataBlock.get();

    ReadRows<algorithmFPType, cpu> quantilesQrderBlock(const_cast<NumericTable &>(quantileOrdersTable), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(quantilesQrderBlock)
    const algorithmFPType *quantileOrders = quantilesQrderBlock.get();

    WriteOnlyRows<algorithmFPType, cpu> quantilesBlock(quantilesTable, 0, nFeatures);
    DAAL_CHECK_BLOCK_STATUS(quantilesBlock)
    algorithmFPType *quantiles = quantilesBlock.get();

    int errorcode = Statistics<algorithmFPType, cpu>::xQuantiles(data, nFeatures, nVectors, nQuantileOrders, quantileOrders, quantiles);

    if(errorcode)
    {
        if(errorcode == __DAAL_VSL_SS_ERROR_BAD_QUANT_ORDER) { return Status(services::ErrorQuantileOrderValueIsInvalid); }
        else { return Status(services::ErrorQuantilesInternal); }
    }

    return Status();
}

} // namespace daal::algorithms::quantiles::internal

} // namespace daal::algorithms::quantiles

} // namespace daal::algorithms

} // namespace daal

#endif
