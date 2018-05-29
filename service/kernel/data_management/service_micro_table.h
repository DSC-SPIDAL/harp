/* file: service_micro_table.h */
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
//  Declaration of MicroTable service functions
//--
*/

#ifndef __SERVICE_MICRO_TABLE_H__
#define __SERVICE_MICRO_TABLE_H__

#include "numeric_table.h"
#include "csr_numeric_table.h"
#include "symmetric_matrix.h"
#include "service_defines.h"

using namespace daal::data_management;

namespace daal
{
namespace internal
{

class MicroTable
{
public:
    MicroTable() : _nt(NULL) {}
    MicroTable(const NumericTable *table) { _nt = const_cast<NumericTable *>(table); }
    MicroTable(NumericTable *table) : _nt(table) {}

    virtual ~MicroTable() {}

    size_t getFullNumberOfColumns() const { return _nt->getNumberOfColumns(); }
    size_t getFullNumberOfRows()    const { return _nt->getNumberOfRows(); }
    NumericTableIface::StorageLayout getDataLayout() const { return _nt->getDataLayout(); }

protected:
    NumericTable *_nt;
};

template <typename T, ReadWriteMode rwflag, CpuType cpu>
class BlockMicroTable : public MicroTable
{
public:
    BlockMicroTable() {}
    BlockMicroTable(const NumericTable *table) : MicroTable(table) {}
    BlockMicroTable(NumericTable *table) : MicroTable(table) {}

    size_t getBlockOfRows(size_t vector_idx, size_t vector_num, T **buf_ptr)
    {
        _nt->getBlockOfRows( vector_idx, vector_num, rwflag, _block );
        *buf_ptr = _block.getBlockPtr();
        return _block.getNumberOfRows();
    }
    void release()
    {
        _nt->releaseBlockOfRows( _block );
    }

protected:
    BlockDescriptor<T> _block;
};

template <typename T, ReadWriteMode rwflag, CpuType cpu>
class FeatureMicroTable : public MicroTable
{
public:
    FeatureMicroTable(const NumericTable *table) : MicroTable(table) {}
    FeatureMicroTable(NumericTable *table) : MicroTable(table) {}

    size_t getBlockOfColumnValues(size_t feature_idx, size_t vector_idx, size_t value_num, T **buf_ptr)
    {
        _nt->getBlockOfColumnValues( feature_idx, vector_idx, value_num, rwflag, _block );
        *buf_ptr = _block.getBlockPtr();
        return _block.getNumberOfRows();
    }
    void release()
    {
        _nt->releaseBlockOfColumnValues( _block );
    }

protected:
    BlockDescriptor<T> _block;
};

template <typename T, ReadWriteMode rwflag, CpuType cpu>
class CSRBlockMicroTable : public MicroTable
{
public:
    CSRBlockMicroTable(const NumericTable *table) : MicroTable(table)
    {
        _snt = dynamic_cast<CSRNumericTableIface *>(_nt);
    }

    CSRBlockMicroTable(NumericTable *table) : MicroTable(table)
    {
        _snt = dynamic_cast<CSRNumericTableIface *>(_nt);
    }

    size_t getSparseBlock(size_t vector_idx, size_t vector_num, T **values_ptr, size_t **column_indices,
                          size_t **row_offsets)
    {
        _snt->getSparseBlock( vector_idx, vector_num, rwflag, _block );
        *values_ptr     = _block.getBlockValuesPtr();
        *column_indices = _block.getBlockColumnIndicesPtr();
        *row_offsets    = _block.getBlockRowIndicesPtr();
        return _block.getNumberOfRows();
    }
    void release()
    {
        _snt->releaseSparseBlock( _block );
    }

protected:
    CSRBlockDescriptor<T> _block;

    CSRNumericTableIface *_snt;
};

template <typename T, ReadWriteMode rwflag, CpuType cpu>
class PackedArrayMicroTable : public MicroTable
{
public:
    PackedArrayMicroTable(const NumericTable *table) : MicroTable(table)
    {
        _pnt = dynamic_cast<PackedArrayNumericTableIface *>(_nt);
    }

    PackedArrayMicroTable(NumericTable *table) : MicroTable(table)
    {
        _pnt = dynamic_cast<PackedArrayNumericTableIface *>(_nt);
    }

    size_t getPackedArray(T **values_ptr)
    {
        _pnt->getPackedArray( rwflag, _block );
        *values_ptr = _block.getBlockPtr();
        return _block.getNumberOfRows();
    }
    void release()
    {
        _pnt->releasePackedArray( _block );
    }

protected:
    BlockDescriptor<T> _block;

    PackedArrayNumericTableIface *_pnt;
};

} // namespace daal::internal
} // namespace daal

#endif
