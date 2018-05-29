/* file: QualityMetricSetParameter.java */
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
 * @ingroup pca_quality_metric_set
 * @{
 */
package com.intel.daal.algorithms.pca.quality_metric_set;

import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__PCA__QUALITY_METRIC_SET__QUALITYMETRICSETPARAMETER"></a>
 * @brief Parameters for the quality metrics set computation for PCA algorithm
 */
public class QualityMetricSetParameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public QualityMetricSetParameter(DaalContext context, long cParameter, long nComponents, long nFeatures) {
        super(context, cParameter);
        cSetNComponents(this.cObject, nComponents);
        cSetNFeatures(this.cObject, nFeatures);
    }

    /**
     * Sets the number of principal components to compute metrics for
     * @param nComponents Number of principal components to compute metrics for
     */
    public void setNComponents(long nComponents) {
        cSetNComponents(cObject, nComponents);
    }

    /**
     * Gets the number of principal components to compute metrics for
     * @return Number of principal components to compute metrics for
     */
    public long getNComponents(long nComponents) {
        return cGetNComponents(cObject);
    }

    /**
     * Sets the number of features in dataset used as input in PCA
     * @param nFeatures Number of features in dataset used as input in PCA
     */
    public void setNFeatures(long nFeatures) {
        cSetNFeatures(cObject, nFeatures);
    }

    /**
     * Gets the number of features in dataset used as input in PCA
     * @return Number of features in dataset used as input in PCA
     */
    public long getNFeatures(long nFeatures) {
        return cGetNFeatures(cObject);
    }

    private native void cSetNComponents(long cObject, long nComponents);
    private native long cGetNComponents(long cObject);

    private native void cSetNFeatures(long cObject, long nFeatures);
    private native long cGetNFeatures(long cObject);
}
/** @} */
