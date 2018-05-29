/* file: kernel_function_types_rbf.h */
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
//  Kernel function parameter structure
//--
*/

#ifndef __KERNEL_FUNCTION_TYPES_RBF_H__
#define __KERNEL_FUNCTION_TYPES_RBF_H__

#include "algorithms/kernel_function/kernel_function_types.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup kernel_function_rbf Radial Basis Function Kernel
 * \copydoc daal::algorithms::kernel_function::rbf
 * @ingroup kernel_function
 * @{
 */
/**
 * \brief Contains classes for computing kernel functions
 */
namespace kernel_function
{
/**
 * \brief Contains classes for computing the radial basis function (RBF) kernel
 */
namespace rbf
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__KERNEL_FUNCTION__RBF__METHOD"></a>
 * Method for computing  kernel functions
 */
enum Method
{
    defaultDense = 0,    /*!< Default method for computing the RBF kernel */
    fastCSR = 1          /*!< Fast: performance-oriented method. Works with Compressed Sparse Rows (CSR) numeric tables */
};

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-STRUCT-ALGORITHMS__KERNEL_FUNCTION__RBF__PARAMETER"></a>
 * \brief Parameters for the radial basis function (RBF) kernel
 *
 * \snippet kernel_function/kernel_function_types_rbf.h RBF input object source code
 */
/* [RBF input object source code] */
struct DAAL_EXPORT Parameter : public ParameterBase
{
    Parameter(double sigma = 1.0);
    double sigma;   /*!< RBF kernel coefficient */
};
/* [RBF input object source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__KERNEL_FUNCTION__RBF__INPUT"></a>
 * \brief %Input objects for the RBF kernel algorithm
 */
class DAAL_EXPORT Input : public kernel_function::Input
{
public:
    Input();
    Input(const Input& other);

    virtual ~Input() {}

    /**
    * Checks input objects of the RBF kernel algorithm
    * \param[in] par     %Input objects of the algorithm
    * \param[in] method   Computation method of the algorithm
    */
    services::Status check(const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;
};
/** @} */
} // namespace interface1
using interface1::Input;
using interface1::Parameter;

} // rbf
} // namespace kernel_function
} // namespace algorithms
} // namespace daal
#endif
