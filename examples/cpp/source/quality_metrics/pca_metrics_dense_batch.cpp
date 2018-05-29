/* file: pca_metrics_dense_batch.cpp */
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
!    C++ example of pca quality metrics in batch processing mode.
!
!    The program computes PCA and quality
!    metrics for the PCA.
!******************************************************************************/

/**
* <a name="DAAL-EXAMPLE-CPP-PCA_QUALITY_METRIC_SET_BATCH"></a>
* \example pca_metrics_dense_batch.cpp
*/
#include "daal.h"
#include "service.h"
#include <iostream>
using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::pca::quality_metric;
using namespace daal::algorithms::pca::quality_metric_set;

/* Input data set parameters */
const string dataFileName = "../data/batch/pca_normalized.csv";
const size_t nVectors = 1000;
const size_t nComponents = 5;

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &dataFileName);

    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(dataFileName, DataSource::doAllocateNumericTable,
        DataSource::doDictionaryFromContext);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock(nVectors);

    /* Create an algorithm for principal component analysis using the SVD method */
    pca::Batch<float, pca::svdDense> algorithm;

    /* Set the algorithm input data */
    algorithm.input.set(pca::data, dataSource.getNumericTable());

    /* Compute results of the PCA algorithm */
    algorithm.compute();

    /* Create a quality metrics algorithm for explained variances, explained variances ratios and noise_variance */
    pca::quality_metric_set::Batch qms(nComponents);

    services::SharedPtr<algorithms::Input> algInput =
        qms.getInputDataCollection()->getInput(explainedVariancesMetrics);

    explained_variance::InputPtr varianceMetrics = explained_variance::Input::cast(algInput);
    varianceMetrics->set(explained_variance::eigenvalues, algorithm.getResult()->get(pca::eigenvalues));

    /* Compute quality metrics of the PCA algorithm */
    qms.compute();

    /* Output quality metrics of the PCA algorithm */
    explained_variance::ResultPtr qmsResult = explained_variance::Result::cast
    (qms.getResultCollection()->getResult(explainedVariancesMetrics));
    printNumericTable(qmsResult->get(explained_variance::explainedVariances),
        "Explained variances:");
    printNumericTable(qmsResult->get(explained_variance::explainedVariancesRatios),
        "Explained variance ratios:");
    printNumericTable(qmsResult->get(explained_variance::noiseVariance),
        "Noise variance:");

    return 0;
}
