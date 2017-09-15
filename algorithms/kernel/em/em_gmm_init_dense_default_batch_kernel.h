/* file: em_gmm_init_dense_default_batch_kernel.h */
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
//  Implementation of em algorithm
//--
*/

#ifndef __EM_GMM_INIT_DENSE_DEFAULT_BATCH_KERNEL_H__
#define __EM_GMM_INIT_DENSE_DEFAULT_BATCH_KERNEL_H__

#include "kernel.h"
#include "service_numeric_table.h"
#include "numeric_table.h"
#include "homogen_numeric_table.h"
#include "service_memory.h"
#include "em_gmm_init_types.h"
#include "em_gmm_init_batch.h"
#include "em_gmm.h"
#include "uniform_kernel.h"

namespace daal
{
namespace algorithms
{
namespace em_gmm
{
namespace init
{
namespace internal
{

using namespace daal::data_management;
using namespace daal::internal;
using namespace daal::services;

template<typename algorithmFPType, CpuType cpu>
class GmmSigma
{
    typedef HomogenNumericTableCPU<algorithmFPType, cpu> HomogenNT;
    typedef SharedPtr<HomogenNT> HomogenNTPtr;
public:
    GmmSigma(em_gmm::CovarianceStorageId _covType, size_t _nComponents, size_t _nFeatures) : covType(_covType), nComponents(_nComponents), nFeatures(_nFeatures),
        sigma(new DataCollection())
    {
        nRows = nFeatures;
        if (covType == em_gmm::diagonal) {nRows = 1;}
        for(size_t i = 0; i < nComponents; i++)
        {
            sigma->push_back(HomogenNTPtr (new HomogenNT(nFeatures, nRows)));
        }
    }
    void setVariance(algorithmFPType *varianceArray)
    {
        for(int k = 0; k < nComponents; k++)
        {
            auto workSigma = static_cast<HomogenNT *>((*sigma)[k].get());
            algorithmFPType *sigmaArray = workSigma->getArray();
            if(covType == em_gmm::diagonal)
            {
                for(int i = 0; i < nFeatures; i++)
                {
                    sigmaArray[i] = varianceArray[i];
                }
            }
            else
            {
                for(int i = 0; i < nFeatures * nFeatures; i++)
                {
                    sigmaArray[i] = 0.0;
                }
                for(int i = 0; i < nFeatures; i++)
                {
                    sigmaArray[i * nFeatures + i] = varianceArray[i];
                }
            }
        }
    }
    DataCollectionPtr &getSigma()
    {
        return sigma;
    }
    Status writeToTables(DataCollectionPtr covariancesToInit)
    {
        for (size_t k = 0; k < nComponents; k++)
        {
            NumericTablePtr covariance = staticPointerCast<NumericTable, SerializationIface>((*covariancesToInit)[k]);
            WriteOnlyRows<algorithmFPType, cpu, NumericTable> covarianceBlock(covariance.get(), 0, nRows);
            DAAL_CHECK_BLOCK_STATUS(covarianceBlock)
            algorithmFPType *covarianceArray = covarianceBlock.get();

            auto workSigma = static_cast<HomogenNT *>((*sigma)[k].get());
            for (size_t i = 0; i < nRows * nFeatures; i++)
            {
                covarianceArray[i] = (workSigma->getArray())[i];
            }
        }
        return Status();
    }

private:
    DataCollectionPtr sigma;
    em_gmm::CovarianceStorageId covType;
    size_t nFeatures;
    size_t nComponents;
    size_t nRows;
};

template<typename algorithmFPType, Method method, CpuType cpu>
class EMInitKernel : public Kernel
{
public:
    services::Status compute(NumericTable &data, NumericTable &weightsToInit, NumericTable &meansToInit,
                             DataCollectionPtr &covariancesToInit, const Parameter &par);
};

template<typename algorithmFPType, Method method, CpuType cpu>
class EMInitKernelTask
{
    typedef HomogenNumericTableCPU<algorithmFPType, cpu> HomogenNT;
    typedef SharedPtr<HomogenNT> HomogenNTPtr;
public:
    EMInitKernelTask(NumericTable &data, NumericTable &weightsToInit,
                     NumericTable &meansToInit, DataCollectionPtr &covariancesToInit, const Parameter &parameter);
    Status compute();
private:
    Status writeValuesToTables();
    Status setSelectedSetAsInitialValues();
    ErrorID runEM();
    Status generateSelectedSet();
    Status initialize();
    Status computeVariance();

    NumericTable &data;
    NumericTable &weightsToInit;
    NumericTable &meansToInit;
    DataCollectionPtr &covariancesToInit;
    const Parameter &parameter;
    const size_t nComponents;
    const size_t nFeatures;
    const size_t nVectors;
    const size_t nTrials;
    const size_t nIterations;
    double accuracyThreshold;
    HomogenNTPtr alpha;
    HomogenNTPtr means;
    algorithmFPType loglikelyhood;
    algorithmFPType maxLoglikelyhood;
    algorithmFPType *varianceArray;
    TArray<algorithmFPType, cpu> varianceArrayPtr;
    TArray<int, cpu> selectedSetPtr;
    int *selectedSet;
    GmmSigma<algorithmFPType, cpu> covs;
    distributions::uniform::internal::UniformKernel<int, distributions::uniform::defaultDense, cpu> uniformKernel;
    distributions::uniform::Parameter<int> uniformParameter;
};


template<typename algorithmFPType>
class EMforKernel : public daal::algorithms::em_gmm::Batch<algorithmFPType, em_gmm::defaultDense>
{
public:
    EMforKernel(const size_t nComponents) : daal::algorithms::em_gmm::Batch<algorithmFPType, em_gmm::defaultDense>(nComponents) {}
    virtual ~EMforKernel()
    {}

    ErrorID run(data_management::NumericTable &inputData,
                data_management::NumericTable &inputWeights,
                data_management::NumericTable &inputMeans,
                data_management::DataCollectionPtr &inputCov,
                const em_gmm::CovarianceStorageId covType,
                algorithmFPType &loglikelyhood);

};

} // namespace internal

} // namespace init

} // namespace em_gmm

} // namespace algorithms

} // namespace daal

#endif
