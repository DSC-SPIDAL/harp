/* file: defines.h */
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
//  Implementation of data dictionary utilities.
//--
*/

#ifndef __DATA_MANAGEMENT_FEATURES_DEFINES_H__
#define __DATA_MANAGEMENT_FEATURES_DEFINES_H__

#include <string>
#include <climits>
#include <cfloat>
#include <limits>

#include "services/daal_defines.h"

namespace daal
{
namespace data_management
{
/**
 * \brief Contains service functionality that simplifies feature handling
 */
namespace features
{
/**
 * @ingroup data_model
 * @{
 */

enum IndexNumType
{
    DAAL_FLOAT32 = 0,
    DAAL_FLOAT64 = 1,
    DAAL_INT32_S = 2,
    DAAL_INT32_U = 3,
    DAAL_INT64_S = 4,
    DAAL_INT64_U = 5,
    DAAL_INT8_S  = 6,
    DAAL_INT8_U  = 7,
    DAAL_INT16_S = 8,
    DAAL_INT16_U = 9,
    DAAL_OTHER_T = 10
};

enum PMMLNumType
{
    DAAL_GEN_FLOAT   = 0,
    DAAL_GEN_DOUBLE  = 1,
    DAAL_GEN_INTEGER = 2,
    DAAL_GEN_BOOLEAN = 3,
    DAAL_GEN_STRING  = 4,
    DAAL_GEN_UNKNOWN = 0xfffffff
};

enum FeatureType
{
    DAAL_CATEGORICAL = 0,
    DAAL_ORDINAL     = 1,
    DAAL_CONTINUOUS  = 2
};

/** @} */

} // namespace features
} // namespace data_management
} // namespace daal

#endif
