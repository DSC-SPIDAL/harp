/* file: decision_tree_regression_training_batch.h */
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
//  Implementation of the interface for Decision tree model-based training in the batch processing mode
//--
*/

#ifndef __DECISION_TREE_REGRESSION_TRAINING_BATCH_H__
#define __DECISION_TREE_REGRESSION_TRAINING_BATCH_H__

#include "algorithms/algorithm.h"
#include "algorithms/decision_tree/decision_tree_regression_training_types.h"
#include "algorithms/decision_tree/decision_tree_regression_model.h"
#include "algorithms/regression/regression_training_batch.h"

namespace daal
{
namespace algorithms
{
namespace decision_tree
{
namespace regression
{
namespace training
{
namespace interface1
{
/**
 * @defgroup decision_tree_regression_training_batch Batch
 * @ingroup decision_tree_regression_training
 * @{
 */

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DECISION_TREE__REGRESSION__TRAINING__BATCHCONTAINER"></a>
 * \brief Class containing methods for Decision tree model-based training using algorithmFPType precision arithmetic
 */
template <typename algorithmFPType, Method method, CpuType cpu>
class DAAL_EXPORT BatchContainer : public TrainingContainerIface<batch>
{
public:
    /**
     * Constructs a container for Decision tree model-based training with a specified environment in the batch processing mode
     * \param[in] daalEnv   Environment object
     */
    BatchContainer(daal::services::Environment::env * daalEnv);

    /** Default destructor */
    ~BatchContainer();

    /**
     * Computes the result of Decision tree model-based training in the batch processing mode
     */
    services::Status compute() DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DECISION_TREE__REGRESSION__TRAINING__BATCH"></a>
 * \brief Provides methods for Decision tree model-based training in the batch processing mode
 * <!-- \n<a href="DAAL-REF-DT-ALGORITHM">Decision tree algorithm description and usage models</a> -->
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations for Decision tree model-based training, double or float
 * \tparam method           Decision tree training method, \ref Method
 *
 * \par Enumerations
 *      - \ref Method  Computation methods
 *
 * \par References
 *      - \ref decision_tree::regression::interface1::Model "decision_tree::regression::Model" class
 *      - \ref prediction::interface1::Batch "prediction::Batch" class
 */
template<typename algorithmFPType = DAAL_ALGORITHM_FP_TYPE, Method method = defaultDense>
class DAAL_EXPORT Batch : public algorithms::regression::training::Batch
{
public:
    typedef algorithms::regression::training::Batch super;

    typedef algorithms::decision_tree::regression::training::Input  InputType;
    typedef algorithms::decision_tree::regression::Parameter        ParameterType;
    typedef algorithms::decision_tree::regression::training::Result ResultType;

    InputType input;    /*!< %Input objects of the algorithm */
    ParameterType parameter; /*!< \ref interface1::Parameter "Parameters" of the algorithm */

    /** Default constructor */
    Batch()
    {
        initialize();
    }

    /**
     * Constructs a Decision tree training algorithm by copying input objects
     * and parameters of another Decision tree training algorithm in the batch processing mode
     * \param[in] other Algorithm to use as the source to initialize the input objects and parameters of the algorithm
     */
    Batch(const Batch<algorithmFPType, method> & other) :
        algorithms::regression::training::Batch(other), input(other.input), parameter(other.parameter)
    {
        initialize();
    }

    virtual algorithms::regression::training::Input* getInput() DAAL_C11_OVERRIDE { return &input; }

    /**
    * Returns the method of the algorithm
    * \return Method of the algorithm
    */
    virtual int getMethod() const DAAL_C11_OVERRIDE { return(int)method; }

    /**
     * Returns the structure that contains the result of Decision tree model-based training
     * \return Structure that contains the result of Decision tree model-based training
     */
    ResultPtr getResult() { return ResultType::cast(_result); }

    /**
     * Resets the results of Decision tree model training algorithm
     */
    void resetResult() // DAAL_C11_OVERRIDE
    {
        _result.reset(new ResultType());
        _res = NULL;
    }

    /**
     * Returns a pointer to a newly allocated Decision tree training algorithm
     * with a copy of the input objects and parameters for this Decision tree training algorithm
     * in the batch processing mode
     * \return Pointer to the newly allocated algorithm
     */
    services::SharedPtr<Batch<algorithmFPType, method> > clone() const
    {
        return services::SharedPtr<Batch<algorithmFPType, method> >(cloneImpl());
    }

protected:
    virtual Batch<algorithmFPType, method> * cloneImpl() const DAAL_C11_OVERRIDE
    {
        return new Batch<algorithmFPType, method>(*this);
    }

    services::Status allocateResult() DAAL_C11_OVERRIDE
    {
        services::Status s = getResult()->template allocate<algorithmFPType>(&input, &parameter, method);
        _res = _result.get();
        return s;
    }

    void initialize()
    {
        _in = &input;
        _ac  = new __DAAL_ALGORITHM_CONTAINER(batch, BatchContainer, algorithmFPType, method)(&_env);
        _par = &parameter;
        _result.reset(new ResultType());
    }
};
/** @} */
} // namespace interface1

using interface1::BatchContainer;
using interface1::Batch;

} // namespace training
} // namespace regression
} // namespace decision_tree
} // namespace algorithms
} // namespace daal

#endif
