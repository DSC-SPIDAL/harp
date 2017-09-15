/* file: pca_dense_correlation_base.h */
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
//  Declaration of template structs that calculate PCA Correlation.
//--
*/

#ifndef __PCA_DENSE_CORRELATION_BASE_H__
#define __PCA_DENSE_CORRELATION_BASE_H__

#include "pca_types.h"
#include "service_lapack.h"
#include "service_defines.h"
#include "service_numeric_table.h"
#include "services/error_handling.h"

using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace internal
{

template <typename algorithmFPType, CpuType cpu>
class PCACorrelationBase : public Kernel
{
public:
    explicit PCACorrelationBase() {};

    virtual ~PCACorrelationBase() {};

protected:
    services::Status computeCorrelationEigenvalues(const data_management::NumericTable& correlation,
        data_management::NumericTable& eigenvectors, data_management::NumericTable& eigenvalues);
    services::Status computeEigenvectorsInplace(size_t nFeatures, algorithmFPType *eigenvectors, algorithmFPType *eigenvalues);
    services::Status sortEigenvectorsDescending(size_t nFeatures, algorithmFPType *eigenvectors, algorithmFPType *eigenvalues);

private:
    void copyArray(size_t size, const algorithmFPType *source, algorithmFPType *destination);
};

template <typename algorithmFPType, CpuType cpu>
void PCACorrelationBase<algorithmFPType, cpu>::copyArray(size_t size, const algorithmFPType *source, algorithmFPType *destination)
{
    if (source != destination)
    {
        for (size_t i = 0; i < size; i++)
        {
            destination[i] = source[i];
        }
    }
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationBase<algorithmFPType, cpu>::computeCorrelationEigenvalues(
    const data_management::NumericTable& correlation,
    data_management::NumericTable& eigenvectors, data_management::NumericTable& eigenvalues)
{
    using data_management::BlockDescriptor;

    const size_t nFeatures = correlation.getNumberOfColumns();

    ReadRows<algorithmFPType, cpu> correlationBlock(const_cast<data_management::NumericTable&>(correlation), 0, nFeatures);
    DAAL_CHECK_BLOCK_STATUS(correlationBlock);
    const algorithmFPType *correlationArray = correlationBlock.get();

    WriteOnlyRows<algorithmFPType, cpu> eigenvectorsBlock(eigenvectors, 0, nFeatures);
    DAAL_CHECK_BLOCK_STATUS(eigenvectorsBlock);
    algorithmFPType *eigenvectorsArray = eigenvectorsBlock.get();

    WriteOnlyRows<algorithmFPType, cpu> eigenvaluesBlock(eigenvalues, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(eigenvaluesBlock);
    algorithmFPType *eigenvaluesArray = eigenvaluesBlock.get();

    copyArray(nFeatures * nFeatures, correlationArray, eigenvectorsArray);

    services::Status s = computeEigenvectorsInplace(nFeatures, eigenvectorsArray, eigenvaluesArray);
    if(s)
        s = sortEigenvectorsDescending(nFeatures, eigenvectorsArray, eigenvaluesArray);
    return s;
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationBase<algorithmFPType, cpu>::computeEigenvectorsInplace(size_t nFeatures,
    algorithmFPType *eigenvectors, algorithmFPType *eigenvalues)
{
    char jobz  = 'V';
    char uplo  = 'U';

    DAAL_INT lwork = 2 * nFeatures * nFeatures + 6 * nFeatures + 1;
    DAAL_INT liwork = 5 * nFeatures + 3;
    DAAL_INT info;

    TArray<algorithmFPType, cpu> work(lwork);
    TArray<DAAL_INT, cpu> iwork(liwork);
    DAAL_CHECK_MALLOC(work.get() && iwork.get());

    Lapack<algorithmFPType, cpu>::xsyevd(&jobz, &uplo, (DAAL_INT *)(&nFeatures), eigenvectors, (DAAL_INT *)(&nFeatures), eigenvalues,
                        work.get(), &lwork, iwork.get(), &liwork, &info);
    if (info != 0)
        return services::Status(services::ErrorPCAFailedToComputeCorrelationEigenvalues);
    return services::Status();
}

template <typename algorithmFPType, CpuType cpu>
services::Status PCACorrelationBase<algorithmFPType, cpu>::sortEigenvectorsDescending(size_t nFeatures,
    algorithmFPType *eigenvectors, algorithmFPType *eigenvalues)
{
    for(size_t i = 0; i < nFeatures / 2; i++)
    {
        const algorithmFPType tmp = eigenvalues[i];
        eigenvalues[i] = eigenvalues[nFeatures - 1 - i];
        eigenvalues[nFeatures - 1 - i] = tmp;
    }

    TArray<algorithmFPType, cpu> eigenvectorTmp(nFeatures);
    DAAL_CHECK_MALLOC(eigenvectorTmp.get());
    for(size_t i = 0; i < nFeatures / 2; i++)
    {
        copyArray(nFeatures, eigenvectors + i * nFeatures, eigenvectorTmp.get());
        copyArray(nFeatures, eigenvectors + nFeatures * (nFeatures - 1 - i), eigenvectors + i * nFeatures);
        copyArray(nFeatures, eigenvectorTmp.get(), eigenvectors + nFeatures * (nFeatures - 1 - i));
    }
    return services::Status();
}

template <ComputeMode mode, typename algorithmFPType, CpuType cpu>
class PCACorrelationKernel : public PCACorrelationBase<algorithmFPType, cpu> {};

} // namespace internal
} // namespace pca
} // namespace algorithms
} // namespace daal

#endif
