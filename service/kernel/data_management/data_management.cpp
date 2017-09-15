/** file data_management.cpp */
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

#include "collection.h"
#include "data_collection.h"
#include "memory_block.h"

namespace daal
{
namespace data_management
{

namespace interface1
{

DataCollection::DataCollection(size_t n) : super(n) {}

DataCollection::DataCollection() : super() {}

DataCollection::DataCollection(const DataCollection& other) : super(other) {}

const SerializationIfacePtr& DataCollection::operator[](size_t index) const
{
    return super::operator[](index);
}

SerializationIfacePtr& DataCollection::operator[](size_t index)
{
    return super::operator[](index);
}

SerializationIfacePtr &DataCollection::get(size_t index)
{
    return super::get(index);
}

const SerializationIfacePtr& DataCollection::get(size_t index) const
{
    return super::get(index);
}

DataCollection& DataCollection::push_back(const SerializationIfacePtr &x)
{
    super::push_back(x);
    return *this;
}

DataCollection& DataCollection::operator << (const SerializationIfacePtr &x)
{
    super::operator << (x);
    return *this;
}

size_t DataCollection::size() const
{
    return super::size();
}

void DataCollection::clear()
{
    super::clear();
}

void DataCollection::erase(size_t pos)
{
    super::erase(pos);
}

MemoryBlock::MemoryBlock(size_t n): _size(n), _value(NULL)
{
    _value = (byte*)daal::services::daal_malloc(n);
}

MemoryBlock::~MemoryBlock()
{
    release();
}

void MemoryBlock::reserve(size_t n)
{
    if(n > size())
    {
        daal::services::daal_free(_value);
        _value = (byte*)daal::services::daal_malloc(n);
        _size = n;
    }
}

void MemoryBlock::release()
{
    if(_value)
    {
        daal::services::daal_free(_value);
        _value = NULL;
        _size = 0;
    }
}

}
}
}
