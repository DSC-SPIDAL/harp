/* file: zscore_dense_default_impl.i */
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
//  Implementation of defaultDense method for zscore normalization algorithm
//--
*/

#ifndef __ZSCORE_DENSE_DEFAULT_IMPL_I__
#define __ZSCORE_DENSE_DEFAULT_IMPL_I__

namespace daal
{
namespace algorithms
{
namespace normalization
{
namespace zscore
{
namespace internal
{

template<typename algorithmFPType, CpuType cpu>
Status ZScoreKernel<algorithmFPType, defaultDense, cpu>::computeMeanVariance_thr(NumericTablePtr &inputTable,
                                                                                 algorithmFPType* resultMean,
                                                                                 algorithmFPType* resultVariance,
                                                                                 const daal::algorithms::Parameter &par)
{
    const size_t _nFeatures = inputTable->getNumberOfColumns();

    Parameter<algorithmFPType, defaultDense> *parameter = static_cast<Parameter<algorithmFPType, defaultDense> *>(const_cast<daal::algorithms::Parameter *>(&par));

    parameter->moments->input.set(low_order_moments::data, inputTable);
    parameter->moments->parameter.estimatesToCompute = low_order_moments::estimatesMeanVariance;
    DAAL_CHECK(parameter->moments->computeNoThrow(), ErrorMeanAndStandardDeviationComputing);

    NumericTablePtr meanTable     = parameter->moments->getResult()->get(low_order_moments::mean);
    NumericTablePtr varianceTable = parameter->moments->getResult()->get(low_order_moments::variance);

    ReadRows<algorithmFPType, cpu, NumericTable> meanBlock( meanTable.get(), 0, 1 );
    DAAL_CHECK_BLOCK_STATUS(meanBlock);
    const algorithmFPType* meanArray = meanBlock.get();

    ReadRows<algorithmFPType, cpu, NumericTable> varianceBlock( varianceTable.get(), 0, 1 );
    DAAL_CHECK_BLOCK_STATUS(varianceBlock);
    const algorithmFPType* varianceArray = varianceBlock.get();

    /* Convert array of variances to inverse sigma's */
   PRAGMA_IVDEP
   PRAGMA_VECTOR_ALWAYS
   PRAGMA_VECTOR_ALWAYS
    for(int j = 0; j < _nFeatures; j++)
    {
        resultMean[j]     = meanArray[j];
        resultVariance[j] = algorithmFPType(1.0) / Math<algorithmFPType, cpu>::sSqrt(varianceArray[j]);
    }

    return Status();
}

} // namespace internal
} // namespace zscore
} // namespace normalization
} // namespace algorithms
} // namespace daal

#endif
