/* file: service_tensor.h */
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
//  Tensor service functions
//--
*/

#ifndef __SERVICE_TENSOR_H__
#define __SERVICE_TENSOR_H__

#include "services/daal_memory.h"
#include "homogen_tensor.h"
#include "service_defines.h"

using namespace daal::data_management;

namespace daal
{
namespace internal
{
template<typename algorithmFPType, typename algorithmFPAccessType, CpuType cpu, ReadWriteMode mode, typename TensorType>
class GetSubtensors
{
public:
    DAAL_NEW_DELETE();

    GetSubtensors(TensorType& data, size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum) : _data(&data)
    {
        getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum);
    }
    GetSubtensors(TensorType *data, size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum) : _data(data), _toReleaseFlag(false)
    {
        if(_data)
            getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum);
    }
    GetSubtensors(TensorType& data, size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, const TensorOffsetLayout& layout) : _data(&data)
    {
        getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, layout);
    }
    GetSubtensors(TensorType *data, size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, const TensorOffsetLayout& layout) : _data(data), _toReleaseFlag(false)
    {
        if(_data)
            getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, layout);
    }
    GetSubtensors(TensorType& data) : _data(&data)
    {
        getSubtensor(0, 0, 0, _data->getDimensionSize(0));
    }
    GetSubtensors(TensorType* data = nullptr) : _data(data), _toReleaseFlag(false)
    {
        if(_data)
            getSubtensor(0, 0, 0, _data->getDimensionSize(0));
    }
    ~GetSubtensors() { release(); }

    algorithmFPAccessType* get() { return _data ? _block.getPtr() : nullptr; }

    algorithmFPAccessType* next(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum)
    {
        if(!_data)
            return nullptr;
        if(_toReleaseFlag)
            _status = _data->releaseSubtensor(_block);
        return getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum);
    }
    algorithmFPAccessType* set(TensorType& data, size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum)
    {
        release();
        _data = &data;
        return getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum);
    }
    algorithmFPAccessType* set(TensorType *data, size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum)
    {
        release();

        if(data)
        {
            _data = data;
            return getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum);
        }
        return nullptr;
    }
    algorithmFPAccessType* set(TensorType& data, size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, const TensorOffsetLayout& layout)
    {
        release();
        _data = &data;
        return getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, layout);
    }
    algorithmFPAccessType* set(TensorType *data, size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, const TensorOffsetLayout& layout)
    {
        release();
        if(data)
        {
            _data = data;
            return getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, layout);
        }
        return nullptr;
    }
    void release()
    {
        if(_toReleaseFlag)
        {
            _data->releaseSubtensor(_block);
            _toReleaseFlag = false;
        }
        _data = nullptr;
        _status.clear();
    }
    size_t getSize()
    {
        return _block.getSize();
    }

    const services::Status& status() const { return _status; }

private:
    algorithmFPAccessType* getSubtensor(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum)
    {
        _status = _data->getSubtensor(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, mode, _block);
        _toReleaseFlag = _status.ok();
        return _block.getPtr();
    }

    algorithmFPAccessType* getSubtensorEx(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, const TensorOffsetLayout& layout)
    {
        _status = _data->getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, mode, _block, layout);
        _toReleaseFlag = _status.ok();
        return _block.getPtr();
    }

    TensorType* _data;
    SubtensorDescriptor<algorithmFPType> _block;
    services::Status _status;
    bool _toReleaseFlag;
};

template<typename algorithmFPType, CpuType cpu, typename TensorType = Tensor>
using ReadSubtensor = GetSubtensors<algorithmFPType, const algorithmFPType, cpu, readOnly, TensorType>;

template<typename algorithmFPType, CpuType cpu, typename TensorType = Tensor>
using WriteSubtensor = GetSubtensors<algorithmFPType, algorithmFPType, cpu, readWrite, TensorType>;

template<typename algorithmFPType, CpuType cpu, typename TensorType = Tensor>
using WriteOnlySubtensor = GetSubtensors<algorithmFPType, algorithmFPType, cpu, writeOnly, TensorType>;


/* Computes product of tensor dimensions from axisFrom (inclusive) up to axisTo (exclusive) */
size_t computeTensorDimensionsProd(const Tensor *tensor, size_t axisFrom, size_t axisTo);

/* Computes product of tensor dimensions before specified axis (exclusive) */
size_t computeTensorOffsetBeforeAxis(const Tensor *tensor, size_t axis);

/* Computes product of tensor dimensions after specified axis (exclusive) */
size_t computeTensorOffsetAfterAxis(const Tensor *tensor, size_t axis);

} // internal namespace
} // daal namespace

#endif
