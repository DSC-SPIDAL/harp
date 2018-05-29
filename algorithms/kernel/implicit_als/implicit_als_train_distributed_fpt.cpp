/* file: implicit_als_train_distributed_fpt.cpp */
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
//  Implementation of implicit als algorithm and types methods.
//--
*/

#include "implicit_als_training_types.h"

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
namespace interface1
{
/**
 * Allocates memory to store a partial result of the implicit ALS training algorithm
 * \param[in] input     Pointer to the input object
 * \param[in] parameter Pointer to the parameter
 * \param[in] method    Algorithm computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT Status DistributedPartialResultStep1::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);
    size_t nFactors = algParameter->nFactors;
    Status st;
    set(outputOfStep1ForStep2, HomogenNumericTable<algorithmFPType>::create(nFactors, nFactors, NumericTable::doAllocate, &st));
    return st;
}

/**
 * Allocates memory to store a partial result of the implicit ALS training algorithm
 * \param[in] input     Pointer to the input object
 * \param[in] parameter Pointer to the parameter
 * \param[in] method    Algorithm computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT Status DistributedPartialResultStep2::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);
    size_t nFactors = algParameter->nFactors;
    Status st;
    set(outputOfStep2ForStep4, HomogenNumericTable<algorithmFPType>::create(nFactors, nFactors, NumericTable::doAllocate, &st));
    return st;
}

/**
 * Allocates memory to store a partial result of the implicit ALS training algorithm
 * \param[in] input     Pointer to the input object
 * \param[in] parameter Pointer to the parameter
 * \param[in] method    Algorithm computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT Status DistributedPartialResultStep3::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const DistributedInput<step3Local> *algInput = static_cast<const DistributedInput<step3Local> *>(input);
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);

    size_t nBlocks = algInput->getNumberOfBlocks();
    size_t offset = algInput->getOffset();

    Collection<size_t> _keys;
    Collection<SerializationIfacePtr> _values;
    for (size_t i = 0; i < nBlocks; i++)
    {
        NumericTablePtr outBlockIndices = algInput->getOutBlockIndices(i);
        if (!outBlockIndices) { continue; }
        _keys.push_back(i);
        _values.push_back(SerializationIfacePtr(
            new PartialModel(*algParameter, offset, outBlockIndices, (algorithmFPType)0.0)));
    }
    KeyValueDataCollectionPtr modelsCollection =
        KeyValueDataCollectionPtr (new KeyValueDataCollection(_keys, _values));
    set(outputOfStep3ForStep4, modelsCollection);
    return Status();
}

/**
 * Allocates memory to store a partial result of the implicit ALS training algorithm
 * \param[in] input     Pointer to the input object
 * \param[in] parameter Pointer to the parameter
 * \param[in] method    Algorithm computation method
 */
template <typename algorithmFPType>
DAAL_EXPORT Status DistributedPartialResultStep4::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{
    const DistributedInput<step4Local> *algInput = static_cast<const DistributedInput<step4Local> *>(input);
    const Parameter *algParameter = static_cast<const Parameter *>(parameter);

    set(outputOfStep4ForStep1, PartialModelPtr(
        new PartialModel(*algParameter, algInput->getNumberOfRows(), (algorithmFPType)0.0)));
    return Status();
}

template DAAL_EXPORT Status DistributedPartialResultStep1::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input,
                                                                               const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT Status DistributedPartialResultStep2::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input,
                                                                               const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT Status DistributedPartialResultStep3::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input,
                                                                               const daal::algorithms::Parameter *parameter, const int method);
template DAAL_EXPORT Status DistributedPartialResultStep4::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input,
                                                                               const daal::algorithms::Parameter *parameter, const int method);

}// namespace interface1
}// namespace training
}// namespace implicit_als
}// namespace algorithms
}// namespace daal
