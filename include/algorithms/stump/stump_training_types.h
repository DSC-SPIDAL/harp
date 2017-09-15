/* file: stump_training_types.h */
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
//  Implementation of the interface of the decision stump training algorithm.
//--
*/

#ifndef __STUMP_TRAINING_TYPES_H__
#define __STUMP_TRAINING_TYPES_H__

#include "algorithms/algorithm.h"
#include "algorithms/classifier/classifier_training_types.h"
#include "algorithms/weak_learner/weak_learner_training_types.h"
#include "algorithms/stump/stump_model.h"

namespace daal
{
namespace algorithms
{
/**
 * \brief Contains classes to work with the decision stump training algorithm
 */
namespace stump
{
/**
 * @defgroup stump_training Training
 * \copydoc daal::algorithms::stump::training
 * @ingroup stump
 * @{
 */
/**
 * \brief Contains classes to train the decision stump model
 */
namespace training
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__STUMP__TRAINING__METHOD"></a>
 * Available methods to train the decision stump model
 */
enum Method
{
    defaultDense = 0        /*!< Default method */
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__STUMP__TRAINING__RESULT"></a>
 * \brief Provides methods to access final results obtained with the compute() method of the decision stump training algorithm
 * in the batch processing mode
 */
class DAAL_EXPORT Result : public daal::algorithms::weak_learner::training::Result
{
public:
    DECLARE_SERIALIZABLE_CAST(Result);
    Result();

    virtual ~Result() {}

    /**
     * Returns the model trained with the Stump algorithm
     * \param[in] id    Identifier of the result, \ref classifier::training::ResultId
     * \return          Model trained with the Stump algorithm
     */
    daal::algorithms::stump::ModelPtr get(classifier::training::ResultId id) const;

    /**
     * Sets the result of the training stage of the stump algorithm
     * \param[in] id      Identifier of the result, \ref classifier::training::ResultId
     * \param[in] value   Pointer to the training result
     */
    void set(classifier::training::ResultId id, daal::algorithms::stump::ModelPtr &value);

    /**
     * Allocates memory to store final results of the decision stump training algorithm
     * \tparam algorithmFPType  Data type to store prediction results
     * \param[in] input         %Input objects for the decision stump training algorithm
     * \param[in] parameter     Parameters of the decision stump training algorithm
     * \param[in] method        Decision stump training method
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Check the correctness of the Result object
     * \param[in] input     Pointer to the input object
     * \param[in] parameter Pointer to the parameters structure
     * \param[in] method    Algorithm computation method
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);

        return services::Status();
    }

    services::Status serializeImpl(data_management::InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(arch);

        return services::Status();
    }

    services::Status deserializeImpl(const data_management::OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(arch);

        return services::Status();
    }
};
typedef services::SharedPtr<Result> ResultPtr;
} // namespace interface1
using interface1::Result;
using interface1::ResultPtr;

} // namespace daal::algorithms::stump::training
/** @} */
}
}
} // namespace daal
#endif // __STUMP_TRAINING_TYPES_H__
