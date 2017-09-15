/* file: df_feature_type_helper.cpp */
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
//  Implementation of service data structures
//--
*/
#include "df_feature_type_helper.h"

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace internal
{

SortedFeaturesHelper::~SortedFeaturesHelper()
{
    if(_data)
        daal::services::daal_free(_data);
}

services::Status SortedFeaturesHelper::alloc(size_t nC, size_t nR)
{
    const size_t newCapacity = nC*(nR + 1);
    if(_data)
    {
        if(newCapacity > _capacity)
        {
            services::daal_free(_data);
            _data = nullptr;
            _capacity = 0;
            _data = (IndexType*)services::daal_malloc(sizeof(IndexType)*newCapacity);
            DAAL_CHECK_MALLOC(_data);
            _capacity = newCapacity;
        }
    }
    else
    {
        _data = (IndexType*)services::daal_malloc(sizeof(IndexType)*newCapacity);
        DAAL_CHECK_MALLOC(_data);
        _capacity = newCapacity;
    }
    _nCols = nC;
    _nRows = nR;
    return services::Status();
}

} /* namespace internal */
} /* namespace decision_forest */
} /* namespace algorithms */
} /* namespace daal */
