/* file: df_predict_dense_default_impl.i */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
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
//  Implementation of auxiliary functions for decision forest predict algorithms
//  (defaultDense) method.
//--
*/

#ifndef __DF_PREDICT_DENSE_DEFAULT_IMPL_I__
#define __DF_PREDICT_DENSE_DEFAULT_IMPL_I__

#include "df_model_impl.h"
#include "service_data_utils.h"
#include "df_feature_type_helper.h"

using namespace daal::internal;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace prediction
{
namespace internal
{

using namespace decision_forest::internal;
//////////////////////////////////////////////////////////////////////////////////////////
// Common service function. Finds node corresponding to the given observation
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, typename TreeType, CpuType cpu>
const typename TreeType::NodeType::Base* findNode(const decision_forest::internal::Tree& t, const algorithmFPType* x)
{
    const TreeType& tree = static_cast<const TreeType&>(t);
    const typename TreeType::NodeType::Base* pNode = tree.top();
    if(tree.hasUnorderedFeatureSplits())
    {
        for(; pNode && pNode->isSplit();)
        {
            auto pSplit = TreeType::NodeType::castSplit(pNode);
            const int sn = (pSplit->featureUnordered ? (int(x[pSplit->featureIdx]) != int(pSplit->featureValue)) :
                daal::data_feature_utils::internal::SignBit<algorithmFPType, cpu>::get(pSplit->featureValue - x[pSplit->featureIdx]));
            pNode = pSplit->kid[sn];
        }
    }
    else
    {
        for(; pNode && pNode->isSplit();)
        {
            auto pSplit = TreeType::NodeType::castSplit(pNode);
            const int sn = daal::data_feature_utils::internal::SignBit<algorithmFPType, cpu>::get(pSplit->featureValue - x[pSplit->featureIdx]);
            pNode = pSplit->kid[sn];
        }
    }
    return pNode;
}

//////////////////////////////////////////////////////////////////////////////////////////
// Common service function. Finds a node corresponding to the given observation
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, typename TreeType, CpuType cpu>
const DecisionTreeNode* findNode(const decision_forest::internal::DecisionTreeTable& t,
    const FeatureTypeHelper<cpu>& featHelper, const algorithmFPType* x)
{
    const DecisionTreeNode* aNode = (const DecisionTreeNode*)t.getArray();
    const DecisionTreeNode* pNode = aNode;
    int iNode = 0;
    if(featHelper.hasUnorderedFeatures())
    {
        for(; pNode && pNode->isSplit();)
        {
            const int sn = (featHelper.isUnordered(pNode->featureIndex) ? (int(x[pNode->featureIndex]) != int(pNode->featureValue())) :
                daal::data_feature_utils::internal::SignBit<algorithmFPType, cpu>::get(algorithmFPType(pNode->featureValue()) - x[pNode->featureIndex]));
            DAAL_ASSERT(pNode->leftIndexOrClass > 0);
            DAAL_ASSERT(sn == 0 || sn == 1);
            DAAL_ASSERT(pNode->leftIndexOrClass + sn > 0 && pNode->leftIndexOrClass + sn < t.getNumberOfRows());
            iNode = pNode->leftIndexOrClass + sn;
            pNode = aNode + (pNode->leftIndexOrClass + sn);
        }
    }
    else
    {
        for(; pNode && pNode->isSplit();)
        {
            const int sn = daal::data_feature_utils::internal::SignBit<algorithmFPType, cpu>::get(algorithmFPType(pNode->featureValue()) - x[pNode->featureIndex]);
            DAAL_ASSERT(pNode->leftIndexOrClass > 0);
            DAAL_ASSERT(sn == 0 || sn == 1);
            DAAL_ASSERT(pNode->leftIndexOrClass + sn > 0 && pNode->leftIndexOrClass + sn < t.getNumberOfRows());
            iNode = pNode->leftIndexOrClass + sn;
            pNode = aNode + (pNode->leftIndexOrClass + sn);
        }
    }
    return pNode;
}

} /* namespace internal */
} /* namespace prediction */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */

#endif
