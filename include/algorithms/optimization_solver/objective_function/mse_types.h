/* file: mse_types.h */
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
//  Implementation of Mean squared error objective function interface.
//--
*/

#ifndef __MSE_TYPES_H__
#define __MSE_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/numeric_table.h"
#include "services/daal_defines.h"
#include "sum_of_functions_types.h"

namespace daal
{
namespace algorithms
{
/**
 * \brief Contains classes for computing the Mean squared error objective function
 */
namespace optimization_solver
{
/**
 * @defgroup mse Mean Squared Error Algorithm
 * \copydoc daal::algorithms::optimization_solver::mse
 * @ingroup objective_function
 * @{
 */
/**
* \brief Contains classes for computing the Mean squared error objective function
*/
namespace mse
{

/**
  * <a name="DAAL-ENUM-ALGORITHMS__OPTIMIZATION_SOLVER__MSE__INPUTID"></a>
  * Available identifiers of input objects of the Mean squared error objective function
  */
enum InputId
{
    argument = (int)sum_of_functions::argument, /*!< Numeric table of size 1 x p with input argument of the objective function */
    data ,                                   /*!< Numeric table of size n x p with data */
    dependentVariables,                     /*!< Numeric table of size n x 1 with dependent variables */
    lastInputId = dependentVariables
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__OPTIMIZATION_SOLVER__MSE__METHOD"></a>
 * Available methods for computing results of Mean squared error objective function
 */
enum Method
{
    defaultDense = 0 /*!< Default: performance-oriented method. */
};

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{

/**
 * <a name="DAAL-STRUCT-ALGORITHMS__OPTIMIZATION_SOLVER__MSE__PARAMETER"></a>
 * \brief %Parameter for Mean squared error objective function
 *
 * \snippet optimization_solver/objective_function/mse_types.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public sum_of_functions::Parameter
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
    Parameter(size_t numberOfTerms,
              data_management::NumericTablePtr batchIndices = data_management::NumericTablePtr(),
              const DAAL_UINT64 resultsToCompute = objective_function::gradient);

    /**
     * Constructs an Parameter by copying input objects and parameters of another Parameter
     * \param[in] other An object to be used as the source to initialize object
     */
    Parameter(const Parameter &other);
    /**
     * Checks the correctness of the parameter
     *
     * \return Status of computations
     */
    virtual services::Status check() const;

    virtual ~Parameter() {}
};
/* [Parameter source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__MSE__INPUT"></a>
 * \brief %Input objects for the Mean squared error objective function
 */
class DAAL_EXPORT Input : public sum_of_functions::Input
{
public:
    /** Default constructor */
    Input();

    /** Copy constructor */
    Input(const Input& other);

    /** Destructor */
    virtual ~Input() {}

    /**
     * Sets one input object for Mean squared error objective function
     * \param[in] id    Identifier of the input object
     * \param[in] ptr   Pointer to the object
     */
    void set(InputId id, const data_management::NumericTablePtr &ptr);

    /**
     * Returns the input numeric table for Mean squared error objective function
     * \param[in] id    Identifier of the input numeric table
     * \return          %Input object that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(InputId id) const;

    /**
     * Checks the correctness of the input
     * \param[in] par       Pointer to the structure of the algorithm parameters
     * \param[in] method    Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;
};
/** @} */
} // namespace interface1
using interface1::Parameter;
using interface1::Input;

} // namespace mse
} // namespace optimization_solver
} // namespace algorithm
} // namespace daal
#endif
