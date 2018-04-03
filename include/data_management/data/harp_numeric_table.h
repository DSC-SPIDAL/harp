/* file: harp_numeric_table.h */
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

#ifndef __HARP_NUMERIC_TABLE_H__
#define __HARP_NUMERIC_TABLE_H__

#include "data_management/data/numeric_table.h"

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
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__HARP_NUMERICTABLE"></a>
 *  \brief Class that provides methods to access data stored as a structure of arrays,
 *         where each (contiguous) array represents values corresponding to a specific feature.
 */
class DAAL_EXPORT HarpNumericTable : public NumericTable
{
public:
    // DECLARE_SERIALIZABLE_TAG();

    /**
     *  Constructor for a Numeric Table
     *  \param[in]  featnum        Number of columns in the table
     *  \param[in]  obsnum         Number of rows in the table
     *  \param[in]  featuresEqual  Flag that makes all features in the Numeric Table Data Dictionary equal
     */
    HarpNumericTable( size_t featnum, size_t obsnum, DictionaryIface::FeaturesEqual featuresEqual = DictionaryIface::notEqual)
        : NumericTable(featnum,obsnum, featuresEqual) {}   

    //added by HarpDAAL
    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesST(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<int>** block) {}

    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<double>** block) {}
    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<float>** block) {}
    virtual void getBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, size_t vector_idx, size_t value_num, ReadWriteMode rwflag, BlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, BlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, BlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesST(size_t feature_start, size_t feature_len, BlockDescriptor<int>** block) {}

    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, BlockDescriptor<double>** block) {}
    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, BlockDescriptor<float>** block) {}
    virtual void releaseBlockOfColumnValuesMT(size_t feature_start, size_t feature_len, BlockDescriptor<int>** block) {}

    virtual void setKeyIdx(long key, long idx) {}

    virtual long* getKeys() {return NULL;}

};

typedef services::SharedPtr<HarpNumericTable> HarpNumericTablePtr;
/** @} */
} // namespace interface1
using interface1::HarpNumericTable;
using interface1::HarpNumericTablePtr;

}
} // namespace daal
#endif
