/* file: mf_sgd_distri.cpp */
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
   !    C++ example of computing mf_sgd decomposition in the distributed processing mode
   !******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-MF_SGD_DISTRI"></a>
 * \example mf_sgd_distri.cpp
 */
#include <cstdlib> 
#include <ctime> 
#include <time.h>
#include <stdlib.h> 
#include <cstring>
#include <fstream>
#include <iostream>
#include <string>

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;

/* default parameters of SGD training */
const double learningRate = 0.005;
const double lambda = 0.002;
const size_t iteration = 10;		    /* num of iterations in SGD training */
const size_t threads = 0;			    /* specify the num of threads used by TBB, when 0 TBB chooses automatic threads num  */
const size_t tbb_grainsize = 0;			/* 0 by auto partitioner of TBB or user specified tbb grainsize   */
const size_t Avx_explicit = 0;			/* 0 to use compiler generated vectorization codes, 1 to use explicit intel intrinsic codes */

/* dimension of vectors in model W and model H */
const int64_t r_dim = 128;

/* input dataset files in csv format */
const string trainDataFile = "../data/distributed/movielens-train.mm";
const string testDataFile = "../data/distributed/movielens-test.mm";

/* choose the precision */
typedef float sgd_float;
// typedef double sgd_float;

int main(int argc, char *argv[])
{

	/* Create an algorithm to compute mf_sgd decomposition  */
	mf_sgd::Distri<step1Local, sgd_float, mf_sgd::defaultSGD> algorithm;

	/* map to hold imported csv data */
	std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<sgd_float>*>*> map_train;

	const int num_Train = algorithm.input.loadData(trainDataFile, NULL, map_train);

	services::SharedPtr<mf_sgd::VPoint<sgd_float> > points_Train(new mf_sgd::VPoint<sgd_float>[num_Train]);

	int64_t row_num_w = 0;
	int64_t col_num_h = 0;
	algorithm.input.convert_format_distri(map_train,  num_Train, points_Train.get(), row_num_w, col_num_h);

	algorithm.input.freeData(map_train);

	/* Create a new dictionary and fill it with the information about data */
    services::SharedPtr<int> points_Train_wPos(new int[num_Train]);
    services::SharedPtr<int> points_Train_hPos(new int[num_Train]);
    services::SharedPtr<sgd_float> points_Train_val(new sgd_float[num_Train]);

    int* wPos_ptr = points_Train_wPos.get();
    int* hPos_ptr = points_Train_hPos.get();
    sgd_float* val_ptr = points_Train_val.get();

    for(int p=0;p<num_Train;p++)
    {
        wPos_ptr[p] = (points_Train.get())[p].wPos;
        hPos_ptr[p] = (points_Train.get())[p].hPos;
        val_ptr[p] = (points_Train.get())[p].val;
    }

    services::SharedPtr<NumericTable> wPos_table(new HomogenNumericTable<int>(wPos_ptr, 1, num_Train)); 
    services::SharedPtr<NumericTable> hPos_table(new HomogenNumericTable<int>(hPos_ptr, 1, num_Train)); 
    services::SharedPtr<NumericTable> val_table(new HomogenNumericTable<sgd_float>(val_ptr, 1, num_Train)); 

	algorithm.input.set(mf_sgd::wPos, wPos_table);
	algorithm.input.set(mf_sgd::hPos, hPos_table);
	algorithm.input.set(mf_sgd::val, val_table);

    services::SharedPtr<NumericTable> matrixW(new HomogenNumericTable<sgd_float>(r_dim, row_num_w, NumericTable::doAllocate, 0.5));
    services::SharedPtr<NumericTable> matrixH(new HomogenNumericTable<sgd_float>(r_dim, col_num_h, NumericTable::doAllocate, 0.5));

    services::SharedPtr<mf_sgd::DistributedPartialResult> pres(new mf_sgd::DistributedPartialResult());

    pres->set(mf_sgd::presWMat, matrixW);
    pres->set(mf_sgd::presHMat, matrixH);

    algorithm.setPartialResult(pres);

	algorithm.parameter.setParameter(learningRate, lambda, r_dim, row_num_w, col_num_h, iteration, threads, tbb_grainsize, Avx_explicit);

	algorithm.compute();

	/* Print the results first 10 rows and 10 columns */
	printNumericTable(pres->get(mf_sgd::presWMat), "Model W Matrix:", 10, 10, 10);
	printNumericTable(pres->get(mf_sgd::presHMat), "Model H Matrix:", 10, 10, 10);

	return 0;
}








