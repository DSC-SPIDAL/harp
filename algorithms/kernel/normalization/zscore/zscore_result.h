/* file: zscore_result.h */
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
//  Implementation of zscore algorithm and types methods.
//--
*/

#ifndef __ZSCORE_RESULT_H__
#define __ZSCORE_RESULT_H__

#include "zscore_types.h"
#include "inner/zscore_result_v1.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace normalization
{
namespace zscore
{

namespace interface2
{

class ResultImpl : public interface1::ResultImpl
{
public:
    DAAL_CAST_OPERATOR(ResultImpl);

    ResultImpl(const size_t n) : interface1::ResultImpl(n) {}
    ResultImpl(const ResultImpl& o) : interface1::ResultImpl(o){}

    using DataCollection::operator[];
    virtual ~ResultImpl() {};

    /**
    * Allocates memory to store final results of the z-score normalization algorithms
    * \param[in] input     Input objects for the z-score normalization algorithm
    * \param[in] parameter Pointer to algorithm parameter
    *
    * \return Status of computations
    */
    template <typename algorithmFPType>
    services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter);

    /**
    * Checks the correctness of the Result object
    * \param[in] in     Pointer to the input object
    * \param[in] par    Pointer to algorithm parameter
    *
    * \return Status of computations
    */
    services::Status check(const daal::algorithms::Input *in, const daal::algorithms::Parameter *par) const;
};

}// namespace interface2

}// namespace zscore
}// namespace normalization
}// namespace algorithms
}// namespace daal

#endif
