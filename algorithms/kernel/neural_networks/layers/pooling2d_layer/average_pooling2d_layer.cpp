/* file: average_pooling2d_layer.cpp */
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
//  Implementation of average pooling2d calculation algorithm and types methods.
//--
*/

#include "average_pooling2d_layer_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace average_pooling2d
{
namespace interface1
{
/**
 * Constructs the parameters of average 2D pooling layer
 * \param[in] firstIndex        Index of the first of two dimensions on which the pooling is performed
 * \param[in] secondIndex       Index of the second of two dimensions on which the pooling is performed
 * \param[in] firstKernelSize   Size of the first dimension of 2D subtensor for which the average element is computed
 * \param[in] secondKernelSize  Size of the second dimension of 2D subtensor for which the average element is computed
 * \param[in] firstStride       Interval over the first dimension on which the pooling is performed
 * \param[in] secondStride      Interval over the second dimension on which the pooling is performed
 * \param[in] firstPadding      Number of data elements to implicitly add to the the first dimension
 *                              of the 2D subtensor on which the pooling is performed
 * \param[in] secondPadding     Number of data elements to implicitly add to the the second dimension
 *                              of the 2D subtensor on which the pooling is performed
 */
Parameter::Parameter(size_t firstIndex, size_t secondIndex, size_t firstKernelSize, size_t secondKernelSize,
          size_t firstStride, size_t secondStride, size_t firstPadding, size_t secondPadding) :
    layers::pooling2d::Parameter(firstIndex, secondIndex, firstKernelSize, secondKernelSize,
                                 firstStride, secondStride, firstPadding, secondPadding)
{}

}// namespace interface1
}// namespace average_pooling2d
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
