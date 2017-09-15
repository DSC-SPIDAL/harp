/* file: decision_tree_regression_model_impl.h */
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
//  Implementation of the class defining the Decision tree model
//--
*/

#ifndef __DECISION_TREE_REGRESSION_MODEL_IMPL_
#define __DECISION_TREE_REGRESSION_MODEL_IMPL_

#include "algorithms/decision_tree/decision_tree_regression_model.h"
#include "regression_model_impl.h"

namespace daal
{
namespace algorithms
{
namespace decision_tree
{
namespace regression
{
namespace interface1
{

struct DecisionTreeNode
{
    size_t dimension;
    size_t leftIndex;
    double cutPointOrDependantVariable;
};

class DecisionTreeTable : public data_management::AOSNumericTable
{
public:
    DecisionTreeTable(size_t rowCount = 0) : data_management::AOSNumericTable(sizeof(DecisionTreeNode), 3, rowCount)
    {
        setFeature<size_t> (0, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, dimension));
        setFeature<size_t> (1, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, leftIndex));
        setFeature<double> (2, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, cutPointOrDependantVariable));
        allocateDataMemory();
    }
};

typedef services::SharedPtr<DecisionTreeTable> DecisionTreeTablePtr;
typedef services::SharedPtr<const DecisionTreeTable> DecisionTreeTableConstPtr;

class Model::ModelImpl : public algorithms::regression::internal::ModelImpl
{
    typedef services::Collection<size_t> NodeIdxArray;
public:
    /**
     * Constructs decision tree model with the specified number of features
     * \param[in] featureCount Number of features
     */
    ModelImpl() : _TreeTable() {}

    /**
     * Returns the decision tree table
     * \return decision tree table
     */
    DecisionTreeTablePtr getTreeTable() { return _TreeTable; }

    /**
     * Returns the decision tree table
     * \return decision tree table
     */
    DecisionTreeTableConstPtr getTreeTable() const { return _TreeTable; }

    /**
     *  Sets a decision tree table
     *  \param[in]  value  decision tree table
     */
    void setTreeTable(const DecisionTreeTablePtr & value) { _TreeTable = value; }

    void traverseDF(algorithms::regression::TreeNodeVisitor& visitor) const
    {
        const DecisionTreeNode* aNode = (const DecisionTreeNode*)_TreeTable->getArray();
        if(aNode)
            traverseNodesDF<algorithms::regression::TreeNodeVisitor>(0, 0, aNode, visitor);
    }

    void traverseBF(algorithms::regression::TreeNodeVisitor& visitor) const
    {
        const DecisionTreeNode* aNode = (const DecisionTreeNode*)_TreeTable->getArray();
        NodeIdxArray aCur;//nodes of current layer
        NodeIdxArray aNext;//nodes of next layer
        if(aNode)
        {
            aCur.push_back(0);
            traverseNodesBF<algorithms::regression::TreeNodeVisitor>(0, aCur, aNext, aNode, visitor);
        }
    }

    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive * arch)
    {
        algorithms::regression::internal::ModelImpl::serialImpl<Archive, onDeserialize>(arch);
        arch->setSharedPtrObj(_TreeTable);

        return services::Status();
    }

private:
    DecisionTreeTablePtr _TreeTable;

    template <typename TVisitor>
    bool traverseNodesDF(size_t level, size_t iRowInTable, const DecisionTreeNode* aNode,
        TVisitor& visitor) const
    {
        const DecisionTreeNode& n = aNode[iRowInTable];
        if(n.dimension != static_cast<size_t>(-1))
        {
            if(!visitor.onSplitNode(level, n.dimension, n.cutPointOrDependantVariable))
                return false; //do not continue traversing
            ++level;
            size_t leftIdx = n.leftIndex; size_t rightIdx = leftIdx + 1;
            if(!traverseNodesDF(level, leftIdx, aNode, visitor))
                return false;
            return traverseNodesDF(level, rightIdx, aNode, visitor);
        }
        return visitor.onLeafNode(level, n.cutPointOrDependantVariable);
    }

    template <typename TVisitor>
    bool traverseNodesBF(size_t level, NodeIdxArray& aCur,
        NodeIdxArray& aNext, const DecisionTreeNode* aNode, TVisitor& visitor) const
    {
        for(size_t i = 0; i < aCur.size(); ++i)
        {
            for(size_t j = 0; j < (level ? 2 : 1); ++j)
            {
                const DecisionTreeNode& n = aNode[aCur[i] + j];
                if(n.dimension != static_cast<size_t>(-1))
                {
                    if(!visitor.onSplitNode(level, n.dimension, n.cutPointOrDependantVariable))
                        return false; //do not continue traversing
                    aNext.push_back(n.leftIndex);
                }
                else
                {
                    if(!visitor.onLeafNode(level, n.cutPointOrDependantVariable))
                        return false; //do not continue traversing
                }
            }
        }
        aCur.clear();
        if(!aNext.size())
            return true;//done
        return traverseNodesBF(level + 1, aNext, aCur, aNode, visitor);
    }
};

} // namespace interface1

using interface1::DecisionTreeTable;
using interface1::DecisionTreeNode;
using interface1::DecisionTreeTablePtr;
using interface1::DecisionTreeTableConstPtr;

} // namespace regression
} // namespace decision_tree
} // namespace algorithms
} // namespace daal

#endif
