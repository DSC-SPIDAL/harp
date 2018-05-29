/* file: multiclassclassifier_train_kernel.h */
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
//++
//  Declaration of template function that trains Multi-class slassifier model.
//--
*/

#ifndef __MULTICLASSCLASSIFIER_TRAIN_KERNEL_H__
#define __MULTICLASSCLASSIFIER_TRAIN_KERNEL_H__

#include "numeric_table.h"
#include "model.h"
#include "algorithm.h"
#include "multi_class_classifier_train_types.h"
#include "service_defines.h"

using namespace daal::data_management;

namespace daal
{
namespace algorithms
{
namespace multi_class_classifier
{
namespace training
{
namespace internal
{

template<Method method, typename AlgorithmFPtype, CpuType cpu>
struct MultiClassClassifierTrainKernel : public Kernel
{
    services::Status compute(const NumericTable *a0, const NumericTable *a1, daal::algorithms::Model *r,
                             const daal::algorithms::Parameter *par);
};

} // namespace internal

} // namespace training

} // namespace multi_class_classifier

} // namespace algorithms

} // namespace daal


#endif