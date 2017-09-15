/* file: implicit_als_train_init_partial_result_fpt.cpp */
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
//  Implementation of implicit als algorithm and types methods.
//--
*/

#include "implicit_als_training_init_types.h"
#include "implicit_als_train_init_parameter.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace implicit_als
{
namespace training
{
namespace init
{
namespace interface1
{
template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResultBase::allocate(size_t nParts)
{
    KeyValueDataCollectionPtr outputCollection (new KeyValueDataCollection());
    KeyValueDataCollectionPtr offsetsCollection(new KeyValueDataCollection());
    for (size_t i = 0; i < nParts; i++)
    {
        (*outputCollection )[i].reset(new HomogenNumericTable<int>(NULL, 1, 0));
        (*offsetsCollection)[i].reset(new HomogenNumericTable<int>(1, 1, NumericTable::doAllocate));
    }
    set(outputOfInitForComputeStep3, outputCollection);
    set(offsets, offsetsCollection);
    return services::Status();
}

template <typename algorithmFPType>
DAAL_EXPORT services::Status PartialResult::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const DistributedInput<step1Local> *algInput = static_cast<const DistributedInput<step1Local> *>(input);
    const DistributedParameter *algParameter = static_cast<const DistributedParameter *>(parameter);
    implicit_als::Parameter modelParameter(algParameter->nFactors);

    services::Status s;
    set(partialModel, PartialModel::create<algorithmFPType>(modelParameter, algInput->getNumberOfItems(), &s));
    DAAL_CHECK_STATUS_VAR(s);

    SharedPtr<HomogenNumericTable<int> > partitionTable = internal::getPartition(algParameter);
    const size_t nParts = partitionTable->getNumberOfRows() - 1;
    int *partitionData = partitionTable->getArray();

    DAAL_CHECK_STATUS(s, this->PartialResultBase::allocate<algorithmFPType>(nParts));

    KeyValueDataCollectionPtr dataPartsCollection(new KeyValueDataCollection());
    for (size_t i = 0; i < nParts; i++)
    {
        (*dataPartsCollection)[i].reset(new CSRNumericTable((algorithmFPType *)NULL, NULL, NULL,
                algInput->get(data)->getNumberOfRows(), size_t(partitionData[i + 1] - partitionData[i])));
    }
    set(outputOfStep1ForStep2, dataPartsCollection);
    return s;
}

template <typename algorithmFPType>
DAAL_EXPORT services::Status DistributedPartialResultStep2::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const DistributedInput<step2Local> *algInput = static_cast<const DistributedInput<step2Local> *>(input);
    KeyValueDataCollectionPtr dataPartsCollection = algInput->get(inputOfStep2FromStep1);
    size_t nParts = dataPartsCollection->size();

    Status s;
    DAAL_CHECK_STATUS(s, this->PartialResultBase::allocate<algorithmFPType>(nParts));

    size_t fullNItems = 0;
    for (size_t i = 0; i < nParts; i++)
    {
        fullNItems += NumericTable::cast((*dataPartsCollection)[i])->getNumberOfColumns();
    }
    set(transposedData, NumericTablePtr(new CSRNumericTable((algorithmFPType *)NULL, NULL, NULL,
            fullNItems, NumericTable::cast((*dataPartsCollection)[0])->getNumberOfRows())));
    return s;
}

template DAAL_EXPORT services::Status PartialResultBase::allocate<DAAL_FPTYPE>(size_t nParts);
template DAAL_EXPORT services::Status PartialResult::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT services::Status DistributedPartialResultStep2::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

}// namespace interface1
}// namespace init
}// namespace training
}// namespace implicit_als
}// namespace algorithms
}// namespace daal
