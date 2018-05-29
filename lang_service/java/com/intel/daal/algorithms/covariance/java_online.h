/* file: java_online.h */
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
//  Implementation of the class that connects Covariance Java Online
//  to C++ algorithm
//--
*/
#ifndef __JAVA_ONLINE_H__
#define __JAVA_ONLINE_H__

#include <jni.h>

#include "algorithms/covariance/covariance_types.h"
#include "algorithms/covariance/covariance_online.h"
#include "java_callback.h"
#include "java_online_container.h"

namespace daal
{
namespace algorithms
{
namespace covariance
{

using namespace daal::data_management;
using namespace daal::services;

/*
 * \brief Class that specifies the default method for partial results initialization
 */
class JavaOnline : public OnlineImpl
{
public:
    /** Default constructor */
    JavaOnline(JavaVM *_jvm, jobject _javaObject)
    {
        JavaOnlineContainer* _container = new JavaOnlineContainer(_jvm, _javaObject);
        _container->setJavaResult(_result);
        _container->setJavaPartialResult(_partialResult);

        _container->setEnvironment(&_env);

        this->_ac = _container;
    };

    virtual ~JavaOnline() {}

    virtual int getMethod() const DAAL_C11_OVERRIDE { return 0; } // To make the class non-abstract

    virtual services::Status setResult(const ResultPtr &result) DAAL_C11_OVERRIDE
    {
        _result = result;
        (static_cast<JavaOnlineContainer*>(this->_ac))->setJavaResult(_result);
        _res = _result.get();
        return services::Status();
    }

    virtual services::Status setPartialResult(const PartialResultPtr &partialResult, bool _initFlag = false) DAAL_C11_OVERRIDE
    {
        _partialResult = partialResult;
        (static_cast<JavaOnlineContainer*>(this->_ac))->setJavaPartialResult(_partialResult);
        _pres = _partialResult.get();
        setInitFlag(_initFlag);
        return services::Status();
    }

protected:
    virtual JavaOnline * cloneImpl() const DAAL_C11_OVERRIDE { return NULL; }

    virtual services::Status allocateResult() DAAL_C11_OVERRIDE
    {
        services::Status s = _result->allocate<double>(_partialResult.get(), _par, 0);
        _res    = _result.get();
        _pres   = _partialResult.get();
        return s;
    }

    virtual services::Status allocatePartialResult() DAAL_C11_OVERRIDE
    {
        services::Status s = _partialResult->allocate<double>(&input, _par, 0);
        _pres   = _partialResult.get();
        return s;
    }

    virtual services::Status initializePartialResult() DAAL_C11_OVERRIDE
    {
        services::Status s = _partialResult->initialize<double>(&input, _par, 0);
        _pres   = _partialResult.get();
        return s;
    }
};

} // namespace daal::algorithms::covariance
} // namespace daal::algorithms
} // namespace daal

#endif
