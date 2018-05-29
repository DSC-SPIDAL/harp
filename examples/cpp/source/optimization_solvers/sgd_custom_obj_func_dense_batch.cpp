/* file: sgd_custom_obj_func_dense_batch.cpp */
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
!    C++ example of the Stochastic gradient descent algorithm with custom
!    objective function
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-SGD_CUSTOM_OBJ_FUNC_DENSE_BATCH"></a>
 * \example sgd_custom_obj_func_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

/* Custom objective function declaration */
#include "custom_obj_func.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::data_management;

string datasetFileName = "../data/batch/custom.csv";

const size_t nIterations = 1000;
const size_t nFeatures = 4;
const float  learningRate = 0.01f;
const double accuracyThreshold = 0.02;

float initialPoint[nFeatures + 1] = {1, 1, 1, 1, 1};

int main(int argc, char *argv[])
{
    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(datasetFileName,
            DataSource::notAllocateNumericTable,
            DataSource::doDictionaryFromContext);

    /* Create Numeric Tables for data and values for dependent variable */
    daal::services::Status s;
    NumericTablePtr data = HomogenNumericTable<>::create(nFeatures, 0, NumericTable::doNotAllocate, &s);
    checkStatus(s);
    NumericTablePtr dependentVariables = HomogenNumericTable<>::create(1, 0, NumericTable::doNotAllocate, &s);
    checkStatus(s);
    NumericTablePtr mergedData = MergedNumericTable::create(data, dependentVariables, &s);
    checkStatus(s);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock(mergedData.get());

    size_t nVectors = data->getNumberOfRows();

    services::SharedPtr<new_objective_function::Batch<> > customObjectiveFunction(new new_objective_function::Batch<>(nVectors));
    customObjectiveFunction->input.set(new_objective_function::data, data);
    customObjectiveFunction->input.set(new_objective_function::dependentVariables, dependentVariables);

    /* Create objects to compute the Stochastic gradient descent result using the default method */
    optimization_solver::sgd::Batch<> sgdAlgorithm(customObjectiveFunction);

    /* Set input objects for the the Stochastic gradient descent algorithm */
    sgdAlgorithm.input.set(optimization_solver::iterative_solver::inputArgument,
                           HomogenNumericTable<>::create(initialPoint, 1, nFeatures + 1, &s));
    checkStatus(s);
    sgdAlgorithm.parameter.learningRateSequence =
        HomogenNumericTable<>::create(1, 1, NumericTable::doAllocate, learningRate, &s);
    checkStatus(s);
    sgdAlgorithm.parameter.nIterations = nIterations;
    sgdAlgorithm.parameter.accuracyThreshold = accuracyThreshold;

    /* Compute the Stochastic gradient descent result */
    s = sgdAlgorithm.compute();
    checkStatus(s);

    /* Print computed the Stochastic gradient descent result */
    printNumericTable(sgdAlgorithm.getResult()->get(optimization_solver::iterative_solver::minimum), "Minimum:");
    printNumericTable(sgdAlgorithm.getResult()->get(optimization_solver::iterative_solver::nIterations), "Number of iterations performed:");

    return 0;
}
