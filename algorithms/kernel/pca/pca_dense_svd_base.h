/* file: pca_dense_svd_base.h */
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
//  Declaration of template structs that calculate PCA SVD.
//--
*/

#ifndef __PCA_DENSE_SVD_BASE_H__
#define __PCA_DENSE_SVD_BASE_H__

#include "service_numeric_table.h"

namespace daal
{
namespace algorithms
{
namespace pca
{
namespace internal
{
enum InputDataType
{
    nonNormalizedDataset = 0,   /*!< Original, non-normalized data set */
    normalizedDataset    = 1,   /*!< Normalized data set whose feature vectors have zero average and unit variance */
    correlation          = 2    /*!< Correlation matrix */
};


template <typename algorithmFPType, CpuType cpu>
class PCASVDKernelBase : public Kernel
{
public:
    PCASVDKernelBase() {}
    virtual ~PCASVDKernelBase() {}

protected:
    services::Status scaleSingularValues(data_management::NumericTable& eigenvaluesTable, size_t nVectors);
};

template <typename algorithmFPType, CpuType cpu>
services::Status PCASVDKernelBase<algorithmFPType, cpu>::scaleSingularValues(NumericTable& eigenvaluesTable, size_t nVectors)
{
    const size_t nFeatures = eigenvaluesTable.getNumberOfColumns();
    daal::internal::WriteRows<algorithmFPType, cpu> block(eigenvaluesTable, 0, 1);
    DAAL_CHECK_BLOCK_STATUS(block);
    algorithmFPType *eigenvalues = block.get();

    for (size_t i = 0; i < nFeatures; i++)
    {
        eigenvalues[i] = eigenvalues[i] * eigenvalues[i] / (nVectors - 1);
    }
    return services::Status();
}

} // namespace internal
} // namespace pca
} // namespace algorithms
} // namespace daal
#endif
