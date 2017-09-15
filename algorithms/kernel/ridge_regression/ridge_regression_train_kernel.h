/* file: ridge_regression_train_kernel.h */
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
//  Declaration of structure containing kernels for ridge regression training.
//--
*/

#ifndef __RIDGE_REGRESSION_TRAIN_KERNEL_H__
#define __RIDGE_REGRESSION_TRAIN_KERNEL_H__

#include "numeric_table.h"
#include "algorithm_base_common.h"
#include "ridge_regression_training_types.h"
#include "service_lapack.h"

namespace daal
{
namespace algorithms
{
namespace ridge_regression
{

using namespace daal::data_management;
using namespace daal::services;

namespace training
{
namespace internal
{

template <typename algorithmFpType, training::Method method, CpuType cpu>
class RidgeRegressionTrainBatchKernel
{};

template <typename algorithmFpType, CpuType cpu>
class RidgeRegressionTrainBatchKernel<algorithmFpType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, ridge_regression::Model *r,
                 const daal::algorithms::Parameter * par);
};

template <typename algorithmfptype, training::Method method, CpuType cpu>
class RidgeRegressionTrainOnlineKernel
{};

template <typename algorithmFpType, CpuType cpu>
class RidgeRegressionTrainOnlineKernel<algorithmFpType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, ridge_regression::Model *r,
                 const daal::algorithms::Parameter * par);

    services::Status finalizeCompute(ridge_regression::Model *a, ridge_regression::Model *r,
                         const daal::algorithms::Parameter * par);
};

template <typename algorithmFpType, training::Method method, CpuType cpu>
class RidgeRegressionTrainDistributedKernel
{};

template <typename algorithmFpType, CpuType cpu>
class RidgeRegressionTrainDistributedKernel<algorithmFpType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(size_t n, daal::algorithms::Model ** partialModels, daal::algorithms::Model * r, const daal::algorithms::Parameter * par);

    services::Status finalizeCompute(ridge_regression::Model *a, ridge_regression::Model *r, const daal::algorithms::Parameter * par);

protected:
    Status merge(daal::algorithms::Model * a, daal::algorithms::Model * r, const daal::algorithms::Parameter * par);

    void mergePartialSums(DAAL_INT dim, DAAL_INT ny, algorithmFpType * axtx, algorithmFpType * axty, algorithmFpType * rxtx, algorithmFpType * rxty);
};

} // namespace internal
} // namespace training
} // namespace ridge_regression
} // namespace algorithms
} // namespace daal

#endif
