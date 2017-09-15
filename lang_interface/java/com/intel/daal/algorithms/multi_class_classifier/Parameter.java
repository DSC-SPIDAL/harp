/* file: Parameter.java */
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
 * @ingroup multi_class_classifier
 * @{
 */
/**
 * @brief Contains classes for computing the results of the multi-class classifier
 */
package com.intel.daal.algorithms.multi_class_classifier;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTI_CLASS_CLASSIFIER__PARAMETER"></a>
 * @brief Parameters of the multi-class classifier algorithm
 */
public class Parameter extends com.intel.daal.algorithms.classifier.Parameter {

    public Parameter(DaalContext context, long cObject) {
        super(context);
        this.cObject = cObject;
    }

    /**
     * Sets maximum number of iterations of the multi-class classifier training algorithm
     * @param nIter Maximum number of iterations of the multi-class classifier training algorithm
     */
    public void setNIter(long nIter) {
        cSetNIter(this.cObject, nIter);
    }

    /**
     * Retrieves maximum number of iterations of the multi-class classifier training algorithm
     * @return Maximum number of iterations of the multi-class classifier training algorithm
     */
    public long getNIter() {
        return cGetNIter(this.cObject);
    }

    /**
     * Sets convergence threshold of the multi-class classifier training algorithm
     * @param eps Convergence threshold of the multi-class classifier training algorithm
     */
    public void setEps(double eps) {
        cSetEps(this.cObject, eps);
    }

    /**
     * Retrieves convergence threshold of the multi-class classifier training algorithm
     * @return Convergence threshold of the multi-class classifier training algorithm
     */
    public double getEps() {
        return cGetEps(this.cObject);
    }

    /**
     * Sets algorithm for two class classifier model training
     * @param training Algorithm for two class classifier model training
     */
    public void setTraining(com.intel.daal.algorithms.classifier.training.TrainingBatch training) {
        cSetTraining(this.cObject, training.cObject);
    }

    /**
     * Sets algorithm for prediction based on two class classifier model
     * @param prediction Algorithm for prediction based on two class classifier model
     */
    public void setPrediction(com.intel.daal.algorithms.classifier.prediction.PredictionBatch prediction) {
        cSetPrediction(this.cObject, prediction.cObject);
    }

    private native void cSetNIter(long parAddr, long nIter);

    private native long cGetNIter(long parAddr);

    private native void cSetEps(long parAddr, double eps);

    private native double cGetEps(long parAddr);

    private native void cSetTraining(long parAddr, long trainingAddr);

    private native void cSetPrediction(long parAddr, long predictionAddr);

}
/** @} */
