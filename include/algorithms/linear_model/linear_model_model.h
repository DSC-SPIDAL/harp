/* file: linear_model_model.h */
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
//  Implementation of the class defining the linear regression model
//--
*/

#ifndef __LINEAR_MODEL_MODEL_H__
#define __LINEAR_MODEL_MODEL_H__

#include "algorithms/algorithm_types.h"
#include "algorithms/regression/regression_model.h"
#include "data_management/data/numeric_table.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup linear_model Linear Model
 * \brief Contains base classes of regression algorithms with linear model
 * @ingroup regression
 */
/**
 * \brief Contains classes of the regression algorithm
 */
namespace linear_model
{
/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @ingroup linear_model
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__LINEAR_MODEL__PARAMETER"></a>
 * \brief Parameters for the regression algorithm
 *
 * \snippet linear_model/linear_model_model.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public daal::algorithms::Parameter
{
    Parameter();
    Parameter(const Parameter &other);
    bool interceptFlag; /*!< Flag that indicates whether the intercept needs to be computed */
};
/* [Parameter source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__LINEAR_MODEL__MODEL"></a>
 * \brief %Base class for models trained with the regression algorithm
 *
 * \par References
 *      - \ref training::interface1::Batch "training::Batch" class
 *      - \ref prediction::interface1::Batch "prediction::Batch" class
 */
class DAAL_EXPORT Model : public regression::Model
{
public:
    DAAL_CAST_OPERATOR(Model)

    virtual ~Model()
    {}

    /**
     * Initializes ridge regression coefficients of the regression model
     */
    virtual services::Status initialize() = 0;

    /**
     * Returns the number of regression coefficients
     * \return Number of regression coefficients
     */
    virtual size_t getNumberOfBetas() const = 0;

    /**
     * Returns the number of responses in the training data set
     * \return Number of responses in the training data set
     */
    virtual size_t getNumberOfResponses() const = 0;

    /**
     * Returns true if the regression model contains the intercept term, and false otherwise
     * \return True if the regression model contains the intercept term, and false otherwise
     */
    virtual bool getInterceptFlag() const = 0;

    /**
     * Returns the numeric table that contains regression coefficients
     * \return Table that contains regression coefficients
     */
    virtual data_management::NumericTablePtr getBeta() = 0;
};

typedef services::SharedPtr<Model> ModelPtr;
typedef services::SharedPtr<const Model> ModelConstPtr;
/** @} */
}
using interface1::Parameter;
using interface1::Model;
using interface1::ModelPtr;
using interface1::ModelConstPtr;

/**
 * Checks the correctness of linear regression model
 * \param[in]  model             The model to check
 * \param[in]  par               The parameter of linear regression algorithm
 * \param[in]  nBeta             Required number of linear regression coefficients
 * \param[in]  nResponses        Required number of responses on the training stage
 *
 * \return Status of computations
 */
DAAL_EXPORT services::Status checkModel(
    linear_model::Model *model, const daal::algorithms::Parameter &par, size_t nBeta, size_t nResponses);

}
}
}
#endif
