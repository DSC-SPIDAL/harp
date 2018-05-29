/* file: em_gmm_dense_default_batch_kernel.h */
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
//  Declaration of template function that calculate ems.
//--
*/

#ifndef __EM_GMM_DENSE_DEFAULT_BATCH_KERNEL_H__
#define __EM_GMM_DENSE_DEFAULT_BATCH_KERNEL_H__

#include "em_gmm.h"
#include "kernel.h"
#include "numeric_table.h"
#include "service_blas.h"
#include "em_gmm_dense_default_batch_task.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace em_gmm
{
namespace internal
{

template<typename algorithmFPType, Method method, CpuType cpu>
class EMKernel : public Kernel
{
public:
    typedef SharedPtr<GmmModel<algorithmFPType, cpu> > GmmModelPtr;
    EMKernel() {};

    services::Status compute(NumericTable &dataTable,
                             NumericTable &initialWeights,
                             NumericTable &initialMeans,
                             NumericTable **initialCovariances,
                             NumericTable &resultWeights,
                             NumericTable &resultMeans,
                             NumericTable **resultCovariances,
                             NumericTable &resultNIterations,
                             NumericTable &resultGoalFunction,
                             const Parameter &par);
};

template<typename algorithmFPType, CpuType cpu>
void stepM_mergePartialSums(
    algorithmFPType *cp_n, algorithmFPType *cp_m,
    algorithmFPType *mean_n, algorithmFPType *mean_m,
    algorithmFPType &w_n, algorithmFPType &w_m,
    size_t nFeatures, GmmModel<algorithmFPType, cpu> *covs);

template<typename algorithmFPType, Method method, CpuType cpu>
class EMKernelTask
{
    typedef SharedPtr<GmmModel<algorithmFPType, cpu> > GmmModelPtr;
    typedef GmmModelDiag<algorithmFPType, cpu> GmmModelDiagType;
    typedef GmmModelFull<algorithmFPType, cpu> GmmModelFullType;

    SharedPtr<GmmModel<algorithmFPType, cpu> > initializeCovariances();
public:
    EMKernelTask(NumericTable &dataTable,
                 NumericTable &initialWeights, NumericTable &initialMeans, NumericTable **initialCovariances,
                 NumericTable &resultWeights, NumericTable &resultMeans, NumericTable **resultCovariances,
                 NumericTable &resultNIterations,
                 NumericTable &resultGoalFunction,
                 const Parameter &par);

    services::Status compute();

    Status initialize();
    services::Status setStartValues();
    void setResultToZero();
    Status stepM_merge(size_t iteration);

    static void stepE(const size_t nVectorsInCurrentBlock, Task<algorithmFPType, cpu> &t, em_gmm::CovarianceStorageId covType);
    static algorithmFPType computePartialLogLikelyhood(const size_t nVectorsInCurrentBlock, Task<algorithmFPType, cpu> &t);
    static Status stepM_partial(const size_t nVectorsInCurrentBlock, Task<algorithmFPType, cpu> &t, em_gmm::CovarianceStorageId covType);
    static void stepM_mergePartialSums(
        algorithmFPType *cp_n, algorithmFPType *cp_m,
        algorithmFPType *mean_n, algorithmFPType *mean_m,
        algorithmFPType &w_n, algorithmFPType &w_m,
        size_t nFeatures, GmmModel<algorithmFPType, cpu> *covs);


    algorithmFPType *alpha;
    algorithmFPType *means;
    algorithmFPType *logAlpha;
    int *iterCounterArray;
    algorithmFPType *logLikelyhoodArray;

    size_t blockSizeDefault;
    size_t nBlocks;

    const DAAL_INT nFeatures;
    const DAAL_INT nVectors;
    const DAAL_INT nComponents;
    algorithmFPType logLikelyhoodCorrection;
    const DAAL_INT maxIterations;
    const algorithmFPType threshold;
    TArray<WriteRows<algorithmFPType, cpu, NumericTable>, cpu> covsPtr;
    GmmModelPtr covs;

    WriteRows<algorithmFPType, cpu, NumericTable> weightsBD;
    WriteRows<algorithmFPType, cpu, NumericTable> meansBD;
    WriteRows<int, cpu, NumericTable> nIterationsBD;
    WriteRows<algorithmFPType, cpu, NumericTable> goalFunctionBD;

    NumericTable &dataTable;
    NumericTable &initialWeights;
    NumericTable &initialMeans;
    NumericTable **initialCovariances;
    NumericTable &resultWeights;
    NumericTable &resultMeans;
    NumericTable **resultCovariances;
    NumericTable &resultNIterations;
    NumericTable &resultGoalFunction;
    const Parameter &par;
};

} // namespace internal

} // namespace em_gmm

} // namespace algorithms

} // namespace daal

#endif
