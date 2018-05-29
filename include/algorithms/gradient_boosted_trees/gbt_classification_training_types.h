/* file: gbt_classification_training_types.h */
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
//  Implementation of gradient boosted trees classification training algorithm interface.
//--
*/

#ifndef __GBT_CLASSIFICATION_TRAINING_TYPES_H__
#define __GBT_CLASSIFICATION_TRAINING_TYPES_H__

#include "algorithms/algorithm.h"
#include "algorithms/classifier/classifier_training_types.h"
#include "algorithms/gradient_boosted_trees/gbt_classification_model.h"
#include "algorithms/gradient_boosted_trees/gbt_training_parameter.h"

namespace daal
{
namespace algorithms
{
namespace gbt
{
namespace classification
{
/**
 * @defgroup gbt_classification_training Training
 * \copydoc daal::algorithms::gbt::classification::training
 * @ingroup gbt_classification
 * @{
 */
/**
 * \brief Contains classes for Gradient Boosted Trees models training
 */
namespace training
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__GBT__CLASSIFICATION__TRAINING__METHOD"></a>
 * \brief Computation methods for gradient boosted trees classification model-based training
 */
enum Method
{
    xboost = 0,       /*!< Extreme boosting (second-order approximation of objective function,
                           regularization on number of leaves and their weights), Chen et al. */
    defaultDense = 0  /*!< Default training method */
};

/**
* <a name="DAAL-ENUM-ALGORITHMS__GBT__CLASSIFICATION__TRAINING__LOSS_FUNCTION_TYPE"></a>
* \brief Loss function type
*/
enum LossFunctionType
{
    crossEntropy, /* Multinomial deviance */
    custom        /* custom function type */
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-STRUCT-ALGORITHMS__GBT__CLASSIFICATION__TRAINING__PARAMETER"></a>
 * \brief Gradient Boosted Trees algorithm parameters
 *
 * \snippet gradient_boosted_trees/gbt_classification_training_types.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public classifier::Parameter, public daal::algorithms::gbt::training::Parameter
{
    /** Default constructor */
    Parameter(size_t nClasses) : classifier::Parameter(nClasses), loss(crossEntropy) {}
    services::Status check() const DAAL_C11_OVERRIDE;
    LossFunctionType loss; /* Defaut is crossEntropy */
};
/* [Parameter source code] */


/**
 * <a name="DAAL-CLASS-ALGORITHMS__GBT__CLASSIFICATION__TRAINING__RESULT"></a>
 * \brief Provides methods to access the result obtained with the compute() method
 *        of model-based training
 */
class DAAL_EXPORT Result : public classifier::training::Result
{
public:
    DECLARE_SERIALIZABLE_CAST(Result);

    Result();
    virtual ~Result() {}

    /**
     * Returns the model trained with the LogitBoost algorithm
     * \param[in] id    Identifier of the result, \ref classifier::training::ResultId
     * \return          Model trained with the LogitBoost algorithm
     */
    ModelPtr get(classifier::training::ResultId id) const;

    /**
    * Sets the result of model-based training
    * \param[in] id      Identifier of the result
    * \param[in] value   Result
    */
    void set(classifier::training::ResultId id, const ModelPtr &value);

    /**
     * Allocates memory to store final results of the LogitBoost training algorithm
     * \param[in] input         %Input of the LogitBoost training algorithm
     * \param[in] parameter     Parameters of the algorithm
     * \param[in] method        LogitBoost computation method
     * \return Status of allocation
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**s
    * Checks the result of model-based training
    * \param[in] input   %Input object for the algorithm
    * \param[in] par     %Parameter of the algorithm
    * \param[in] method  Computation method
    * \return Status of checking
    */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        return daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);
    }
};
typedef services::SharedPtr<Result> ResultPtr;

} // namespace interface1
using interface1::Parameter;
using interface1::Result;
using interface1::ResultPtr;

} // namespace daal::algorithms::gbt::classification::training
/** @} */
}
}
}
} // namespace daal
#endif // __GBT_CLASSIFICATION_TRAINING_TYPES_H__
