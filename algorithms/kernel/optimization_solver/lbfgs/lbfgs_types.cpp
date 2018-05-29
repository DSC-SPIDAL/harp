/* file: lbfgs_types.cpp */
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
//  Implementation of lbfgs solver classes.
//--
*/

#include "algorithms/optimization_solver/lbfgs/lbfgs_types.h"
#include "numeric_table.h"
#include "serialization_utils.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace optimization_solver
{
namespace lbfgs
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_LBFGS_RESULT_ID);

Parameter::Parameter(sum_of_functions::BatchPtr function, size_t nIterations, double accuracyThreshold,
                     size_t batchSize, size_t correctionPairBatchSize, size_t m, size_t L, size_t seed) :
    optimization_solver::iterative_solver::Parameter(function, nIterations, accuracyThreshold, false, batchSize),
    correctionPairBatchSize(correctionPairBatchSize), m(m), L(L), seed(seed),
    stepLengthSequence(HomogenNumericTable<>::create(1, 1, NumericTableIface::doAllocate, 1.0)), engine(engines::mt19937::Batch<>::create())
{}

services::Status Parameter::check() const
{
    services::Status s = iterative_solver::Parameter::check();
    if(!s) return s;

    DAAL_CHECK_EX(m != 0, ErrorIncorrectParameter, ArgumentName, "m");
    DAAL_CHECK_EX(L != 0, ErrorIncorrectParameter, ArgumentName, "L");
    DAAL_CHECK_EX(batchSize != 0, ErrorIncorrectParameter, ArgumentName, "batchSize");

    if(batchIndices.get() != NULL)
    {
        s |= checkNumericTable(batchIndices.get(), batchIndicesStr(), 0, 0, batchSize, nIterations);
        if(!s) return s;
    }

    if(correctionPairBatchIndices.get() != NULL)
    {
        s |= checkNumericTable(correctionPairBatchIndices.get(), correctionPairBatchIndicesStr(), 0, 0,
                              correctionPairBatchSize, (nIterations / L));
        if(!s) return s;
    }

    if(stepLengthSequence.get() != NULL)
    {
        if(stepLengthSequence->getNumberOfColumns() != 1 && stepLengthSequence->getNumberOfColumns() != nIterations)
        {
            return services::Status(Error::create(ErrorIncorrectNumberOfFeatures,
                                             ArgumentName, stepLengthSequenceStr()));
        }
        s |= checkNumericTable(stepLengthSequence.get(), stepLengthSequenceStr(), 0, 0, 0, 1);
    }
    return s;
}

Input::Input() {}
Input::Input(const Input& other) {}

NumericTablePtr Input::get(OptionalDataId id) const
{
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalArgument);
    if(pOpt.get())
    {
        return NumericTable::cast(pOpt->get(id));
    }
    return NumericTablePtr();
}

void Input::set(OptionalDataId id, const NumericTablePtr &ptr)
{
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalArgument);
    if(!pOpt.get())
    {
        pOpt = algorithms::OptionalArgumentPtr(new algorithms::OptionalArgument(lastOptionalData + 1));
        set(iterative_solver::optionalArgument, pOpt);
    }
    pOpt->set(id, ptr);
}

static services::Status checkCorrectionPairsData(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par,
                                     const SerializationIfacePtr &pItem,
                                     bool bInput)
{
    const ErrorDetailID det = bInput ? OptionalInput : OptionalResult;
    NumericTablePtr pData = NumericTable::cast(pItem);
    if(!pData.get())
    {
        return services::Status(Error::create(bInput ?
                                  ErrorIncorrectOptionalInput : ErrorIncorrectOptionalResult, det, correctionPairsStr()));
    }
    const Input *algInput = static_cast<const Input *>(input);
    auto arg = algInput->get(iterative_solver::inputArgument);
    if(pData->getNumberOfColumns() != arg->getNumberOfRows())
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfColumns, det, correctionPairsStr()));
    }
    const Parameter *algParam = static_cast<const Parameter *>(par);
    if(pData->getNumberOfRows() != 2 * algParam->m)
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfRows, det, correctionPairsStr()));
    }
    return services::Status();
}

static services::Status checkCorrectionIndexData(const SerializationIfacePtr &pItem, bool bInput)
{
    const ErrorDetailID det = bInput ? OptionalInput : OptionalResult;
    NumericTablePtr pData = NumericTable::cast(pItem);
    if(!pData.get())
    {
        return services::Status(Error::create(bInput ?
                                  ErrorIncorrectOptionalInput : ErrorIncorrectOptionalResult, det, correctionIndicesStr()));
    }
    if(pData->getNumberOfColumns() != 2)
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfColumns, det, correctionIndicesStr()));
    }
    if(pData->getNumberOfRows() != 1)
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfRows, det, correctionIndicesStr()));
    }
    return services::Status();
}

static services::Status checkAverageArgumentLIterations(const daal::algorithms::Input *input,
        const SerializationIfacePtr &pItem, bool bInput)
{
    const ErrorDetailID det = bInput ? OptionalInput : OptionalResult;
    NumericTablePtr pData = NumericTable::cast(pItem);
    if(!pData.get())
    {
        return services::Status(Error::create(bInput ?
                                  ErrorIncorrectOptionalInput : ErrorIncorrectOptionalResult, det, averageArgumentLIterationsStr()));
    }
    const Input *algInput = static_cast<const Input *>(input);
    auto arg = algInput->get(iterative_solver::inputArgument);
    if(pData->getNumberOfColumns() != arg->getNumberOfRows())
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfColumns, det, averageArgumentLIterationsStr()));
    }
    if(pData->getNumberOfRows() != 2)
    {
        return services::Status(Error::create(ErrorIncorrectNumberOfRows, det, averageArgumentLIterationsStr()));
    }
    return services::Status();
}

services::Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    services::Status s = super::check(par, method);
    if(!s) return s;

    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalArgument);
    if(!pOpt.get())
    {
        return services::Status();    //ok
    }

    if(pOpt->size() != lastOptionalData + 1)
    {
        return services::Status(ErrorIncorrectOptionalInput);
    }
    //checking correction pairs table
    {
        auto pItem = pOpt->get(correctionPairs);
        if(pItem.get())
        {
            s |= checkCorrectionPairsData(this, par, pItem, true);
            if(!s) return s;
        }
    }

    //checking correction index table
    {
        auto pItem = pOpt->get(correctionIndices);
        if(pItem.get())
        {
            s |= checkCorrectionIndexData(pItem, true);
            if(!s) return s;
        }
    }

    //checking average argument for L iterations table
    {
        auto pItem = pOpt->get(averageArgumentLIterations);
        if(pItem.get())
        {
            s |= checkAverageArgumentLIterations(this, pItem, true);
            if(!s) return s;
        }
    }

    return s;
}

NumericTablePtr Result::get(OptionalDataId id) const
{
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalResult);
    if(pOpt.get())
    {
        return NumericTable::cast(pOpt->get(id));
    }
    return NumericTablePtr();
}

void Result::set(OptionalDataId id, const NumericTablePtr &ptr)
{
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalResult);
    if(!pOpt.get())
    {
        pOpt = algorithms::OptionalArgumentPtr(new algorithms::OptionalArgument(lastOptionalData + 1));
        set(iterative_solver::optionalResult, pOpt);
    }
    pOpt->set(id, ptr);
}

services::Status Result::check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par,
                   int method) const
{
    services::Status s;
    DAAL_CHECK_STATUS(s, super::check(input, par, method));
    if(!static_cast<const Parameter *>(par)->optionalResultRequired)
    {
        return services::Status();
    }
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalResult);
    if(!pOpt.get())
    {
        return services::Status(ErrorNullOptionalResult);
    }
    if(pOpt->size() != lastOptionalData + 1)
    {
        return services::Status(ErrorIncorrectOptionalResult);
    }
    //checking correction pairs table
    {
        auto pItem = pOpt->get(correctionPairs);
        if(!pItem.get())
        {
            return services::Status(Error::create(ErrorNullOptionalResult, OptionalResult, correctionPairsStr()));
        }
        DAAL_CHECK_STATUS(s, checkCorrectionPairsData(input, par, pItem, false));
    }

    //checking correction index table
    {
        auto pItem = pOpt->get(correctionIndices);
        if(!pItem.get())
        {
            return services::Status(Error::create(ErrorNullOptionalResult, OptionalResult, correctionIndicesStr()));
        }
        DAAL_CHECK_STATUS(s, checkCorrectionIndexData(pItem, false));
    }

    //checking average argument for L iterations table
    {
        auto pItem = pOpt->get(averageArgumentLIterations);
        if(!pItem.get())
        {
            return services::Status(Error::create(ErrorNullOptionalResult,
                                             OptionalResult, averageArgumentLIterationsStr()));
        }
        DAAL_CHECK_STATUS(s, checkAverageArgumentLIterations(input, pItem, false));
    }

    return s;
}

} // namespace interface1
} // namespace lbfgs
} // namespace optimization_solver
} // namespace algorithm
} // namespace daal
