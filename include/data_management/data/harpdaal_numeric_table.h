/* file: harpdaal_numeric_table.h */
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
//  Implementation of a heterogeneous table stored as a structure of arrays.
//--
*/

#ifndef __HARPDAAL_NUMERIC_TABLE_H__
#define __HARPDAAL_NUMERIC_TABLE_H__

#include "services/base.h"
#include "services/daal_defines.h"
#include "services/daal_memory.h"
#include "services/error_handling.h"
#include "algorithms/algorithm_types.h"
#include "data_management/data/data_collection.h"
#include "data_management/data/data_dictionary.h"

#include "data_management/data/numeric_types.h"

#include "data_management/data/numeric_table.h"
#include "data_management/data/partitioned_numeric_table.h"
#include "tbb/scalable_allocator.h"
#include "daal_shared_ptr.h"

#include "hbwmalloc.h"

namespace daal
{
namespace data_management
{

namespace interface1
{

/**
 * @brief free memory allocated by scalable_malloc
 */
class HarpDAALSCALDeleter : public daal::services::interface1::DeleterIface
{

public:
    void operator() (const void *ptr) DAAL_C11_OVERRIDE
    {
        scalable_free((void *)ptr);
    }
};

/**
 * @brief free memory space allocated by memkind on HBM
 */
class HarpDAALHBMDeleter : public daal::services::interface1::DeleterIface
{

public:
    void operator() (const void *ptr) DAAL_C11_OVERRIDE
    {
        hbw_free((void *)ptr);
    }
};

template<typename DataType = DAAL_DATA_TYPE>
class DAAL_EXPORT HarpDAALBlockDescriptor
{/*{{{*/
public:
    /** \private */
    HarpDAALBlockDescriptor() : _ptr(), _buffer(), _capacity(0), _ncols(0), _nrows(0), _colsOffset(0), _rowsOffset(0), _rwFlag(0), _pPtr(0), _rawPtr(0)
    {}

    /** \private */
    ~HarpDAALBlockDescriptor() { freeBuffer(); }

    /**
     *  Gets a pointer to the buffer
     *  \return Pointer to the block
     */
    inline DataType *getBlockPtr() const
    {
        if(_rawPtr)
        {
            return (DataType *)_rawPtr;
        }
        return _ptr.get();
    }

    /**
     *  Gets a pointer to the buffer
     *  \return Pointer to the block
     */
    inline services::SharedPtr<DataType> getBlockSharedPtr() const
    {
        if(_rawPtr)
        {
            return services::SharedPtr<DataType>(services::reinterpretPointerCast<DataType, byte>(*_pPtr), (DataType *)_rawPtr);
        }
        return _ptr;
    }

    /**
     *  Returns the number of columns in the block
     *  \return Number of columns
     */
    inline size_t getNumberOfColumns() const { return _ncols; }

    /**
     *  Returns the number of rows in the block
     *  \return Number of rows
     */
    inline size_t getNumberOfRows() const { return _nrows; }

    /**
     * Reset internal values and pointers to zero values
     */
    inline void reset()
    {
        _colsOffset = 0;
        _rowsOffset = 0;
        _rwFlag = 0;
        _pPtr = NULL;
        _rawPtr = NULL;
    }

public:
    /**
     *  Sets data pointer to use for in-place calculation
     *  \param[in] ptr      Pointer to the buffer
     *  \param[in] nColumns Number of columns
     *  \param[in] nRows    Number of rows
     */
    inline void setPtr( DataType *ptr, size_t nColumns, size_t nRows )
    {
        _ptr   = services::SharedPtr<DataType>(ptr, services::EmptyDeleter());
        _ncols = nColumns;
        _nrows = nRows;
    }

    /**
     *  \param[in] pPtr Pointer to the shared pointer that handles the memory
     *  \param[in] rawPtr Pointer to she shifted memory
     *  \param[in] nColumns Number of columns
     *  \param[in] nRows Number of rows
     */
    void setPtr(services::SharedPtr<byte> *pPtr, byte *rawPtr, size_t nColumns, size_t nRows )
    {
        _pPtr = pPtr;
        _rawPtr = rawPtr;
        _ncols = nColumns;
        _nrows = nRows;
    }

    /**
     *  Allocates memory of (nColumns * nRows + auxMemorySize) size
     *  \param[in] nColumns      Number of columns
     *  \param[in] nRows         Number of rows
     *  \param[in] auxMemorySize Memory size
     *
     *  \return true if memory of (nColumns * nRows + auxMemorySize) size is allocated successfully
     */
    inline bool resizeBuffer( size_t nColumns, size_t nRows, size_t auxMemorySize = 0 )
    {/*{{{*/

        _ncols = nColumns;
        _nrows = nRows;

        size_t newSize = nColumns * nRows * sizeof(DataType) + auxMemorySize;

        if ( newSize  > _capacity )
        {
            freeBuffer();
            _buffer = services::SharedPtr<DataType>((DataType *)daal::services::daal_malloc(newSize), services::ServiceDeleter());
            if ( _buffer != 0 )
            {
                _capacity = newSize;
            }
            else
            {
                return false;
            }

        }

        _ptr = _buffer;
        if(!auxMemorySize)
        {
            if(_aux_ptr)
            {
                _aux_ptr = services::SharedPtr<DataType>();
            }
        }
        else
        {
            _aux_ptr = services::SharedPtr<DataType>(_buffer, _buffer.get() + nColumns * nRows);
        }

        return true;
    }/*}}}*/
    
    /**
     * @brief allocate buffer memory by using scalable_malloc, invoked from multi-threading 
     * operations
     *
     * @param nColumns
     * @param nRows
     * @param auxMemorySize
     *
     * @return 
     */
    bool resizeBufferSCAL( size_t nColumns, size_t nRows, size_t auxMemorySize = 0 )
    {/*{{{*/

        _ncols = nColumns;
        _nrows = nRows;

        size_t newSize = nColumns * nRows * sizeof(DataType) + auxMemorySize;

        if ( newSize  > _capacity )
        {
            this->freeBuffer();
            _buffer = services::SharedPtr<DataType>((DataType *)scalable_malloc(newSize), HarpDAALSCALDeleter());
            if ( _buffer != 0 )
            {
                _capacity = newSize;
            }
            else
            {
                return false;
            }

        }

        _ptr = _buffer;
        if(!auxMemorySize)
        {
            if(_aux_ptr)
            {
                _aux_ptr = services::SharedPtr<DataType>();
            }
        }
        else
        {
            _aux_ptr = services::SharedPtr<DataType>(_buffer, _buffer.get() + nColumns * nRows);
        }

        return true;
    }/*}}}*/

    /**
     * @brief allocate buffer memory space on 
     * High bandwidth memory (HBM) by invoking memkind library
     *
     * @param nColumns
     * @param nRows
     * @param auxMemorySize
     *
     * @return 
     */
    bool resizeBufferHBM( size_t nColumns, size_t nRows, size_t auxMemorySize = 0 )
    {/*{{{*/

        _ncols = nColumns;
        _nrows = nRows;

        size_t newSize = nColumns * nRows * sizeof(DataType) + auxMemorySize;

        if ( newSize  > _capacity )
        {
            this->freeBuffer();
            //check availability of hbm 
            if (!hbw_check_available())
                _buffer = services::SharedPtr<DataType>((DataType *)hbw_malloc(newSize), HarpDAALHBMDeleter());
            else // rollback to scalable_malloc
                _buffer = services::SharedPtr<DataType>((DataType *)scalable_malloc(newSize), HarpDAALSCALDeleter());

            if ( _buffer != 0 )
            {
                _capacity = newSize;
            }
            else
            {
                return false;
            }

        }

        _ptr = _buffer;
        if(!auxMemorySize)
        {
            if(_aux_ptr)
            {
                _aux_ptr = services::SharedPtr<DataType>();
            }
        }
        else
        {
            _aux_ptr = services::SharedPtr<DataType>(_buffer, _buffer.get() + nColumns * nRows);
        }

        return true;

    }/*}}}*/

    /**
     *  Sets parameters of the block
     *  \param[in]  columnIdx   Index of the first column in the block
     *  \param[in]  rowIdx      Index of the first row in the block
     *  \param[in]  rwFlag      Flag specifying read/write access to the block
     */
    inline void setDetails( size_t columnIdx, size_t rowIdx, int rwFlag )
    {
        _colsOffset = columnIdx;
        _rowsOffset = rowIdx;
        _rwFlag     = rwFlag;
    }

    /**
     *  Gets the number of columns in the numeric table preceding the first element in the block
     *  \return columns offset
     */
    inline size_t getColumnsOffset() const { return _colsOffset; }

    /**
     *  Gets the number of rows in the numeric table preceding the first element in the block
     *  \return rows offset
     */
    inline size_t getRowsOffset() const { return _rowsOffset; }

    /**
     *  Gets the flag specifying read/write access to the block
     *  \return flag
     */
    inline size_t getRWFlag() const { return _rwFlag; }

    /**
     *  Gets a pointer o the additional memory buffer
     *  \return pointer
     */
    inline void  *getAdditionalBufferPtr() const { return _aux_ptr.get(); }
    inline services::SharedPtr<DataType> getAdditionalBufferSharedPtr() const { return _aux_ptr; }

protected:
    /**
     *  Frees the buffer
     */
    void freeBuffer()
    {
        if(_buffer)
        {
            _buffer = services::SharedPtr<DataType>();
        }
        _capacity = 0;
    }

private:
    services::SharedPtr<DataType> _ptr;
    size_t    _nrows;
    size_t    _ncols;

    size_t _colsOffset;
    size_t _rowsOffset;
    int    _rwFlag;

    services::SharedPtr<DataType> _aux_ptr;

    services::SharedPtr<DataType> _buffer; /*<! Pointer to the buffer */
    size_t    _capacity;                   /*<! Buffer size in bytes */

    services::SharedPtr<byte> *_pPtr;
    byte *_rawPtr;
};/*}}}*/

/**
 * @ingroup numeric_tables
 * @{
 */
/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__HARP_NUMERICTABLE"></a>
 *  \brief Class that provides methods to access data stored as a structure of arrays,
 *         where each (contiguous) array represents values corresponding to a specific feature.
 */
class DAAL_EXPORT HarpDAALNumericTable : public PartitionedNumericTable
{
public:

    /**
     *  Constructor for a HarpDAALNumericTable 
     */
    HarpDAALNumericTable() : PartitionedNumericTable() {}   

    HarpDAALNumericTable(services::Status &stat)
        : PartitionedNumericTable(stat) {}   

    /**
     * Constructs an empty HarpDAAL Numeric Table
     * \param[out] stat  Status of the HarpDAAL Numeric Table construction
     */
    static services::SharedPtr<HarpDAALNumericTable> create(services::Status *stat = NULL)
    {
        DAAL_DEFAULT_CREATE_IMPL(HarpDAALNumericTable);
    }

    /**
     * Constructs an empty HarpDAAL Numeric Table with a single partition
     * \param[out] stat  Status of the HarpDAAL Numeric Table construction
     */
    static services::SharedPtr<HarpDAALNumericTable> create(const KeyType &key,
                                                               const NumericTablePtr &partition,
                                                               services::Status *stat = NULL)
    {
        services::Status status;

        services::SharedPtr<HarpDAALNumericTable> table = HarpDAALNumericTable::create(&status);
        if (!status)
        {
            services::internal::tryAssignStatus(stat, status);
            return services::SharedPtr<HarpDAALNumericTable>();
        }

        status |= table->addPartition(key, partition);
        if (!status)
        {
            services::internal::tryAssignStatus(stat, status);
            return services::SharedPtr<HarpDAALNumericTable>();
        }

        return table;
    }

    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<int>** block) {}

    virtual void getBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<int>** block) {}

    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<int>** block) {}

    virtual void getBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, HarpDAALBlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesSTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesMTHBM(size_t feature_start, size_t feature_len, HarpDAALBlockDescriptor<int>** block) {}

    // virtual void setKeyIdx(long key, long idx) {}
    // virtual long* getKeys() {return NULL;}

};

typedef services::SharedPtr<HarpDAALNumericTable> HarpDAALNumericTablePtr;
/** @} */
} // namespace interface1
using interface1::HarpDAALNumericTable;
using interface1::HarpDAALNumericTablePtr;

}
} // namespace daal
#endif
