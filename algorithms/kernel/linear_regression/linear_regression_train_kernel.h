/* file: linear_regression_train_kernel.h */
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
//  Declaration of structure containing kernels for linear regression
//  training.
//--
*/

#ifndef __LINEAR_REGRESSION_TRAIN_KERNEL_H__
#define __LINEAR_REGRESSION_TRAIN_KERNEL_H__

#include "numeric_table.h"
#include "algorithm_base_common.h"
#include "linear_regression_training_types.h"
#include "linear_model_train_normeq_kernel.h"
#include "linear_model_train_qr_kernel.h"
#include "algorithm_kernel.h"

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
using namespace daal::data_management;
using namespace daal::services;
using namespace daal::algorithms::linear_model::normal_equations::training::internal;


template <typename algorithmFPType, training::Method method, CpuType cpu>
class BatchKernel
{};

template <typename algorithmFPType, CpuType cpu>
class KernelHelper : public KernelHelperIface<algorithmFPType, cpu>
{
public:
    Status computeBetasImpl(DAAL_INT p, const algorithmFPType *a,algorithmFPType *aCopy,
                            DAAL_INT ny, algorithmFPType *b, bool inteceptFlag) const;
};

template <typename algorithmFPType, CpuType cpu>
class BatchKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::normal_equations::training::internal::UpdateKernel <algorithmFPType, cpu>     UpdateKernelType;
    typedef linear_model::normal_equations::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(const NumericTable &x, const NumericTable &y, NumericTable &xtx,
                   NumericTable &xty, NumericTable &beta, bool interceptFlag) const;
};

template <typename algorithmFPType, CpuType cpu>
class BatchKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::qr::training::internal::UpdateKernel <algorithmFPType, cpu>     UpdateKernelType;
    typedef linear_model::qr::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(const NumericTable &x, const NumericTable &y, NumericTable &r,
                   NumericTable &qty, NumericTable &beta, bool interceptFlag) const;
};

template <typename algorithmFPType, training::Method method, CpuType cpu>
class OnlineKernel
{};

template <typename algorithmFPType, CpuType cpu>
class OnlineKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::normal_equations::training::internal::UpdateKernel <algorithmFPType, cpu>     UpdateKernelType;
    typedef linear_model::normal_equations::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(const NumericTable &x, const NumericTable &y, NumericTable &xtx, NumericTable &xty,
                   bool interceptFlag) const;
    Status finalizeCompute(const NumericTable &xtx, const NumericTable &xty, NumericTable &xtxFinal, NumericTable &xtyFinal,
                           NumericTable &beta, bool interceptFlag) const;
};

template <typename algorithmFPType, CpuType cpu>
class OnlineKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::qr::training::internal::UpdateKernel <algorithmFPType, cpu>     UpdateKernelType;
    typedef linear_model::qr::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(const NumericTable &x, const NumericTable &y, NumericTable &r, NumericTable &qty,
                   bool interceptFlag) const;
    Status finalizeCompute(const NumericTable &r, const NumericTable &qty, NumericTable &rFinal,
                           NumericTable &qtyFinal, NumericTable &beta, bool interceptFlag) const;
};


template <typename algorithmFPType, training::Method method, CpuType cpu>
class DistributedKernel
{};

template <typename algorithmFPType, CpuType cpu>
class DistributedKernel<algorithmFPType, training::normEqDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::normal_equations::training::internal::MergeKernel   <algorithmFPType, cpu>    MergeKernelType;
    typedef linear_model::normal_equations::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(size_t n, NumericTable **partialxtx, NumericTable **partialxty,
                   NumericTable &xtx, NumericTable &xty) const;
    Status finalizeCompute(const NumericTable &xtx, const NumericTable &xty, NumericTable &xtxFinal, NumericTable &xtyFinal,
                           NumericTable &beta, bool interceptFlag) const;
};

template <typename algorithmFPType, CpuType cpu>
class DistributedKernel<algorithmFPType, training::qrDense, cpu> : public daal::algorithms::Kernel
{
    typedef linear_model::qr::training::internal::MergeKernel   <algorithmFPType, cpu>    MergeKernelType;
    typedef linear_model::qr::training::internal::FinalizeKernel<algorithmFPType, cpu>    FinalizeKernelType;
public:
    Status compute(size_t n, NumericTable **partialr, NumericTable **partialqty,
                   NumericTable &r, NumericTable &qty) const;
    Status finalizeCompute(const NumericTable &r, const NumericTable &qty, NumericTable &rFinal,
                           NumericTable &qtyFinal, NumericTable &beta, bool interceptFlag) const;
};

} // internal
} // training
} // linear_regression
} // algorithms
} // daal

#endif
