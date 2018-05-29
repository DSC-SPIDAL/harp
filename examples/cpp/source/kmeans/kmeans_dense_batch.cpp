/* file: kmeans_dense_batch.cpp */
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
!    C++ example of dense K-Means clustering in the batch processing mode
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-KMEANS_DENSE_BATCH"></a>
 * \example kmeans_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;

/* Input data set parameters */
string datasetFileName     = "../data/batch/kmeans_dense.csv";

/* K-Means algorithm parameters */
const size_t nClusters   = 20;
const size_t nIterations = 5;

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &datasetFileName);

    /* Initialize FileDataSource to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(datasetFileName, DataSource::doAllocateNumericTable,
                                                 DataSource::doDictionaryFromContext);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock();

    /* Get initial clusters for the K-Means algorithm */
    kmeans::init::Batch<float, kmeans::init::randomDense> init(nClusters);

    init.input.set(kmeans::init::data, dataSource.getNumericTable());
    init.compute();

    NumericTablePtr centroids = init.getResult()->get(kmeans::init::centroids);

    /* Create an algorithm object for the K-Means algorithm */
    kmeans::Batch<> algorithm(nClusters, nIterations);

    algorithm.input.set(kmeans::data,           dataSource.getNumericTable());
    algorithm.input.set(kmeans::inputCentroids, centroids);

    algorithm.compute();

    /* Print the clusterization results */
    printNumericTable(algorithm.getResult()->get(kmeans::assignments), "First 10 cluster assignments:", 10);
    printNumericTable(algorithm.getResult()->get(kmeans::centroids  ), "First 10 dimensions of centroids:", 20, 10);
    printNumericTable(algorithm.getResult()->get(kmeans::objectiveFunction), "Objective function value:");

    return 0;
}
