/* file: kmeans_init_kernel.h */
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
//  Declaration of template function that computes K-means.
//--
*/

#ifndef _KMEANS_INIT_H
#define _KMEANS_INIT_H

#include "kmeans_init_types.h"
#include "kernel.h"
#include "numeric_table.h"
#include "service_rng.h"
#include "memory_block.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace kmeans
{
namespace init
{
namespace internal
{

#define isPlusPlusMethod(method)\
    ((method == kmeans::init::plusPlusDense) || (method == kmeans::init::plusPlusCSR) || \
    (method == kmeans::init::parallelPlusDense) || (method == kmeans::init::parallelPlusCSR))

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitKernel: public Kernel
{
public:
    services::Status compute(size_t na, const NumericTable *const *a, size_t nr, const NumericTable *const *r, const Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class KMeansinitKernel<plusPlusDense, algorithmFPType, cpu> : public Kernel
{
public:
    services::Status compute(size_t na, const NumericTable *const *a, size_t nr, const NumericTable *const *r, const Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class KMeansinitKernel<parallelPlusDense, algorithmFPType, cpu> : public Kernel
{
public:
    services::Status compute(size_t na, const NumericTable *const *a, size_t nr, const NumericTable *const *r, const Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class KMeansinitKernel<plusPlusCSR, algorithmFPType, cpu> : public Kernel
{
public:
    services::Status compute(size_t na, const NumericTable *const *a, size_t nr, const NumericTable *const *r, const Parameter *par);
};

template <typename algorithmFPType, CpuType cpu>
class KMeansinitKernel<parallelPlusCSR, algorithmFPType, cpu> : public Kernel
{
public:
    services::Status compute(size_t na, const NumericTable *const *a, size_t nr, const NumericTable *const *r, const Parameter *par);
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep1LocalKernel: public Kernel
{
public:
    services::Status compute(const NumericTable* pData, const Parameter *par,
        NumericTable* pNumPartialClusters, NumericTablePtr& pPartialClusters);
};

template <typename algorithmFPType, CpuType cpu>
class KMeansinitPlusPlusStep1LocalKernel : public Kernel
{
public:
    services::Status compute(const NumericTable* pData, const Parameter *par,
        const NumericTable* pNumPartialClusters, NumericTable*& pPartialClusters);
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep2MasterKernel: public Kernel
{
public:
    services::Status finalizeCompute(size_t na, const NumericTable *const *a, NumericTable* ntClusters, const Parameter *par);
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep2LocalKernel : public Kernel
{
public:
    services::Status compute(const DistributedStep2LocalPlusPlusParameter* par,
        const NumericTable* pData, const NumericTable* pNewCenters, NumericTable** aLocalData,
        NumericTable* pRes, NumericTable* pOutputForStep5);
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep3MasterKernel : public Kernel
{
public:
    KMeansinitStep3MasterKernel() : _brng(nullptr), _rngState(nullptr){}
    ~KMeansinitStep3MasterKernel() { delete _brng; }
    services::Status compute(const Parameter* par, const KeyValueDataCollection* pInputColl,
        MemoryBlock* pRngState,
        KeyValueDataCollection* pOutputColl);

protected:
    services::Status init(size_t seed, data_management::MemoryBlock* pRngState);
    services::Status saveRngState();

protected:
    daal::internal::BaseRNGs<cpu>* _brng;
    MemoryBlock* _rngState;
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep4LocalKernel : public Kernel
{
public:
    services::Status compute(const NumericTable* pData, const NumericTable* pInput, NumericTable** aLocalData, NumericTable* pOutput);
};

template <Method method, typename algorithmFPType, CpuType cpu>
class KMeansinitStep5MasterKernel : public Kernel
{
public:
    services::Status compute(const data_management::DataCollection* pCandidates,
        const data_management::DataCollection* pRating,
        NumericTable* pResCand, NumericTable* pResRating);
    services::Status finalizeCompute(const Parameter *par, const NumericTable* ntCand, const NumericTable* ntWeights,
        const MemoryBlock* pRngState, NumericTable* pCentroids);
};

} // namespace daal::algorithms::kmeans::init::internal
} // namespace daal::algorithms::kmeans::init
} // namespace daal::algorithms::kmeans
} // namespace daal::algorithms
} // namespace daal

#endif
