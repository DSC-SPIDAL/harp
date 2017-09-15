/* file: truncated_gaussian_impl.i */
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
//  Implementation of truncated gaussian initializer
//--
*/
#ifndef __TRUNCATED_GAUSSIAN_INITIALIZER_IMPL_I__
#define __TRUNCATED_GAUSSIAN_INITIALIZER_IMPL_I__

#include "initializers_impl.i"

namespace daal
{
namespace algorithms
{
namespace neural_networks
{
namespace initializers
{
namespace truncated_gaussian
{
namespace internal
{
/* sqrt(2) */
template<typename algorithmFPType> inline algorithmFPType sqrt_2        (void){ return algorithmFPType(0.0); }
template<>                         inline float           sqrt_2<float> (void){ uint32_t iv = 0x3FB504F3; float v = *(float*)&iv; return v; }
template<>                         inline double          sqrt_2<double>(void){ uint64_t iv = 0x3FF6A09E667F3BCD; double v = *(double*)&iv; return v; }

/* result = sigma * [ ICDF( CDF(a) + uniform(0,1) * ( CDF(b) - CDF(a)) ) ] + mean;
    CDF(a) = 0, if a = -inf;
    CDF(b) = 1, if b = +inf;
*/
template<typename algorithmFPType, Method method, CpuType cpu>
Status TruncatedGaussianKernel<algorithmFPType, method, cpu>::compute(
    const TruncatedGaussianInitializerTaskDescriptor<algorithmFPType> &desc)
{
    auto engine = initializers::internal::getEngineImpl<cpu>(desc.engine);
    DAAL_CHECK_INITIALIZER_ENGINE(engine);

    Tensor *resultTensor = desc.result;
    size_t size = resultTensor->getSize();

    algorithmFPType mean     = (algorithmFPType)desc.mean;
    algorithmFPType sigma    = (algorithmFPType)desc.sigma;
    algorithmFPType cdf_b    = getCDFNormal(desc.b, mean, sigma);
    algorithmFPType cdf_a    = getCDFNormal(desc.a, mean, sigma);
    algorithmFPType cdf_diff = cdf_b - cdf_a;

    WriteOnlySubtensor<algorithmFPType, cpu, Tensor> resultSubtensor(resultTensor, 0, 0, 0, resultTensor->getDimensionSize(0));
    DAAL_CHECK_BLOCK_STATUS(resultSubtensor);
    algorithmFPType *resultArray = resultSubtensor.get();

    RNGs<algorithmFPType, cpu> rng;
    DAAL_CHECK(
        !rng.uniform(size, resultArray, engine->getState(), 0.0, 1.0),
        ErrorIncorrectErrorcodeFromGenerator
    );

    size_t nBlocks = size / _nElemsInBlock;
    nBlocks += (nBlocks * _nElemsInBlock != size);

    daal::threader_for(nBlocks, nBlocks, [ & ](int block)
    {
        size_t nElemsToProcess = _nElemsInBlock;
        size_t shift = block * _nElemsInBlock;

        algorithmFPType *resultLocal = &resultArray[shift];

        if( block == nBlocks - 1 )
        {
            nElemsToProcess = size - shift;
        }

        for(size_t i = 0; i < nElemsToProcess; i++)
        {
            resultLocal[i] = cdf_a + resultLocal[i] * cdf_diff;
        }

        Math<algorithmFPType,cpu>::vCdfNormInv(nElemsToProcess, resultLocal, resultLocal);

        for(size_t i = 0; i < nElemsToProcess; i++)
        {
            resultLocal[i] = sigma * resultLocal[i] + mean;
        }
    } );

    return Status();
}

template<typename algorithmFPType, Method method, CpuType cpu>
algorithmFPType TruncatedGaussianKernel<algorithmFPType, method, cpu>::getCDFNormal(
    algorithmFPType p, algorithmFPType mean, algorithmFPType sigma)
{
    return (algorithmFPType)0.5 * ( (algorithmFPType)1.0 + Math<algorithmFPType, cpu>::sErf(
        (p - mean) / (sigma * sqrt_2<algorithmFPType>() )) );
}

} // internal
} // namespace truncated_gaussian
} // namespace initializers
} // namespace neural_networks
} // namespace algorithms
} // namespace daal

#endif
