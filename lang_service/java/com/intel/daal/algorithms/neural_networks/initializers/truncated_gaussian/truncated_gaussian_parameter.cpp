/* file: truncated_gaussian_parameter.cpp */
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

#include <jni.h>
#include "neural_networks/initializers/truncated_gaussian/JTruncatedGaussianParameter.h"

#include "daal.h"

#include "common_helpers.h"

USING_COMMON_NAMESPACES();
using namespace daal::algorithms::neural_networks;

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cSetA
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cSetA
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble a, jint prec)
{
    if(prec == 0)
    {
        (((initializers::truncated_gaussian::Parameter<double> *)cParameter))->a = (double)a;
    }
    else
    {
        (((initializers::truncated_gaussian::Parameter<float> *)cParameter))->a = (double)a;
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cSetB
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cSetB
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble b, jint prec)
{
    if(prec == 0)
    {
        (((initializers::truncated_gaussian::Parameter<double> *)cParameter))->b = (double)b;
    }
    else
    {
        (((initializers::truncated_gaussian::Parameter<float> *)cParameter))->b = (double)b;
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cGetA
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cGetA
  (JNIEnv *env, jobject thisObj, jlong cParameter, jint prec)
{
    if(prec == 0)
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<double> *)cParameter))->a);
    }
    else
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<float> *)cParameter))->a);
    }
    return 0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cGetB
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cGetB
  (JNIEnv *env, jobject thisObj, jlong cParameter, jint prec)
{
    if(prec == 0)
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<double> *)cParameter))->b);
    }
    else
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<float> *)cParameter))->b);
    }
    return 0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cSetMean
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cSetMean
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble mean, jint prec)
{
    if(prec == 0)
    {
        (((initializers::truncated_gaussian::Parameter<double> *)cParameter))->mean = (double)mean;
    }
    else
    {
        (((initializers::truncated_gaussian::Parameter<float> *)cParameter))->mean = (double)mean;
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cSetSigma
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cSetSigma
  (JNIEnv *env, jobject thisObj, jlong cParameter, jdouble sigma, jint prec)
{
    if(prec == 0)
    {
        (((initializers::truncated_gaussian::Parameter<double> *)cParameter))->sigma = (double)sigma;
    }
    else
    {
        (((initializers::truncated_gaussian::Parameter<float> *)cParameter))->sigma = (double)sigma;
    }
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cGetMean
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cGetMean
  (JNIEnv *env, jobject thisObj, jlong cParameter, jint prec)
{
    if(prec == 0)
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<double> *)cParameter))->mean);
    }
    else
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<float> *)cParameter))->mean);
    }
    return 0;
}

/*
 * Class:     com_intel_daal_algorithms_neural_networks_initializers_truncated_gaussian_TruncatedGaussianParameter
 * Method:    cGetSigma
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_intel_daal_algorithms_neural_1networks_initializers_truncated_1gaussian_TruncatedGaussianParameter_cGetSigma
  (JNIEnv *env, jobject thisObj, jlong cParameter, jint prec)
{
    if(prec == 0)
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<double> *)cParameter))->sigma);
    }
    else
    {
        return (jdouble)((((initializers::truncated_gaussian::Parameter<float> *)cParameter))->sigma);
    }
    return 0;
}
