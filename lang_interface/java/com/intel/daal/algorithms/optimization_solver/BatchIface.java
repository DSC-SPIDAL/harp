/* file: BatchIface.java */
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

/**
 * @addtogroup optimization_solver
 * @{
 */
/**
 * @brief Contains classes for computing the optimization solvers
 */
package com.intel.daal.algorithms.optimization_solver;

import com.intel.daal.algorithms.AnalysisBatch;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.ComputeMode;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__BATCH"></a>
 * @brief %Base interface for the Optimization solver algorithm in the batch processing mode
 * <!-- \n<a href="DAAL-REF-OPTIMIZATION_SOLVER-ALGORITHM">Optimization solver algorithm description and usage models</a> -->
 */
public abstract class BatchIface extends com.intel.daal.algorithms.AnalysisBatch {

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the optimization solver algorithm in the batch processing mode
     * @param context  Context to manage the optimization solver algorithm
     */
    public BatchIface(DaalContext context) {
        super(context);
    }

    /**
     * Returns the newly allocated Optimization solver algorithm
     * with a copy of input objects and parameters of this Optimization solver algorithm
     * @param context    Context to manage the Optimization solver algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public abstract BatchIface clone(DaalContext context);
}
/** @} */
