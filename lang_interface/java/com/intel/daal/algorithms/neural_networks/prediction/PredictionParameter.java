/* file: PredictionParameter.java */
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
 * @ingroup neural_networks_prediction
 * @{
 */
package com.intel.daal.algorithms.neural_networks.prediction;

import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.Precision;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__NEURAL_NETWORKS__PREDICTION__PREDICTIONPARAMETER"></a>
 * \brief Class representing the parameters of neural network
 */
public class PredictionParameter extends com.intel.daal.algorithms.Parameter {
    /**
     * Constructs the parameters of neural network algorithm
     * @param context   Context to manage the parameter object
     */
    public PredictionParameter(DaalContext context) {
        super(context);
        cObject = cInit();
    }

    public PredictionParameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     *  Gets the size of the batch to be processed by the neural network
     */
    public long getBatchSize() {
        return cGetBatchSize(cObject);
    }

    /**
     *  Sets the size of the batch to be processed by the neural network
     *  @param batchSize Size of the batch to be processed by the neural network
     */
    public void setBatchSize(long batchSize) {
        cSetBatchSize(cObject, batchSize);
    }

    private native long cInit();
    private native long cGetBatchSize(long cParameter);
    private native void cSetBatchSize(long cParameter, long batchSize);
}
/** @} */
