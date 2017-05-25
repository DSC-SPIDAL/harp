/* file: kmeans_dense_batch.cpp */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
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

#include <ctime> 
#include <time.h>
#include <stdlib.h> 

#include "data_management/data/numeric_table.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;

/* Input data set parameters */
// string datasetFileName     = "../data/batch/kmeans_dense.csv";
// use absolute path
// string datasetFileName     = "/home/langshichen/Lib/__release_lnx/daal/examples/data/batch/kmeans_dense.csv";

/* K-Means algorithm parameters */
size_t nClusters   = 200;
size_t nIterations = 10;

size_t input_row = 10000;
size_t input_col = 50000;

int main(int argc, char *argv[])
{

	clock_t start_compute;
	clock_t stop_compute;
	double compute_time;

	if (argc > 1)
		nClusters = atoi(argv[1]);

	if (argc > 2)
		nIterations = atoi(argv[2]);

	if (argc > 3)
		input_row = atoi(argv[3]);

	if (argc > 4)
		input_col = atoi(argv[4]);
	
    // checkArguments(argc, argv, 1, &datasetFileName);

    /* Initialize FileDataSource to retrieve the input data from a .csv file */
    // FileDataSource<CSVFeatureManager> dataSource(datasetFileName, DataSource::doAllocateNumericTable,
                                                 // DataSource::doDictionaryFromContext);

    /* Retrieve the data from the input file */
    // dataSource.loadDataBlock();
	// int input_row = dataSource.getNumericTable()->getNumberOfRows();
	// int input_col = dataSource.getNumericTable()->getNumberOfColumns();

	printf("input matrix row: %d, col: %d\n", input_row, input_col);
	printf("nCluster: %d, nIteration: %d\n", nClusters, nIterations);

	// create an input matrix 
	services::SharedPtr<HomogenNumericTable<double> > input_mat(new HomogenNumericTable<double>(input_col, input_row, NumericTable::doAllocate, 0.5));
    /* Get initial clusters for the K-Means algorithm */

    kmeans::init::Batch<double,kmeans::init::randomDense> init(nClusters);
	
    // init.input.set(kmeans::init::data, dataSource.getNumericTable());
    init.input.set(kmeans::init::data, input_mat);

	// start_compute = clock();

    init.compute();

	// stop_compute = clock();

	// compute_time += (double)(stop_compute - start_compute)*1000.0/CLOCKS_PER_SEC;

	
    NumericTablePtr centroids = init.getResult()->get(kmeans::init::centroids);

    /* Create an algorithm object for the K-Means algorithm */
	// default method lloydDense datatype double
    kmeans::Batch<> algorithm(nClusters, nIterations);

    // algorithm.input.set(kmeans::data,           dataSource.getNumericTable());
    algorithm.input.set(kmeans::data, input_mat);
    algorithm.input.set(kmeans::inputCentroids, centroids);

	// start_compute = clock();
	struct timespec ts1;
	struct timespec ts2;

	clock_gettime(CLOCK_MONOTONIC, &ts1);

    algorithm.compute();

	clock_gettime(CLOCK_MONOTONIC, &ts2);
	// stop_compute = clock();

	// compute_time += (double)(stop_compute - start_compute)*1000.0/CLOCKS_PER_SEC;

	long diff = 1000000000L *(ts2.tv_sec - ts1.tv_sec) + ts2.tv_nsec - ts1.tv_nsec;
	// double compute_time = (double)(stop_compute - start_compute)*1000.0/CLOCKS_PER_SEC;
	compute_time = (double)(diff)/1000000L;

    /* Print the clusterization results */
    printNumericTable(algorithm.getResult()->get(kmeans::assignments), "First 10 cluster assignments:", 10);
    printNumericTable(algorithm.getResult()->get(kmeans::centroids  ), "First 10 dimensions of centroids:", 20, 10);
    printNumericTable(algorithm.getResult()->get(kmeans::goalFunction), "Goal function value:");

	printf("Computation Time elapsed in ms: %f\n", compute_time);

    return 0;
}
