/* file: service_threading.h */
/*******************************************************************************
* Copyright 2015-2018 Intel Corporation
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
//  Declaration of service threding classes and utilities
//--
*/
#ifndef __SERVICE_THREADING_H__
#define __SERVICE_THREADING_H__
#include "threading.h"

namespace daal
{

class Mutex
{
public:
    Mutex();
    ~Mutex();
    void lock();
    void unlock();
private:
    void* _impl;
};

class AutoLock
{
public:
    AutoLock(Mutex& m) : _m(m){ _m.lock(); }
    ~AutoLock() { _m.unlock(); }
private:
    Mutex& _m;
};

#define AUTOLOCK(m) AutoLock __autolock(m);

template<typename F>
class task_impl : public task
{
public:
    DAAL_NEW_DELETE();
    virtual void run()
    {
        _func();
    }
    virtual void destroy()
    {
        delete this;
    }
    static task_impl<F>* create(const F& o)
    {
        return new task_impl<F>(o);
    }

private:
    task_impl(const F& o) : task(), _func(o){}
    F _func;
};

class task_group
{
public:
    task_group() : _impl(NULL)
    {
        _impl = _daal_new_task_group();
    }
    ~task_group()
    {
        if(_impl)
            _daal_del_task_group(_impl);
    }
    template<typename F>
    void run(F &f)
    {
        if(_impl)
            _daal_run_task_group(_impl, task_impl<F>::create(f));
        else
            f();
    }
    void wait()
    {
        if(_impl)
            _daal_wait_task_group(_impl);
    }

protected:
    void* _impl;
};

} // namespace daal

#endif
