/* file: layer_backward.h */
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
//  Implementation of neural network layer.
//--
*/

#ifndef __LAYER_BACKWARD_H__
#define __LAYER_BACKWARD_H__

#include "algorithms/algorithm.h"
#include "data_management/data/tensor.h"
#include "services/daal_defines.h"
#include "algorithms/neural_networks/layers/layer_backward_types.h"

namespace daal
{
namespace algorithms
{
/**
 * \brief Contains classes for training and prediction using neural network
 */
namespace neural_networks
{
/**
 * \brief Contains classes for neural network layers
 */
namespace layers
{
/**
 * \brief Contains classes for the backward stage of the neural network layer
 */
namespace backward
{
namespace interface1
{
/**
 * @ingroup layers_backward
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__BACKWARD__LAYERIFACE"></a>
 *  \brief Abstract class which defines interface for the layer
 */
class LayerIface : public daal::algorithms::Analysis<batch>
{
public:
    typedef algorithms::neural_networks::layers::backward::Input  InputType;
    typedef algorithms::neural_networks::layers::Parameter        ParameterType;
    typedef algorithms::neural_networks::layers::backward::Result ResultType;

    virtual ~LayerIface() {};

    /**
     * Returns the structure that contains results of the layer
     * \return Structure that contains results of the layer
     */
    virtual backward::ResultPtr getLayerResult() = 0;

    /**
     * Returns the structure that contains input objects of the layer
     * \return Structure that contains input objects of the layer
     */
    virtual InputType *getLayerInput() = 0;

    /**
     * Returns the structure that contains parameters of the layer
     * \return Structure that contains parameters of the layer
     */
    virtual ParameterType *getLayerParameter() = 0;

    /**
     * Returns a pointer to the newly allocated backward neural network layer with a copy of input objects
     * and parameters of this layer
     * \return Pointer to the newly allocated backward layer
     */
    services::SharedPtr<daal::algorithms::neural_networks::layers::backward::interface1::LayerIface> clone() const
    {
        return services::SharedPtr<LayerIface>(cloneImpl());
    }

    /**
     * Allocates memory buffers needed for the computations
     */
    virtual services::Status allocateResult() = 0;

    /**
     * Connects two layers in neural network by getting tensor with gradient
     * from the result of the previous layer and adding it to the input object of this layer algorithm
     * \param[in] result        Structure that contains results of the previous layer
     * \param[in] resultIndex   Index of the tensor with gradient in the structure that contains
     *                          results of the previous layer
     * \param[in] inputIndex    Index in the input object of this layer algorithm
     *                          where the tensor with gradient should be placed
     */
     virtual services::Status addInput(backward::ResultPtr result, size_t resultIndex, size_t inputIndex) = 0;

protected:
    virtual LayerIface *cloneImpl() const = 0;
};

typedef services::SharedPtr<LayerIface> LayerIfacePtr;
/** @} */
/**
 * @ingroup layers_backward
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__BACKWARD__LAYERIFACE"></a>
 *  \brief Implements the abstract interface LayerIface. LayerIfaceImpl is, in turn, the base class
 *         for the classes interfacing the layers.
 */
class LayerIfaceImpl : public LayerIface
{
public:
    typedef LayerIface super;

    typedef super::InputType     InputType;
    typedef super::ParameterType ParameterType;
    typedef super::ResultType    ResultType;

    virtual ~LayerIfaceImpl() {};

    /**
     * \copydoc LayerIface::addInput
     */
    virtual services::Status addInput(backward::ResultPtr result, size_t resultIndex, size_t inputIndex) DAAL_C11_OVERRIDE
    {
        return getLayerInput()->addInputGradient(result->getGradient(resultIndex), inputIndex);
    }
};

typedef services::SharedPtr<LayerIfaceImpl> LayerIfaceImplPtr;
/** @} */
} // namespace interface1

using interface1::LayerIface;
using interface1::LayerIfacePtr;
using interface1::LayerIfaceImpl;
using interface1::LayerIfaceImplPtr;
} // namespace backward
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal
#endif
