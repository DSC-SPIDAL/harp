/* file: gbt_classification_model.h */
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
//  Implementation of class defining gradient boosted trees classification model.
//--
*/

#ifndef __GBT_CLASSIFICATION_MODEL_H__
#define __GBT_CLASSIFICATION_MODEL_H__

#include "algorithms/classifier/classifier_model.h"
#include "algorithms/regression/tree_traverse.h"

namespace daal
{
namespace algorithms
{
namespace gbt
{
/**
 * @defgroup gbt_classification Gradient Boosted Trees Classification
 * \copydoc daal::algorithms::gbt::classification
 * @ingroup classification
 */
/**
 * \brief Contains classes for the gbt classification algorithm
 */
namespace classification
{
/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @ingroup gbt_classification
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__GBT__CLASSIFICATION__MODEL"></a>
 * \brief %Model of the classifier trained by the gbt::training::Batch algorithm.
 *
 * \par References
 *      - \ref classification::training::interface1::Batch "training::Batch" class
 *      - \ref classification::prediction::interface1::Batch "prediction::Batch" class
 */
class DAAL_EXPORT Model : public classifier::Model
{
public:
    DECLARE_MODEL(Model, classifier::Model)

    /**
     * Constructs the model
     * \param[in]  nFeatures Number of features in the dataset
     * \param[out] stat      Status of the model construction
     */
    static services::SharedPtr<Model> create(size_t nFeatures, services::Status *stat = NULL);

    /**
     *  Gets number of trees in the model
     *  \return number of trees
     */
    virtual size_t numberOfTrees() const = 0;

    /**
     *  Performs Depth First Traversal of i-th tree
     *  \param[in] iTree    Index of the tree to traverse
     *  \param[in] visitor  This object gets notified when tree nodes are visited
     */
    virtual void traverseDF(size_t iTree, regression::TreeNodeVisitor& visitor) const = 0;

    /**
     *  Performs Breadth First Traversal of i-th tree
     *  \param[in] iTree    Index of the tree to traverse
     *  \param[in] visitor  This object gets notified when tree nodes are visited
     */
    virtual void traverseBF(size_t iTree, regression::TreeNodeVisitor& visitor) const = 0;

    /**
     *  Removes all trees from the model
     */
    virtual void clear() = 0;

protected:
    Model() : classifier::Model()
    {}
};
/** @} */
typedef services::SharedPtr<Model> ModelPtr;
} // namespace interface1
using interface1::Model;
using interface1::ModelPtr;

} // namespace classification
} // namespace gbt
} // namespace algorithms
} // namespace daal
#endif
