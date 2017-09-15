/* file: linear_regression_train_kernel.h */
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
//  Declaration of structure containing kernels for linear regression
//  training.
//--
*/

#ifndef __LINEAR_REGRESSION_TRAIN_KERNEL_H__
#define __LINEAR_REGRESSION_TRAIN_KERNEL_H__

#include "numeric_table.h"
#include "algorithm_base_common.h"
#include "linear_regression_training_types.h"
#include "service_lapack.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace training
{
namespace internal
{

template <typename algorithmFPType, training::Method method, CpuType cpu>
class LinearRegressionTrainBatchKernel
{};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainBatchKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, linear_regression::Model *r,
                 const daal::algorithms::Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainBatchKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, linear_regression::Model *r,
                 const daal::algorithms::Parameter *par);
};


template <typename algorithmFPType, training::Method method, CpuType cpu>
class LinearRegressionTrainOnlineKernel
{};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainOnlineKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, linear_regression::Model *r,
                 const daal::algorithms::Parameter *par);
    services::Status finalizeCompute(linear_regression::Model *a, linear_regression::Model *r,
                         const daal::algorithms::Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainOnlineKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(NumericTable *x, NumericTable *y, linear_regression::Model *r,
                 const daal::algorithms::Parameter *par);
    services::Status finalizeCompute(linear_regression::Model *a, linear_regression::Model *r,
                         const daal::algorithms::Parameter *par);
};


template <typename algorithmFPType, training::Method method, CpuType cpu>
class LinearRegressionTrainDistributedKernel
{};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainDistributedKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(size_t n, daal::algorithms::Model **partialModels, daal::algorithms::Model *r,
                 const daal::algorithms::Parameter *par);
    services::Status finalizeCompute(linear_regression::Model *a, linear_regression::Model *r,
                         const daal::algorithms::Parameter *par);
protected:
    services::Status merge(daal::algorithms::Model *a, daal::algorithms::Model *r, const daal::algorithms::Parameter *par);
    void mergePartialSums(DAAL_INT dim, DAAL_INT ny, algorithmFPType *axtx, algorithmFPType *axty, algorithmFPType *rxtx, algorithmFPType *rxty);
};

template <typename algorithmFPType, CpuType cpu>
class LinearRegressionTrainDistributedKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
public:
    services::Status compute(size_t n, daal::algorithms::Model **partialModels, daal::algorithms::Model *r,
                 const daal::algorithms::Parameter *par);
    services::Status finalizeCompute(linear_regression::Model *a, linear_regression::Model *r,
                         const daal::algorithms::Parameter *par);
protected:
    services::Status merge(daal::algorithms::Model *a, daal::algorithms::Model *r,
                           const daal::algorithms::Parameter *par);
};

} // internal
} // training
} // linear_regression
} // algorithms
} // daal

#endif
