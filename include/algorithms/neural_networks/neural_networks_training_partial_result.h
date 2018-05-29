/* file: neural_networks_training_partial_result.h */
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
//  Implementation of neural network algorithm interface.
//--
*/

#ifndef __NEURAL_NETWORKS_TRAINING_PARTIAL_RESULT_H__
#define __NEURAL_NETWORKS_TRAINING_PARTIAL_RESULT_H__

#include "algorithms/algorithm.h"

#include "services/daal_defines.h"
#include "data_management/data/data_serialize.h"
#include "data_management/data/numeric_table.h"
#include "algorithms/neural_networks/neural_networks_training_model.h"
#include "algorithms/neural_networks/neural_networks_training_result.h"

namespace daal
{
namespace algorithms
{
/**
 * \brief Contains classes for training and prediction using neural network
 */
namespace neural_networks
{
namespace training
{
/**
 * @ingroup neural_networks_training
 * @{
 */
/**
 * <a name="DAAL-ENUM-ALGORITHMS__NEURAL_NETWORKS__TRAINING__STEP1LOCALPARTIALRESULTID"></a>
 * \brief Available identifiers of partial results of the neural network training algorithm
 * required by the first distributed step
 */
enum Step1LocalPartialResultId
{
    derivatives,
    batchSize,
    lastStep1LocalPartialResultId = batchSize
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__NEURAL_NETWORKS__TRAINING__STEP2MASTERPARTIALRESULTID"></a>
 * \brief Available identifiers of partial results of the neural network training algorithm
 *  equired by the second distributed step
 */
enum Step2MasterPartialResultId
{
    resultFromMaster,
    lastStep2MasterPartialResultId = resultFromMaster
};

/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__TRAINING__PARTIALRESULT"></a>
 * \brief Provides methods to access partial result obtained with the compute() method of the
 *  neural network training algorithm in the distributed processing mode
 */
class DAAL_EXPORT PartialResult : public daal::algorithms::PartialResult
{
public:
    DECLARE_SERIALIZABLE_CAST(PartialResult);

    PartialResult();

    virtual ~PartialResult() {}

    /**
     * Returns partial result of the neural network model based training
     * \param[in] id    Identifier of the result
     * \return          Result that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(Step1LocalPartialResultId id) const;

    /**
     * Sets partial result of neural network model based training
     * \param[in] id      Identifier of the result
     * \param[in] value   Result
     */
    void set(Step1LocalPartialResultId id, const data_management::NumericTablePtr &value);

    /**
     * Registers user-allocated memory to store partial results of the neural network model based training
     * \param[in] input Pointer to an object containing %input data
     * \param[in] parameter %Parameter of the neural network training
     * \param[in] method Computation method for the algorithm
     *
     * \return Status of computations
     */
    template<typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Checks result of the neural network algorithm
     * \param[in] input   %Input object of algorithm
     * \param[in] par     %Parameter of algorithm
     * \param[in] method  Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        return daal::algorithms::PartialResult::serialImpl<Archive, onDeserialize>(arch);
    }
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__TRAINING__DISTRIBUTEDPARTIALRESULT"></a>
 * \brief Provides methods to access partial result obtained with the compute() method of the
 * neural network training algorithm in the distributed processing mode
 */
class DAAL_EXPORT DistributedPartialResult : public daal::algorithms::PartialResult
{
public:
    DECLARE_SERIALIZABLE_CAST(DistributedPartialResult);

    DistributedPartialResult();

    /**
     * Returns the partial result of the neural network model based training
     * \param[in] id    Identifier of the partial result
     * \return          Partial result that corresponds to the given identifier
     */
    training::ResultPtr get(Step2MasterPartialResultId id) const;

    /**
     * Sets the partial result of neural network model based training
     * \param[in] id      Identifier of the partial result
     * \param[in] value   Partial result
     */
    void set(Step2MasterPartialResultId id, const training::ResultPtr &value);

    /**
     * Registers user-allocated memory to store partial results of the neural network model based training
     * \param[in] input Pointer to an object containing %input data
     * \param[in] method Computation method for the algorithm
     * \param[in] parameter %Parameter of the neural network training
     *
     * \return Status of computations
     */
    template<typename algorithmFPType>
    DAAL_EXPORT services::Status allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);

    /**
     * Checks partial result of the neural network algorithm
     * \param[in] input   %Input object of algorithm
     * \param[in] par     %Parameter of algorithm
     * \param[in] method  Computation method
     *
     * \return Status of computations
     */
    services::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        return daal::algorithms::PartialResult::serialImpl<Archive, onDeserialize>(arch);
    }
};

typedef services::SharedPtr<PartialResult> PartialResultPtr;
typedef services::SharedPtr<DistributedPartialResult> DistributedPartialResultPtr;
} // namespace interface1

using interface1::PartialResult;
using interface1::PartialResultPtr;
using interface1::DistributedPartialResult;
using interface1::DistributedPartialResultPtr;

/** @} */
}
}
}
} // namespace daal
#endif
