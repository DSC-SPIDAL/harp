/* file: neural_networks_training_model.cpp */
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
//  Implementation of model of the training stage of neural network
//--
*/

#include "neural_networks_weights_and_biases.h"
#include "neural_networks_training_model.h"
#include "neural_networks_training_partial_result.h"
#include "neural_networks_training_result.h"
#include "serialization_utils.h"
#include "daal_strings.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace training
{

namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS(Result, SERIALIZATION_NEURAL_NETWORKS_TRAINING_RESULT_ID);
__DAAL_REGISTER_SERIALIZATION_CLASS(PartialResult, SERIALIZATION_NEURAL_NETWORKS_TRAINING_PARTIAL_RESULT_ID);
__DAAL_REGISTER_SERIALIZATION_CLASS(DistributedPartialResult, SERIALIZATION_NEURAL_NETWORKS_TRAINING_DISTRIBUTED_PARTIAL_RESULT_ID);
__DAAL_REGISTER_SERIALIZATION_CLASS(Model, SERIALIZATION_NEURAL_NETWORKS_TRAINING_MODEL_ID);
}

/** \brief Constructor */
Model::Model() : _backwardLayers(new BackwardLayers()), _storeWeightDerivativesInTable(false) { }

Model::Model(services::Status &st) : _storeWeightDerivativesInTable(false)
{
    _backwardLayers.reset(new BackwardLayers());
    if (!_backwardLayers)
        st.add(services::ErrorMemoryAllocationFailed);
}

ModelPtr Model::create(services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL(Model);
}

/**
 * Sets table containing weights and biases of one forward layer of neural network
 * \param[in] idx               Index of the forward layer
 * \param[in] weightsAndBiases  Table containing weights and biases of one forward layer of neural network
 */
services::Status Model::setWeightsAndBiases(size_t idx, const data_management::NumericTablePtr &table)
{
    return _weightsAndBiases->copyFromTable(table, idx);
}

/**
 * Returns the weights and biases of the forward layer of neural network as numeric table
 * \param[in] idx Index of the backward layer
 * \return   Weights and biases derivatives container
 */
data_management::NumericTablePtr Model::getWeightsAndBiases(size_t idx) const
{
    return _weightsAndBiases->copyToTable(idx);
}

/**
 * Returns the weights and biases derivatives of all backward layers of neural network as numeric table
 * \return   Weights and biases derivatives container
 */
data_management::NumericTablePtr Model::getWeightsAndBiasesDerivatives() const
{
    return _weightsAndBiasesDerivatives->copyToTable();
}

/**
 * Returns the weights and biases derivatives of the backward layer of neural network as numeric table
 * \param[in] idx Index of the backward layer
 * \return   Weights and biases derivatives container
 */
data_management::NumericTablePtr Model::getWeightsAndBiasesDerivatives(size_t idx) const
{
    return _weightsAndBiasesDerivatives->copyToTable(idx);
}

}
}
}
}
