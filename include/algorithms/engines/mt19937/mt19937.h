/* file: mt19937.h */
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
//  Implementation of the Mersenne Twister engine in the batch processing mode
//--
*/

#ifndef __MT19937_H__
#define __MT19937_H__

#include "algorithms/engines/mt19937/mt19937_types.h"
#include "algorithms/engines/engine.h"

namespace daal
{
namespace algorithms
{
namespace engines
{
namespace mt19937
{
/**
 * @defgroup engines_mt19937_batch Batch
 * @ingroup engines_mt19937
 * @{
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__ENGINES__MT19937__BATCHCONTAINER"></a>
 * \brief Provides methods to run implementations of the mt19937 engine.
 *        This class is associated with the \ref mt19937::interface1::Batch "mt19937::Batch" class
 *        and supports the method of mt19937 engine computation in the batch processing mode
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of mt19937 engine, double or float
 * \tparam method           Computation method of the engine, mt19937::Method
 * \tparam cpu              Version of the cpu-specific implementation of the engine, daal::CpuType
 */
template<typename algorithmFPType, Method method, CpuType cpu>
class DAAL_EXPORT BatchContainer : public daal::algorithms::AnalysisContainerIface<batch>
{
public:
    /**
     * Constructs a container for the mt19937 engine with a specified environment
     * in the batch processing mode
     * \param[in] daalEnv   Environment object
     */
    BatchContainer(daal::services::Environment::env *daalEnv);
    ~BatchContainer();
    /**
     * Computes the result of the mt19937 engine in the batch processing mode
     *
     * \return Status of computations
     */
    services::Status compute() DAAL_C11_OVERRIDE;
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__ENGINES__MT19937__BATCH"></a>
 * \brief Provides methods for mt19937 engine computations in the batch processing mode
 *
 * \tparam algorithmFPType  Data type to use in intermediate computations of mt19937 engine, double or float
 * \tparam method           Computation method of the engine, mt19937::Method
 *
 * \par Enumerations
 *      - mt19937::Method          Computation methods for the mt19937 engine
 *
 * \par References
 *      - \ref engines::interface1::Input  "engines::Input" class
 *      - \ref engines::interface1::Result "engines::Result" class
 */
template<typename algorithmFPType = DAAL_ALGORITHM_FP_TYPE, Method method = defaultDense>
class DAAL_EXPORT Batch : public engines::BatchBase
{
public:
    typedef engines::BatchBase super;

    typedef typename super::InputType  InputType;
    typedef typename super::ResultType ResultType;

    /**
     * Creates mt19937 engine
     * \param[in] seed  Initial condition for mt19937 engine
     *
     * \return Pointer to mt19937 engine
     */
    static services::SharedPtr<Batch<algorithmFPType, method> > create(size_t seed = 777);

    /**
     * Returns method of the engine
     * \return Method of the engine
     */
    virtual int getMethod() const DAAL_C11_OVERRIDE { return (int)method; }

    /**
     * Returns the structure that contains results of mt19937 engine
     * \return Structure that contains results of mt19937 engine
     */
    ResultPtr getResult() { return _result; }

    /**
     * Registers user-allocated memory to store results of mt19937 engine
     * \param[in] result  Structure to store results of mt19937 engine
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
     * Returns a pointer to the newly allocated mt19937 engine
     * with a copy of input objects and parameters of this mt19937 engine
     * \return Pointer to the newly allocated engine
     */
    services::SharedPtr<Batch<algorithmFPType, method> > clone() const
    {
        return services::SharedPtr<Batch<algorithmFPType, method> >(cloneImpl());
    }

    /**
     * Allocates memory to store the result of the mt19937 engine
     *
     * \return Status of computations
     */
    virtual services::Status allocateResult() DAAL_C11_OVERRIDE
    {
        services::Status s = this->_result->template allocate<algorithmFPType>(&(this->input), NULL, (int) method);
        this->_res = this->_result.get();
        return s;
    }

protected:
    Batch(size_t seed = 777)
    {
        initialize();
    }

    Batch(const Batch<algorithmFPType, method> &other): super(other)
    {
        initialize();
    }

    virtual Batch<algorithmFPType, method> *cloneImpl() const DAAL_C11_OVERRIDE
    {
        return new Batch<algorithmFPType, method>(*this);
    }

    void initialize()
    {
        Analysis<batch>::_ac = new __DAAL_ALGORITHM_CONTAINER(batch, BatchContainer, algorithmFPType, method)(&_env);
        _in = &input;
        _result.reset(new ResultType());
    }

private:
    ResultPtr _result;
};
typedef services::SharedPtr<Batch<> > mt19937Ptr;
typedef services::SharedPtr<const Batch<> > mt19937ConstPtr;

} // namespace interface1
using interface1::BatchContainer;
using interface1::Batch;
using interface1::mt19937Ptr;
using interface1::mt19937ConstPtr;
/** @} */
} // namespace mt19937
} // namespace engines
} // namespace algorithms
} // namespace daal
#endif
