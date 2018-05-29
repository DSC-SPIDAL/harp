/* file: Algorithm.java */
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

/**
 * @defgroup algorithms Algorithms
 * @{
 */
/**
 * @defgroup base_algorithms Base Classes
 * @ingroup algorithms
 * @{
 */
/**
 * @brief Contains classes that implement algorithms for data analysis (data mining), and data modeling (training and prediction).
 *        These algorithms include matrix decompositions, clustering algorithms, classification and regression algorithms,
 *        as well as association rules discovery.
 */
package com.intel.daal.algorithms;

import com.intel.daal.services.ContextClient;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__ALGORITHM"></a>
 * @brief Algorithm is the base class for the classes interfacing the major
 *        stages of data processing: Analysis, Training and Prediction.
 */
public abstract class Algorithm extends ContextClient {

    /**
     * @brief Pointer to C++ implementation of the Algorithm
     */
    public long cObject;

    /**
     * Constructs the algorithm
     * @param context  Context to manage the algorithm
     */
    public Algorithm(DaalContext context) {
        super(context);
    }

    public abstract void checkComputeParams();

    /**
     * Returns the newly allocated algorithm with a copy of input objects
     * and parameters of this algorithm
     * @return The newly allocated algorithm
     */
    @Override
    public abstract void dispose();

    public abstract Algorithm clone(DaalContext context);
}
/** @} */
/** @} */
