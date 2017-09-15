/* file: sgd_types_fpt.cpp */
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
//  Implementation of sgd solver classes.
//--
*/

#include "algorithms/optimization_solver/iterative_solver/iterative_solver_types.h"
#include "algorithms/optimization_solver/sgd/sgd_types.h"
#include "data_management/data/memory_block.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace optimization_solver
{
namespace sgd
{
namespace interface1
{

template <typename algorithmFPType>
services::Status Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method)
{
    services::Status s = super::allocate<algorithmFPType>(input, par, method);
    if(!s) return s;
    const BaseParameter *algParam = static_cast<const BaseParameter *>(par);
    if(!algParam->optionalResultRequired)
        return services::Status();
    algorithms::OptionalArgumentPtr pOpt = get(iterative_solver::optionalResult);
    if(pOpt.get())
    {
        if(pOpt->size() != optionalDataSize)
            return services::Status(services::ErrorIncorrectParameter);//error, will be found in check
    }
    else
    {
        pOpt = algorithms::OptionalArgumentPtr(new algorithms::OptionalArgument(optionalDataSize));
        set(iterative_solver::optionalResult, pOpt);
    }
    MemoryBlockPtr pMem = MemoryBlock::cast(pOpt->get(rngState));
    if(!pMem.get())
    {
        if(!algParam->batchIndices.get())
        {
            pMem = MemoryBlockPtr(new MemoryBlock());
            pOpt->set(rngState, pMem);
        }
    }
    // NumericTablePtr pTbl = NumericTable::cast(pOpt->get(pastUpdateVector));
    if(algParam->optionalResultRequired)
    {
        const Input *algInput = static_cast<const Input *>(input);
        size_t argumentSize = algInput->get(iterative_solver::inputArgument)->getNumberOfRows();
        NumericTablePtr pTbl = NumericTablePtr(new HomogenNumericTable<int>(1, 1, NumericTable::doAllocate, 0));
        pOpt->set(iterative_solver::lastIteration, pTbl);
        if(method == (int) momentum)
        {
            if(!pOpt->get(pastUpdateVector))
            {
                pTbl = NumericTablePtr(new HomogenNumericTable<algorithmFPType>(1, argumentSize, NumericTable::doAllocate, 0.0));
                pOpt->set(pastUpdateVector, pTbl);
            }
        }
        if(method == (int) miniBatch)
        {
            if(!pOpt->get(pastWorkValue))
            {
                pTbl = NumericTablePtr(new HomogenNumericTable<algorithmFPType>(1, argumentSize, NumericTable::doAllocate, 0.0));
                pOpt->set(pastWorkValue, pTbl);
            }
        }
    }
    return services::Status();
}
template DAAL_EXPORT services::Status Result::allocate<DAAL_FPTYPE>(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method);

} // namespace interface1
} // namespace sgd
} // namespace optimization_solver
} // namespace algorithm
} // namespace daal
