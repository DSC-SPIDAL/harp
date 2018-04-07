/* file: InitParameter.java */
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
 * @ingroup implicit_als_init
 * @{
 */
package com.intel.daal.algorithms.implicit_als.training.init;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__IMPLICIT_ALS__TRAINING__INIT__INITPARAMETER"></a>
 * @brief Parameters of the implicit ALS initialization algorithm
 */
public class InitParameter extends com.intel.daal.algorithms.Parameter {

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public InitParameter(DaalContext context, long parAddr) {
        super(context);
        this.cObject = parAddr;
    }

    /**
     * Sets the nUsers parameter
     * @param fullNUsers
     */
    public void setFullNUsers(long fullNUsers) {
        cSetFullNUsers(this.cObject, fullNUsers);
    }

    /**
     * Gets the value of the nUsers parameter
     * @return nUsers
     */
    public long getFullNUsers() {
        return cGetFullNUsers(this.cObject);
    }

    /**
     * Sets the nFactors parameter
     * @param nFactors
     */
    public void setNFactors(long nFactors) {
        cSetNFactors(this.cObject, nFactors);
    }

    /**
     * Gets the value of the nFactors parameter
     * @return nFactors
     */
    public long getNFactors() {
        return cGetNFactors(this.cObject);
    }

    /**
    * @DAAL_DEPRECATED
    * Sets the seed parameter
    * @param seed
    */
    public void setSeed(long seed) {
        cSetSeed(this.cObject, seed);
    }

    /**
    * @DAAL_DEPRECATED
     * Gets the value of the seed parameter
     * @return seed
     */
    public long getSeed() {
        return cGetSeed(this.cObject);
    }

    /**
     * Sets the engine to be used by the algorithm
     * @param engine to be used by the algorithm
     */
    public void setEngine(com.intel.daal.algorithms.engines.BatchBase engine) {
        cSetEngine(cObject, engine.cObject);
    }

    private native void cSetFullNUsers(long algAddr, long nUsers);

    private native long cGetFullNUsers(long algAddr);

    private native void cSetNFactors(long algAddr, long nFactors);

    private native long cGetNFactors(long algAddr);

    private native void cSetSeed(long algAddr, long seed);

    private native long cGetSeed(long algAddr);

    private native void cSetEngine(long cObject, long cEngineObject);
}
/** @} */