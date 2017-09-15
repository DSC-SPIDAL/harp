/* file: df_model_impl_common.h */
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
//  Implementation of common and template functions for decision forest model
//--
*/

#ifndef __DF_MODEL_IMPL_COMMON__
#define __DF_MODEL_IMPL_COMMON__

#include "df_model_impl.h"

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace internal
{

template <typename NodeLeaf>
void writeLeaf(const NodeLeaf& l, DecisionTreeNode& row);

template <typename NodeType, typename NodeBase>
void nodeToTable(const NodeBase& node, size_t iRow, size_t& iCur, DecisionTreeNode* aRow)
{
    DecisionTreeNode& row = aRow[iRow];
    if(node.isSplit())
    {
        const typename NodeType::Split& s = *NodeType::castSplit(&node);
        row.featureIndex = s.featureIdx;
        row.featureValueOrResponse = s.featureValue;
        row.leftIndexOrClass = iCur++; //+1 for left kid
        ++iCur;//+1 for right kid
        nodeToTable<NodeType, NodeBase>(*s.kid[0], row.leftIndexOrClass, iCur, aRow);
        nodeToTable<NodeType, NodeBase>(*s.kid[1], row.leftIndexOrClass + 1, iCur, aRow);
    }
    else
    {
        const typename NodeType::Leaf& l = *NodeType::castLeaf(&node);
        row.featureIndex = -1;
        writeLeaf<typename NodeType::Leaf>(l, row);
    }
}

template <typename TNodeType, typename TAllocator>
DecisionTreeTable* TreeImpl<TNodeType, TAllocator>::convertToTable() const
{
    const size_t nNode = top() ? top()->numChildren() + 1 : 0;
    auto pTbl = new DecisionTreeTable(nNode);
    if(top())
    {
        DecisionTreeNode* aNode = (DecisionTreeNode*)pTbl->getArray();
        size_t iRow = 0; //index of the current available row in the table
        nodeToTable<TNodeType, typename TNodeType::Base>(*top(), iRow++, iRow, aNode);
    }
    return pTbl;
}

template <typename TreeNodeVisitor>
bool visitLeaf(size_t level, const DecisionTreeNode& row, TreeNodeVisitor& visitor);

template <typename TVisitor>
bool traverseNodeDF(size_t level, size_t iRowInTable, const decision_forest::internal::DecisionTreeNode* aNode,
    TVisitor& visitor)
{
    const decision_forest::internal::DecisionTreeNode& n = aNode[iRowInTable];
    if(n.isSplit())
    {
        if(!visitor.onSplitNode(level, n.featureIndex, n.featureValue()))
            return false; //do not continue traversing
        ++level;
        if(n.leftIndexOrClass && !traverseNodeDF(level, n.leftIndexOrClass, aNode, visitor))
            return false; //do not continue traversing
        return (n.leftIndexOrClass ? traverseNodeDF(level, n.leftIndexOrClass + 1, aNode, visitor) : true);
    }
    return visitLeaf<TVisitor>(level, n, visitor);
}

typedef services::Collection<size_t> NodeIdxArray;
template <typename TVisitor>
static bool traverseNodesBF(size_t level, NodeIdxArray& aCur,
    NodeIdxArray& aNext, const DecisionTreeNode* aNode, TVisitor& visitor)
{
    for(size_t i = 0; i < aCur.size(); ++i)
    {
        for(size_t j = 0; j < (level ? 2 : 1); ++j)
        {
            const DecisionTreeNode& n = aNode[aCur[i] + j];//right is next to left
            if(n.isSplit())
            {
                if(!visitor.onSplitNode(level, n.featureIndex, n.featureValue()))
                    return false; //do not continue traversing
                if(n.leftIndexOrClass)
                    aNext.push_back(n.leftIndexOrClass);
            }
            else
            {
                if(!visitLeaf<TVisitor>(level, n, visitor))
                    return false; //do not continue traversing
            }
        }
    }
    aCur.clear();
    if(!aNext.size())
        return true;//done
    return traverseNodesBF(level + 1, aNext, aCur, aNode, visitor);
}

} // namespace internal
} // namespace decision_forest
} // namespace algorithms
} // namespace daal

#endif
