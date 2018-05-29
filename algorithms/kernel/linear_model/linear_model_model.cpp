/* file: linear_model_model.cpp */
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
//  Implementation of the class defining the regression model
//--
*/

#include "linear_model_model_impl.h"
#include "algorithms/linear_model/linear_model_model.h"
#include "service_numeric_table.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace linear_model
{
using namespace daal::services;
using namespace daal::data_management;

namespace interface1
{
Parameter::Parameter() : algorithms::Parameter(), interceptFlag(true) {}
Parameter::Parameter(const Parameter &other) : algorithms::Parameter(other), interceptFlag(other.interceptFlag) {}
}

namespace internal
{
ModelInternal::ModelInternal() : _interceptFlag(true), _beta() {}

ModelInternal::ModelInternal(const NumericTablePtr &beta, const linear_model::Parameter &par) :
    _beta(beta), _interceptFlag(par.interceptFlag)
{}

Status ModelInternal::initialize()
{
    const size_t nRows = _beta->getNumberOfRows();
    daal::internal::WriteOnlyRows<float, sse2> betaRows(*_beta, 0, nRows);
    DAAL_CHECK_BLOCK_STATUS(betaRows);
    float *betaArray = betaRows.get();
    const size_t betaSize = _beta->getNumberOfColumns() * nRows;
    for(size_t i = 0; i < betaSize; i++)
    {
        betaArray[i] = 0.0f;
    }
    return Status();
}

size_t ModelInternal::getNumberOfBetas() const { return _beta->getNumberOfColumns(); }

size_t ModelInternal::getNumberOfFeatures() const { return getNumberOfBetas() - 1; }

size_t ModelInternal::getNumberOfResponses() const { return _beta->getNumberOfRows(); }

bool ModelInternal::getInterceptFlag() const { return _interceptFlag; }

NumericTablePtr ModelInternal::getBeta() { return _beta; }

Status ModelInternal::setToZero(NumericTable &table)
{
    const size_t nRows = table.getNumberOfRows();
    daal::internal::WriteOnlyRows<float, sse2> tableRows(table, 0, nRows);
    DAAL_CHECK_BLOCK_STATUS(tableRows);
    float *tableArray = tableRows.get();

    const size_t nCols = table.getNumberOfColumns();

    for(size_t i = 0; i < nCols * nRows; i++)
    {
        tableArray[i] = 0.0f;
    }

    return Status();
}

} // namespace internal

Status checkModel(linear_model::Model* model, const daal::algorithms::Parameter &par, size_t nBeta, size_t nResponses)
{
    DAAL_CHECK(model, ErrorNullModel);

    const Parameter &parameter = static_cast<const Parameter &>(par);
    DAAL_CHECK_EX(model->getInterceptFlag() == parameter.interceptFlag, ErrorIncorrectParameter, ParameterName, interceptFlagStr());

    return checkNumericTable(model->getBeta().get(), betaStr(), 0, 0, nBeta, nResponses);
}
}
}
}
