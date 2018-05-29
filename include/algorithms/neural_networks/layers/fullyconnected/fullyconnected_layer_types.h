/* file: fullyconnected_layer_types.h */
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
//  Fully-connected layer parameter structure.
//--
*/

#ifndef __FULLYCONNECTED_LAYER_TYPES_H__
#define __FULLYCONNECTED_LAYER_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/tensor.h"
#include "data_management/data/homogen_tensor.h"
#include "services/daal_defines.h"
#include "algorithms/neural_networks/layers/layer_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
/**
 * @defgroup fullyconnected Fully-connected Layer
 * \copydoc daal::algorithms::neural_networks::layers::fullyconnected
 * @ingroup layers
 * @{
 */
namespace fullyconnected
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__NEURAL_NETWORKS__LAYERS__FULLYCONNECTED__METHOD"></a>
 * Available methods to compute forward and backward fully-connected layer
 */
enum Method
{
    defaultDense = 0,    /*!< Default: performance-oriented method. */
};

/**
 * Available identifiers of results of the forward fully-connected layer
 * and input objects for the backward fully-connected layer
 */
enum LayerDataId
{
    auxData    = 0, /*!< Data processed at the forward stage of the layer */
    auxWeights = 1, /*!< Weights used at the forward stage of the layer */
    lastLayerDataId = auxWeights
};

namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__LAYERS__FULLYCONNECTED__PARAMETER"></a>
 * \brief Fully-connected layer parameters
 */
class DAAL_EXPORT Parameter: public layers::Parameter
{
public:
    /**
     *  Main constructor
     *  \param[in] _nOutputs A number of layer outputs m. The parameter required to initialize the layer
     */
    Parameter(size_t _nOutputs);

    size_t nOutputs; /*!< A number of layer outputs. The parameter required to initialize the layer */
};

} // namespace interface1
using interface1::Parameter;

} // namespace fullyconnected
/** @} */
} // namespace layers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal
#endif
