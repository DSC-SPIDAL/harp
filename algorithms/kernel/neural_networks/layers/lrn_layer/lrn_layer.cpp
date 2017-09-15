/* file: lrn_layer.cpp */
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
//  Implementation of lrn calculation algorithm and types methods.
//--
*/

#include "lrn_layer_types.h"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace layers
{
namespace lrn
{
namespace interface1
{
/**
*  Constructs parameters of the local response normalization layer
*  \param[in] dimension_ Numeric table of size 1 x 1 with index of type size_t to calculate local response normalization.
*  \param[in] kappa_     Value of hyper-parameter kappa
*  \param[in] alpha_     Value of hyper-parameter alpha
*  \param[in] beta_      Value of hyper-parameter beta
*  \param[in] nAdjust_   Value of hyper-parameter n
*/
Parameter::Parameter(data_management::NumericTablePtr dimension_,
                     const double kappa_,
                     const double alpha_,
                     const double beta_ ,
                     const size_t nAdjust_) :
    dimension(dimension_),
    kappa(kappa_),
    alpha(alpha_),
    beta(beta_),
    nAdjust(nAdjust_)
{};

    /**
 * Checks the correctness of the parameter
 */
services::Status Parameter::check() const
{
    services::SharedPtr<services::Error> error(new services::Error());
    if(dimension.get() == NULL)
    {
        error->setId(services::ErrorIncorrectParameter);
    }
    else if(dimension->getNumberOfRows() != 1)
    {
        error->setId(services::ErrorIncorrectNumberOfObservations);
    }
    else if(dimension->getNumberOfColumns() != 1)
    {
        error->setId(services::ErrorIncorrectNumberOfFeatures);
    }
    if(error->id() != services::NoErrorMessageFound)
    {
        error->addStringDetail(services::ArgumentName, "dimension");
        return services::Status(error);
    }
    return services::Status();
}

}// namespace interface1
}// namespace lrn
}// namespace layers
}// namespace neural_networks
}// namespace algorithms
}// namespace daal
