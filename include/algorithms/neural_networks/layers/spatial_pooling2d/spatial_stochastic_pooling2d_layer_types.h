/* file: spatial_stochastic_pooling2d_layer_types.h */
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
//  Implementation of spatial pyramid stochastic 2D pooling layer.
//--
*/

#ifndef __SPATIAL_STOCHASTIC_POOLING2D_LAYER_TYPES_H__
#define __SPATIAL_STOCHASTIC_POOLING2D_LAYER_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/tensor.h"
#include "data_management/data/homogen_tensor.h"
#include "services/daal_defines.h"
#include "algorithms/neural_networks/layers/spatial_pooling2d/spatial_pooling2d_layer_types.h"
#include "algorithms/engines/mt19937/mt19937.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
/**
 * @defgroup spatial_stochastic_pooling2d Two-dimensional Spatial pyramid stochastic Pooling Layer
 * \copydoc daal::algorithms::neural_networks::layers::spatial_stochastic_pooling2d
 * @ingroup spatial_pooling2d
 * @{
 */
namespace spatial_stochastic_pooling2d
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__NEURAL_NETWORKS__LAYERS__SPATIAL_STOCHASTIC_POOLING2D__METHOD"></a>
 * \brief Computation methods for the spatial pyramid stochastic 2D pooling layer
 */
enum Method
{
    defaultDense = 0    /*!< Default: performance-oriented method */
};

/**
 * \brief Identifiers of input tensors for the backward spatial pyramid stochastic 2D pooling layer
 *        and results for the forward spatial pyramid stochastic 2D pooling layer
 */
enum LayerDataId
{
    auxSelectedIndices,         /*!< p-dimensional tensor that stores the positions of spatial pyramid stochastic elements */
    lastLayerDataId = auxSelectedIndices
};

/**
 * \brief Identifiers of input numeric tables for the backward spatial pyramid stochastic 2D pooling layer
 *        and results for the forward spatial pyramid stochastic 2D pooling layer
 */
enum LayerDataNumericTableId
{
    auxInputDimensions  = lastLayerDataId + 1,         /*!< Numeric table of size 1 x p that contains the sizes
                                          of the dimensions of the input data tensor */
    lastLayerDataNumericTableId = auxInputDimensions
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-STRUCT-ALGORITHMS__NEURAL_NETWORKS__LAYERS__SPATIAL_STOCHASTIC_POOLING2D__PARAMETER"></a>
 * \brief Parameters for the spatial pyramid stochastic 2D pooling layer
 *
 * \snippet neural_networks/layers/spatial_pooling2d/spatial_stochastic_pooling2d_layer_types.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter: public spatial_pooling2d::Parameter
{
    /**
     * Constructs parameters of the spatial pyramid stochastic 2D pooling layer
     * \param[in] pyramidHeight     Value of the pyramid height.
     * \param[in] firstIndex        First dimension index along which spatial pyramid pooling is performed
     * \param[in] secondIndex       Second dimension index along which spatial pyramid pooling is performed
     */
    Parameter(size_t pyramidHeight, size_t firstIndex, size_t secondIndex) :
        spatial_pooling2d::Parameter(pyramidHeight, firstIndex, secondIndex), seed(777), engine(engines::mt19937::Batch<>::create())
    {}

    size_t seed;               /*!< Seed for random numbers generation.  \DAAL_DEPRECATED_USE{ engine } */
    engines::EnginePtr engine; /*!< Engine for random numbers generation. */
};
/* [Parameter source code] */

} // interface1
using interface1::Parameter;

} // namespace spatial_stochastic_pooling2d
/** @} */
} // namespace layers
} // namespace neural_networks
} // namespace algorithm
} // namespace daal

#endif
