/* file: dtrees_model_impl.h */
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
//  Implementation of the class defining the decision forest model
//--
*/

#ifndef __DTREES_MODEL_IMPL__
#define __DTREES_MODEL_IMPL__

#include "env_detect.h"
#include "daal_shared_ptr.h"
#include "service_defines.h"
#include "data_management/data/aos_numeric_table.h"
#include "service_memory.h"

typedef size_t ClassIndexType;
typedef double ModelFPType;
typedef ModelFPType ClassificationFPType; //type of features stored in classification model
typedef ModelFPType RegressionFPType;     //type of features and regression response stored in the model

//#define DEBUG_CHECK_IMPURITY

namespace daal
{
namespace algorithms
{
namespace dtrees
{
namespace internal
{

struct DecisionTreeNode
{
    int featureIndex; //split: index of the feature, leaf: -1
    ClassIndexType leftIndexOrClass;    //split: left node index, classification leaf: class index
    ModelFPType featureValueOrResponse; //split: feature value, regression tree leaf: response
    bool isSplit() const { return featureIndex != -1; }
    ModelFPType featureValue() const { return featureValueOrResponse; }
};

class DecisionTreeTable : public data_management::AOSNumericTable
{
public:
    DecisionTreeTable(size_t rowCount = 0) : data_management::AOSNumericTable(sizeof(DecisionTreeNode), 3, rowCount)
    {
        setFeature<int>(0, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, featureIndex));
        setFeature<ClassIndexType>(1, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, leftIndexOrClass));
        setFeature<ModelFPType>(2, DAAL_STRUCT_MEMBER_OFFSET(DecisionTreeNode, featureValueOrResponse));
        allocateDataMemory();
    }
};
typedef services::SharedPtr<DecisionTreeTable> DecisionTreeTablePtr;
typedef services::SharedPtr<const DecisionTreeTable> DecisionTreeTableConstPtr;

template <typename TResponse, typename THistogramm>
class ClassifierResponse
{
public:
    TResponse value; //majority votes response

#ifdef KEEP_CLASSES_PROBABILITIIES
    THistogramm* hist;  //histogramm
    size_t       size;  //number of classes in histogramm
    ClassifierResponse() : hist(nullptr), value(0){}
    ~ClassifierResponse() { if(hist) daal::services::daal_free(hist); }
#else
    ClassifierResponse() : value(0){}
#endif
    ClassifierResponse(const ClassifierResponse&) = delete;
    ClassifierResponse& operator=(const ClassifierResponse& o) = delete;
};

struct TreeNodeBase
{
    virtual ~TreeNodeBase(){}
    virtual bool isSplit() const = 0;
    virtual size_t numChildren() const = 0;

#ifdef DEBUG_CHECK_IMPURITY
    TreeNodeBase() : count(0), impurity(0){}
    size_t count;
    double impurity;
#else
    TreeNodeBase(){}
#endif
};

template <typename algorithmFPType>
struct TreeNodeSplit : public TreeNodeBase
{
    typedef algorithmFPType FeatureType;
    FeatureType featureValue;
    TreeNodeBase* kid[2];
    int featureIdx;
    bool featureUnordered;

    TreeNodeSplit() { kid[0] = kid[1] = nullptr; }
    const TreeNodeBase* left() const { return kid[0]; }
    const TreeNodeBase* right() const { return kid[1]; }
    TreeNodeBase* left()  { return kid[0]; }
    TreeNodeBase* right() { return kid[1]; }

    void set(int featIdx, algorithmFPType featValue, bool bUnordered)
    {
        DAAL_ASSERT(featIdx >= 0);
        featureValue = featValue;
        featureIdx = featIdx;
        featureUnordered = bUnordered;
    }
    virtual bool isSplit() const { return true; }
    virtual size_t numChildren() const { return (kid[0] ? kid[0]->numChildren() + 1 : 0) + (kid[1] ? kid[1]->numChildren() + 1 : 0); }
};

template <typename TResponseType>
struct TreeNodeLeaf: public TreeNodeBase
{
    TResponseType response;

    TreeNodeLeaf(){}
    virtual bool isSplit() const { return false; }
    virtual size_t numChildren() const { return 0; }
};

template<typename algorithmFPType>
struct TreeNodeRegression
{
    typedef TreeNodeBase Base;
    typedef TreeNodeSplit<algorithmFPType> Split;
    typedef TreeNodeLeaf<algorithmFPType> Leaf;

    static Leaf* castLeaf(Base* n) { return static_cast<Leaf*>(n); }
    static const Leaf* castLeaf(const Base* n) { return static_cast<const Leaf*>(n); }
    static Split* castSplit(Base* n) { return static_cast<Split*>(n); }
    static const Split* castSplit(const Base* n) { return static_cast<const Split*>(n); }
};

template<typename algorithmFPType>
struct TreeNodeClassification
{
    typedef TreeNodeBase Base;
    typedef TreeNodeSplit<algorithmFPType> Split;
    typedef TreeNodeLeaf<ClassifierResponse<ClassIndexType, size_t>> Leaf;

    static Leaf* castLeaf(Base* n) { return static_cast<Leaf*>(n); }
    static const Leaf* castLeaf(const Base* n) { return static_cast<const Leaf*>(n); }
    static Split* castSplit(Base* n) { return static_cast<Split*>(n); }
    static const Split* castSplit(const Base* n) { return static_cast<const Split*>(n); }
};

template <typename NodeType>
class HeapMemoryAllocator
{
public:
    HeapMemoryAllocator(size_t dummy){}
    typename NodeType::Leaf* allocLeaf();
    typename NodeType::Split* allocSplit();
    void free(typename NodeType::Base* n);
    void reset(){}
    bool deleteRecursive() const { return true; }
};
template <typename NodeType>
typename NodeType::Leaf* HeapMemoryAllocator<NodeType>::allocLeaf() { return new typename NodeType::Leaf(); }

template <typename NodeType>
typename NodeType::Split* HeapMemoryAllocator<NodeType>::allocSplit() { return new typename NodeType::Split(); }

template <typename NodeType>
void HeapMemoryAllocator<NodeType>::free(typename NodeType::Base* n) { delete n; }

class MemoryManager
{
public:
    MemoryManager(size_t chunkSize) : _chunkSize(chunkSize), _posInChunk(0), _iCurChunk(-1){}
    ~MemoryManager() { destroy(); }

    void* alloc(size_t nBytes);
    //free all allocated memory without destroying of internal storage
    void reset();
    //physically destroy internal storage
    void destroy();

private:
    services::Collection<byte*> _aChunk;
    const size_t _chunkSize; //size of a chunk to be allocated
    size_t _posInChunk; //index of the first free byte in the current chunk
    int _iCurChunk;     //index of the current chunk to allocate from
};

template <typename NodeType>
class ChunkAllocator
{
public:
    ChunkAllocator(size_t nNodesInChunk) :
        _man(nNodesInChunk*(sizeof(typename NodeType::Leaf) + sizeof(typename NodeType::Split))){}
    typename NodeType::Leaf* allocLeaf();
    typename NodeType::Split* allocSplit();
    void free(typename NodeType::Base* n);
    void reset() { _man.reset(); }
    bool deleteRecursive() const { return false; }

private:
    MemoryManager _man;
};
template <typename NodeType>
typename NodeType::Leaf* ChunkAllocator<NodeType>::allocLeaf()
{
    return new (_man.alloc(sizeof(typename NodeType::Leaf))) typename NodeType::Leaf();
}

template <typename NodeType>
typename NodeType::Split* ChunkAllocator<NodeType>::allocSplit()
{
    return new (_man.alloc(sizeof(typename NodeType::Split))) typename NodeType::Split();
}

template <typename NodeType>
void ChunkAllocator<NodeType>::free(typename NodeType::Base* n) {}


template <typename NodeType, typename Allocator>
void deleteNode(typename NodeType::Base* n, Allocator& a)
{
    if(n->isSplit())
    {
        typename NodeType::Split* s = static_cast<typename NodeType::Split*>(n);
        if(s->left())
            deleteNode<NodeType, Allocator>(s->left(), a);
        if(s->right())
            deleteNode<NodeType, Allocator>(s->right(), a);
    }
    a.free(n);
}

class Tree : public Base
{
public:
    Tree(){}
    virtual ~Tree();
};
typedef services::SharedPtr<Tree> TreePtr;

template <typename TNodeType, typename TAllocator = ChunkAllocator<TNodeType> >
class TreeImpl : public Tree
{
public:
    typedef TAllocator Allocator;
    typedef TNodeType NodeType;
    typedef TreeImpl<TNodeType, TAllocator> ThisType;

    TreeImpl(typename NodeType::Base* t, bool bHasUnorderedFeatureSplits) :
        _allocator(_cNumNodesHint), _top(t), _hasUnorderedFeatureSplits(bHasUnorderedFeatureSplits){}
    TreeImpl() : _allocator(_cNumNodesHint), _top(nullptr), _hasUnorderedFeatureSplits(false){}
    ~TreeImpl() { destroy(); }
    void destroy();
    void reset(typename NodeType::Base* t, bool bHasUnorderedFeatureSplits)
    {
        destroy();
        _top = t;
        _hasUnorderedFeatureSplits = bHasUnorderedFeatureSplits;
    }
    const typename NodeType::Base* top() const { return _top; }
    Allocator& allocator() { return _allocator; }
    bool hasUnorderedFeatureSplits() const { return _hasUnorderedFeatureSplits; }
    DecisionTreeTable* convertToTable() const;

private:
    static const size_t _cNumNodesHint = 512; //number of nodes as a hint for allocator to grow by
    Allocator _allocator;
    typename NodeType::Base* _top;
    bool _hasUnorderedFeatureSplits;
};
template<typename Allocator = ChunkAllocator<TreeNodeRegression<RegressionFPType> > >
using TreeImpRegression = TreeImpl<TreeNodeRegression<RegressionFPType>, Allocator>;

template<typename Allocator = ChunkAllocator<TreeNodeClassification<ClassificationFPType> > >
using TreeImpClassification = TreeImpl<TreeNodeClassification<ClassificationFPType>, Allocator>;

class ModelImpl
{
public:
    ModelImpl();
    virtual ~ModelImpl();

    size_t size() const { return _nTree.get(); }
    bool reserve(size_t nTrees);
    bool resize(size_t nTrees);
    void clear();

    const data_management::DataCollection* serializationData() const
    {
        return _serializationData.get();
    }

    const DecisionTreeTable* at(size_t i) const
    {
        return (const DecisionTreeTable*)(*_serializationData)[i].get();
    }

protected:
    void destroy();
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive * arch)
    {
        arch->setSharedPtrObj(_serializationData);

        if(onDeserialize)
            _nTree.set(_serializationData->size());

        return services::Status();
    }

protected:
    data_management::DataCollectionPtr _serializationData; //collection of DecisionTreeTables
    daal::services::Atomic<size_t> _nTree;
};

template <typename NodeType, typename Allocator>
void TreeImpl<NodeType, Allocator>::destroy()
{
    if(_top)
    {
        if(allocator().deleteRecursive())
            deleteNode<NodeType, Allocator>(_top, allocator());
        _top = nullptr;
        allocator().reset();
    }
}

} // namespace internal
} // namespace dtrees
} // namespace algorithms
} // namespace daal

#define __DAAL_REGISTER_SERIALIZATION_CLASS2(ClassName, ImplClassName, Tag)\
    static data_management::SerializationIface* creator##ClassName() { return new ImplClassName(); }\
    data_management::SerializationDesc ClassName::_desc(creator##ClassName, Tag); \
    int ClassName::serializationTag() { return _desc.tag(); }\
    int ClassName::getSerializationTag() const { return _desc.tag(); }

#endif
