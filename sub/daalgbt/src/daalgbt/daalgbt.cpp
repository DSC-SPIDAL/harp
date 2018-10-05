/* file: gbt_cls_dense_batch.cpp */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation.
*
* This software and the related documents are Intel copyrighted  materials,  and
* your use of  them is  governed by the  express license  under which  they were
* provided to you (License).  Unless the License provides otherwise, you may not
* use, modify, copy, publish, distribute,  disclose or transmit this software or
* the related documents without Intel's prior written permission.
*
* This software and the related documents  are provided as  is,  with no express
* or implied  warranties,  other  than those  that are  expressly stated  in the
* License.
*******************************************************************************/

/*
!  Content:
!    C++ example of gradient boosted trees classification in the batch processing mode.
!
!    The program trains the gradient boosted trees classification model on a training
!    datasetFileName and computes classification for the test data.
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-GBT_CLS_DENSE_BATCH"></a>
 * \example gbt_cls_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"
#include <chrono>
#include <vector>

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::gbt::classification;

//
// As Daal Library does not suppoert log for timing and option parser,
// here make it hard coded. Call from python can make it easier to use.
//

///* Input data set parameters */
//const string trainDatasetFileName = "./train.csv";
//const string testDatasetFileName  = "./test.csv";
//const size_t categoricalFeaturesIndices[] = { };
//const size_t nFeatures  = 50;  /* Number of features in training and testing data sets */
//
///* Gradient boosted trees training parameters */
//const size_t maxIterations = 500;
//const size_t minObservationsInLeafNode = 1;
//
//const size_t nClasses = 2;  /* Number of classes */

string trainDatasetFileName = "./train.csv";
string testDatasetFileName  = "./test.csv";
vector<size_t> categoricalFeaturesIndices;
size_t nFeatures  = 50;  /* Number of features in training and testing data sets */

/* Gradient boosted trees training parameters */
size_t maxIterations = 500;
size_t minObservationsInLeafNode = 1;

size_t nClasses = 2;  /* Number of classes */


training::ResultPtr trainModel();
void testModel(const training::ResultPtr& res);
void loadData(const std::string& fileName, NumericTablePtr& pData, NumericTablePtr& pDependentVar);

int main(int argc, char *argv[])
{
    //checkArguments(argc, argv, 2, &trainDatasetFileName, &testDatasetFileName);
    //Usage: daalgbt <trainfile> <testfile> <epoch> <nClasses> <nFeatures>
    //

    if (argc < 6){
        cout << "Usage: daalgbt <trainfile> <testfile> <epoch> <nClasses> <nFeatures> <categoricalFeatures>\n";
        return -1;
    }

    // arguments
    trainDatasetFileName = string(argv[1]);
    testDatasetFileName = string(argv[2]);
    maxIterations = stoi(argv[3]);
    nClasses = stoi(argv[4]);
    nFeatures = stoi(argv[5]);

    if (argc == 7){
        //get cate features
        string s = argv[6];
        size_t pos = 0;
        std::string token;
        cout << "categoricalFeatures:" ;
        while ((pos = s.find(",")) != std::string::npos) {
            token = s.substr(0, pos);
            std::cout << token << " ";
            categoricalFeaturesIndices.push_back(stoi(token));
            s.erase(0, pos + 1);
        }
        std::cout << std::endl;
    }


    // run it

    auto start = std::chrono::system_clock::now();
    training::ResultPtr trainingResult = trainModel();
    auto end = std::chrono::system_clock::now();
    std::chrono::duration<double> diff = end-start;
    std::cout << "Training time : " << diff.count() << " s\n";

    start = std::chrono::system_clock::now();
    testModel(trainingResult);
    end = std::chrono::system_clock::now();
    diff = end-start;
    std::cout << "Testing time : " << diff.count() << " s\n";

    return 0;
}

training::ResultPtr trainModel()
{
    /* Create Numeric Tables for training data and dependent variables */
    NumericTablePtr trainData;
    NumericTablePtr trainDependentVariable;

    loadData(trainDatasetFileName, trainData, trainDependentVariable);

    /* Create an algorithm object to train the gradient boosted trees classification model */
    training::Batch<> algorithm(nClasses);

    /* Pass a training data set and dependent values to the algorithm */
    algorithm.input.set(classifier::training::data, trainData);
    algorithm.input.set(classifier::training::labels, trainDependentVariable);

    algorithm.parameter().maxIterations = maxIterations;
    algorithm.parameter().featuresPerNode = nFeatures;
    algorithm.parameter().minObservationsInLeafNode = minObservationsInLeafNode;

    /* Build the gradient boosted trees classification model */
    algorithm.compute();

    /* Retrieve the algorithm results */
    training::ResultPtr trainingResult = algorithm.getResult();
    return trainingResult;
}

void testModel(const training::ResultPtr& trainingResult)
{
    /* Create Numeric Tables for testing data and ground truth values */
    NumericTablePtr testData;
    NumericTablePtr testGroundTruth;

    loadData(testDatasetFileName, testData, testGroundTruth);

    //printNumericTable(testData, "testData (first 2 rows):", 2);
    //printNumericTable(testGroundTruth, "Ground truth (first 2 rows):", 2);

    /* Create an algorithm object to predict values of gradient boosted trees classification */
    prediction::Batch<> algorithm(nClasses);

    /* Pass a testing data set and the trained model to the algorithm */
    algorithm.input.set(classifier::prediction::data, testData);
    algorithm.input.set(classifier::prediction::model, trainingResult->get(classifier::training::model));

    /* Predict values of gradient boosted trees classification */
    algorithm.compute();

    /* Retrieve the algorithm results */
    classifier::prediction::ResultPtr predictionResult = algorithm.getResult();
    //printNumericTable(predictionResult->get(classifier::prediction::prediction),
    //    "Gragient boosted trees prediction results (first 10 rows):", 10);
    //printNumericTable(testGroundTruth, "Ground truth (first 10 rows):", 10);
}

void loadData(const std::string& fileName, NumericTablePtr& pData, NumericTablePtr& pDependentVar)
{
    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> trainDataSource(fileName,
        DataSource::notAllocateNumericTable,
        DataSource::doDictionaryFromContext);

    /* Create Numeric Tables for training data and dependent variables */
    pData.reset(new HomogenNumericTable<>(nFeatures, 0, NumericTable::notAllocate));
    pDependentVar.reset(new HomogenNumericTable<>(1, 0, NumericTable::notAllocate));
    NumericTablePtr mergedData(new MergedNumericTable(pData, pDependentVar));

    /* Retrieve the data from input file */
    trainDataSource.loadDataBlock(mergedData.get());

    NumericTableDictionaryPtr pDictionary = pData->getDictionarySharedPtr();
    //for(size_t i = 0, n = sizeof(categoricalFeaturesIndices) / sizeof(categoricalFeaturesIndices[0]); i < n; ++i)
    std::vector<size_t>::const_iterator i;
    for(i = categoricalFeaturesIndices.begin(); i != categoricalFeaturesIndices.end(); ++i)
        (*pDictionary)[(*i)].featureType = data_feature_utils::DAAL_CATEGORICAL;
}
