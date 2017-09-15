/* file: moments_online.h */
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
//  Implementation of LowOrderMoments algorithm and types methods
//--
*/
#ifndef __MOMENTS_ONLINE__
#define __MOMENTS_ONLINE__

#include "low_order_moments_types.h"
#include "service_numeric_table.h"

using namespace daal::internal;
using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace low_order_moments
{

/**
 * Allocates memory to store partial results of the low order %moments algorithm
 * \param[in] input     Pointer to the structure with input objects
 * \param[in] parameter Pointer to the structure of algorithm parameters
 * \param[in] method    Computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    services::Status s;
    size_t nFeatures = 0;
    DAAL_CHECK_STATUS(s, static_cast<const InputIface *>(input)->getNumberOfColumns(nFeatures));

    set(nObservations, NumericTablePtr(new HomogenNumericTable<size_t>(1, 1, NumericTable::doAllocate)));
    for(size_t i = 1; i < lastPartialResultId + 1; i++)
    {
        Argument::set(i, NumericTablePtr(new HomogenNumericTable<algorithmFPType>(nFeatures, 1, NumericTable::doAllocate)));
    }
    return s;
}

template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::initialize(const daal::algorithms::Input *_in, const daal::algorithms::Parameter *parameter, const int method)
{
    Input *input = const_cast<Input *>(static_cast<const Input *>(_in));

    services::Status s;
    DAAL_CHECK_STATUS(s, get(nObservations)->assign((algorithmFPType)0.0))
    DAAL_CHECK_STATUS(s, get(partialSum)->assign((algorithmFPType)0.0))
    DAAL_CHECK_STATUS(s, get(partialSumSquares)->assign((algorithmFPType)0.0))
    DAAL_CHECK_STATUS(s, get(partialSumSquaresCentered)->assign((algorithmFPType)0.0))

    ReadRows<algorithmFPType, sse2> dataBlock(input->get(data).get(), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(dataBlock)
    const algorithmFPType* firstRow = dataBlock.get();

    WriteOnlyRows<algorithmFPType, sse2> partialMinimumBlock(get(partialMinimum).get(), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(partialMinimumBlock)
    algorithmFPType* partialMinimumArray = partialMinimumBlock.get();

    WriteOnlyRows<algorithmFPType, sse2> partialMaximumBlock(get(partialMaximum).get(), 0, 1);
    DAAL_CHECK_BLOCK_STATUS(partialMaximumBlock)
    algorithmFPType* partialMaximumArray = partialMaximumBlock.get();

    size_t nColumns = input->get(data)->getNumberOfColumns();

    for(size_t j = 0; j < nColumns; j++)
    {
        partialMinimumArray[j] = firstRow[j];
        partialMaximumArray[j] = firstRow[j];
    }
    return s;
}

}// namespace low_order_moments
}// namespace algorithms
}// namespace daal

#endif
