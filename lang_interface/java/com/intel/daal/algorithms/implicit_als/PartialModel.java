/* file: PartialModel.java */
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
 * @ingroup implicit_als
 * @{
 */
package com.intel.daal.algorithms.implicit_als;

import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__IMPLICIT_ALS__PARTIALMODEL"></a>
 *
 */
public class PartialModel extends com.intel.daal.algorithms.Model {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public PartialModel(DaalContext context, long cModel) {
        super(context, cModel);
    }

    /**
     * Constructs a partial implicit ALS model from the indices and factors stored in the numeric tables
     * @param context   Context to manage the partial model
     * @param factors   Numeric table containing factors stored in row-major order
     * @param indices   Numeric table containing the indices of factors
     */
    public PartialModel(DaalContext context, NumericTable factors, NumericTable indices) {
        super(context);
        this.cObject = cNewPartialModel(factors.getCObject(), indices.getCObject());
    }

    /**
     * Returns the numeric table containing factors stored in row-major order
     * @return Numeric table containing factors stored in row-major order
     */
    public NumericTable getFactors() {
        return (NumericTable)Factory.instance().createObject(getContext(), cGetFactors(this.getCObject()));
    }

    /**
     * Returns the numeric table containing the indices of factors
     * @return Numeric table containing the indices of factors
     */
    public NumericTable getIndices() {
        return (NumericTable)Factory.instance().createObject(getContext(), cGetIndices(this.getCObject()));
    }

    protected native long cGetFactors(long partialModelAddr);
    protected native long cGetIndices(long partialModelAddr);
    protected native long cNewPartialModel(long factorsAddr, long indicesAddr);
}
/** @} */
