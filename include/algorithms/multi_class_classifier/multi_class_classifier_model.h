/* file: multi_class_classifier_model.h */
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
//  Multiclass tcc parameter structure
//--
*/

#ifndef __MULTI_CLASS_CLASSIFIER_MODEL_H__
#define __MULTI_CLASS_CLASSIFIER_MODEL_H__

#include "services/daal_defines.h"
#include "algorithms/model.h"
#include "algorithms/classifier/classifier_model.h"
#include "algorithms/classifier/classifier_predict.h"
#include "algorithms/classifier/classifier_training_batch.h"

namespace daal
{
namespace algorithms
{
/**
 * @defgroup multi_class_classifier Multi-class Classifier
 * \copydoc daal::algorithms::multi_class_classifier
 * @ingroup classification
 * @{
 */
/**
 * \brief Contains classes for computing the results of the multi-class classifier algorithm
 */
namespace multi_class_classifier
{

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * @ingroup multi_class_classifier
 * @{
 */
/**
 * <a name="DAAL-STRUCT-ALGORITHMS__MULTI_CLASS_CLASSIFIER__PARAMETERBASE"></a>
 * \brief Parameters of the multi-class classifier algorithm
 *
 * \snippet multi_class_classifier/multi_class_classifier_model.h ParameterBase source code
 */
/* [ParameterBase source code] */
struct DAAL_EXPORT ParameterBase : public daal::algorithms::classifier::Parameter
{
    ParameterBase(size_t nClasses): daal::algorithms::classifier::Parameter(nClasses), training(), prediction() {}
    services::SharedPtr<classifier::training::Batch> training;          /*!< Two-class classifier training stage */
    services::SharedPtr<classifier::prediction::Batch> prediction;      /*!< Two-class classifier prediction stage */

    services::Status check() const DAAL_C11_OVERRIDE;
};
/* [ParameterBase source code] */

/**
 * <a name="DAAL-STRUCT-ALGORITHMS__MULTI_CLASS_CLASSIFIER__PARAMETER"></a>
 * \brief Optional multi-class classifier algorithm  parameters that are used with the MultiClassClassifierWu prediction method
 *
 * \snippet multi_class_classifier/multi_class_classifier_model.h Parameter source code
 */
/* [Parameter source code] */
struct DAAL_EXPORT Parameter : public ParameterBase
{
    Parameter(size_t nClasses, size_t maxIterations = 100, double accuracyThreshold = 1.0e-12) :
        ParameterBase(nClasses), maxIterations(maxIterations), accuracyThreshold(accuracyThreshold) {}

    size_t maxIterations;     /*!< Maximum number of iterations */
    double accuracyThreshold; /*!< Convergence threshold */

    services::Status check() const DAAL_C11_OVERRIDE;
};
/* [Parameter source code] */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTI_CLASS_CLASSIFIER__MODEL"></a>
 * \brief Model of the classifier trained by the multi_class_classifier::training::Batch algorithm.
 */
class DAAL_EXPORT Model : public daal::algorithms::classifier::Model
{
public:
    DECLARE_MODEL(Model, classifier::Model);

    /**
     * Constructs multi-class classifier model
     * \param[in] nFeatures Number of features in the dataset
     * \param[in] par       Parameters of the multi-class classifier algorithm
     * \DAAL_DEPRECATED_USE{ Model::create }
     */
    Model(size_t nFeatures, const ParameterBase *par);

    /**
     * Empty constructor for deserialization
     * \DAAL_DEPRECATED_USE{ Model::create }
     */
    Model();

    /**
     * Constructs multi-class classifier model
     * \param[in] nFeatures Number of features in the dataset
     * \param[in] par       Parameters of the multi-class classifier algorithm
     * \param[out] stat     Status of the model construction
     * \return Multi-class classifier model
     */
    static services::SharedPtr<Model> create(size_t nFeatures, const ParameterBase *par, services::Status* stat = NULL);

    virtual ~Model();

    /**
     * Returns a collection of two-class classifier models in a multi-class classifier model
     * \return  Collection of two-class classifier models
     */
    data_management::DataCollectionPtr getMultiClassClassifierModel()
    {
        return _models;
    }

    /**
     * Returns a pointer to the array of two-class classifier models in a multi-class classifier model
     * \return Pointer to the array of two-class classifier models
     * \DAAL_DEPRECATED_USE{ Model::getTwoClassClassifierModel }
     */
    DAAL_DEPRECATED classifier::ModelPtr *getTwoClassClassifierModels();

    /**
     * Set two-class classifier model into a multi-class classifier model
     * \param[in] idx    Index of two-class classifier model in a collection
     * \param[in] model  Two-class classifier model to add into collection
     */
    void setTwoClassClassifierModel(size_t idx, const classifier::ModelPtr& model);

    /**
     * Returns a two-class classifier model in a multi-class classifier model
     * \param[in]  idx   Index of the two-class classifier model in a multi-class classifier model
     * \return             Two-class classifier model
     */
    classifier::ModelPtr getTwoClassClassifierModel(size_t idx) const;

    /**
     * Returns a number of two-class classifiers associated with the model
     * \return        Number of two-class classifiers associated with the model
     */
    size_t getNumberOfTwoClassClassifierModels() const
    {
        return _models->size();
    }

    /**
     * Retrieves the number of features in the dataset was used on the training stage
     * \return Number of features in the dataset was used on the training stage
     */
    size_t getNumberOfFeatures() const DAAL_C11_OVERRIDE { return _nFeatures; }

protected:
    size_t _nFeatures;
    data_management::DataCollectionPtr _models;              /* Collection of two-class classifiers associated with the model */
    classifier::ModelPtr *_modelsArray;

    Model(size_t nFeatures, const ParameterBase *par, services::Status &st);

    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        classifier::Model::serialImpl<Archive, onDeserialize>(arch);
        arch->set(_nFeatures);
        arch->setSharedPtrObj(_models);

        return services::Status();
    }

    services::Status serializeImpl(data_management::InputDataArchive *archive) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(archive);

        return services::Status();
    }

    services::Status deserializeImpl(const data_management::OutputDataArchive *archive) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(archive);

        return services::Status();
    }
};
typedef services::SharedPtr<Model> ModelPtr;
} // namespace interface1
using interface1::ParameterBase;
using interface1::Parameter;
using interface1::Model;
using interface1::ModelPtr;

} // namespace multi_class_classifier
/** @} */
} // namespace algorithms
} // namespace daal
#endif
