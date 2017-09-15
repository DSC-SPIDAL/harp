/** file soa_numeric_table.cpp */
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

#include "soa_numeric_table.h"

namespace daal
{
namespace data_management
{
namespace interface1
{

SOANumericTable::SOANumericTable(size_t nColumns, size_t nRows, DictionaryIface::FeaturesEqual featuresEqual):
    NumericTable(nColumns, nRows, featuresEqual), _arrays(nColumns), _arraysInitialized(0), _partialMemStatus(notAllocated)
{
    _layout = soa;

    if( !resizePointersArray(nColumns) )
    {
        this->_status.add(services::ErrorMemoryAllocationFailed);
        return;
    }
}

services::SharedPtr<SOANumericTable> SOANumericTable::create(size_t nColumns, size_t nRows,
                                                             DictionaryIface::FeaturesEqual featuresEqual, services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(SOANumericTable, nColumns, nRows, featuresEqual);
}

SOANumericTable::SOANumericTable( NumericTableDictionaryPtr ddict, size_t nRows, AllocationFlag memoryAllocationFlag):
        NumericTable(ddict), _arraysInitialized(0), _partialMemStatus(notAllocated)
{
    _layout = soa;
    this->_status |= setNumberOfRowsImpl( nRows );
    if( !resizePointersArray( getNumberOfColumns() ) )
    {
        this->_status.add(services::ErrorMemoryAllocationFailed);
        return;
    }
    if( memoryAllocationFlag == doAllocate )
    {
        this->_status |= allocateDataMemoryImpl();
    }
}

services::SharedPtr<SOANumericTable> SOANumericTable::create(NumericTableDictionaryPtr ddict, size_t nRows,
                                                             AllocationFlag memoryAllocationFlag,
                                                             services::Status *stat)
{
    DAAL_DEFAULT_CREATE_IMPL_EX(SOANumericTable, ddict, nRows, memoryAllocationFlag);
}

SOANumericTable::SOANumericTable(size_t nColumns, size_t nRows, DictionaryIface::FeaturesEqual featuresEqual, services::Status &st):
        NumericTable(nColumns, nRows, featuresEqual, st), _arrays(nColumns), _arraysInitialized(0), _partialMemStatus(notAllocated)
{
    _layout = soa;
    if (!resizePointersArray(nColumns))
    {
        st.add(services::ErrorMemoryAllocationFailed);
        return;
    }
}

SOANumericTable::SOANumericTable(NumericTableDictionaryPtr ddict, size_t nRows, AllocationFlag memoryAllocationFlag, services::Status &st):
    NumericTable(ddict, st), _arraysInitialized(0), _partialMemStatus(notAllocated)
{
    _layout = soa;
    st |= setNumberOfRowsImpl( nRows );
    if( !resizePointersArray( getNumberOfColumns() ) )
    {
        st.add(services::ErrorMemoryAllocationFailed);
        return;
    }
    if( memoryAllocationFlag == doAllocate )
    {
        st |= allocateDataMemoryImpl();
    }
}

}
}
}
