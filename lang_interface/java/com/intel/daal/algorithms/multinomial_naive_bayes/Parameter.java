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
 * @ingroup multinomial_naive_bayes
 * @{
 */
/**
 * @brief Contains classes for computing the Naive Bayes
 */
package com.intel.daal.algorithms.multinomial_naive_bayes;

import com.intel.daal.data_management.data.Factory;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MULTINOMIAL_NAIVE_BAYES__PARAMETER"></a>
 * @brief Parameters for multinomial naive Bayes algorithm
 */
public class Parameter extends com.intel.daal.algorithms.classifier.Parameter {

    public Parameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     *  Sets prior class estimates, numeric table of size [nClasses x 1]
     *  @param priorClassEstimates  Prior class estimates
     */
    public void setPriorClassEstimates(NumericTable priorClassEstimates) {
        cSetPriorClassEstimates(this.cObject, priorClassEstimates.getCObject());
    }

    /**
     *  Returnss prior class estimates, numeric table of size [nClasses x 1]
     *  @return  Prior class estimates
     */
    public NumericTable getPriorClassEstimates() {
        NumericTable nt = (NumericTable)Factory.instance().createObject(getContext(), cGetPriorClassEstimates(this.cObject));
        return nt;
    }

    /**
     *  Sets imagined occurrences of the each feature, numeric table of size [1 x nFeatures]
     *  @param alpha  Imagined occurrences of the each feature
     */
    public void setAlpha(NumericTable alpha) {
        cSetAlpha(this.cObject, alpha.getCObject());
    }

    /**
     *  Returnss imagined occurrences of the each feature, numeric table of size [1 x nFeatures]
     *  @return  Imagined occurrences of the each feature
     */
    public NumericTable getAlpha() {
        NumericTable nt = (NumericTable)Factory.instance().createObject(getContext(), cGetAlpha(this.cObject));
        return nt;
    }

    private native void cSetPriorClassEstimates(long parAddr, long ntAddr);

    private native long cGetPriorClassEstimates(long parAddr);

    private native void cSetAlpha(long parAddr, long ntAddr);

    private native long cGetAlpha(long parAddr);
}
/** @} */
