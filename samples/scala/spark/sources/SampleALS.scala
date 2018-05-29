/* file: SampleALS.scala */
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

//import org.apache.spark.mllib.recommendation.ALS
import daal_for_mllib.ALS

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.distributed.RowMatrix

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel

import java.io._

object SampleALS extends App {
    val conf = new SparkConf().setAppName("Spark ALS")
    val sc = new SparkContext(conf)

    val data = sc.textFile("/Spark/ALS/data/ALS.txt")
    val ratings = data.map(_.split(' ') match { case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    // Build the recommendation model using ALS
    val users = 1000
    val products = 1000
    val rank = 10
    val numIterations = 10
    val model = ALS.trainImplicit(users, products, ratings, rank, numIterations)

    // Evaluate the model on rating data
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }

    val predictions = model.predict(usersProducts).map { case Rating(user, product, rate) => ((user, product), rate) }
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) => ((user, product), rate)}.join(predictions)
    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
        val err = (r1 - r2)
        err * err
    }.mean()

    println("Mean Squared Error = " + MSE)

    sc.stop()
}
