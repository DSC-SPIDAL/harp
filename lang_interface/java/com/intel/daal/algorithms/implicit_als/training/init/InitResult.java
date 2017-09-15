/* file: InitResult.java */
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

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.algorithms.implicit_als.Model;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__IMPLICIT_ALS__TRAINING__INIT__INITRESULT"></a>
 * @brief Provides methods to access the results of computing the initial model for the
 * implicit ALS training algorithm
 */
public final class InitResult extends com.intel.daal.algorithms.Result {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the result for the implicit ALS initialization algorithm in the distributed processing mode
     * @param context Context to manage the result for the implicit ALS initialization algorithm in the distributed processing mode
     */
    public InitResult(DaalContext context) {
        super(context);
        this.cObject = cNewResult();
    }

    public InitResult(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Returns the result of computing the initial model for the implicit ALS training algorithm
     * @param id   Identifier of the result
     * @return         Result that corresponds to the given identifier
     */
    public Model get(InitResultId id) {
        if (id != InitResultId.model) {
            throw new IllegalArgumentException("id unsupported");
        }
        return new Model(getContext(), cGetResultModel(cObject, id.getValue()));
    }

    /**
     * Sets the result of computing the initial model for the implicit ALS training algorithm
     * @param id    Identifier of the result
     * @param value Result that corresponds to the given identifier
     */
    public void set(InitResultId id, Model value) {
        int idValue = id.getValue();
        if (id != InitResultId.model) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetResultModel(cObject, idValue, value.getCObject());
    }

    private native long cNewResult();

    private native long cGetResultModel(long cResult, int id);

    private native void cSetResultModel(long cResult, int id, long cModel);
}
/** @} */
