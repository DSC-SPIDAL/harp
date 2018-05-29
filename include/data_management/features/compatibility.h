/* file: compatibility.h */
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

#ifndef __DATA_MANAGEMENT_FEATURES_COMPATIBILITY_H__
#define __DATA_MANAGEMENT_FEATURES_COMPATIBILITY_H__

#include "data_management/features/defines.h"
#include "data_management/features/internal/helpers.h"
#include "data_management/data/internal/conversion.h"

namespace daal
{
namespace data_management
{
/**
 * \brief Contains service functionality that simplifies feature handling
 */
namespace data_feature_utils
{

const int NumOfIndexNumTypes = (int)(data_management::features::DAAL_OTHER_T);

/* Usings for IndexNumType */
using data_management::features::IndexNumType;
using data_management::features::DAAL_FLOAT32;
using data_management::features::DAAL_FLOAT64;
using data_management::features::DAAL_INT32_S;
using data_management::features::DAAL_INT32_U;
using data_management::features::DAAL_INT64_S;
using data_management::features::DAAL_INT64_U;
using data_management::features::DAAL_INT8_S;
using data_management::features::DAAL_INT8_U;
using data_management::features::DAAL_INT16_S;
using data_management::features::DAAL_INT16_U;
using data_management::features::DAAL_OTHER_T;

/* Usings for PMMLNumType */
using data_management::features::PMMLNumType;
using data_management::features::DAAL_GEN_FLOAT;
using data_management::features::DAAL_GEN_DOUBLE;
using data_management::features::DAAL_GEN_INTEGER;
using data_management::features::DAAL_GEN_BOOLEAN;
using data_management::features::DAAL_GEN_STRING;
using data_management::features::DAAL_GEN_UNKNOWN;

/* Usings for FeatureType */
using data_management::features::FeatureType;
using data_management::features::DAAL_CATEGORICAL;
using data_management::features::DAAL_ORDINAL;
using data_management::features::DAAL_CONTINUOUS;

/* Usings for InternalNumType */
typedef data_management::internal::ConversionDataType InternalNumType;
using data_management::internal::DAAL_SINGLE;
using data_management::internal::DAAL_DOUBLE;
using data_management::internal::DAAL_INT32;
using data_management::internal::DAAL_OTHER;

/* Usings for helper functions */
using data_management::features::internal::getIndexNumType;
using data_management::features::internal::getPMMLNumType;
using data_management::internal::vectorConvertFuncType;
using data_management::internal::vectorStrideConvertFuncType;
DAAL_EXPORT vectorConvertFuncType getVectorUpCast(int, int);
DAAL_EXPORT vectorConvertFuncType getVectorDownCast(int, int);
DAAL_EXPORT vectorStrideConvertFuncType getVectorStrideUpCast(int, int);
DAAL_EXPORT vectorStrideConvertFuncType getVectorStrideDownCast(int, int);


template<typename T>
inline InternalNumType getInternalNumType()
{
    return data_management::internal::getConversionDataType<T>();
}

} // namespace data_feature_utils
} // namespace data_management
} // namespace daal

namespace DataFeatureUtils = daal::data_management::data_feature_utils;

#endif
