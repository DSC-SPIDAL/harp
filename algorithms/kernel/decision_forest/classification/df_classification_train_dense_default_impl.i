/* file: df_classification_train_dense_default_impl.i */
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
//  Implementation of auxiliary functions for decision forest classification
//  (defaultDense) method.
//--
*/

#ifndef __DF_CLASSIFICATION_TRAIN_DENSE_DEFAULT_IMPL_I__
#define __DF_CLASSIFICATION_TRAIN_DENSE_DEFAULT_IMPL_I__

#include "df_train_dense_default_impl.i"
#include "df_classification_train_kernel.h"
#include "df_classification_model_impl.h"
#include "df_predict_dense_default_impl.i"

#define OOBClassificationData size_t

using namespace daal::algorithms::decision_forest::training::internal;
using namespace daal::algorithms::decision_forest::internal;

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace classification
{
namespace training
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// UnorderedRespHelper
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class UnorderedRespHelper : public DataHelper<algorithmFPType, ClassIndexType, cpu>
{
public:
    typedef ClassIndexType TResponse;
    typedef DataHelper<algorithmFPType, ClassIndexType, cpu> super;
    typedef typename decision_forest::internal::TreeImpClassification<> TreeType;
    typedef typename TreeType::NodeType NodeType;
    typedef typename decision_forest::internal::TVector<float, cpu, decision_forest::internal::ScalableAllocator<cpu>> Histogramm;

    struct ImpurityData
    {
        algorithmFPType var; //impurity is a variance
        Histogramm      hist;

        ImpurityData(){}
        ImpurityData(size_t nClasses) : hist(nClasses), var(0){}
        ImpurityData(const ImpurityData& o): var(o.var), hist(o.hist){}
        algorithmFPType value() const { return var; }
        void init(size_t nClasses) { var = 0; hist.resize(nClasses, 0); }
    };
    typedef SplitData<algorithmFPType, ImpurityData> TSplitData;

public:
    UnorderedRespHelper(const decision_forest::internal::SortedFeaturesHelper* sortedFeatHelper, size_t nClasses) :
        super(sortedFeatHelper), _nClasses(nClasses), _histLeft(nClasses), _impLeft(nClasses), _impRight(nClasses){}
    virtual bool init(const NumericTable* data, const NumericTable* resp, const IndexType* aSample) DAAL_C11_OVERRIDE;

    void calcImpurity(const IndexType* aIdx, size_t n, ImpurityData& imp) const;
    bool findBestSplitForFeature(const algorithmFPType* featureVal, const IndexType* aIdx,
        size_t n, size_t nMinSplitPart, const algorithmFPType accuracy, const ImpurityData& curImpurity, TSplitData& split) const
    {
        return split.featureUnordered ? findBestSplitCategoricalFeature(featureVal, aIdx, n, nMinSplitPart, accuracy, curImpurity, split) :
            findBestSplitOrderedFeature(featureVal, aIdx, n, nMinSplitPart, accuracy, curImpurity, split);
    }
    bool terminateCriteria(ImpurityData& imp, algorithmFPType impurityThreshold) const
    {
        return imp.value() < impurityThreshold;
    }

    int findBestSplitForFeatureSorted(algorithmFPType* featureBuf, IndexType iFeature, const IndexType* aIdx,
        size_t n, size_t nMinSplitPart, const ImpurityData& curImpurity, TSplitData& split) const;
    void finalizeBestSplit(const IndexType* aIdx, size_t n, IndexType iFeature,
        size_t idxFeatureValueBestSplit, TSplitData& bestSplit, IndexType* bestSplitIdx) const;
    void simpleSplit(const algorithmFPType* featureVal, const IndexType* aIdx, TSplitData& split) const;

    TResponse predict(const decision_forest::internal::Tree& t, const algorithmFPType* x) const
    {
        const TreeType& tree = static_cast<const TreeType&>(t);
        const typename TreeType::NodeType::Base* pNode = decision_forest::prediction::internal::findNode<algorithmFPType, TreeType, cpu>(t, x);
        DAAL_ASSERT(pNode);
        return TreeType::NodeType::castLeaf(pNode)->response.value;
    }

    algorithmFPType predictionError(TResponse prediction, TResponse response) const
    {
        return algorithmFPType(prediction != response);
    }

    algorithmFPType predictionError(const decision_forest::internal::Tree& t, const algorithmFPType* x,
        const NumericTable* resp, size_t iRow, byte* oobBuf) const
    {
        ReadRows<algorithmFPType, cpu> y(const_cast<NumericTable*>(resp), iRow, 1);
        const TResponse response(this->predict(t, x));
        if(oobBuf)
        {
            OOBClassificationData* ptr = ((OOBClassificationData*)oobBuf) + _nClasses*iRow;
            ptr[response]++;
        }
        return this->predictionError(response, *y.get());
    }

    void setLeafData(typename TreeType::NodeType::Leaf& node, const IndexType* idx, size_t n, ImpurityData& imp) const
    {
        DAAL_ASSERT(n > 0);
#ifdef DEBUG_CHECK_IMPURITY
        node.count = n;
        node.impurity = imp.var;
        {
            Histogramm res(_nClasses, 0);
            for(size_t i = 0; i < n; ++i)
            {
                const ClassIndexType iClass = this->_aResponse[idx[i]].val;
                res[iClass] += 1;
            }
            for(size_t i = 0; i < _nClasses; ++i)
                DAAL_ASSERT(res[i] == imp.hist[i]);
        }
#endif
        auto maxVal = imp.hist[0];
        ClassIndexType maxClass = 0;
        for(size_t i = 1; i < _nClasses; ++i)
        {
            if(maxVal < imp.hist[i])
            {
                maxVal = imp.hist[i];
                maxClass = i;
            }
        }
        node.response.value = maxClass;
#ifdef KEEP_CLASSES_PROBABILITIIES
        node.response.size = imp.hist.size();
        node.response.hist = imp.hist.detach();
#endif
    }

#ifdef DEBUG_CHECK_IMPURITY
    void checkImpurity(const IndexType* ptrIdx, size_t n, const ImpurityData& expected) const;
#endif

private:
    size_t nClasses() const { return _nClasses; }
    void calcGini(size_t n, ImpurityData& imp) const
    {
        const algorithmFPType cDiv(1. / (algorithmFPType(n)*algorithmFPType(n)));
        algorithmFPType var(1.);
        PRAGMA_IVDEP
        PRAGMA_VECTOR_ALWAYS
        for(size_t i = 0; i < _nClasses; ++i)
            var -= cDiv*algorithmFPType(imp.hist[i]) * algorithmFPType(imp.hist[i]);
        imp.var = var;
        if(!isPositive<algorithmFPType, cpu>(imp.var))
            imp.var = 0; //roundoff error
    }

    static void calcPrevImpurity(ImpurityData& imp, ClassIndexType x, size_t n, size_t l)
    {
        algorithmFPType delta = (2.*algorithmFPType(n) - algorithmFPType(l))*imp.var + 2.*(algorithmFPType(imp.hist[x]) - algorithmFPType(n));
        imp.var += algorithmFPType(l)*delta / (algorithmFPType(n - l)*algorithmFPType(n - l));
        imp.hist[x] -= l;
    }
    static void flush(ImpurityData& left, ImpurityData& right, ClassIndexType xi, size_t n, size_t k, size_t& ll)
    {
        algorithmFPType tmp = algorithmFPType(k)*(2.*algorithmFPType(ll) + left.var*algorithmFPType(k)) - 2. *algorithmFPType(ll)*algorithmFPType(left.hist[xi]);
        left.hist[xi] += ll;
        left.var = tmp / (algorithmFPType(k + ll)*algorithmFPType(k + ll));
        calcPrevImpurity(right, xi, n - k, ll);
        ll = 0;
    }
    void computeRightHist(const ImpurityData& curImpurity, TSplitData& split) const;

    bool findBestSplitOrderedFeature(const algorithmFPType* featureVal, const IndexType* aIdx,
        size_t n, size_t nMinSplitPart, const algorithmFPType accuracy, const ImpurityData& curImpurity, TSplitData& split) const;
    bool findBestSplitCategoricalFeature(const algorithmFPType* featureVal, const IndexType* aIdx,
        size_t n, size_t nMinSplitPart, const algorithmFPType accuracy, const ImpurityData& curImpurity, TSplitData& split) const;
private:
    const size_t _nClasses;
    //set of buffers for pre-sorted features processing, used in findBestSplitForFeatureSorted only
    mutable TVector<IndexType, cpu, DefaultAllocator> _idxFeatureBuf;
    mutable TVector<float, cpu, DefaultAllocator> _samplesPerClassBuf;
    mutable Histogramm _histLeft;
    //work variables used in memory saving mode only
    mutable ImpurityData _impLeft;
    mutable ImpurityData _impRight;
};

#ifdef DEBUG_CHECK_IMPURITY
template <typename algorithmFPType, CpuType cpu>
void UnorderedRespHelper<algorithmFPType, cpu>::checkImpurity(const IndexType* ptrIdx, size_t n, const ImpurityData& expected) const
{
    Histogramm hist;
    hist.resize(_nClasses, 0);
    for(size_t i = 0; i < n; ++i)
    {
        const ClassIndexType iClass = this->_aResponse[ptrIdx[i]].val;
        hist[iClass] += 1;
    }
    const algorithmFPType cDiv(1. / (algorithmFPType(n)*algorithmFPType(n)));
    algorithmFPType var(1.);
    for(size_t i = 0; i < _nClasses; ++i)
        var -= cDiv*algorithmFPType(hist[i]) * algorithmFPType(hist[i]);
    for(size_t i = 0; i < _nClasses; ++i)
        DAAL_ASSERT(hist[i] == expected.hist[i]);
    DAAL_ASSERT(!(fabs(var - expected.var) > 0.001));
}
#endif

template <typename algorithmFPType, CpuType cpu>
bool UnorderedRespHelper<algorithmFPType, cpu>::init(const NumericTable* data, const NumericTable* resp, const IndexType* aSample)
{
    if(!super::init(data, resp, aSample))
        return false;
    if(this->_sortedFeatHelper)
    {
        //init work buffers for the computation using pre-sorted features
        const auto nDiffFeatMax = this->sortedFeatures().getMaxNumberOfDiffValues();
        _idxFeatureBuf.reset(nDiffFeatMax);
        _samplesPerClassBuf.reset(nClasses()*nDiffFeatMax);
        return _idxFeatureBuf.get() && _samplesPerClassBuf.get();
    }
    return true;
}

template <typename algorithmFPType, CpuType cpu>
void UnorderedRespHelper<algorithmFPType, cpu>::calcImpurity(const IndexType* aIdx, size_t n, ImpurityData& imp) const
{
    imp.init(_nClasses);
    for(size_t i = 0; i < n; ++i)
    {
        const ClassIndexType iClass = this->_aResponse[aIdx[i]].val;
        imp.hist[iClass] += 1;
    }
    calcGini(n, imp);
}

template <typename algorithmFPType, CpuType cpu>
void UnorderedRespHelper<algorithmFPType, cpu>::simpleSplit(const algorithmFPType* featureVal,
    const IndexType* aIdx, TSplitData& split) const
{
    split.featureValue = featureVal[0];
    split.left.init(_nClasses);
    const ClassIndexType iClass1(this->_aResponse[aIdx[0]].val);
    split.left.hist[iClass1] = 1;

    split.right.init(_nClasses);
    const ClassIndexType iClass2(this->_aResponse[aIdx[1]].val);
    split.right.hist[iClass2] = 1;
    split.nLeft = 1;
    split.iStart = 0;
}

template <typename algorithmFPType, CpuType cpu>
void UnorderedRespHelper<algorithmFPType, cpu>::computeRightHist(const ImpurityData& curImpurity, TSplitData& split) const
{
    split.right.hist.reset(_nClasses);
    auto histTotal = curImpurity.hist.get();
    auto histRight = split.right.hist.get();
    auto histLeft = split.left.hist.get();
    PRAGMA_IVDEP
    PRAGMA_VECTOR_ALWAYS
    for(size_t iClass = 0; iClass < _nClasses; ++iClass)
        histRight[iClass] = histTotal[iClass] - histLeft[iClass];
}

template <typename algorithmFPType, CpuType cpu>
bool UnorderedRespHelper<algorithmFPType, cpu>::findBestSplitOrderedFeature(const algorithmFPType* featureVal, const IndexType* aIdx,
    size_t n, size_t nMinSplitPart, const algorithmFPType accuracy, const ImpurityData& curImpurity, TSplitData& split) const
{
    ClassIndexType xi = this->_aResponse[aIdx[0]].val;
    _impLeft.init(_nClasses);
    _impRight = curImpurity;

    const bool bBestFromOtherFeatures = !(split.impurityDecrease < 0);
    const algorithmFPType vBestFromOtherFeatures = bBestFromOtherFeatures ? algorithmFPType(n)*(curImpurity.var - split.impurityDecrease) : -1;

    bool bFound = false;
    algorithmFPType vBest = -1;
    IndexType iBest = -1;

    size_t nEqualRespValues = 1;
    size_t iStartEqualRespValues = 0;
    const algorithmFPType last = featureVal[n - nMinSplitPart];
    for(size_t i = 1; i < (n - nMinSplitPart + 1); ++i)
    {
        const bool bSameFeaturePrev(featureVal[i] <= featureVal[i - 1] + accuracy);
        if(bSameFeaturePrev || i < nMinSplitPart)
        {
            //can't make a split
            //update impurity and continue
            if(xi == this->_aResponse[aIdx[i]].val)
            {
                //prev response was the same
                ++nEqualRespValues;
            }
            else
            {
                flush(_impLeft, _impRight, xi, n, iStartEqualRespValues, nEqualRespValues);
#ifdef DEBUG_CHECK_IMPURITY
                checkImpurity(aIdx, i, _impLeft);
                checkImpurity(aIdx + i, n - i, _impRight);
#endif
                xi = this->_aResponse[aIdx[i]].val;
                nEqualRespValues = 1;
                iStartEqualRespValues = i;
            }
            continue;
        }

        DAAL_ASSERT(nEqualRespValues);
        flush(_impLeft, _impRight, xi, n, iStartEqualRespValues, nEqualRespValues);
#ifdef DEBUG_CHECK_IMPURITY
        checkImpurity(aIdx, i, _impLeft);
        checkImpurity(aIdx + i, n - i, _impRight);
#endif
        xi = this->_aResponse[aIdx[i]].val;
        nEqualRespValues = 1;
        iStartEqualRespValues = i;
        if(!isPositive<algorithmFPType, cpu>(_impLeft.var))
            _impLeft.var = 0;
        if(!isPositive<algorithmFPType, cpu>(_impRight.var))
            _impRight.var = 0;

        const algorithmFPType v = algorithmFPType(i)*_impLeft.var + algorithmFPType(n - i)*_impRight.var;
        if(iBest < 0)
        {
            if(bBestFromOtherFeatures && isGreater<algorithmFPType, cpu>(v, vBestFromOtherFeatures))
            {
                if(featureVal[i] < last)
                    continue;
                break;
            }
        }
        else if(isGreater<algorithmFPType, cpu>(v, vBest))
        {
            if(featureVal[i] < last)
                continue;
            break;
        }
        bFound = true;
        vBest = v;
        split.left.var = _impLeft.var;
        split.right.var = _impRight.var;
        split.left.hist = _impLeft.hist;
        iBest = i;
        split.nLeft = i;
        if(featureVal[i] < last)
            continue;
    }

    if(bFound)
    {
        DAAL_ASSERT(iBest > 0);
        algorithmFPType impurityDecrease = curImpurity.var - vBest / algorithmFPType(n);
        if(isZero<algorithmFPType, cpu>(impurityDecrease))
            return false;
        split.impurityDecrease = impurityDecrease;
        computeRightHist(curImpurity, split);
#ifdef DEBUG_CHECK_IMPURITY
        checkImpurity(aIdx, split.nLeft, split.left);
        checkImpurity(aIdx + split.nLeft, n - split.nLeft, split.right);
#endif
        split.featureValue = featureVal[iBest - 1];
        split.iStart = 0;
        DAAL_ASSERT(split.nLeft >= nMinSplitPart);
        DAAL_ASSERT((n - split.nLeft) >= nMinSplitPart);
    }
    return bFound;
}

template <typename algorithmFPType, CpuType cpu>
bool UnorderedRespHelper<algorithmFPType, cpu>::findBestSplitCategoricalFeature(const algorithmFPType* featureVal, const IndexType* aIdx,
    size_t n, size_t nMinSplitPart, const algorithmFPType accuracy, const ImpurityData& curImpurity, TSplitData& split) const
{
    DAAL_ASSERT(n >= 2*nMinSplitPart);
    _impRight.init(_nClasses);
    bool bFound = false;
    const bool bBestFromOtherFeatures = !(split.impurityDecrease < 0);
    const algorithmFPType vBestFromOtherFeatures = bBestFromOtherFeatures ? algorithmFPType(n)*(curImpurity.var - split.impurityDecrease) : -1;
    algorithmFPType vBest = -1;
    IndexType iBest = -1;
    for(size_t i = 0; i < n - nMinSplitPart;)
    {
        size_t count = 1;
        _impLeft.init(_nClasses);
        const algorithmFPType first = featureVal[i];
        ClassIndexType xi = this->_aResponse[aIdx[i]].val;
        _impLeft.hist[xi] = 1;
        const size_t iStart = i;
        for(++i; (i < n) && (featureVal[i] == first); ++count, ++i)
        {
            xi = this->_aResponse[aIdx[i]].val;
            ++_impLeft.hist[xi];
        }
        if(count < nMinSplitPart)
            continue;
        for(size_t j = 0; j < _nClasses; ++j)
            _impRight.hist[j] = curImpurity.hist[j] - _impLeft.hist[j];
        calcGini(count, _impLeft);
        calcGini(n - count, _impRight);
        const algorithmFPType v = algorithmFPType(count)*_impLeft.var + algorithmFPType(n - count)*_impRight.var;
        if(iBest < 0)
        {
            if(bBestFromOtherFeatures && isGreater<algorithmFPType, cpu>(v, vBestFromOtherFeatures))
                continue;
        }
        else if(isGreater<algorithmFPType, cpu>(v, vBest))
            continue;
        iBest = i;
        vBest = v;
        split.left.var = _impLeft.var;
        split.right.var = _impRight.var;
        split.left.hist = _impLeft.hist;
        split.nLeft = count;
        split.iStart = iStart;
        split.featureValue = first;
        bFound = true;
    }
    if(bFound)
    {
        algorithmFPType impurityDecrease = curImpurity.var - vBest / algorithmFPType(n);
        if(isZero<algorithmFPType, cpu>(impurityDecrease))
            return false;
        split.impurityDecrease = impurityDecrease;
        computeRightHist(curImpurity, split);
    }
    return bFound;
}

template <typename algorithmFPType, CpuType cpu>
int UnorderedRespHelper<algorithmFPType, cpu>::findBestSplitForFeatureSorted(algorithmFPType* featureBuf, IndexType iFeature,
    const IndexType* aIdx, size_t n, size_t nMinSplitPart,
    const ImpurityData& curImpurity, TSplitData& split) const
{
    const auto nDiffFeatMax = this->sortedFeatures().getMaxNumberOfDiffValues(iFeature);
    _idxFeatureBuf.setValues(nDiffFeatMax, algorithmFPType(0));
    _samplesPerClassBuf.setValues(nClasses()*nDiffFeatMax, 0);
    auto nFeatIdx = _idxFeatureBuf.get();
    auto nSamplesPerClass = _samplesPerClassBuf.get();

    algorithmFPType bestImpDecrease = split.impurityDecrease < 0 ? split.impurityDecrease :
        algorithmFPType(n)*(split.impurityDecrease + algorithmFPType(1.) - curImpurity.var);
    algorithmFPType sumNode = 0;
    {
        //direct access to sorted features data in order to facilitate vectorization
        const SortedFeaturesHelper::IndexType* sortedFeaturesIdx = this->sortedFeatures().data(iFeature);
        const auto aResponse = this->_aResponse.get();
        PRAGMA_VECTOR_ALWAYS
        for(size_t i = 0; i < n; ++i)
        {
            const IndexType iSample = aIdx[i];
            const typename super::Response& r = aResponse[aIdx[i]];
            const SortedFeaturesHelper::IndexType idx = sortedFeaturesIdx[r.idx];
            ++nFeatIdx[idx];
            const ClassIndexType iClass = r.val;
            ++nSamplesPerClass[idx*_nClasses + iClass];
        }
    }
    //init histogram for the left part
    _histLeft.setAll(0);
    auto histLeft = _histLeft.get();
    size_t nLeft = 0;
    int idxFeatureBestSplit = -1; //index of best feature value in the array of sorted feature values
    for(size_t i = 0; i < nDiffFeatMax; ++i)
    {
        if(!nFeatIdx[i])
            continue;
        nLeft = (split.featureUnordered ? nFeatIdx[i] : nLeft + nFeatIdx[i]);
        if((nLeft == n) //last split
            || ((n - nLeft) < nMinSplitPart))
            break;

        if(!split.featureUnordered)
        {
            PRAGMA_IVDEP
            PRAGMA_VECTOR_ALWAYS
            for(size_t iClass = 0; iClass < _nClasses; ++iClass)
                histLeft[iClass] += nSamplesPerClass[i*_nClasses + iClass];
        }
        if(nLeft < nMinSplitPart)
            continue;

        if(split.featureUnordered)
        {
            PRAGMA_IVDEP
            PRAGMA_VECTOR_ALWAYS
            //one against others
            for(size_t iClass = 0; iClass < _nClasses; ++iClass)
                histLeft[iClass] = nSamplesPerClass[i*_nClasses + iClass];
        }

        auto histTotal = curImpurity.hist.get();
        algorithmFPType sumLeft = 0;
        algorithmFPType sumRight = 0;
        PRAGMA_IVDEP
        PRAGMA_VECTOR_ALWAYS
        for(size_t iClass = 0; iClass < _nClasses; ++iClass)
        {
            sumLeft += histLeft[iClass] * histLeft[iClass];
            sumRight += (histTotal[iClass] - histLeft[iClass]) * (histTotal[iClass] - histLeft[iClass]);
        }

        const algorithmFPType decrease = sumLeft / algorithmFPType(nLeft) + sumRight / algorithmFPType(n - nLeft);
        if(decrease > bestImpDecrease)
        {
            split.left.hist = _histLeft;
            split.left.var = sumLeft;
            split.right.var = sumRight;
            split.nLeft = nLeft;
            idxFeatureBestSplit = i;
            bestImpDecrease = decrease;
        }
    }
    if(idxFeatureBestSplit >= 0)
    {
        computeRightHist(curImpurity, split);
        split.impurityDecrease = curImpurity.var + bestImpDecrease/algorithmFPType(n) - algorithmFPType(1);
    }
    return idxFeatureBestSplit;
}

template <typename algorithmFPType, CpuType cpu>
void UnorderedRespHelper<algorithmFPType, cpu>::finalizeBestSplit(const IndexType* aIdx, size_t n,
    IndexType iFeature, size_t idxFeatureValueBestSplit, TSplitData& bestSplit, IndexType* bestSplitIdx) const
{
    DAAL_ASSERT(bestSplit.nLeft > 0);
    const algorithmFPType divL = algorithmFPType(1.) / algorithmFPType(bestSplit.nLeft);
    const algorithmFPType divR = algorithmFPType(1.) / algorithmFPType(n - bestSplit.nLeft);
    bestSplit.left.var = 1. - bestSplit.left.var*divL*divL;
    bestSplit.right.var = 1. - bestSplit.right.var*divR*divR;
    IndexType* bestSplitIdxRight = bestSplitIdx + bestSplit.nLeft;
    size_t iLeft = 0;
    size_t iRight = 0;
    int iRowSplitVal = -1;
    const auto aResponse = this->_aResponse.get();
    const SortedFeaturesHelper::IndexType* sortedFeaturesIdx = this->sortedFeatures().data(iFeature);
    for(size_t i = 0; i < n; ++i)
    {
        const IndexType iSample = aIdx[i];
        const SortedFeaturesHelper::IndexType idx = sortedFeaturesIdx[aResponse[iSample].idx];
        if((bestSplit.featureUnordered && (idx != idxFeatureValueBestSplit)) || ((!bestSplit.featureUnordered) && (idx > idxFeatureValueBestSplit)))
        {
            DAAL_ASSERT(iRight < n - bestSplit.nLeft);
            bestSplitIdxRight[iRight++] = iSample;
        }
        else
        {
            if(idx == idxFeatureValueBestSplit)
                iRowSplitVal = aResponse[iSample].idx;
            DAAL_ASSERT(iLeft < bestSplit.nLeft);
            bestSplitIdx[iLeft++] = iSample;
        }
    }
    DAAL_ASSERT(iRight == n - bestSplit.nLeft);
    DAAL_ASSERT(iLeft == bestSplit.nLeft);
    bestSplit.iStart = 0;
    DAAL_ASSERT(iRowSplitVal >= 0);
    bestSplit.featureValue = this->getValue(iFeature, iRowSplitVal);
}

//////////////////////////////////////////////////////////////////////////////////////////
// TreeThreadCtx class for classification
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, CpuType cpu>
class TreeThreadCtx : public TreeThreadCtxBase<algorithmFPType, cpu>
{
public:
    typedef TreeThreadCtxBase<algorithmFPType, cpu> super;
    TreeThreadCtx(algorithmFPType* _varImp = nullptr) : super(_varImp){}
    bool init(const decision_forest::training::Parameter& par, const NumericTable* x, size_t nClasses)
    {
        if(!super::init(par, x))
            return false;
        _nClasses = nClasses;
        if(par.resultsToCompute & decision_forest::training::computeOutOfBagError)
        {
            size_t sz = sizeof(OOBClassificationData)*nClasses*x->getNumberOfRows();
            this->oobBuf = service_calloc<byte, cpu>(sz);
            if(!this->oobBuf)
                return false;
        }
        return true;
    }

    void reduceTo(TreeThreadCtx& other, size_t nVars, size_t nSamples) const
    {
        super::reduceTo(other, nVars, nSamples);
        if(this->oobBuf)
        {
            OOBClassificationData* dst = (OOBClassificationData*)other.oobBuf;
            const OOBClassificationData* src = (const OOBClassificationData*)this->oobBuf;
            for(size_t i = 0, n = _nClasses*nSamples; i < n; ++i)
                dst[i] += src[i];
        }
    }

    Status finalizeOOBError(const NumericTable* resp, algorithmFPType& res) const
    {
        DAAL_ASSERT(this->oobBuf);
        const size_t nSamples = resp->getNumberOfRows();
        ReadRows<algorithmFPType, cpu> y(const_cast<NumericTable*>(resp), 0, nSamples);
        DAAL_CHECK_BLOCK_STATUS(y);
        size_t nPredicted = 0.;
        size_t nError = 0.;
        for(size_t i = 0; i < nSamples; ++i)
        {
            const OOBClassificationData* ptr = ((const OOBClassificationData*)this->oobBuf) + i*_nClasses;
            size_t maxIdx = 0;
            OOBClassificationData maxVal = ptr[0];
            for(size_t j = 1; j < _nClasses; ++j)
            {
                if(maxVal < ptr[j])
                {
                    maxVal = ptr[j];
                    maxIdx = j;
                }
            }
            if(maxVal == 0)
                continue;
            nPredicted++;
            if(maxIdx != size_t(y.get()[i]))
                ++nError;
        }
        res = algorithmFPType(nError) / algorithmFPType(nPredicted);
        return Status();
    }


private:
    size_t _nClasses;
};


//////////////////////////////////////////////////////////////////////////////////////////
// TrainBatchTask for classification
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, decision_forest::classification::training::Method method, CpuType cpu>
class TrainBatchTask : public TrainBatchTaskBase<algorithmFPType, UnorderedRespHelper<algorithmFPType, cpu>, cpu>
{
    typedef TrainBatchTaskBase<algorithmFPType, UnorderedRespHelper<algorithmFPType, cpu>, cpu> super;
public:
    typedef TreeThreadCtx<algorithmFPType, cpu> ThreadCtxType;
    TrainBatchTask(const NumericTable *x, const NumericTable *y,
        const decision_forest::training::Parameter& par,
        const decision_forest::internal::FeatureTypeHelper<cpu>& featHelper,
        const decision_forest::internal::SortedFeaturesHelper* sortedFeatHelper,
        typename super::ThreadCtxType& ctx, size_t dummy) :
        super(x, y, par, featHelper, sortedFeatHelper, ctx, dummy)
    {
        if(!this->_nFeaturesPerNode)
        {
            size_t nF(daal::internal::Math<algorithmFPType, cpu>::sSqrt(x->getNumberOfColumns()));
            const_cast<size_t&>(this->_nFeaturesPerNode) = (nF < 1 ? 1 : nF);
        }
    }
};

//////////////////////////////////////////////////////////////////////////////////////////
// ClassificationTrainBatchKernel
//////////////////////////////////////////////////////////////////////////////////////////
template <typename algorithmFPType, Method method, CpuType cpu>
services::Status ClassificationTrainBatchKernel<algorithmFPType, method, cpu>::compute(
    const NumericTable *x, const NumericTable *y, decision_forest::classification::Model& m,
    Result& res,
    const decision_forest::classification::training::Parameter& par)
{
    ResultData rd(par, res.get(variableImportance).get(), res.get(outOfBagError).get());
    return computeImpl<algorithmFPType, cpu,
        daal::algorithms::decision_forest::classification::internal::ModelImpl,
        TrainBatchTask<algorithmFPType, method, cpu> >
        (x, y, *static_cast<daal::algorithms::decision_forest::classification::internal::ModelImpl*>(&m),
        rd, par, par.nClasses);
}

} /* namespace internal */
} /* namespace training */
} /* namespace classification */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */

#endif
