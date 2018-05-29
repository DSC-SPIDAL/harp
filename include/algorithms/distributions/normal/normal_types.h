/* file: normal_types.h */
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
//  Implementation of normal distribution.
//--
*/

#ifndef __NORMAL__TYPES_H__
#define __NORMAL__TYPES_H__

#include "algorithms/distributions/distribution_types.h"

namespace daal
{
namespace algorithms
{
namespace distributions
{
/**
 * @defgroup distributions_normal Normal Distribution
 * \copydoc daal::algorithms::distributions::normal
 * @ingroup distributions
 * @{
 */
/**
 * \brief Contains classes for normal distribution
 */
namespace normal
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__DISTRIBUTIONS__NORMAL__METHOD"></a>
 * Available methods to compute normal distribution
 */
enum Method
{
    icdf         = 0,     /*!< Default: Inverse cumulative distribution function method. */
    defaultDense = 0    /*!< Default: performance-oriented method. */
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DISTRIBUTIONS__NORMAL__PARAMETER"></a>
 * \brief Normal distribution parameters
 */
template<typename algorithmFPType>
class DAAL_EXPORT Parameter: public distributions::ParameterBase
{
public:
    /**
     *  Main constructor
     *  \param[in] _a     Mean
     *  \param[in] _sigma Standard deviation
     */
    Parameter(algorithmFPType _a = 0.0, algorithmFPType _sigma = 1.0): a(_a), sigma(_sigma) {}

    algorithmFPType a;        /*!< Mean */
    algorithmFPType sigma;    /*!< Standard deviation */

     /**
     * Check the correctness of the %Parameter object
     */
    services::Status check() const DAAL_C11_OVERRIDE;
};

} // namespace interface1
using interface1::Parameter;

} // namespace normal
/** @} */
} // namespace distributions
} // namespace algorithms
} // namespace daal

#endif
