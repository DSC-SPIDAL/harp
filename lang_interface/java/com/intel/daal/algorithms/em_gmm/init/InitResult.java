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
 * @ingroup em_gmm_init
 * @{
 */
package com.intel.daal.algorithms.em_gmm.init;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.DataCollection;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__EM_GMM__INIT__INITRESULT"></a>
 * @brief Provides methods to access final results obtained with the compute() method of InitBatch
 *        for the computation of initial values for the EM for GMM algorithm
 */
public final class InitResult extends com.intel.daal.algorithms.Result {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the result of the initial values computation for the EM for GMM algorithm
     * @param context   Context to manage the result of the initial values computation for the EM for GMM algorithm
     */
    public InitResult(DaalContext context) {
        super(context);
        this.cObject = cNewResult();
    }

    public InitResult(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Returns the initial values for the EM for GMM algorithm
     * @param id   %Result identifier
     * @return         %Result that corresponds to the given identifier
     */
    public NumericTable get(InitResultId id) {
        if (id != InitResultId.weights && id != InitResultId.means) {
            throw new IllegalArgumentException("id unsupported");
        }
        return (NumericTable)Factory.instance().createObject(getContext(), cGetResultTable(cObject, id.getValue()));
    }

    /**
    * Returns the collection of initialized covariances for the EM for GMM algorithm
    * @param id   %Result identifier
    * @return         %Result that corresponds to the given identifier
    */
    public DataCollection get(InitResultCovariancesId id) {
        if (id != InitResultCovariancesId.covariances) {
            throw new IllegalArgumentException("id unsupported");
        }
        return new DataCollection(getContext(), cGetCovariancesDataCollection(cObject, id.getValue()));
    }

    /**
     * Returns the covariance with a given index from the collection of initialized covariances
     * @param id    Identifier of the collection of covariances
     * @param index Index of the covariance to be returned
     * @return          Pointer to the covariance table
     */
    public NumericTable get(InitResultCovariancesId id, int index) {
        if (id != InitResultCovariancesId.covariances) {
            throw new IllegalArgumentException("index arguments for this id unsupported");
        }
        return (NumericTable)Factory.instance().createObject(getContext(), cGetResultCovarianceTable(cObject, id.getValue(), index));
    }

    /**
     * Sets initial values for the EM for GMM algorithm
     * @param id    %Result identifier
     * @param value Numeric table for the result
     */
    public void set(InitResultId id, NumericTable value) {
        int idValue = id.getValue();
        if (id != InitResultId.weights && id != InitResultId.means) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetResultTable(cObject, idValue, value.getCObject());
    }

    /**
     * Sets the collection of covariances for initialization of the EM for GMM algorithm
     * @param id    Identifier of the collection of covariances
     * @param value Collection of covariances
     */
    public void set(InitResultCovariancesId id, DataCollection value) {
        int idValue = id.getValue();
        if (id != InitResultCovariancesId.covariances) {
            throw new IllegalArgumentException("id unsupported");
        }
        cSetCovarianceCollection(cObject, idValue, value.getCObject());
    }

    private native long cNewResult();

    private native long cGetResultTable(long cResult, int id);

    private native long cGetCovariancesDataCollection(long cResult, int id);

    private native long cGetResultCovarianceTable(long cResult, int id, int index);

    private native void cSetResultTable(long cResult, int id, long cNumericTable);

    private native void cSetCovarianceCollection(long cResult, int id, long cDataCollection);
}
/** @} */
