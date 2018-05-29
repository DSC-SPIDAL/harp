/* file: classifier_model.h */
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
//  Implementation of the class defining the model of the classification  algorithm
//--
*/

#ifndef __CLASSIFIER_MODEL_H__
#define __CLASSIFIER_MODEL_H__

#include "algorithms/algorithm.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup classification Classification
 * \brief Contains classes for work with the classification algorithms
 * @ingroup training_and_prediction
 */
namespace classifier
{

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @ingroup classifier
 * @{
 */
/**
 * <a name="DAAL-STRUCT-ALGORITHMS__CLASSIFIER__PARAMETER"></a>
 * \brief Base class for the parameters of the classification algorithm
 *
 * \snippet classifier/classifier_model.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public daal::algorithms::Parameter
{
    Parameter(size_t nClasses = 2) : nClasses(nClasses) {}

    size_t nClasses;        /*!< Number of classes */

    services::Status check() const DAAL_C11_OVERRIDE;
};
/* [Parameter source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__CLASSIFIER__MODEL"></a>
 * \brief Base class for the model of the classification algorithm
 */
class DAAL_EXPORT Model : public daal::algorithms::Model
{
public:
    DAAL_CAST_OPERATOR(Model);

    virtual ~Model() {}

    /**
     *  Retrieves the number of features in the dataset was used on the training stage
     *  \DAAL_DEPRECATED_USE{ Model::getNumberOfFeatures }
     *  \return Number of features in the dataset was used on the training stage
     */
    virtual size_t getNFeatures() const { return getNumberOfFeatures(); }

    /**
     *  Retrieves the number of features in the dataset was used on the training stage
     *  \return Number of features in the dataset was used on the training stage
     */
    virtual size_t getNumberOfFeatures() const = 0;

    /**
     *  Sets the number of features in the dataset was used on the training stage
     *  \DAAL_DEPRECATED
     *  \param[in]  nFeatures  Number of features in the dataset was used on the training stage
     */
    virtual void setNFeatures(size_t nFeatures) {}
};
/** @} */
typedef services::SharedPtr<Model> ModelPtr;
typedef services::SharedPtr<const Model> ModelConstPtr;
} // namespace interface1
using interface1::Parameter;
using interface1::Model;
using interface1::ModelPtr;
using interface1::ModelConstPtr;

}
}
}
#endif
