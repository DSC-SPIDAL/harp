/* file: multinomial_naive_bayes_training_types.h */
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
//  Naive Bayes classifier parameter structure used in the training stage
//--
*/

#ifndef __NAIVE_BAYES_TRAINING_TYPES_H__
#define __NAIVE_BAYES_TRAINING_TYPES_H__

#include "algorithms/naive_bayes/multinomial_naive_bayes_model.h"
#include "data_management/data/data_collection.h"
#include "algorithms/classifier/classifier_training_types.h"

namespace daal
{
namespace algorithms
{
namespace multinomial_naive_bayes
{
/**
 * @defgroup multinomial_naive_bayes_training Training
 * \copydoc daal::algorithms::multinomial_naive_bayes::training
 * @ingroup multinomial_naive_bayes
 * @{
 */
/**
* \brief Contains classes for training the naive Bayes model
*/
namespace training
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__TRAINING__METHOD"></a>
 * Available methods for computing the results of the naive Bayes algorithm
 */
enum Method
{
    defaultDense = 0, /*!< Default Training method for the multinomial naive Bayes */
    fastCSR      = 1  /*!< Training method for the multinomial naive Bayes with sparse data in CSR format */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__TRAINING__STEP2MASTERINPUTID"></a>
 * Available identifiers of the step 2 input
 */
enum Step2MasterInputId
{
    partialModels,                     /*!< Collection of partial models trained on local nodes */
    lastStep2MasterInputId = partialModels
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__TRAINING__PARTIALRESULT"></a>
 * \brief Provides methods to access partial results obtained with the compute() method of the
 *        naive Bayes training algorithm
 *        in the online or distributed processing
 */
class DAAL_EXPORT PartialResult : public classifier::training::PartialResult
{
public:
    DECLARE_SERIALIZABLE_CAST(PartialResult);

    PartialResult();
    virtual ~PartialResult() {}

    /**
     * Returns the partial model trained with the classification algorithm
     * \param[in] id    Identifier of the partial model, \ref classifier::training::PartialResultId
     * \return          Model trained with the classification algorithm
     */
    multinomial_naive_bayes::PartialModelPtr get(classifier::training::PartialResultId id) const;

    /**
     * Allocates memory for storing partial results of the naive Bayes training algorithm
     * \param[in] input        Pointer to input object
     * \param[in] parameter    Pointer to parameter
     * \param[in] method       Computation method
     *
     * \return Status of computations
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Initializes memory for storing partial results of the naive Bayes training algorithm
     * \param[in] input        Pointer to input object
     * \param[in] parameter    Pointer to parameter
     * \param[in] method       Computation method
     *
     * \return Status of initialization
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status initialize(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
    * Returns number of columns in the naive Bayes partial result
    * \return Number of columns in the partial result
    */
    size_t getNumberOfFeatures() const;

    /**
     * Checks partial result of the naive Bayes training algorithm
     * \param[in] input      Algorithm %input object
     * \param[in] parameter  Algorithm %parameter
     * \param[in] method     Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    /**
    * Checks partial result of the naive Bayes training algorithm
    * \param[in] parameter  Algorithm %parameter
    * \param[in] method     Computation method
    *
     * \return Status of computations
    */
    services::Status check(const daal::algorithms::Parameter *parameter, int method)  const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        classifier::training::PartialResult::serialImpl<Archive, onDeserialize>(arch);

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
typedef services::SharedPtr<PartialResult> PartialResultPtr;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__TRAINING__RESULT"></a>
 * \brief Provides methods to access final results obtained with the compute() method of the
 *        naive Bayes training algorithm
 *        in the batch processing mode or with the finalizeCompute() method
 *       in the distributed or online processing mode
 */
class DAAL_EXPORT Result : public classifier::training::Result
{
public:
    DECLARE_SERIALIZABLE_CAST(Result);
    Result();
    virtual ~Result() {}

    /**
     * Returns the model trained with the naive Bayes training algorithm
     * \param[in] id    Identifier of the result, \ref classifier::training::ResultId
     * \return          Model trained with the classification algorithm
     */
    multinomial_naive_bayes::ModelPtr get(classifier::training::ResultId id) const;

    /**
     * Allocates memory for storing final result computed with naive Bayes training algorithm
     * \param[in] input      Pointer to input object
     * \param[in] parameter  Pointer to parameter
     * \param[in] method     Computation method
     *
     * \return Status of computations
     */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
    * Allocates memory for storing final result computed with naive Bayes training algorithm
    * \param[in] partialResult      Pointer to partial result structure
    * \param[in] parameter          Pointer to parameter structure
    * \param[in] method             Computation method
    *
     * \return Status of computations
    */
    template <typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::PartialResult *partialResult, const daal::algorithms::Parameter *parameter, const int method);

    /**
    * Checks the correctness of Result object
    * \param[in] partialResult Pointer to the partial results structure
    * \param[in] parameter     Parameter of the algorithm
    * \param[in] method        Computation method
    *
     * \return Status of computations
    */
    services::Status check(const daal::algorithms::PartialResult *partialResult, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    /**
     * Checks the final result of the naive Bayes training algorithm
     * \param[in] input      %Input of algorithm
     * \param[in] parameter  %Parameter of algorithm
     * \param[in] method     Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        classifier::training::Result::serialImpl<Archive, onDeserialize>(arch);

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

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__TRAINING__DISTRIBUTEDINPUT"></a>
 * \brief Input objects of the naive Bayes training algorithm in the distributed processing mode
 */
class DAAL_EXPORT DistributedInput : public classifier::training::InputIface
{
public:
    DistributedInput();
    DistributedInput(const DistributedInput& other) : classifier::training::InputIface(other){}

    virtual ~DistributedInput() {}

    virtual size_t getNumberOfFeatures() const DAAL_C11_OVERRIDE;

    /**
     * Returns input objects of the classification algorithm in the distributed processing mode
     * \param[in] id    Identifier of the input objects
     * \return          Input object that corresponds to the given identifier
     */
    data_management::DataCollectionPtr get(Step2MasterInputId id) const;

    /**
     * Adds input object on the master node in the training stage of the classification algorithm
     * \param[in] id            Identifier of the input object
     * \param[in] partialResult Pointer to the object
     */
    void add(const Step2MasterInputId &id, const PartialResultPtr &partialResult);

    /**
     * Sets input object in the training stage of the classification algorithm
     * \param[in] id   Identifier of the object
     * \param[in] value Pointer to the object
     */
    void set(Step2MasterInputId id, const data_management::DataCollectionPtr &value);

    /**
     * Checks input parameters in the training stage of the classification algorithm
     * \param[in] parameter %Parameter of the algorithm
     * \param[in] method    Algorithm method
     */
    services::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;
};

} // namespace interface1
using interface1::DistributedInput;
using interface1::PartialResult;
using interface1::PartialResultPtr;
using interface1::Result;
using interface1::ResultPtr;

} // namespace training
/** @} */
} // namespace multinomial_naive_bayes
} // namespace algorithms
} // namespace daal
#endif
