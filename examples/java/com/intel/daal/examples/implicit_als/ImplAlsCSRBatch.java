/* file: ImplAlsCSRBatch.java */
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
 //     Java example of the implicit alternating least squares (ALS) algorithm in
 //     the batch processing mode.
 //
 //     The program trains the implicit ALS model on a training data set.
 ////////////////////////////////////////////////////////////////////////////////
 */

/**
 * <a name="DAAL-EXAMPLE-JAVA-IMPLICITALSCSRBATCH">
 * @example ImplAlsCSRBatch.java
 */

package com.intel.daal.examples.implicit_als;

import com.intel.daal.algorithms.implicit_als.Model;
import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.examples.utils.Service;
import com.intel.daal.services.DaalContext;

class ImplAlsCSRBatch {
    private static long            nFactors             = 2;
    private static Model           initialModel;
    private static Model           trainedModel;
    private static CSRNumericTable data;
    /* Input data set parameters */
    private static final String    trainDatasetFileName = "../data/batch/implicit_als_csr.csv";

    private static DaalContext context = new DaalContext();

    public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {

        initializeModel();

        trainModel();

        testModel();

        context.dispose();
    }

    private static void initializeModel() throws java.io.FileNotFoundException, java.io.IOException {

        /* Read trainDatasetFileName from a file and create a numeric table for storing the input data */
        data = Service.createSparseTable(context, trainDatasetFileName);

        /* Create an algorithm object to initialize the implicit ALS model with the default method */
        InitBatch initAlgorithm = new InitBatch(context, Float.class, InitMethod.fastCSR);
        initAlgorithm.parameter.setNFactors(nFactors);

        /* Pass a training data set and dependent values to the algorithm */
        initAlgorithm.input.set(InitInputId.data, data);

        /* Initialize the implicit ALS model */
        InitResult initResult = initAlgorithm.compute();

        initialModel = initResult.get(InitResultId.model);
    }

    private static void trainModel() throws java.io.FileNotFoundException, java.io.IOException {
        /* Create an algorithm object to train the implicit ALS model with the default method */
        TrainingBatch alsTrain = new TrainingBatch(context, Float.class, TrainingMethod.fastCSR);
        alsTrain.parameter.setNFactors(nFactors);

        alsTrain.input.set(NumericTableInputId.data, data);
        alsTrain.input.set(ModelInputId.inputModel, initialModel);

        /* Build the implicit ALS model */
        TrainingResult trainingResult = alsTrain.compute();

        trainedModel = trainingResult.get(TrainingResultId.model);
    }

    private static void testModel() {
        /* Create an algorithm object to predict recommendations of the implicit ALS model */
        RatingsBatch algorithm = new RatingsBatch(context, Float.class, RatingsMethod.defaultDense);
        algorithm.parameter.setNFactors(nFactors);

        algorithm.input.set(RatingsModelInputId.model, trainedModel);

        RatingsResult result = algorithm.compute();

        NumericTable predictedRatings = result.get(RatingsResultId.prediction);

        Service.printNumericTable("Predicted ratings:", predictedRatings);
    }
}
