/* file: df_model.cpp */
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
//  Implementation of the class defining the decision forest model
//--
*/

#include "df_model_impl.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace decision_forest
{

namespace internal
{
Tree::~Tree()
{
}

} // namespace internal


namespace internal
{

ModelImpl::ModelImpl() : _nTree(0)
{
}

ModelImpl::~ModelImpl()
{
    destroy();
}

void ModelImpl::destroy()
{
    _serializationData.reset();
}

bool ModelImpl::reserve(size_t nTrees)
{
    if(_serializationData.get())
        return false;
    _nTree.set(0);
    _serializationData.reset(new DataCollection(nTrees));
    return _serializationData.get();
}

void MemoryManager::destroy()
{
    for(size_t i = 0; i < _aChunk.size(); ++i)
        daal_free(_aChunk[i]);
    _aChunk.clear();
    _posInChunk = 0;
    _iCurChunk = -1;
}

void* MemoryManager::alloc(size_t nBytes)
{
    DAAL_ASSERT(nBytes <= _chunkSize);
    size_t pos = 0; //pos in the chunk to allocate from
    if((_iCurChunk >= 0) && (_posInChunk + nBytes <= _chunkSize))
    {
        //allocate from the current chunk
        pos = _posInChunk;
    }
    else
    {
        if(!_aChunk.size() || _iCurChunk + 1 >= _aChunk.size())
        {
            //allocate a new chunk
            DAAL_ASSERT(_aChunk.size() ? _iCurChunk >= 0 : _iCurChunk < 0);
            byte* ptr = (byte*)services::daal_malloc(_chunkSize);
            if(!ptr)
                return nullptr;
            _aChunk.push_back(ptr);
        }
        //there are free chunks, make next available a current one and allocate from it
        _iCurChunk++;
        pos = 0;
        _posInChunk = 0;
    }
    //allocate from the current chunk
    _posInChunk += nBytes;
    return _aChunk[_iCurChunk] + pos;
}

void MemoryManager::reset()
{
    _iCurChunk = -1;
    _posInChunk = 0;
}

} // namespace internal
} // namespace decision_forest
} // namespace algorithms
} // namespace daal
