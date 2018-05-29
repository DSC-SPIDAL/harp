/* file: stump_train_kernel.h */
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
//  Declaration of template function that trains Decision Stump.
//--
*/

#ifndef __STUMP_TRAIN_KERNEL_H__
#define __STUMP_TRAIN_KERNEL_H__

#include "stump_training_types.h"
#include "stump_model.h"
#include "kernel.h"
#include "numeric_table.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace stump
{
namespace training
{
namespace internal
{

template <Method method, typename algorithmFPtype , CpuType cpu>
class StumpTrainKernel : public Kernel
{
public:
    services::Status compute(size_t n, const NumericTable *const *a, Model *r, const Parameter *par);

private:
    void StumpQSort( size_t n, algorithmFPtype *x, algorithmFPtype *w, algorithmFPtype *z );

    services::Status stumpRegressionOrdered(size_t nVectors,
                                const algorithmFPtype *x, const algorithmFPtype *w, const algorithmFPtype *z,
                                algorithmFPtype sumW, algorithmFPtype sumM, algorithmFPtype sumS,
                                algorithmFPtype &minS, algorithmFPtype& splitPoint,
                                algorithmFPtype& lMean, algorithmFPtype& rMean);

    services::Status stumpRegressionCategorical(size_t n, size_t nCategories,
                                    const int *x, const algorithmFPtype *w, const algorithmFPtype *z,
                                    algorithmFPtype sumW, algorithmFPtype sumM, algorithmFPtype sumS,
                                    algorithmFPtype &minS, algorithmFPtype& splitPoint,
                                    algorithmFPtype& lMean, algorithmFPtype& rMean);

    void computeSums(size_t n, const algorithmFPtype *w, const algorithmFPtype *z, algorithmFPtype& sumW, algorithmFPtype& sumM,
                     algorithmFPtype& sumS);

    services::Status doStumpRegression(size_t n, size_t dim, const NumericTable *x, const algorithmFPtype *w,
        const algorithmFPtype *z, size_t& splitFeature, algorithmFPtype& splitPoint,
        algorithmFPtype& leftValue, algorithmFPtype& rightValue);
};

} // namespace daal::algorithms::stump::training::internal
}
}
}
} // namespace daal

#endif
