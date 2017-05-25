/* file: mf_sgd_batch.h */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Implementation of the interface for the mf_sgd decomposition algorithm in the
//  batch processing mode
//--
*/

#ifndef __MF_SGD_BATCH_H__
#define __MF_SGD_BATCH_H__

#include "algorithms/algorithm.h"
#include "data_management/data/numeric_table.h"
#include "services/daal_defines.h"
#include "algorithms/mf_sgd/mf_sgd_types.h"

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{

namespace interface1
{
/** @defgroup mf_sgd_batch Batch
* @ingroup mf_sgd_without_pivoting
* @{
*/
/**
 * <a name="DAAL-CLASS-ALGORITHMS__mf_sgd__BATCHCONTAINER"></a>
 * \brief Provides methods to run implementations of the mf_sgd decomposition algorithm in the batch processing mode
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of the mf_sgd decomposition algorithm, double or float
 * \tparam method           Computation method of the mf_sgd decomposition algorithm, \ref daal::algorithms::mf_sgd::Method
 *
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class DAAL_EXPORT BatchContainer : public daal::algorithms::AnalysisContainerIface<batch> 
{
public:
    /**
     * Constructs a container for the mf_sgd decomposition algorithm with a specified environment
     * in the batch processing mode
     * \param[in] daalEnv   Environment object
     */
    BatchContainer(daal::services::Environment::env *daalEnv);
    /** Default destructor */
    virtual ~BatchContainer();
    /**
     * Computes the result of the mf_sgd decomposition algorithm in the batch processing mode
     */
    virtual void compute() DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__mf_sgd__BATCH"></a>
 * \brief Computes the results of the mf_sgd decomposition algorithm in the batch processing mode.
 * \n<a href="DAAL-REF-mf_sgd-ALGORITHM">mf_sgd decomposition algorithm description and usage models</a>
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations for the mf_sgd decomposition algorithm, double or float
 * \tparam method           Computation method of the algorithm, \ref daal::algorithms::mf_sgd::Method
 *
 * \par Enumerations
 *      - \ref Method   Computation methods for the mf_sgd decomposition algorithm
 */
template<typename algorithmFPType = double, Method method = defaultSGD>
class DAAL_EXPORT Batch : public daal::algorithms::Analysis<batch>
{
public:
    Input input;            /*!< Input object */
    Parameter parameter;    /*!< mf_sgd decomposition parameters */

    Batch()
    {
        initialize();
    }

    /**
     * Constructs a mf_sgd decomposition algorithm by copying input objects and parameters
     * of another mf_sgd decomposition algorithm
     * \param[in] other An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    Batch(const Batch<algorithmFPType, method> &other)
    {
        initialize();
        input.set(dataTrain, other.input.get(dataTrain));		/* !< copy input training dataset */
        input.set(dataTest, other.input.get(dataTest));			/* !< copy input test dataset */
        parameter = other.parameter;
    }

    /**
    * Returns method of the algorithm
    * \return Method of the algorithm
    */
    virtual int getMethod() const DAAL_C11_OVERRIDE { return(int)method; }

    /**
     * Returns the structure that contains the results of the mf_sgd decomposition algorithm
     * \return Structure that contains the results of the mf_sgd decomposition algorithm
     */
    services::SharedPtr<Result> getResult() { return _result; }

    /**
     * Register user-allocated memory to store the results of the mf_sgd decomposition algorithm
     * \return Structure to store the results of the mf_sgd decomposition algorithm
     */
    void setResult(const services::SharedPtr<Result>& res)
    {
        DAAL_CHECK(res, ErrorNullResult)
        _result = res;
        _res = _result.get();
    }

    /**
     * Returns a pointer to the newly allocated mf_sgd decomposition algorithm
     * with a copy of input objects and parameters of this mf_sgd decomposition algorithm
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

    virtual void allocateResult() DAAL_C11_OVERRIDE
    {
         /*  the function to allocate the result */
        _result = services::SharedPtr<Result>(new Result());
        _result->allocate<algorithmFPType>(&input, _par, 0);
        _res = _result.get();
    }

    void initialize()
    {
        Analysis<batch>::_ac = new __DAAL_ALGORITHM_CONTAINER(batch, BatchContainer, algorithmFPType, method)(&_env);
        _in   = &input;
        _par  = &parameter;
    }

private:
    services::SharedPtr<Result> _result;
};
/** @} */
} // namespace interface1
using interface1::BatchContainer;
using interface1::Batch;

} // namespace daal::algorithms::mf_sgd
}
} // namespace daal
#endif
