/* file: spatial_pooling2d_layer_backward_types.h */
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
//  Implementation of backward 2D spatial layer.
//--
*/

#ifndef __SPATIAL_POOLING2D_LAYER_BACKWARD_TYPES_H__
#define __SPATIAL_POOLING2D_LAYER_BACKWARD_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/tensor.h"
#include "data_management/data/homogen_tensor.h"
#include "services/daal_defines.h"
#include "algorithms/neural_networks/layers/layer_backward_types.h"
#include "algorithms/neural_networks/layers/spatial_pooling2d/spatial_pooling2d_layer_types.h"
#include "algorithms/neural_networks/layers/spatial_pooling2d/spatial_pooling2d_layer_forward_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace spatial_pooling2d
{
/**
 * @defgroup spatial_pooling2d_backward Backward Two-dimensional Spatial Pyramid Pooling Layer
 * \copydoc daal::algorithms::neural_networks::layers::spatial_pooling2d::backward
 * @ingroup spatial_pooling2d
 * @{
 */
/**
 * \brief Contains classes for backward two-dimensional (2D) spatial layer
 */
namespace backward
{

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__SPATIAL_POOLING2D__BACKWARD__INPUT"></a>
 * \brief %Input objects for the backward 2D spatial layer
 */
class DAAL_EXPORT Input : public layers::backward::Input
{
public:
    typedef layers::backward::Input super;
    /** Default constructor */
    Input();

    /** Copy constructor */
    Input(const Input& other);

    virtual ~Input() {}

    using layers::backward::Input::get;
    using layers::backward::Input::set;

    /**
    * Checks an input object for the backward 2D pooling layer
    * \param[in] parameter Algorithm parameter
    * \param[in] method Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    /**
     * Return the collection with gradient size
     * \return The collection with gradient size
     */
    virtual services::Collection<size_t> getGradientSize() const;

protected:
    virtual data_management::NumericTablePtr getAuxInputDimensions() const = 0;

    size_t computeInputDimension(size_t maskDim, size_t kernelSize, size_t padding, size_t stride) const;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__SPATIAL_POOLING2D__BACKWARD__RESULT"></a>
 * \brief Provides methods to access the result obtained with the compute() method
 *        of the backward 2D spatial layer
 */
class DAAL_EXPORT Result : public layers::backward::Result
{
public:
    /** Default constructor */
    Result();
    virtual ~Result() {}

    /**
     * Allocates memory to store the result of the backward 2D pooling layer
     * \param[in] input Pointer to an object containing the input data
     * \param[in] method Computation method for the layer
     * \param[in] parameter %Parameter of the backward 2D pooling layer
     *
     * \return Status of computations
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Checks the result of the backward 2D pooling layer
     * \param[in] input %Input object for the layer
     * \param[in] parameter %Parameter of the layer
     * \param[in] method Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;
};

} // namespace interface1
using interface1::Input;
using interface1::Result;
} // namespace backward
/** @} */
} // namespace spatial_pooling2d
} // namespace layers
} // namespace neural_networks
} // namespace algorithm
} // namespace daal

#endif
