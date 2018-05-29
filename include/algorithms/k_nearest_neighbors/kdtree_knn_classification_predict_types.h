/* file: kdtree_knn_classification_predict_types.h */
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
//  Implementation of the K-Nearest Neighbors (kNN) algorithm interface
//--
*/

#ifndef __KDTREE_KNN_CLASSIFICATION_PREDICT_TYPES_H__
#define __KDTREE_KNN_CLASSIFICATION_PREDICT_TYPES_H__

#include "algorithms/algorithm.h"
#include "data_management/data/data_serialize.h"
#include "algorithms/k_nearest_neighbors/kdtree_knn_classification_model.h"
#include "algorithms/classifier/classifier_predict_types.h"

namespace daal
{
namespace algorithms
{

/**
 * \brief Contains classes of the KD-tree based kNN algorithm
 */
namespace kdtree_knn_classification
{

/**
 * @defgroup kdtree_knn_classification_prediction Prediction
 * \copydoc daal::algorithms::kdtree_knn_classification::prediction
 * @ingroup kdtree_knn_classification
 * @{
 */
/**
 * \brief Contains a class for making KD-tree based kNN model-based prediction
 */
namespace prediction
{

/**
 * <a name="DAAL-ENUM-ALGORITHMS__KDTREE_KNN_CLASSIFICATION__PREDICTION__METHOD"></a>
 * \brief Available methods for making KD-tree based kNN model-based prediction
 */
enum Method
{
    defaultDense = 0 /*!< Default method */
};

/**
 * \brief Contains version 1.0 of the Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface
 */
namespace interface1
{

/**
 * <a name="DAAL-CLASS-ALGORITHMS__KDTREE_KNN_CLASSIFICATION__PREDICTION__INPUT"></a>
 * \brief Provides an interface for input objects for making KD-tree based kNN model-based prediction
 */
class DAAL_EXPORT Input : public classifier::prediction::Input
{
    typedef classifier::prediction::Input super;
public:
    /** Default constructor */
    Input();

    using super::get;
    using super::set;

    /**
     * Returns the input Model object in the prediction stage of the KD-tree based kNN algorithm
     * \param[in] id    Identifier of the input Model object
     * \return          %Input object that corresponds to the given identifier
     */
    kdtree_knn_classification::ModelPtr get(classifier::prediction::ModelInputId id) const;

    /**
     * Sets the input NumericTable object in the prediction stage of the classification algorithm
     * \param[in] id    Identifier of the input object
     * \param[in] ptr   Pointer to the input object
     */
    void set(classifier::prediction::NumericTableInputId id, const data_management::NumericTablePtr &ptr);

    /**
     * Sets the input Model object in the prediction stage of the KD-tree based kNN algorithm
     * \param[in] id      Identifier of the input object
     * \param[in] value   Input Model object
     */
    void set(classifier::prediction::ModelInputId id, const kdtree_knn_classification::interface1::ModelPtr & value);

    /**
     * Checks the correctness of the input object
     * \param[in] parameter Pointer to the structure of the algorithm parameters
     * \param[in] method    Computation method
     */
    services::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;
};

} // namespace interface1

using interface1::Input;

} // namespace prediction
/** @} */
} // namespace kdtree_knn_classification
} // namespace algorithms
} // namespace daal

#endif
