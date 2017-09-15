/* file: AssocRulesAprioriBatch.java */
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

/*
 //  Content:
 //     Java example of association rules mining
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-APRIORIBATCH">
 * @example AssocRulesAprioriBatch.java
 */

package com.intel.daal.examples.association_rules;

import com.intel.daal.algorithms.association_rules.*;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

class AssocRulesAprioriBatch {
    /* Input data set parameters */
    private static final String dataset  = "../data/batch/apriori.csv";

    /* Apriori algorithm parameters */
    private static final double minSupport    = 0.001; /* Minimum support */
    private static final double minConfidence = 0.7;   /* Minimum confidence */

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {
        /* Retrieve the input data */
        FileDataSource dataSource = new FileDataSource(context, dataset,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        dataSource.loadDataBlock();

        /* Create an algorithm to mine association rules using the Apriori method */
        Batch alg = new Batch(context, Float.class, Method.apriori);

        /* Set an input object for the algorithm */
        NumericTable input = dataSource.getNumericTable();
        alg.input.set(InputId.data, input);

        /* Set Apriori algorithm parameters */
        alg.parameter.setMinSupport(minSupport);
        alg.parameter.setMinConfidence(minConfidence);

        /* Find large item sets and construct association rules */
        Result res = alg.compute();

        HomogenNumericTable largeItemsets = (HomogenNumericTable) res.get(ResultId.largeItemsets);
        HomogenNumericTable largeItemsetsSupport = (HomogenNumericTable) res.get(ResultId.largeItemsetsSupport);

        /* Print the large item sets */
        Service.printAprioriItemsets(largeItemsets, largeItemsetsSupport);

        HomogenNumericTable antecedentItemsets = (HomogenNumericTable) res.get(ResultId.antecedentItemsets);
        HomogenNumericTable consequentItemsets = (HomogenNumericTable) res.get(ResultId.consequentItemsets);
        HomogenNumericTable confidence = (HomogenNumericTable) res.get(ResultId.confidence);

        /* Print the association rules */
        Service.printAprioriRules(antecedentItemsets, consequentItemsets, confidence);

        context.dispose();
    }
}
