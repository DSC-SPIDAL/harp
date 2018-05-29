/* file: linear_regression_quality_metric_set_batch.h */
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
//  Interface for the linear regression quality metric batch.
//--
*/

#ifndef __LINEAR_REGRESSION_QUALITY_METRIC_SET_BATCH_H__
#define __LINEAR_REGRESSION_QUALITY_METRIC_SET_BATCH_H__

#include "algorithms/algorithm.h"
#include "algorithms/algorithm_quality_metric_set_batch.h"
#include "algorithms/linear_regression/linear_regression_quality_metric_set_types.h"

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
/**
 * \brief Contains classes to check the quality of the model trained with the linear regression algorithm
 */
namespace quality_metric_set
{

namespace interface1
{
/**
 * @defgroup linear_regression_quality_metric_set_batch Batch
 * @ingroup linear_regression_quality_metric_set
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__LINEAR_REGRESSION__QUALITY_METRIC_SET__BATCH"></a>
 * \brief Class that represents a quality metric set to check the model trained with linear regression algorithm
 *
 * \par Enumerations
 *      - \ref QualityMetricId  Identifiers of quality metrics provided by the library
 *
 * \par References
 *      - \ref algorithms::quality_metric_set::interface1::InputAlgorithmsCollection "algorithms::quality_metric_set::InputAlgorithmsCollection" class
 */
class DAAL_EXPORT Batch : public algorithms::quality_metric_set::Batch
{
public:
    Parameter parameter;    /*!< Parameters of the algorithm */

    /**
     * Constructs a quality metric set for the model trained with the linear regression algorithm
     * \param[in] useDefaultMetrics     Flag. If true, a quality metric set is initialized with the quality metrics provided by the library
     * \param[in] nBeta                 Number of beta coefficients (p) of linear regression model used for prediction
     * \param[in] nBetaReducedModel     Number of beta coefficients (p0) used for prediction with reduced linear regression model where p - p0 of p beta coefficients are set to 0
     */
    Batch(size_t nBeta, size_t nBetaReducedModel, bool useDefaultMetrics = true):
        algorithms::quality_metric_set::Batch(useDefaultMetrics), parameter(nBeta, nBetaReducedModel)
    {
        _inputData = InputDataCollectionPtr(new InputDataCollection());
        if (_useDefaultMetrics)
        {
            initializeQualityMetrics();
        }
        _resultCollection = ResultCollectionPtr(new ResultCollection());
    }

    virtual ~Batch() {}

    /**
     * Returns the structure that contains a computed quality metric set
     * \return Structure that contains a computed quality metric set
     */
    ResultCollectionPtr getResultCollection()
    {
        return services::staticPointerCast<ResultCollection,
                                           algorithms::quality_metric_set::ResultCollection>(_resultCollection);
    }

    /**
     * Returns the collection of input objects of the quality metrics algorithm
     * \return Collection of input objects of the quality metrics algorithm
     */
    InputDataCollectionPtr getInputDataCollection()
    {
        return services::staticPointerCast<InputDataCollection,
                                           algorithms::quality_metric_set::InputDataCollection>(_inputData);
    }

protected:
    virtual void initializeQualityMetrics();
};
/** @} */
} // namespace interface1
using interface1::Batch;

}
}
}
}
#endif
