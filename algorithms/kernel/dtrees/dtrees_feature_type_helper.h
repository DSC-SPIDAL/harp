/* file: dtrees_feature_type_helper.h */
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
//  Implementation of a service class that provides optimal access to the feature types
//--
*/

#ifndef __DTREES_FEATURE_TYPE_HELPER_H__
#define __DTREES_FEATURE_TYPE_HELPER_H__

#include "service_memory.h"
#include "service_numeric_table.h"

typedef double ModelFPType;

namespace daal
{
namespace algorithms
{
namespace dtrees
{
namespace internal
{

//////////////////////////////////////////////////////////////////////////////////////////
// Helper class, provides optimal access to the feature types
//////////////////////////////////////////////////////////////////////////////////////////
class FeatureTypes
{
public:
    FeatureTypes(): _bAllUnordered(false){}
    ~FeatureTypes();
    bool init(const NumericTable& data);

    bool isUnordered(size_t iFeature) const
    {
        return _bAllUnordered || (_aFeat && findInBuf(iFeature));
    }

    bool hasUnorderedFeatures() const { return (_bAllUnordered || _nFeat); }

private:
    void allocBuf(size_t n);
    void destroyBuf();
    bool findInBuf(size_t iFeature) const;

private:
    bool* _aFeat = nullptr; //buffer with minimal required features data
    size_t _nFeat = 0; //size of the buffer
    bool _bAllUnordered = false;
    int _firstUnordered = -1;
    int _lastUnordered = -1;
};

struct BinParams
{
    BinParams(size_t _maxBins, size_t _minBinSize) : maxBins(_maxBins), minBinSize(_minBinSize){}
    BinParams(const BinParams& o) : maxBins(o.maxBins), minBinSize(o.minBinSize){}

    size_t maxBins = 256;
    size_t minBinSize = 5;
};

//////////////////////////////////////////////////////////////////////////////////////////
// IndexedFeatures. Creates and stores index of every feature
// Sorts every feature and creates the mapping: features value -> index of the value
// in the sorted array of unique values of the feature in increasing order
//////////////////////////////////////////////////////////////////////////////////////////
class IndexedFeatures
{
public:
    typedef unsigned int IndexType;
    struct FeatureEntry
    {
        DAAL_NEW_DELETE();
        IndexType    numIndices = 0; //number of indices or bins
        ModelFPType* binBorders = nullptr; //right bin borders

        services::Status allocBorders();
        ~FeatureEntry();
    };

public:
    IndexedFeatures() : _data(nullptr), _entries(nullptr), _nCols(0), _nRows(0), _capacity(0), _maxNumIndices(0){}
    ~IndexedFeatures();

    template <typename algorithmFPType, CpuType cpu>
    services::Status init(const NumericTable& nt, const FeatureTypes* featureTypes = nullptr,
        const BinParams* pBimPrm = nullptr);

    //get max number of indices for that feature
    IndexType numIndices(size_t iCol) const
    {
        return _entries[iCol].numIndices;
    }

    //get max number of indices among all features
    IndexType maxNumIndices() const { return _maxNumIndices;  }

    //returns true if the feature is mapped to bins
    bool isBinned(size_t iCol) const { DAAL_ASSERT(iCol < _nCols); return !!_entries[iCol].binBorders; }

    //returns right border of the bin if the feature is a binned one
    ModelFPType binRightBorder(size_t iCol, size_t iBin) const
    {
        DAAL_ASSERT(isBinned(iCol));
        DAAL_ASSERT(iBin < numIndices(iCol));
        return _entries[iCol].binBorders[iBin];
    }

    //for low-level optimization
    const IndexType* data(size_t iFeature) const { return _data + _nRows*iFeature; }

    size_t nRows() const { return _nRows; }
    size_t nCols() const { return _nCols; }

protected:
    services::Status alloc(size_t nCols, size_t nRows);

protected:
    IndexType* _data;
    FeatureEntry* _entries;
    size_t _nRows;
    size_t _nCols;
    size_t _capacity;
    size_t _maxNumIndices;
};

} /* namespace internal */
} /* namespace dtrees */
} /* namespace algorithms */
} /* namespace daal */

#endif
