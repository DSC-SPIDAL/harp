/* file: DistributedStep2MasterInput.java */
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
 * @ingroup covariance_distributed
 * @{
 */
package com.intel.daal.algorithms.covariance;

import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__COVARIANCE__DISTRIBUTEDSTEP2MASTERINPUT"></a>
 * @brief %Input objects for the correlation or variance-covariance matrix algorithm
 * in the second step of the distributed processing mode
 */
public final class DistributedStep2MasterInput extends com.intel.daal.algorithms.Input {
    public long cAlgorithm;
    public Precision prec;
    public Method                               method;  /*!< Computation method for the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public DistributedStep2MasterInput(DaalContext context, long cAlgorithm, Precision prec, Method method) {
        super(context);
        this.cObject = cInit(cAlgorithm, prec.getValue(), method.getValue());

        this.cAlgorithm = cAlgorithm;
        this.prec = prec;
        this.method = method;
    }

    /**
     * Adds a partial result to the end of the data collection of input objects for the correlation or
     * variance-covariance matrix algorithm in the second step of the distributed processing mode
     * @param id            Identifier of the input object
     * @param pres          Partial result obtained in the first step of the distributed processing mode
     */
    public void add(DistributedStep2MasterInputId id, PartialResult pres) {
        cAddInput(cObject, id.getValue(), pres.getCObject());
    }

    public void setCInput(long cInput) {
        this.cObject = cInput;
        cSetCInputObject(this.cObject, this.cAlgorithm, prec.getValue(), method.getValue());
    }

    private native long cInit(long algAddr, int prec, int method);
    private native void cSetCInputObject(long inputAddr, long algAddr, int prec, int method);
    private native void cAddInput(long algAddr, int id, long presAddr);
}
/** @} */
