/* file: boosting_predict.h */
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
//  Implementation of base classes defining interface for prediction
//  based on Boosting algorithm model.
//--
*/

#ifndef __BOOSTING_PREDICT_H__
#define __BOOSTING_PREDICT_H__

#include "algorithms/classifier/classifier_predict.h"
#include "algorithms/boosting/boosting_model.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup boosting Boosting
 * \brief Contains classes to work with boosting algorithms
 * @ingroup classification
 */
namespace boosting
{
/**
 * \brief Contains classes for prediction based on %boosting models.
 */
namespace prediction
{

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @defgroup boosting_prediction Prediction
 * \copydoc daal::algorithms::boosting::prediction
 * @ingroup boosting
 * @{
 */
/**
 * @defgroup boosting_prediction_batch Batch
 * @ingroup boosting_prediction
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__BOOSTING__PREDICTION__BATCH"></a>
 * \brief %Base class for predicting results of %boosting classifiers
 *
 * \par Enumerations
 *      - \ref classifier::prediction::NumericTableInputId  Identifiers of input NumericTable objects for %boosting algorithms
 *      - \ref classifier::prediction::ModelInputId         Identifiers of input Model objects for %boosting algorithms
 *      - \ref classifier::prediction::ResultId             Result identifiers for %boosting algorithm training
 *
 * \par References
 *      - \ref classifier::prediction::interface1::Input "classifier::prediction::Input" class
 *      - \ref classifier::prediction::interface1::Result "classifier::prediction::Result" class
 */
class Batch : public classifier::prediction::Batch
{
public:
    typedef classifier::prediction::Batch super;

    typedef super::InputType                InputType;
    typedef algorithms::boosting::Parameter ParameterType;
    typedef super::ResultType               ResultType;

    Batch() {}

    /**
     * Constructs %boosting classifier prediction algorithm by copying input objects and parameters
     * of another %boosting classifier prediction algorithm
     * \param[in] other An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    Batch(const Batch &other) : classifier::prediction::Batch(other) {}

    virtual ~Batch() {}

    /**
     * Returns a pointer to the newly allocated %boosting classifier prediction algorithm with a copy of input objects
     * and parameters of this %boosting classifier prediction algorithm
     * \return Pointer to the newly allocated algorithm
     */
    services::SharedPtr<Batch> clone() const
    {
        return services::SharedPtr<Batch>(cloneImpl());
    }

protected:
    virtual Batch * cloneImpl() const DAAL_C11_OVERRIDE = 0;
};
/** @} */
} // namespace interface1
using interface1::Batch;

} // namespace daal::algorithms::boosting::prediction
}
}
} // namespace daal
#endif // __BOOSTING_PREDICT_H__
