/* file: assoc_rules_apriori_batch.cpp */
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

/*
!  Content:
!    C++ example of association rules mining
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-APRIORI_BATCH"></a>
 * \example assoc_rules_apriori_batch.cpp
 */

#include "daal.h"
#include "service.h"
using namespace std;
using namespace daal;
using namespace daal::algorithms;

/* Input data set parameters */
string datasetFileName = "../data/batch/apriori.csv";

/* Apriori algorithm parameters */
const double minSupport     = 0.001;    /* Minimum support */
const double minConfidence  = 0.7;      /* Minimum confidence */

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &datasetFileName);

    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(datasetFileName, DataSource::doAllocateNumericTable,
                                                 DataSource::doDictionaryFromContext);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock();

    /* Create an algorithm to mine association rules using the Apriori method */
    association_rules::Batch<> algorithm;

    /* Set the input object for the algorithm */
    algorithm.input.set(association_rules::data, dataSource.getNumericTable());

    /* Set the Apriori algorithm parameters */
    algorithm.parameter.minSupport = minSupport;
    algorithm.parameter.minConfidence = minConfidence;

    /* Find large item sets and construct association rules */
    algorithm.compute();

    /* Get computed results of the Apriori algorithm */
    association_rules::ResultPtr res = algorithm.getResult();

    /* Print the large item sets */
    printAprioriItemsets(res->get(association_rules::largeItemsets),
                         res->get(association_rules::largeItemsetsSupport));

    /* Print the association rules */
    printAprioriRules(res->get(association_rules::antecedentItemsets),
                      res->get(association_rules::consequentItemsets),
                      res->get(association_rules::confidence));

    return 0;
}
