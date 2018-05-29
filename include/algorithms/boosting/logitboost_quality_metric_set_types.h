/* file: logitboost_quality_metric_set_types.h */
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
//  Interface for the LogitBoost algorithm quality metrics
//--
*/

#ifndef __LOGITBOOST_QUALITY_METRIC_SET_TYPES_H__
#define __LOGITBOOST_QUALITY_METRIC_SET_TYPES_H__

#include "services/daal_shared_ptr.h"
#include "algorithms/algorithm_quality_metric_set_types.h"
#include "algorithms/classifier/multiclass_confusion_matrix_types.h"

namespace daal
{
namespace algorithms
{
namespace logitboost
{
/**
 * @defgroup logitboost_quality_metric_set Quality Metrics
 * \copydoc daal::algorithms::logitboost::quality_metric_set
 * @ingroup logitboost
 * @{
 */
namespace quality_metric_set
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__LOGITBOOST__QUALITY_METRIC_SET__QUALITYMETRICID"></a>
 * Available identifiers of the quality metrics available for the model trained with the LogitBoost algorithm
 */
enum QualityMetricId
{
    confusionMatrix,    /*!< Confusion matrix */
    lastQualityMetricId = confusionMatrix
};

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
* <a name="DAAL-STRUCT-ALGORITHMS__LOGITBOOST__QUALITY_METRIC_SET__PARAMETER"></a>
* \brief Parameters for the LogitBoost compute() method
*
* \snippet boosting/logitboost_quality_metric_set_types.h Parameter source code
*/
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public daal::algorithms::Parameter
{
    Parameter(size_t nClasses = 2);
    virtual ~Parameter() {}

    size_t nClasses;        /*!< Number of classes */
};
/* [Parameter source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOGITBOOST__QUALITY_METRIC_SET__RESULTCOLLECTION"></a>
 * \brief Class that implements functionality of the collection of result objects of the quality metrics algorithm
 *        specialized for using with the LogitBoost training algorithm
 */
class DAAL_EXPORT ResultCollection : public algorithms::quality_metric_set::ResultCollection
{
public:
    ResultCollection() {}
    virtual ~ResultCollection() {}

    /**
     * Returns the result of the quality metrics algorithm
     * \param[in] id   Identifier of the result
     * \return         Result that corresponds to the given identifier
     */
    classifier::quality_metric::multiclass_confusion_matrix::ResultPtr getResult(QualityMetricId id) const;
};
typedef services::SharedPtr<ResultCollection> ResultCollectionPtr;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LOGITBOOST__QUALITY_METRIC_SET__INPUTDATACOLLECTION"></a>
 * \brief Class that implements functionality of the collection of input objects of the quality metrics algorithm
 *        specialized for using with the LogitBoost training algorithm
 */
class DAAL_EXPORT InputDataCollection : public algorithms::quality_metric_set::InputDataCollection
{
public:
    InputDataCollection() {}
    virtual ~InputDataCollection() {}

    /**
     * Returns the input object of the quality metrics algorithm
     * \param[in] id    Identifier of the input object
     * \return          %Input object that corresponds to the given identifier
     */
    classifier::quality_metric::multiclass_confusion_matrix::InputPtr getInput(QualityMetricId id) const;
};
typedef services::SharedPtr<InputDataCollection> InputDataCollectionPtr;
}
using interface1::Parameter;
using interface1::ResultCollection;
using interface1::ResultCollectionPtr;
using interface1::InputDataCollection;
using interface1::InputDataCollectionPtr;

}
/** @} */
}
}
}

#endif // __LOGITBOOST_QUALITY_METRIC_SET_TYPES_H__
