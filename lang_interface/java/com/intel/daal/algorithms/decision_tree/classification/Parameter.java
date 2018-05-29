/* file: Parameter.java */
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
 * @ingroup decision_tree_classification
 */
/**
 * @brief Contains classes of the decision tree classification algorithm
 */
package com.intel.daal.algorithms.decision_tree.classification;

import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.decision_tree.PruningId;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__DECISION_TREE__CLASSIFICATION__TRAINING__PARAMETER"></a>
 * @brief Base class for parameters of the decision tree classification algorithm
 */
public class Parameter extends com.intel.daal.algorithms.classifier.Parameter {

    public Parameter(DaalContext context, long cParameter) {
        super(context, cParameter);
    }

    /**
     * Returns the pruning method for decision tree training algorithm
     * @return Pruning method for decision tree
     */
    public PruningId getPruning() {
        return new PruningId(cGetPruning(this.cObject));
    }

    /**
     * Sets the pruning method for decision tree training algorithm
     * @param value   Pruning method for decision tree
     */
    public void setPruning(PruningId value) {
        cSetPruning(this.cObject, value.getValue());
    }

    /**
     * Returns the maximum tree depth. 0 means unlimited depth.
     * @return Maximum tree depth
     */
    public long getMaxTreeDepth() {
        return cGetMaxTreeDepth(this.cObject);
    }

    /**
     * Sets the maximum tree depth, 0 means unlimited depth
     * @param value   Maximum tree depth
     */
    public void setMaxTreeDepth(long value) {
        cSetMaxTreeDepth(this.cObject, value);
    }

    /**
     * Returns the minimum number of observations in the leaf node
     * @return Minimum number of observations in the leaf node
     */
    public long getMinObservationsInLeafNodes() {
        return cGetMinObservationsInLeafNodes(this.cObject);
    }

    /**
     * Sets the minimum number of observations in the leaf node
     * @param value   Minimum number of observations in the leaf node
     */
    public void setMinObservationsInLeafNodes(long value) {
        cSetMinObservationsInLeafNodes(this.cObject, value);
    }

    /**
     * Returns the split criterion for decision tree classification
     * @return Split criterion for decision tree classification
     */
    public SplitCriterionId getSplitCriterion() {
        return new SplitCriterionId(cGetSplitCriterion(this.cObject));
    }

    /**
     * Sets the split criterion for decision tree classification
     * @param value   Split criterion for decision tree classification
     */
    public void setSplitCriterion(SplitCriterionId value) {
        cSetSplitCriterion(this.cObject, value.getValue());
    }

    private native int  cGetPruning(long parAddr);
    private native void cSetPruning(long parAddr, int value);

    private native long cGetMaxTreeDepth(long parAddr);
    private native void cSetMaxTreeDepth(long parAddr, long value);

    private native long cGetMinObservationsInLeafNodes(long parAddr);
    private native void cSetMinObservationsInLeafNodes(long parAddr, long value);

    private native int  cGetSplitCriterion(long parAddr);
    private native void cSetSplitCriterion(long parAddr, int value);

}
/** @} */
