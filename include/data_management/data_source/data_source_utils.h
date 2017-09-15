/* file: data_source_utils.h */
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
//  Declaration and implementation of the base data source class.
//--
*/

#ifndef __DATA_SOURCE_UTILS_H__
#define __DATA_SOURCE_UTILS_H__

#include "data_management/data_source/data_source_dictionary.h"
#include "data_management/data/numeric_table.h"

namespace daal
{
namespace data_management
{

namespace interface1
{
/**
 * @ingroup data_sources
 * @{
 */
/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__STRINGROWFEATUREMANAGERIFACE"></a>
 *  \brief Abstract interface class that defines the interface to parse and convert the raw data represented as a string into a numeric format.
 *         The string must represent a row of data, a dictionary, or a vector of features
 */
class StringRowFeatureManagerIface
{
public:
    /**
     *  Parses a string that represents features of a data set and constructs a dictionary
     *  \param[in]  rawRowData   Array of characters with a string that contains information about features of the data set
     *  \param[in]  rawDataSize  Size of the rawRowData array
     *  \param[out] dict         Pointer to the dictionary constructed from the string
     */
    virtual void parseRowAsDictionary( char *rawRowData, size_t rawDataSize, DataSourceDictionary *dict ) = 0;

    /**
     *  Parses a string that represents a feature vector and converts it into a numeric representation
     *  \param[in]  rawRowData   Array of characters with a string that represents the feature vector
     *  \param[in]  rawDataSize  Size of the rawRowData array
     *  \param[in]  dict         Pointer to the dictionary
     *  \param[out] nt           Pointer to a Numeric Table to store the result of parsing
     *  \param[in]  ntRowIndex   Position in the Numeric Table at which to store the result of parsing
     */
    virtual void parseRowIn ( char *rawRowData, size_t rawDataSize, DataSourceDictionary *dict, NumericTable *nt,
                              size_t  ntRowIndex  ) = 0;
};
/** @} */
} // namespace interface1
using interface1::StringRowFeatureManagerIface;

}
}

#endif
