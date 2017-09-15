/* file: classifier_model_impl.h */
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
//  Implementation of the class defining the classifier model
//--
*/

#ifndef __CLASSIFIER_MODEL_IMPL_H__
#define __CLASSIFIER_MODEL_IMPL_H__

#include "algorithms/classifier/classifier_model.h"

namespace daal
{
namespace algorithms
{
namespace classifier
{
namespace internal
{
class ModelInternal
{
public:
    ModelInternal(size_t nFeatures = 0) : _nFeatures(nFeatures)
    {}

    void setNumberOfFeatures(size_t nFeatures) { _nFeatures = nFeatures; }
    virtual size_t getNumberOfFeatures() const { return _nFeatures; }
protected:
    size_t _nFeatures;
};

class ModelImpl : public classifier::Model,
                  public ModelInternal
{
public:
    typedef ModelInternal   ImplType;

    ModelImpl(size_t nFeatures = 0) : ImplType(nFeatures)
    {}

    void setNumberOfFeatures(size_t nFeatures) { ImplType::setNumberOfFeatures(nFeatures); }
    size_t getNumberOfFeatures() const DAAL_C11_OVERRIDE { return ImplType::getNumberOfFeatures(); }

    void setNFeatures(size_t nFeatures) DAAL_C11_OVERRIDE { setNumberOfFeatures(nFeatures); }
    size_t getNFeatures() const DAAL_C11_OVERRIDE { return getNumberOfFeatures(); }

protected:
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive * arch)
    {
        classifier::Model::serialImpl<Archive, onDeserialize>(arch);
        arch->set(ImplType::_nFeatures);

        return services::Status();
    }
};
}
}
}
}
#endif
