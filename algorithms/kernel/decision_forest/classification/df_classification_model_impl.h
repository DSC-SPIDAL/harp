/* file: df_classification_model_impl.h */
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
//  Implementation of the class defining the decision forest model
//--
*/

#ifndef __DF_CLASSIFICATION_MODEL_IMPL__
#define __DF_CLASSIFICATION_MODEL_IMPL__

#include "df_model_impl.h"
#include "algorithms/decision_forest/decision_forest_classification_model.h"
#include "classifier_model_impl.h"

namespace daal
{
namespace algorithms
{
namespace decision_forest
{
namespace classification
{
namespace internal
{

class ModelImpl : public decision_forest::classification::Model, public algorithms::classifier::internal::ModelInternal, public decision_forest::internal::ModelImpl
{
public:
    typedef decision_forest::internal::ModelImpl ImplType;
    typedef algorithms::classifier::internal::ModelInternal ClassifierImplType;
    typedef decision_forest::internal::TreeImpClassification<> TreeType;
    ModelImpl(size_t nFeatures = 0) : ClassifierImplType(nFeatures) {}
    ~ModelImpl(){}

    //Implementation of classifier::Model
    virtual size_t getNumberOfFeatures() const DAAL_C11_OVERRIDE { return ClassifierImplType::getNumberOfFeatures(); }
    virtual size_t getNFeatures() const DAAL_C11_OVERRIDE { return getNumberOfFeatures(); }
    virtual void setNFeatures(size_t nFeatures) DAAL_C11_OVERRIDE { ClassifierImplType::setNumberOfFeatures(nFeatures); }

    //Implementation of decision_forest::classification::Model
    virtual size_t numberOfTrees() const DAAL_C11_OVERRIDE;
    virtual void traverseDF(size_t iTree, classifier::TreeNodeVisitor& visitor) const DAAL_C11_OVERRIDE;
    virtual void traverseBF(size_t iTree, classifier::TreeNodeVisitor& visitor) const DAAL_C11_OVERRIDE;

    virtual services::Status serializeImpl(data_management::InputDataArchive * arch) DAAL_C11_OVERRIDE;
    virtual services::Status deserializeImpl(const data_management::OutputDataArchive * arch) DAAL_C11_OVERRIDE;

    bool add(const TreeType& tree);
};

} // namespace internal
} // namespace classification
} // namespace decision_forest
} // namespace algorithms
} // namespace daal

#endif
