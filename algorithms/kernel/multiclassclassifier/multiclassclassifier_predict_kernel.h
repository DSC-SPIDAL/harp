/* file: multiclassclassifier_predict_kernel.h */
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
//  Declaration of template function that computes prediction results using
//  Multi-class classifier model.
//--
*/

#ifndef __MULTICLASSCLASSIFIER_PREDICT_FPK_H__
#define __MULTICLASSCLASSIFIER_PREDICT_FPK_H__

#include "numeric_table.h"
#include "model.h"
#include "algorithm.h"
#include "multi_class_classifier_predict_types.h"
#include "service_defines.h"
#include "service_arrays.h"

using namespace daal::data_management;
using namespace daal::services::internal;

namespace daal
{
namespace algorithms
{
namespace multi_class_classifier
{
namespace prediction
{
namespace internal
{
template<prediction::Method pmethod, training::Method tmethod, typename algorithmFPType, CpuType cpu>
struct MultiClassClassifierPredictKernel : public Kernel
{
    services::Status compute(const NumericTable *a, const daal::algorithms::Model *m, NumericTable *r,
                             const daal::algorithms::Parameter *par);
};

template<typename algorithmFPType, CpuType cpu>
size_t getMultiClassClassifierPredictBlockSize()
{
    return 128;
}

template<typename algorithmFPType, CpuType cpu>
services::Status getNonEmptyClassMap(size_t &nClasses, const Model *model, size_t *nonEmptyClassMap)
{
    TArray<bool, cpu> nonEmptyClassBuffer(nClasses);
    DAAL_CHECK_MALLOC(nonEmptyClassBuffer.get());
    bool *nonEmptyClass = (bool *)nonEmptyClassBuffer.get();
    for (size_t i = 0; i < nClasses; i++)
        nonEmptyClass[i] = false;

    for (size_t i = 1, imodel = 0; i < nClasses; i++)
    {
        for (size_t j = 0; j < i; j++, imodel++)
        {
            const bool ijModelNotEmpty(model->getTwoClassClassifierModel(imodel));
            nonEmptyClass[i] |= ijModelNotEmpty;
            nonEmptyClass[j] |= ijModelNotEmpty;
        }
    }

    size_t nNonEmptyClasses = 0;
    for (size_t i = 0; i < nClasses; i++)
    {
        if (nonEmptyClass[i])
            nonEmptyClassMap[nNonEmptyClasses++] = i;
    }
    nClasses = nNonEmptyClasses;
    return services::Status();
}

} // namespace internal
} // namespace prediction
} // namespace multi_class_classifier
} // namespace algorithms
} // namespace daal

#endif
