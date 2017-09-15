/* file: bernoulli.h */
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
//  Implementation of the bernoulli distribution
//--
*/

#ifndef __BERNOULLI_H__
#define __BERNOULLI_H__

#include "algorithms/distributions/distribution.h"
#include "algorithms/distributions/bernoulli/bernoulli_types.h"

namespace daal
{
namespace algorithms
{
namespace distributions
{
namespace bernoulli
{
/**
 * @defgroup distributions_bernoulli_batch Batch
 * @ingroup distributions_bernoulli
 * @{
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__DISTRIBUTIONS__BERNOULLI__BATCHCONTAINER"></a>
 * \brief Provides methods to run implementations of the bernoulli distribution.
 *        This class is associated with the \ref bernoulli::interface1::Batch "bernoulli::Batch" class
 *        and supports the method of bernoulli distribution computation in the batch processing mode
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of bernoulli distribution, double or float
 * \tparam method           Computation method of the distribution, bernoulli::Method
 * \tparam cpu              Version of the cpu-specific implementation of the distribution, daal::CpuType
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class DAAL_EXPORT BatchContainer : public daal::algorithms::AnalysisContainerIface<batch>
{
public:
    /**
     * Constructs a container for the bernoulli distribution with a specified environment
     * in the batch processing mode
     * \param[in] daalEnv   Environment object
     */
    BatchContainer(daal::services::Environment::env *daalEnv);
    ~BatchContainer();
    /**
     * Computes the result of the bernoulli distribution in the batch processing mode
     *
     * \return Status of computations
     */
    services::Status compute() DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DISTRIBUTIONS__BERNOULLI__BATCH"></a>
 * \brief Provides methods for bernoulli distribution computations in the batch processing mode
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of bernoulli distribution, double or float
 * \tparam method           Computation method of the distribution, bernoulli::Method
 *
 * \par Enumerations
 *      - bernoulli::Method          Computation methods for the bernoulli distribution
 *
 * \par References
 *      - \ref distributions::interface1::Input "distributions::Input" class
 *      - \ref distributions::interface1::Result "distributions::Result" class
 */
template<typename algorithmFPType = DAAL_ALGORITHM_FP_TYPE, Method method = defaultDense>
class DAAL_EXPORT Batch : public distributions::BatchBase
{
public:
    typedef distributions::BatchBase super;

    /**
     * Constructs bernoulli distribution
     *  \param[in] p     Success probability of a trial, value from [0.0; 1.0]
     */
    Batch(algorithmFPType p) : parameter(p)
    {
        initialize();
    }

    /**
     * Constructs bernoulli distribution by copying input objects and parameters of another bernoulli distribution
     * \param[in] other Bernoulli distribution
     */
    Batch(const Batch<algorithmFPType, method> &other): super(other), parameter(other.parameter)
    {
        initialize();
    }

    /**
     * Returns method of the distribution
     * \return Method of the distribution
     */
    virtual int getMethod() const DAAL_C11_OVERRIDE { return(int) method; }

    /**
     * Returns the structure that contains results of bernoulli distribution
     * \return Structure that contains results of bernoulli distribution
     */
    ResultPtr getResult() { return _result; }

    /**
     * Registers user-allocated memory to store results of bernoulli distribution
     * \param[in] result  Structure to store results of bernoulli distribution
     *
     * \return Status of computations
     */
    services::Status setResult(const ResultPtr& result)
    {
        DAAL_CHECK(result, services::ErrorNullResult)
        _result = result;
        _res = _result.get();
        return services::Status();
    }

    /**
     * Returns a pointer to the newly allocated bernoulli distribution
     * with a copy of input objects and parameters of this bernoulli distribution
     * \return Pointer to the newly allocated distribution
     */
    services::SharedPtr<Batch<algorithmFPType, method> > clone() const
    {
        return services::SharedPtr<Batch<algorithmFPType, method> >(cloneImpl());
    }

    /**
     * Allocates memory to store the result of the bernoulli distribution
     *
     * \return Status of computations
     */
    virtual services::Status allocateResult() DAAL_C11_OVERRIDE
    {
        _par = &parameter;
        services::Status s = this->_result->template allocate<algorithmFPType>(&(this->input), &parameter, (int) method);
        this->_res = this->_result.get();
        return s;
    }

    Parameter<algorithmFPType> parameter; /*!< %Parameters of the bernoulli distribution */

protected:
    virtual Batch<algorithmFPType, method> *cloneImpl() const DAAL_C11_OVERRIDE
    {
        return new Batch<algorithmFPType, method>(*this);
    }

    void initialize()
    {
        Analysis<batch>::_ac = new __DAAL_ALGORITHM_CONTAINER(batch, BatchContainer, algorithmFPType, method)(&_env);
        _in = &input;
        _par = &parameter;
        _result.reset(new Result());
    }

private:
    ResultPtr _result;
};

} // namespace interface1
using interface1::BatchContainer;
using interface1::Batch;
/** @} */
} // namespace bernoulli
} // namespace distributions
} // namespace algorithms
} // namespace daal
#endif
