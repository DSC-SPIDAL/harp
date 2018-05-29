/* file: abs_types.h */
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
//  Implementation of the absolute value function interface.
//--
*/

#ifndef __ABS_TYPES_H__
#define __ABS_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/numeric_table.h"
#include "data_management/data/csr_numeric_table.h"
#include "data_management/data/homogen_numeric_table.h"
#include "services/daal_defines.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup math Math Functions
 * \copydoc daal::algorithms::math
 * @ingroup analysis
 * @{
 */
/**
 * \brief Contains classes for computing math functions
 */
namespace math
{
/**
 * @defgroup abs Absolute Value(abs)
 * \copydoc daal::algorithms::math::abs
 * @ingroup math
 * @{
 */
/**
 * \brief Contains classes for computing the absolute value function
 */
namespace abs
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__ABS__METHOD"></a>
 * Computation methods for computing the absolute value function
 * @ingroup abs
 */
enum Method
{
    defaultDense = 0,       /*!< Default: performance-oriented method. */
    fastCSR = 1             /*!< Fast: performance-oriented method. Works with Compressed Sparse Rows(CSR) numeric tables */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__ABS__INPUTID"></a>
 * Available identifiers of input objects for the absolute value function
 * @ingroup abs
 */
enum InputId
{
    data,                /*!< %Input data table */
    lastInputId = data
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__ABS__RESULTID"></a>
 * Available identifiers of the result of the absolute value function
 * @ingroup abs
 */
enum ResultId
{
    value,      /*!< Table to store the result */
    lastResultId = value
};

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__MATH__ABS__INPUT"></a>
 * \brief %Input objects for the absolute value function
 */
class DAAL_EXPORT Input : public daal::algorithms::Input
{
public:
    /** Default constructor */
    Input();

    /** Copy constructor */
    Input(const Input& other) : daal::algorithms::Input(other){}

    virtual ~Input() {}

    /**
     * Returns an input object for the absolute value function
     * \param[in] id    Identifier of the input object
     * \return          %Input object that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(InputId id) const;

    /**
     * Sets an input object for the absolute value function
     * \param[in] id    Identifier of the input object
     * \param[in] ptr   Pointer to the object
     */
    void set(InputId id, const data_management::NumericTablePtr &ptr);

    /**
     * Checks an input object for the absolute value function
     * \param[in] par     Function parameter
     * \param[in] method  Computation method
     *
     * \return Status of computation
     */
    services::Status check(const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MATH__ABS__RESULT"></a>
 * \brief %Result obtained with the compute() method of the absolute value function in the batch processing mode
 */
class DAAL_EXPORT Result : public daal::algorithms::Result
{
public:
    DECLARE_SERIALIZABLE_CAST(Result);
    /** Default constructor */
    Result();

    virtual ~Result() {};

    /**
     * Allocates memory to store the result of the absolute value function
     * \param[in] input  %Input object for the absolute value function
     * \param[in] par    %Parameter of the absolute value function
     * \param[in] method Computation method of the absolute value function
     *
     * \return Status of computation
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, const int method);

    /**
     * Returns the result of the absolute value function
     * \param[in] id   Identifier of the result
     * \return         Result that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(ResultId id) const;

    /**
     * Sets the result of the absolute value function
     * \param[in] id    Identifier of the result
     * \param[in] ptr   Result
     */
    void set(ResultId id, const data_management::NumericTablePtr &ptr);

    /**
     * Checks the result of the absolute value function
     * \param[in] in   %Input of the absolute value function
     * \param[in] par     %Parameter of the absolute value function
     * \param[in] method  Computation method of the absolute value function
     *
     * \return Status of computation
     */
    services::Status check(const daal::algorithms::Input *in, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        return daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);
    }
};
typedef services::SharedPtr<Result> ResultPtr;
/** @} */
/** @} */
} // namespace interface1
using interface1::Input;
using interface1::Result;
using interface1::ResultPtr;

} // namespace abs
} // namespace math
} // namespace algorithm
} // namespace daal
#endif
