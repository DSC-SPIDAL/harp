/* file: concat_layer_backward_types.h */
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
//  Implementation of the backward concat layer
//--
*/

#ifndef __CONCAT_LAYER_BACKWARD_TYPES_H__
#define __CONCAT_LAYER_BACKWARD_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/tensor.h"
#include "data_management/data/homogen_tensor.h"
#include "services/daal_defines.h"
#include "algorithms/neural_networks/layers/layer_backward_types.h"
#include "algorithms/neural_networks/layers/concat/concat_layer_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
/**
 * \brief Contains classes for the concat layer
 */
namespace concat
{
/**
 * @defgroup concat_backward Backward Concat Layer
 * \copydoc daal::algorithms::neural_networks::layers::concat::backward
 * @ingroup concat
 * @{
 */
/**
 * \brief Contains classes for the backward concat layer
 */
namespace backward
{
/**
* \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
*/
namespace interface1
{

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__CONCAT__BACKWARD__INPUT"></a>
 * \brief %Input parameters for the backward concat layer
 */
class DAAL_EXPORT Input : public layers::backward::Input
{
public:
    typedef layers::backward::Input super;
    /** \brief Default constructor */
    Input();

    /** Copy constructor */
    Input(const Input& other);

    virtual ~Input() {}

    /**
     * Returns an input object for the backward concat layer
     */
    using layers::backward::Input::get;

    /**
     * Sets an input object for the backward concat layer
     */
    using layers::backward::Input::set;

    /**
    * Returns input object of the backward concat layer
    * \param[in] id    Identifier of the input object
    * \return          %Input LayerData that corresponds to the given identifier
    */
    data_management::NumericTablePtr get(layers::concat::LayerDataId id) const;

    /**
    * Sets input for the backward concat layer
    * \param[in] id      Identifier of the input object
    * \param[in] value   Pointer to the object
    */
    void set(layers::concat::LayerDataId id, const data_management::NumericTablePtr &value);

    /**
    * Checks an input object for the layer algorithm
    * \param[in] par     %Parameter of algorithm
    * \param[in] method  Computation method of the algorithm
    *
     * \return Status of computations
    */
    services::Status check(const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__CONCAT__BACKWARD__RESULT"></a>
 * \brief Provides methods to access the result obtained with the compute() method of the backward concat layer
 */
class DAAL_EXPORT Result : public layers::backward::Result
{
public:
    DECLARE_SERIALIZABLE_CAST(Result);
    /** \brief Default constructor */
    Result();
    virtual ~Result() {};

    /**
     * Returns the result of the backward concat layer
     */
    using layers::backward::Result::get;

    /**
     * Sets the result of the backward concat layer
     */
    using layers::backward::Result::set;

    /**
    * Returns result object of the backward concat layer
    * \param[in] id       Identifier of the result object
    * \param[in] index    Index of the result object
    * \return             %Input ResultLayerData that corresponds to the given identifier
    */
    data_management::TensorPtr get(layers::backward::ResultLayerDataId id, size_t index) const;

    /**
     * Sets result for the backward concat layer
     * \param[in] id       Identifier of the result object
     * \param[in] value    Pointer to the object
     * \param[in] index    Index of the result object
     */
    void set(layers::backward::ResultLayerDataId id, const data_management::TensorPtr &value, size_t index);

    /**
     * Returns resulting gradient of the backward concat layer
     * \param[in] index Index of the tensor with gradient
     * \return Resulting gradient that corresponds to the given index
     */
    virtual data_management::TensorPtr getGradient(size_t index) const DAAL_C11_OVERRIDE;

    /**
     * Checks the result of the backward concat layer
     * \param[in] input   %Input object for the algorithm
     * \param[in] par     %Parameter of the algorithm
     * \param[in] method  Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

    /**
    * Allocates memory to store the result of the backward concat layer
     * \param[in] input     Pointer to an object containing the input data
     * \param[in] method    Computation method for the algorithm
     * \param[in] parameter %Parameter of the backward concat layer
     *
     * \return Status of computations
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Returns the layout of the result object for the layer algorithm
     * \return Layout of the result object for the layer algorithm
     */
    virtual LayerResultLayout getLayout() const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        return daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);
    }

    size_t getElem(data_management::NumericTablePtr nt, size_t index) const;
};
typedef services::SharedPtr<Result> ResultPtr;

} // namespace interface1
using interface1::Input;
using interface1::Result;
using interface1::ResultPtr;
} // namespace backward

/** @} */
} // namespace concat
} // namespace layers
} // namespace neural_networks
} // namespace algorithm
} // namespace daal
#endif
