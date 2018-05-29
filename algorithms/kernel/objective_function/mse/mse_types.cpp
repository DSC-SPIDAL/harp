/* file: mse_types.cpp */
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
//  Implementation of mse classes.
//--
*/

#include "algorithms/optimization_solver/objective_function/mse_types.h"
#include "numeric_table.h"
#include "daal_strings.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace optimization_solver
{
namespace mse
{
namespace interface1
{
/**
 * Constructs the parameter of Mean squared error objective function
 * \param[in] numberOfTerms    The number of terms in the function
 * \param[in] batchIndices     Numeric table of size 1 x m where m is batch size that represent
                               a batch of indices used to compute the function results, e.g.,
                               value of the sum of the functions. If no indices are provided,
                               all terms will be used in the computations.
 * \param[in] resultsToCompute 64 bit integer flag that indicates the results to compute
 */
Parameter::Parameter(size_t numberOfTerms, data_management::NumericTablePtr batchIndices, const DAAL_UINT64 resultsToCompute) :
                     sum_of_functions::Parameter(numberOfTerms, batchIndices, resultsToCompute)
{}

/**
 * Constructs an Parameter by copying input objects and parameters of another Parameter
 * \param[in] other An object to be used as the source to initialize object
 */
Parameter::Parameter(const Parameter &other) :
    sum_of_functions::Parameter(other)
{}

/**
 * Checks the correctness of the parameter
 */
services::Status Parameter::check() const
{
    return sum_of_functions::Parameter::check();
}

/** Default constructor */
Input::Input() : sum_of_functions::Input(lastInputId + 1)
{}

Input::Input(const Input& other) : sum_of_functions::Input(other){}

/**
 * Sets one input object for Mean squared error objective function
 * \param[in] id    Identifier of the input object
 * \param[in] ptr   Pointer to the object
 */
void Input::set(InputId id, const data_management::NumericTablePtr &ptr)
{
    Argument::set(id, ptr);
}

/**
 * Returns the input numeric table for Mean squared error objective function
 * \param[in] id    Identifier of the input numeric table
 * \return          %Input object that corresponds to the given identifier
 */
data_management::NumericTablePtr Input::get(InputId id) const
{
    return services::staticPointerCast<data_management::NumericTable, data_management::SerializationIface>(Argument::get(id));
}

/**
 * Checks the correctness of the input
 * \param[in] par       Pointer to the structure of the algorithm parameters
 * \param[in] method    Computation method
 */
services::Status Input::check(const daal::algorithms::Parameter *par, int method) const
{
    sum_of_functions::Input::check(par, method);
    DAAL_CHECK(Argument::size() == 3, services::ErrorIncorrectNumberOfInputNumericTables);

    services::Status s = checkNumericTable(get(data).get(), dataStr(), 0, 0);
    if(!s)
        return s;

    const size_t nColsInData = get(data)->getNumberOfColumns();
    const size_t nRowsInData = get(data)->getNumberOfRows();

    s = checkNumericTable(get(dependentVariables).get(), dependentVariablesStr(), 0, 0, 1, nRowsInData);
    s |= checkNumericTable(get(argument).get(), argumentStr(), 0, 0, 1, nColsInData + 1);
    return s;
}

} // namespace interface1
} // namespace mse
} // namespace optimization_solver
} // namespace algorithm
} // namespace daal
