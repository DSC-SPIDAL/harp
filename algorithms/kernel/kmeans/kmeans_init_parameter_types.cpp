/* file: kmeans_init_parameter_types.cpp */
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
//  Implementation of kmeans classes.
//--
*/

#include "algorithms/kmeans/kmeans_init_types.h"
#include "daal_defines.h"
#include "daal_strings.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace kmeans
{
namespace init
{
namespace interface1
{

Parameter::Parameter(size_t _nClusters, size_t _offset, size_t _seed) : nClusters(_nClusters), offset(_offset), nRowsTotal(0), seed(_seed),
    oversamplingFactor(0.5), nRounds(5), engine(engines::mt19937::Batch<>::create()) {}

/**
 * Constructs parameters of the algorithm that computes initial clusters for the K-Means algorithm
 * by copying another parameters object
 * \param[in] other    Parameters of the K-Means algorithm
 */
Parameter::Parameter(const Parameter &other) : nClusters(other.nClusters), offset(other.offset), nRowsTotal(other.nRowsTotal), seed(other.seed),
    oversamplingFactor(other.oversamplingFactor), nRounds(other.nRounds), engine(other.engine) {}

services::Status Parameter::check() const
{
    DAAL_CHECK_EX(nClusters > 0, ErrorIncorrectParameter, ParameterName, nClustersStr());
    return services::Status();
}

DistributedStep2LocalPlusPlusParameter::DistributedStep2LocalPlusPlusParameter(size_t _nClusters, bool bFirstIteration) :
    Parameter(_nClusters), outputForStep5Required(false), firstIteration(bFirstIteration)
{}

DistributedStep2LocalPlusPlusParameter::DistributedStep2LocalPlusPlusParameter(const DistributedStep2LocalPlusPlusParameter &other) :
    Parameter(other), outputForStep5Required(other.outputForStep5Required), firstIteration(other.firstIteration)
{}

services::Status DistributedStep2LocalPlusPlusParameter::check() const
{
    return services::Status();
}

} // namespace interface1
} // namespace init
} // namespace kmeans
} // namespace algorithm
} // namespace daal
