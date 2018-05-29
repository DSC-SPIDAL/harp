/* file: DistributedStep2MasterPartialResult.java */
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
 * @ingroup qr_distributed
 * @{
 */
package com.intel.daal.algorithms.qr;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.ComputeStep;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__QR__DISTRIBUTEDSTEP2MASTERPARTIALRESULT"></a>
 * @brief Provides methods to access partial results obtained with the compute() method of  the QR decomposition algorithm on the second
 * step in the distributed processing mode
 */
public final class DistributedStep2MasterPartialResult extends com.intel.daal.algorithms.PartialResult {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public DistributedStep2MasterPartialResult(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Returns partial result of the QR decomposition algorithm.
     * @param id    Identifier of partial result
     * @return      Partial result that corresponds to the given identifier
     */
    public KeyValueDataCollection get(DistributedPartialResultCollectionId id) {
        if (id == DistributedPartialResultCollectionId.outputOfStep2ForStep3) {
            return new KeyValueDataCollection(getContext(), cGetKeyValueDataCollection(getCObject(), id.getValue()));
        } else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    /**
     * Returns the result of the QR decomposition algorithm with the matrix R calculated
     * @param id    Identifier of the result
     * @return      Result that corresponds to the given identifier
     */
    public Result get(DistributedPartialResultId id) {
        if (id == DistributedPartialResultId.finalResultFromStep2Master) {
            return new Result(getContext(), cGetResult(getCObject(), id.getValue()));
        } else {
            throw new IllegalArgumentException("id unsupported");
        }
    }

    private native long cGetKeyValueDataCollection(long presAddr, int id);

    private native long cGetResult(long presAddr, int id);
}
/** @} */
