/* file: Model.java */
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
 * @defgroup multi_class_classifier Multi-class Classifier
 * @brief Contains classes for computing the results of the multi-class classifier algorithm
 * @ingroup classification
 * @{
 */
package com.intel.daal.algorithms.multi_class_classifier;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTI_CLASS_CLASSIFIER__MODEL"></a>
 * @brief Model of the classifier trained by the multi_class_classifier.training.TrainingBatch algorithm.
 */
public class Model extends com.intel.daal.algorithms.classifier.Model {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public Model(DaalContext context, long cModel) {
        super(context, cModel);
    }

    /**
     * Returns a two-class classifier model in a multi-class classifier model
     * @param idx   Index of two-class classifier model in a collection
     * @return      Two-class classifier model
     */
    public com.intel.daal.algorithms.classifier.Model getTwoClassClassifierModel(long idx) {
        return (com.intel.daal.algorithms.classifier.Model)Factory.instance().createObject(getContext(), cGetTwoClassClassifierModel(this.getCObject(), idx));
    }

    /**
     * Returns a number of two-class classifiers associated with the model
     * @return Number of two-class classifiers associated with the model
     */
    public long getNumberOfTwoClassClassifierModels() {
        return cGetNumberOfTwoClassClassifierModels(this.getCObject());
    }

    private native long cGetNumberOfTwoClassClassifierModels(long modelAddr);
    private native long cGetTwoClassClassifierModel(long modelAddr, long idx);
}
/** @} */
