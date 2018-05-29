/* file: algorithm_quality_metric_batch.h */
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
//  Interface for the quality metrics in the batch processing mode.
//--
*/

#ifndef __ALGORITHM_QUALITY_METRIC_BATCH_H__
#define __ALGORITHM_QUALITY_METRIC_BATCH_H__

#include "algorithms/analysis.h"

namespace daal
{
namespace algorithms
{
/**
 * \brief Contains classes to compute quality metrics
 */
namespace quality_metric
{

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @addtogroup base_algorithms
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__QUALITY_METRIC__BATCH"></a>
 * \brief Provides methods to compute quality metrics of an algorithm in the batch processing mode.
 *        Quality metric is a numerical characteristic or a set of connected numerical characteristics
 *        that represents the qualitative aspect of a computed statistical estimate, model,
 *        or decision-making result.
 */
class DAAL_EXPORT Batch : public Analysis<batch>
{
public:
    Batch() : Analysis<batch>() {}
    virtual ~Batch() {}

    /**
     * Sets an input object
     * \param[in] input Pointer to the input object
     */
    virtual void setInput(const algorithms::Input *input) = 0;

    /**
     * Returns the structure that contains computed quality metrics
     * \return Structure that contains computed quality metrics
     */
     algorithms::ResultPtr getResult() const
     {
        return getResultImpl();
     }

protected:
     virtual algorithms::ResultPtr getResultImpl() const = 0;
};
/** @} */
} // namespace interface1
using interface1::Batch;

} // namespace quality_metric
} // namespace algorithms
} // namespace daal
#endif
