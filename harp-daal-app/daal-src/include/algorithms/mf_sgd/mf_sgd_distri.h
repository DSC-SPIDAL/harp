/* file: mf_sgd_distri.h */
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
//  distributed processing mode, users could use MPI or Hadoop to handle the communication
//  tasks
//--
*/

#ifndef __MF_SGD_DISTRI_H__
#define __MF_SGD_DISTRI_H__

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
/** @defgroup mf_sgd_distri 
* @ingroup mf_sgd_distri
* @{
*/
/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__BATCHCONTAINER"></a>
 * \brief Provides methods to run implementations of the mf_sgd decomposition algorithm in distributed mode 
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of the mf_sgd decomposition algorithm, double or float
 * \tparam method           Computation method of the mf_sgd decomposition algorithm, \ref daal::algorithms::mf_sgd::Method
 *
 */
template<ComputeStep step, typename algorithmFPType, Method method, CpuType cpu>
class DAAL_EXPORT DistriContainer : public daal::algorithms::AnalysisContainerIface<distributed>
{
public:
    /**
     * Constructs a container for the mf_sgd algorithm with a specified environment
     * in the distributed mode
     * \param[in] daalEnv   Environment object
     */
    DistriContainer(daal::services::Environment::env *daalEnv);
    /** Default destructor */
    virtual ~DistriContainer();
    /**
     * Computes the result of the mf_sgd algorithm in the distributed processing mode
     */
    virtual void compute() DAAL_C11_OVERRIDE;

    virtual void finalizeCompute() DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD_DISTRIBUTED"></a>
 * \brief Computes the results of the mf_sgd algorithm in the distributed processing mode.
 * \n<a href="DAAL-REF-mf_sgd-ALGORITHM">mf_sgd decomposition algorithm description and usage models</a>
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations for the mf_sgd decomposition algorithm, double or float
 * \tparam method           Computation method of the algorithm, \ref daal::algorithms::mf_sgd::Method
 *
 * \par Enumerations
 * \ref Method   Computation methods for the mf_sgd decomposition algorithm
 */
template<ComputeStep step, typename algorithmFPType = double, Method method = defaultSGD>
class DAAL_EXPORT Distri : public daal::algorithms::Analysis<distributed>
{
public:
    Input input;            /*!< Input object */
    Parameter parameter;    /*!< mf_sgd decomposition parameters */

	/* default constructor */
    Distri()
    {
        initialize();
    }

    /**
     * Constructs a mf_sgd decomposition algorithm by copying input objects and parameters
     * of another mf_sgd decomposition algorithm
     * @param[in] other An algorithm to be used as the source to initialize the input objects
     *                  and parameters of the algorithm
     */
    Distri(const Distri<step, algorithmFPType, method> &other)
    {
        initialize();
        input.set(dataTrain, other.input.get(dataTrain)); /* copy the input training dataset */
        parameter = other.parameter;
    }

	/**
	 * @brief Returns method of the algorithm
	 * @return 
	 */
    virtual int getMethod() const DAAL_C11_OVERRIDE { return(int)method; }

    /**
     * @brief Returns the structure that contains the results of the mf_sgd decomposition algorithm
     * @return Structure that contains the results of the mf_sgd decomposition algorithm
     */
    services::SharedPtr<Result> getResult() { return _result;}

    /**
     * @brief Returns the structure that contains the partial results of the mf_sgd decomposition algorithm
     * @return Structure that contains the partial results of the mf_sgd decomposition algorithm
     */
    services::SharedPtr<DistributedPartialResult> getPartialResult() { return _presult;}

    /**
     * @brief Register user-allocated memory to store the results of the mf_sgd decomposition algorithm
	 * @param[in] user-allocated Result object
     */
    void setResult(const services::SharedPtr<Result>& res)
    {
        DAAL_CHECK(res, ErrorNullResult)
        _result = res;
        _res = _result.get();
    }

	/**
	 * @brief Register user-allocated memory to store the partial results of the mf_sgd decomposition algorithm
	 * @param[in] user-allocated DistributedPartialResult object
	 */
    void setPartialResult(const services::SharedPtr<DistributedPartialResult>& pres)
    {
        _presult = pres;
        _pres = pres.get();
    }

    /**
     * @brief Returns a pointer to the newly allocated mf_sgd decomposition algorithm
     * with a copy of input objects and parameters of this mf_sgd decomposition algorithm
     * @return Pointer to the newly allocated algorithm
     */
    services::SharedPtr<Distri<step, algorithmFPType, method> > clone() const
    {
        return services::SharedPtr<Distri<step, algorithmFPType, method> >(cloneImpl());
    }

protected:

	/**
	 * @brief a copy function  
	 * @return a pointer to copyed Distri object 
	 */
    virtual Distri<step, algorithmFPType, method> * cloneImpl() const DAAL_C11_OVERRIDE
    {
        return new Distri<step, algorithmFPType, method>(*this);
    }

    virtual void allocateResult() DAAL_C11_OVERRIDE {}

    virtual void allocatePartialResult() DAAL_C11_OVERRIDE {}

	virtual void initializePartialResult() DAAL_C11_OVERRIDE {}

    void initialize()
    {
        Analysis<distributed>::_ac = new __DAAL_ALGORITHM_CONTAINER(distributed, DistriContainer, step, algorithmFPType, method)(&_env);
        _in   = &input;
        _par  = &parameter;
    }

private:
    services::SharedPtr<Result> _result;
    services::SharedPtr<DistributedPartialResult> _presult;
};
/** @} */
} // namespace interface1
using interface1::DistriContainer;
using interface1::Distri;

} // namespace daal::algorithms::mf_sgd
}
} // namespace daal
#endif
