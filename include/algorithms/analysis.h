/* file: analysis.h */
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
//  Implementation of base classes defining algorithm interface.
//--
*/

#ifndef __ANALYSIS_H__
#define __ANALYSIS_H__

#include "algorithms/algorithm_base.h"

namespace daal
{
/**
 * @defgroup algorithms Algorithms
 * @{
 */
namespace algorithms
{
/**
 * @defgroup analysis Analysis
 * \brief Contains classes for analysis algorithms that are intended to uncover the underlying structure
 *        of a data set and to characterize it by a set of quantitative measures, such as statistical moments,
 *        correlations coefficients, and so on.
 * @ingroup algorithms
 * @{
 */
/**
 * <a name="DAAL-CLASS-ALGORITHMS__ANALYSISCONTAINERIFACE"></a>
 * \brief Abstract interface class that provides virtual methods to access and run implementations
 *        of the analysis algorithms. It is associated with the Analysis class
 *        and supports the methods for computation and finalization of the analysis results
 *        in the batch, distributed, and online modes.
 *        The methods of the container are defined in derivative containers
 *        defined for each algorithm of data analysis.
 * \tparam mode Computation mode of the algorithm, \ref ComputeMode
 */
template<ComputeMode mode> class AnalysisContainerIface : public AlgorithmContainerImpl<mode>
{
public:
    AnalysisContainerIface(daal::services::Environment::env *daalEnv = 0): AlgorithmContainerImpl<mode>(daalEnv) {}
};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__ANALYSIS"></a>
 * \brief Provides methods for execution of operations over data, such as computation of Summary Statistics estimates.
 *        The methods of the class support different computation modes: batch, distributed, and online(see \ref ComputeMode).
 *        Classes that implement specific algorithms of the data analysis are derived classes of the \ref Analysis class.
 *        The class additionally provides virtual methods for validation of input and output parameters
 *        of the algorithms.
 * \tparam mode Computation mode of the algorithm, \ref ComputeMode
 */
template<ComputeMode mode> class Analysis: public AlgorithmImpl<mode> {};
}
}
/** @} */
/** @} */
#endif
