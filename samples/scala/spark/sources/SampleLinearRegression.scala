/* file: SampleLinearRegression.scala */
//==============================================================================
// Copyright 2017-2018 Intel Corporation
// All Rights Reserved.
//
// If this software  was obtained under  the Intel Simplified  Software License,
// the following terms apply:
//
// The source code,  information and material  ("Material") contained  herein is
// owned by Intel Corporation  or its suppliers or licensors,  and title to such
// Material remains with Intel Corporation  or its suppliers or  licensors.  The
// Material  contains  proprietary  information  of Intel or  its suppliers  and
// licensors.  The Material is protected by worldwide  copyright laws and treaty
// provisions.  No part  of  the  Material  may  be  used,  copied,  reproduced,
// modified, published, uploaded, posted,  transmitted, distributed or disclosed
// in any  way without  Intel's  prior express  written  permission.  No license
// under any patent,  copyright  or other  intellectual  property rights  in the
// Material is  granted   to or  conferred   upon  you,  either   expressly,  by
// implication,  inducement,  estoppel  or  otherwise.  Any  license  under such
// intellectual  property  rights  must be  express  and  approved  by Intel  in
// writing.
//
// Unless otherwise agreed  by Intel in  writing,  you  may not remove  or alter
// this notice  or any other  notice embedded in  Materials by  Intel or Intel's
// suppliers or licensors in any way.
//
//
// If this software was  obtained under  the Apache  License,  Version  2.0 (the
// "License"), the following terms apply:
//
// You may not  use this  file except in  compliance with  the License.  You may
// obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
//
//
// Unless  required  by  applicable   law or  agreed  to  in  writing,  software
// distributed under the License is  distributed  on an "AS IS"  BASIS,  WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//
// See the  License  for  the  specific   language  governing   permissions  and
// limitations under the License.
//==============================================================================

package DAAL

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import daal_for_mllib.LinearRegression

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionModel

import com.intel.daal.algorithms.linear_regression.training.TrainingMethod

object SampleLinearRegression extends App {

    def calculateMSE(model: LinearRegressionModel, data: RDD[LabeledPoint]) : Double = {  
        val valuesAndPreds = data.map( point => {
             val prediction = model.predict(point.features)
             (point.label, prediction)
           }
        )
        valuesAndPreds.map{ case(v, p) => math.pow((v - p), 2) }.mean()
    }

    val conf = new SparkConf().setAppName("Spark Linear Regression")
    val sc = new SparkContext(conf)

    val data = sc.textFile("/Spark/LinearRegression/data/LinearRegression.txt")
    val parsedData = data.map { line => {
        val parts = line.split(',')
        LabeledPoint(parts(0).toDouble, Vectors.dense(parts(1).split(' ').map(_.toDouble)))
    }}.cache()

    val model = LinearRegression.train(parsedData, TrainingMethod.normEqDense)

    val MSE = calculateMSE(model, parsedData)
    println("Mean Squared Error = " + MSE)

    sc.stop()
}
