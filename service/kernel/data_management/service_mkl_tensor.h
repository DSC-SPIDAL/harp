/* file: service_mkl_tensor.h */
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
//  Declaration and implementation of the base class for numeric n-cubes.
//--
*/


#ifndef __MKL_TENSOR_H__
#define __MKL_TENSOR_H__

#include "services/daal_defines.h"
#include "data_management/data/tensor.h"
#include "data_management/data/homogen_tensor.h"
#include "data_management/data/data_serialize.h"

using namespace daal::data_management;

namespace daal
{
namespace internal
{

template<typename DataType = double>
class DAAL_EXPORT MklTensor : public Tensor
{
public:
    DECLARE_SERIALIZABLE_TAG();

    DAAL_CAST_OPERATOR(MklTensor<DataType>)

    /** \private */
    MklTensor() : Tensor(&_layout), _layout(services::Collection<size_t>()),
        _dnnPtr(NULL), _dnnLayout(NULL), _isDnnLayout(false),
        _plainPtr(NULL), _plainLayout(NULL), _isPlainLayout(false)
    {
    }

    MklTensor(size_t nDim, const size_t *dimSizes);

    MklTensor(size_t nDim, const size_t *dimSizes, AllocationFlag memoryAllocationFlag);

    MklTensor(const services::Collection<size_t> &dims);

    MklTensor(const services::Collection<size_t> &dims, AllocationFlag memoryAllocationFlag);

    /** \private */
    virtual ~MklTensor()
    {
        freeDataMemory();
        freeDnnLayout();
        freePlainLayout();
    }

    DataType* getDnnArray()
    {
        if (_dnnLayout)
        {
            syncPlainToDnn();
            _isPlainLayout = false;
            return _dnnPtr;
        }

        return _plainPtr;
    }

    void* getDnnLayout()
    {
        if (_dnnLayout)
        {
            return _dnnLayout;
        }

        return _plainLayout;
    }

    services::Status setDnnLayout(void* dnnLayout);

    services::Status syncDnnToPlain();

    DataType* getPlainArray()
    {
        syncDnnToPlain();
        _isDnnLayout = false;
        return _plainPtr;
    }

    bool isPlainLayout()
    {
        return _isPlainLayout;
    }

    bool isDnnLayout()
    {
        return _isDnnLayout;
    }

    TensorOffsetLayout& getTensorLayout()
    {
        return _layout;
    }

    virtual TensorOffsetLayout createDefaultSubtensorLayout() const DAAL_C11_OVERRIDE
    {
        return TensorOffsetLayout(_layout);
    }

    virtual TensorOffsetLayout createRawSubtensorLayout() const DAAL_C11_OVERRIDE
    {
        TensorOffsetLayout layout(_layout);
        layout.sortOffsets();
        return layout;
    }

    virtual services::Status setDimensions(size_t nDim, const size_t *dimSizes) DAAL_C11_OVERRIDE
    {
        if(!dimSizes)
        {
            return services::Status(services::ErrorNullParameterNotSupported);
        }

        _layout = TensorOffsetLayout(services::Collection<size_t>(nDim, dimSizes));
        return setPlainLayout();
    }

    virtual services::Status setDimensions(const services::Collection<size_t>& dimensions) DAAL_C11_OVERRIDE
    {
        if(!dimensions.size())
        {
            return services::Status(services::ErrorNullParameterNotSupported);
        }

        _layout = TensorOffsetLayout(dimensions);
        return setPlainLayout();
    }

    services::Status assign(const DataType initValue)
    {
        size_t size = getSize();

        for(size_t i = 0; i < size; i++)
        {
            _plainPtr[i] = initValue;
        }

        _isDnnLayout = false;
        return services::Status();
    }

    services::Status freeDnnLayout();

    services::Status getSubtensorEx(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
                        ReadWriteMode rwflag, SubtensorDescriptor<double> &block,
                        const TensorOffsetLayout& layout) DAAL_C11_OVERRIDE;
    services::Status getSubtensorEx(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
                        ReadWriteMode rwflag, SubtensorDescriptor<float> &block,
                        const TensorOffsetLayout& layout) DAAL_C11_OVERRIDE;
    services::Status getSubtensorEx(size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
                        ReadWriteMode rwflag, SubtensorDescriptor<int> &block,
                        const TensorOffsetLayout& layout) DAAL_C11_OVERRIDE;

    services::Status getSubtensor(size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
        ReadWriteMode rwflag, SubtensorDescriptor<double>& subtensor ) DAAL_C11_OVERRIDE
    {
        return getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, rwflag, subtensor, _layout );
    }

    services::Status getSubtensor(size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
        ReadWriteMode rwflag, SubtensorDescriptor<float>& subtensor ) DAAL_C11_OVERRIDE
    {
        return getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, rwflag, subtensor, _layout );
    }

    services::Status getSubtensor(size_t fixedDims, const size_t* fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum,
        ReadWriteMode rwflag, SubtensorDescriptor<int>& subtensor ) DAAL_C11_OVERRIDE
    {
        return getSubtensorEx(fixedDims, fixedDimNums, rangeDimIdx, rangeDimNum, rwflag, subtensor, _layout );
    }

    services::Status releaseSubtensor(SubtensorDescriptor<double> &block) DAAL_C11_OVERRIDE;
    services::Status releaseSubtensor(SubtensorDescriptor<float>  &block) DAAL_C11_OVERRIDE;
    services::Status releaseSubtensor(SubtensorDescriptor<int>    &block) DAAL_C11_OVERRIDE;

    DAAL_DEPRECATED_VIRTUAL virtual TensorPtr getSampleTensor(size_t firstDimIndex) DAAL_C11_OVERRIDE
    {
        syncDnnToPlain();

        services::Collection<size_t> newDims = getDimensions();
        if(!_plainPtr || newDims.size() == 0 || newDims[0] <= firstDimIndex) { return TensorPtr(); }
        newDims[0] = 1;
        const size_t *_dimOffsets = &((_layout.getOffsets())[0]);
        return TensorPtr(new HomogenTensor<DataType>(newDims, _plainPtr + _dimOffsets[0]*firstDimIndex));
    }

protected:
    virtual services::Status allocateDataMemoryImpl(daal::MemType type = daal::dram) DAAL_C11_OVERRIDE;

    virtual services::Status freeDataMemoryImpl() DAAL_C11_OVERRIDE;

    services::Status serializeImpl  (InputDataArchive  *archive) DAAL_C11_OVERRIDE
    {
        serialImpl<InputDataArchive, false>( archive );

        return services::Status();
    }

    services::Status deserializeImpl(const OutputDataArchive *archive) DAAL_C11_OVERRIDE
    {
        serialImpl<const OutputDataArchive, true>( archive );

        return services::Status();
    }

    template<typename Archive, bool onDeserialize>
    void serialImpl( Archive *archive )
    {
        Tensor::serialImpl<Archive, onDeserialize>( archive );

        archive->setObj( &_layout );

        if (!onDeserialize)
        {
            syncDnnToPlain();
        }
        else
        {
            freeDnnLayout();
            freePlainLayout();
            setPlainLayout();
        }

        bool isAllocated = (_memStatus != notAllocated);
        archive->set( isAllocated );

        if( onDeserialize )
        {
            freeDataMemory();

            if( isAllocated )
            {
                allocateDataMemory();
            }
        }

        _isDnnLayout = false;
        _isPlainLayout = false;

        if(_memStatus != notAllocated)
        {
            archive->set( _plainPtr, getSize() );

            _isPlainLayout = true;
        }
    }

private:
    template <typename T>
    services::Status getTSubtensor( size_t fixedDims, const size_t *fixedDimNums, size_t rangeDimIdx, size_t rangeDimNum, int rwFlag,
                        SubtensorDescriptor<T> &block, const TensorOffsetLayout& layout );
    template <typename T>
    services::Status releaseTSubtensor( SubtensorDescriptor<T> &block );

    services::Status freePlainLayout();
    services::Status setPlainLayout();
    services::Status syncPlainToDnn();

private:
    TensorOffsetLayout  _layout;

    DataType           *_dnnPtr;
    void               *_dnnLayout;
    bool                _isDnnLayout;

    DataType           *_plainPtr;
    void               *_plainLayout;
    bool                _isPlainLayout;
};

}
} // namespace daal

#endif
