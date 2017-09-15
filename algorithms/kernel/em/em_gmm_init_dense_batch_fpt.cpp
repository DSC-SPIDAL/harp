/* file: em_gmm_init_dense_batch_fpt.cpp */
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
//  Implementation of EMforKernel
//--
*/

#include "em_gmm_init_dense_default_batch_kernel.h"
#include "data_management/data/homogen_numeric_table.h"

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

template<typename algorithmFPType>
ErrorID EMforKernel<algorithmFPType>::run(data_management::NumericTable &inputData,
                data_management::NumericTable &inputWeights,
                data_management::NumericTable &inputMeans,
                data_management::DataCollectionPtr &inputCov,
                const em_gmm::CovarianceStorageId covType,
                algorithmFPType &loglikelyhood)
{
    this->input.set(daal::algorithms::em_gmm::data, NumericTablePtr(&inputData, EmptyDeleter()));
    this->input.set(daal::algorithms::em_gmm::inputWeights, NumericTablePtr(&inputWeights, EmptyDeleter()));
    this->input.set(daal::algorithms::em_gmm::inputMeans, NumericTablePtr(&inputMeans, EmptyDeleter()));
    this->input.set(daal::algorithms::em_gmm::inputCovariances, inputCov);
    this->parameter.covarianceStorage = covType;

    daal::algorithms::em_gmm::ResultPtr emResult(new daal::algorithms::em_gmm::Result());
    emResult->set(daal::algorithms::em_gmm::weights, NumericTablePtr(&inputWeights, EmptyDeleter()));
    emResult->set(daal::algorithms::em_gmm::means, NumericTablePtr(&inputMeans, EmptyDeleter()));
    emResult->set(daal::algorithms::em_gmm::covariances, inputCov);

    SharedPtr<HomogenNumericTable<algorithmFPType> > loglikelyhoodValueTable(
        new HomogenNumericTable<algorithmFPType>(1, 1, NumericTable::doAllocate));

    NumericTablePtr nIterationsValueTable(new HomogenNumericTable<int>(1, 1, NumericTable::doAllocate));
    emResult->set(daal::algorithms::em_gmm::goalFunction, loglikelyhoodValueTable);
    emResult->set(daal::algorithms::em_gmm::nIterations, nIterationsValueTable);

    this->setResult(emResult);
    services::Status s = this->computeNoThrow();
    if(!s)
    {
        return ErrorEMInitNoTrialConverges;
    }
    loglikelyhood = loglikelyhoodValueTable->getArray()[0];
    return ErrorID(0);
}

template class EMforKernel<DAAL_FPTYPE>;

}
} // namespace init
} // namespace em_gmm
} // namespace algorithms
} // namespace daal
