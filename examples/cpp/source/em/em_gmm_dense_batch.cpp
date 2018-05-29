/* file: em_gmm_dense_batch.cpp */
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
!    C++ example of the expectation-maximization (EM) algorithm for the
!    Gaussian mixture model (GMM)
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-EM_GMM_BATCH"></a>
 * \example em_gmm_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::data_management;

typedef float  dataFPType;          /* Data floating-point type */

/* Input data set parameters */
const std::string datasetFileName = "../data/batch/em_gmm.csv" ;
const size_t nComponents   = 2;
size_t nFeatures;

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &datasetFileName);

    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(datasetFileName, DataSource::doAllocateNumericTable,
            DataSource::doDictionaryFromContext);
    nFeatures = dataSource.getNumberOfColumns();

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock();

    /* Create algorithm objects to initialize the EM algorithm for the GMM
     * computing the number of components using the default method */
    em_gmm::init::Batch<> initAlgorihm(nComponents);

   /* Set an input data table for the initialization algorithm */
    initAlgorihm.input.set(em_gmm::init::data, dataSource.getNumericTable());

    /* Compute initial values for the EM algorithm for the GMM with the default parameters */
    initAlgorihm.compute();

    em_gmm::init::ResultPtr resultInit = initAlgorihm.getResult();

    /* Create algorithm objects for the EM algorithm for the GMM computing the number of components using the default method */
    em_gmm::Batch<> algorithm(nComponents);

    /* Set an input data table for the algorithm */
    algorithm.input.set(em_gmm::data, dataSource.getNumericTable());
    algorithm.input.set(em_gmm::inputValues, initAlgorihm.getResult());

    /* Compute the results of the EM algorithm for the GMM with the default parameters */
    algorithm.compute();

    em_gmm::ResultPtr result = algorithm.getResult();

    /* Print the results */
    printNumericTable(result->get(em_gmm::weights), "Weights");
    printNumericTable(result->get(em_gmm::means), "Means");
    for(size_t i = 0; i < nComponents; i++)
    {
        printNumericTable(result->get(em_gmm::covariances, i), "Covariance");
    }

    return 0;
}
