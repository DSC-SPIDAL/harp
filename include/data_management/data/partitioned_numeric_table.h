/* file: partitioned_numeric_table.h */
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

#ifndef __PARTITIONED_NUMERIC_TABLE_H__
#define __PARTITIONED_NUMERIC_TABLE_H__

#include "data_management/data/data_collection.h"
#include "data_management/data/merged_numeric_table.h"
#include "services/internal/error_handling_helpers.h"

namespace daal
{
namespace data_management
{
namespace interface1
{
/**
 * @ingroup numeric_tables
 * @{
 */
/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__PARTITIONEDNUMERICTABLE"></a>
 *  \brief Class that provides methods to access a collection of numeric tables as if they are joined by columns
 */
class DAAL_EXPORT PartitionedNumericTable : public NumericTable
{
private:
    typedef NumericTable super;

public:
    DECLARE_SERIALIZABLE_TAG();
    DECLARE_SERIALIZABLE_IMPL();

    /* */
    typedef size_t KeyType;

    /* */
    PartitionedNumericTable();

    /**
     * Constructs an empty Partitioned Numeric Table
     * \param[out] stat  Status of the Partitioned Numeric Table construction
     */
    static services::SharedPtr<PartitionedNumericTable> create(services::Status *stat = NULL)
    {
        DAAL_DEFAULT_CREATE_IMPL(PartitionedNumericTable);
    }

    /**
     * Constructs an empty Partitioned Numeric Table with a single partition
     * \param[out] stat  Status of the Partitioned Numeric Table construction
     */
    static services::SharedPtr<PartitionedNumericTable> create(const KeyType &key,
                                                               const NumericTablePtr &partition,
                                                               services::Status *stat = NULL)
    {
        services::Status status;

        services::SharedPtr<PartitionedNumericTable> table = PartitionedNumericTable::create(&status);
        if (!status)
        {
            services::internal::tryAssignStatus(stat, status);
            return services::SharedPtr<PartitionedNumericTable>();
        }

        status |= table->addPartition(key, partition);
        if (!status)
        {
            services::internal::tryAssignStatus(stat, status);
            return services::SharedPtr<PartitionedNumericTable>();
        }

        return table;
    }

    NumericTablePtr getPartition(const KeyType &key) const
    {
        return NumericTable::cast(_partitionsMap->get(key));
    }

    NumericTablePtr getPartitionByIndex(size_t index) const
    {
        return NumericTable::cast(_partitionsMap->getValueByIndex(index));
    }

    services::Status addPartition(const KeyType &key, const NumericTablePtr &partition)
    {
        _partitionsMap->set(key, partition);
        services::Status status = _mergedTable->addNumericTable(partition);
        super::_obsnum = _mergedTable->getNumberOfRows();
        return status;
    }

    size_t getNumberOfPartitions() const
    {
        return _partitionsMap->size();
    }

    services::Status resize(size_t nrow) DAAL_C11_OVERRIDE
    {
        super::_obsnum = nrow;
        return _mergedTable->resize(nrow);
    }

    MemoryStatus getDataMemoryStatus() const DAAL_C11_OVERRIDE
    {
        return _mergedTable->getDataMemoryStatus();
    }

    services::Status getBlockOfRows(size_t vector_idx, size_t vector_num,
                                    ReadWriteMode rwflag, BlockDescriptor<double>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfRows(vector_idx, vector_num, rwflag, block);
    }

    services::Status getBlockOfRows(size_t vector_idx, size_t vector_num,
                                    ReadWriteMode rwflag, BlockDescriptor<float>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfRows(vector_idx, vector_num, rwflag, block);
    }

    services::Status getBlockOfRows(size_t vector_idx, size_t vector_num,
                                    ReadWriteMode rwflag, BlockDescriptor<int>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfRows(vector_idx, vector_num, rwflag, block);
    }

    services::Status releaseBlockOfRows(BlockDescriptor<double>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfRows(block);
    }

    services::Status releaseBlockOfRows(BlockDescriptor<float>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfRows(block);
    }

    services::Status releaseBlockOfRows(BlockDescriptor<int>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfRows(block);
    }

    services::Status getBlockOfColumnValues(size_t feature_idx, size_t vector_idx, size_t value_num,
                                            ReadWriteMode rwflag, BlockDescriptor<double>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfColumnValues(feature_idx, vector_idx, value_num, rwflag, block);
    }

    services::Status getBlockOfColumnValues(size_t feature_idx, size_t vector_idx, size_t value_num,
                                            ReadWriteMode rwflag, BlockDescriptor<float>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfColumnValues(feature_idx, vector_idx, value_num, rwflag, block);
    }

    services::Status getBlockOfColumnValues(size_t feature_idx, size_t vector_idx, size_t value_num,
                                            ReadWriteMode rwflag, BlockDescriptor<int>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->getBlockOfColumnValues(feature_idx, vector_idx, value_num, rwflag, block);
    }

    services::Status releaseBlockOfColumnValues(BlockDescriptor<double>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfColumnValues(block);
    }

    services::Status releaseBlockOfColumnValues(BlockDescriptor<float>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfColumnValues(block);
    }

    services::Status releaseBlockOfColumnValues(BlockDescriptor<int>& block) DAAL_C11_OVERRIDE
    {
        return _mergedTable->releaseBlockOfColumnValues(block);
    }

    services::Status allocateBasicStatistics() DAAL_C11_OVERRIDE
    {
        return _mergedTable->allocateBasicStatistics();
    }

protected:
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        services::Status status = NumericTable::serialImpl<Archive, onDeserialize>(arch);
        arch->setSharedPtrObj(_mergedTable);
        arch->setSharedPtrObj(_partitionsMap);
        return status;
    }

    services::Status setNumberOfRowsImpl(size_t nrow) DAAL_C11_OVERRIDE
    {
        return _mergedTable->setNumberOfRows(nrow);
    }

    services::Status setNumberOfColumnsImpl(size_t ncol) DAAL_C11_OVERRIDE
    {
        return _ddict->setNumberOfFeatures(ncol);
    }

    explicit PartitionedNumericTable(services::Status &st);

private:
    MergedNumericTablePtr _mergedTable;
    KeyValueDataCollectionPtr _partitionsMap;
};
typedef services::SharedPtr<PartitionedNumericTable> PartitionedNumericTablePtr;

/** @} */
} // namespace interface1

using interface1::PartitionedNumericTable;
using interface1::PartitionedNumericTablePtr;

} // namespace data_management
} // namespace daal

#endif
