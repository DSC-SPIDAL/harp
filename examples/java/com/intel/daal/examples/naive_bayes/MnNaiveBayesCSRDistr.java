/* file: MnNaiveBayesCSRDistr.java */
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
 //     Java example of Naive Bayes classification in the distributed processing
 //     mode.
 //
 //     The program trains the Naive Bayes model on a supplied training data set
 //     in compressed sparse rows (CSR) format and then performs classification
 //     of previously unseen data.
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-MULTINOMIALNAIVEBAYESCSRDISTRIBUTED">
 * @example MnNaiveBayesCSRDistr.java
 */

package com.intel.daal.examples.naive_bayes;

import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.multinomial_naive_bayes.Model;
import com.intel.daal.algorithms.multinomial_naive_bayes.prediction.*;
import com.intel.daal.algorithms.multinomial_naive_bayes.training.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

class MnNaiveBayesCSRDistr {

    /* Input data set parameters */
    private static final String[] trainDatasetFileNames     = { "../data/distributed/naivebayes_train_csr_1.csv",
            "../data/distributed/naivebayes_train_csr_2.csv", "../data/distributed/naivebayes_train_csr_3.csv",
            "../data/distributed/naivebayes_train_csr_4.csv" };
    private static final String[] trainGroundTruthFileNames = { "../data/distributed/naivebayes_train_labels_1.csv",
            "../data/distributed/naivebayes_train_labels_2.csv", "../data/distributed/naivebayes_train_labels_3.csv",
            "../data/distributed/naivebayes_train_labels_4.csv" };

    private static final String testDatasetFileName     = "../data/distributed/naivebayes_test_csr.csv";
    private static final String testGroundTruthFileName = "../data/distributed/naivebayes_test_labels.csv";

    private static final int  nBlocks              = 4;
    private static final int  nTrainVectorsInBlock = 8000;
    private static final int  nTestObservations    = 2000;
    private static final long nClasses             = 20;

    /* Parameters for the Naive Bayes algorithm */
    private static TrainingResult   trainingResult;
    private static PredictionResult predictionResult;

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {

        trainModel();

        testModel();

        printResults();

        context.dispose();
    }

    private static void trainModel() throws java.io.FileNotFoundException, java.io.IOException {
        TrainingPartialResult[] pres = new TrainingPartialResult[nBlocks];

        for (int node = 0; node < nBlocks; node++) {
            DaalContext localContext = new DaalContext();

            /* Initialize FileDataSource to retrieve the input data from a .csv file */
            FileDataSource trainGroundTruthSource = new FileDataSource(localContext, trainGroundTruthFileNames[node],
                    DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                    DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);

            /* Retrieve the data from input file */
            trainGroundTruthSource.loadDataBlock(nTrainVectorsInBlock);

            /* Create Numeric Tables for training data and labels */
            CSRNumericTable trainData = Service.createSparseTable(context, trainDatasetFileNames[node]);
            NumericTable labels = trainGroundTruthSource.getNumericTable();

            /* Create algorithm objects to train the Naive Bayes model */
            TrainingDistributedStep1Local algorithm = new TrainingDistributedStep1Local(localContext, Float.class,
                    TrainingMethod.fastCSR, nClasses);

            /* Set the input data */
            algorithm.input.set(InputId.data,   trainData);
            algorithm.input.set(InputId.labels, labels);

            /* Build a partial Naive Bayes model */
            pres[node] = algorithm.compute();

            pres[node].changeContext(context);

            localContext.dispose();
        }

        /* Build the final Naive Bayes model on the master node*/
        TrainingDistributedStep2Master algorithm = new TrainingDistributedStep2Master(context, Float.class,
                TrainingMethod.fastCSR, nClasses);

        /* Set partial Naive Bayes models built on local nodes */
        for (int node = 0; node < nBlocks; node++) {
            algorithm.input.add(TrainingDistributedInputId.partialModels, pres[node]);
        }

        /* Build the final Naive Bayes model */
        algorithm.compute();

        trainingResult = algorithm.finalizeCompute();
    }

    private static void testModel() throws java.io.FileNotFoundException, java.io.IOException {

        FileDataSource testDataSource = new FileDataSource(context, testDatasetFileName,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        testDataSource.loadDataBlock(nTestObservations);

        /* Create algorithm objects to predict Naive Bayes values with the fastCSR method */
        PredictionBatch algorithm = new PredictionBatch(context, Float.class, PredictionMethod.fastCSR, nClasses);

        /* Create Numeric Table for test data */
        CSRNumericTable testData = Service.createSparseTable(context, testDatasetFileName);

        /* Pass a testing data set and the trained model to the algorithm */
        algorithm.input.set(NumericTableInputId.data, testData);
        Model model = trainingResult.get(TrainingResultId.model);
        algorithm.input.set(ModelInputId.model, model);

        /* Compute the prediction results */
        predictionResult = algorithm.compute();
    }

    private static void printResults() throws java.io.FileNotFoundException, java.io.IOException {

        FileDataSource testGroundTruth = new FileDataSource(context, testGroundTruthFileName,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        testGroundTruth.loadDataBlock(nTestObservations);

        NumericTable expected = testGroundTruth.getNumericTable();
        NumericTable prediction = predictionResult.get(PredictionResultId.prediction);
        Service.printClassificationResult(expected, prediction, "Ground truth", "Classification results",
                "NaiveBayes classification results (first 20 observations):", 20);
    }
}
