/* file: gbt_regression_model.cpp */
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
//  Implementation of the class defining the gradient boosted trees model
//--
*/

#include "algorithms/gradient_boosted_trees/gbt_regression_model.h"
#include "serialization_utils.h"
#include "gbt_regression_model_impl.h"

using namespace daal::data_management;
using namespace daal::services;
using namespace daal::algorithms::gbt::internal;

namespace daal
{
namespace algorithms
{
namespace gbt
{
using namespace daal::algorithms::gbt::internal;

namespace regression
{
namespace interface1
{
__DAAL_REGISTER_SERIALIZATION_CLASS2(Model, internal::ModelImpl, SERIALIZATION_GBT_REGRESSION_MODEL_ID);
Model::Model(){}

ModelPtr Model::create(size_t nFeatures, services::Status *stat)
{
    daal::algorithms::gbt::regression::ModelPtr pRes(new gbt::regression::internal::ModelImpl(nFeatures));
    if((!pRes.get()) && stat)
        stat->add(services::ErrorMemoryAllocationFailed);
    return pRes;
}

}

namespace internal
{

size_t ModelImpl::numberOfTrees() const
{
    return ImplType::numberOfTrees();
}

void ModelImpl::traverseDF(size_t iTree, algorithms::regression::TreeNodeVisitor& visitor) const
{
    ImplType::traverseDF(iTree, visitor);
}

void ModelImpl::traverseBF(size_t iTree, algorithms::regression::TreeNodeVisitor& visitor) const
{
    ImplType::traverseBF(iTree, visitor);
}

services::Status ModelImpl::serializeImpl(data_management::InputDataArchive  * arch)
{
    auto s = algorithms::regression::Model::serialImpl<data_management::InputDataArchive, false>(arch);
    s.add(algorithms::regression::internal::ModelInternal::serialImpl<data_management::InputDataArchive, false>(arch));
    return s.add(ImplType::serialImpl<data_management::InputDataArchive, false>(arch));
}

services::Status ModelImpl::deserializeImpl(const data_management::OutputDataArchive * arch)
{
    auto s = algorithms::regression::Model::serialImpl<const data_management::OutputDataArchive, true>(arch);
    s.add(algorithms::regression::internal::ModelInternal::serialImpl<const data_management::OutputDataArchive, true>(arch));
    return s.add(ImplType::serialImpl<const data_management::OutputDataArchive, true>(arch));
}

} // namespace interface1
} // namespace regression
} // namespace gbt
} // namespace algorithms
} // namespace daal
