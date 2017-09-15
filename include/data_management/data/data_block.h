/* file: data_block.h */
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
//  Implementation of the DataBlock type
//--
*/

#ifndef __DAAL_DATABLOCK_H__
#define __DAAL_DATABLOCK_H__

#include "services/base.h"
#include "services/daal_shared_ptr.h"

namespace daal
{
namespace data_management
{

namespace interface1
{
/**
 * @ingroup serialization
 * @{
 */
/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__DATABLOCKIFACE"></a>
 * \brief Abstract interface class for a data management component responsible for a pointer to a byte array and its size.
 * This class declares the most general methods for data access.
 */
class DataBlockIface : public Base
{
public:
    virtual ~DataBlockIface() {}
    /**
    * Returns a pointer to a byte array stored in DataBlock
    * \return Pointer to the byte array stored in DataBlock
    */
    virtual byte *getPtr() const = 0;
    /**
    * Returns a pointer to a byte array stored in DataBlock
    * \return Pointer to the byte array stored in DataBlock
    */
    virtual services::SharedPtr<byte> getSharedPtr() const = 0;
    /**
     * Returns the size of a byte array stored in DataBlock
     * \return Size of the byte array stored in DataBlock
     */
    virtual size_t getSize() const = 0;
    /**
     * Sets a pointer to a byte array
     * \param[in] ptr Pointer to the byte array
     */
    virtual void setPtr(byte *ptr) = 0;
    /**
     * Sets a pointer to a byte array
     * \param[in] ptr Pointer to the byte array
     */
    virtual void setPtr(const services::SharedPtr<byte> &ptr) = 0;
    /**
     * Sets the size of a byte array
     * \param[in] size Size of the byte array
     */
    virtual void setSize(size_t size) = 0;
};
/**
 * <a name="DAAL-CLASS-DATA_MANAGEMENT__DATABLOCK"></a>
 * \brief Class that stores a pointer to a byte array and its size. Not responsible for memory management
 */
class DAAL_EXPORT DataBlock : public DataBlockIface
{
public:
    /**
     * Default constructor. Creates an empty DataBlock of zero size with a zero pointer to a byte array
     */
    DataBlock() : _ptr(), _size(0)
    {}
    /**
     * Constructor. Creates DataBlock with a user-defined byte array
     * \param ptr Pointer to the byte array
     * \param size Size of the byte array
     */
    DataBlock(byte * ptr, size_t size) : _ptr(ptr, services::EmptyDeleter()), _size(size)
    {}
    /**
     * Constructor. Creates DataBlock with a user-defined byte array
     * \param ptr Pointer to the byte array
     * \param size Size of the byte array
     */
    DataBlock(const services::SharedPtr<byte> &ptr, size_t size) : _ptr(ptr), _size(size)
    {}
    /**
     * Constructor. Creates an empty DataBlock of a predefined size
     * \param size Size of the byte array
     */
    DataBlock(size_t size) : _ptr(), _size(size)
    {}
    /**
     * Copy constructor. Copies a pointer and the size stored in another DataBlock
     * \param block Reference to DataBlock
     */
    DataBlock(const DataBlock &block)
    {
       _ptr = block._ptr;
       _size = block._size;
    }

    virtual ~DataBlock() {}

    virtual byte *getPtr() const DAAL_C11_OVERRIDE
    {
        return _ptr.get();
    }

    virtual services::SharedPtr<byte> getSharedPtr() const DAAL_C11_OVERRIDE
    {
        return _ptr;
    }

    virtual size_t getSize() const DAAL_C11_OVERRIDE
    {
        return _size;
    }

    virtual void setPtr(byte *ptr) DAAL_C11_OVERRIDE
    {
        _ptr = services::SharedPtr<byte>(ptr, services::EmptyDeleter());
    }

    virtual void setPtr(const services::SharedPtr<byte> &ptr) DAAL_C11_OVERRIDE
    {
        _ptr = ptr;
    }

    virtual void setSize(size_t size) DAAL_C11_OVERRIDE
    {
        _size = size;
    }

private:
    services::SharedPtr<byte> _ptr;
    size_t _size;
};
typedef services::SharedPtr<DataBlock> DataBlockPtr;
/** @} */
} // namespace interface1
using interface1::DataBlock;
using interface1::DataBlockPtr;
using interface1::DataBlockIface;
}
}

#endif
