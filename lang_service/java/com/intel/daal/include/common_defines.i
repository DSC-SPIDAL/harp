/* file: common_defines.i */
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

#include "JComputeMode.h"
#include "JComputeStep.h"

#define jBatch          com_intel_daal_algorithms_ComputeMode_batchValue
#define jOnline         com_intel_daal_algorithms_ComputeMode_onlineValue
#define jDistributed    com_intel_daal_algorithms_ComputeMode_distributedValue

#define jStep1Local     com_intel_daal_algorithms_ComputeStep_step1LocalValue
#define jStep2Master    com_intel_daal_algorithms_ComputeStep_step2MasterValue
#define jStep3Local     com_intel_daal_algorithms_ComputeStep_step3LocalValue


namespace daal
{

const int SERIALIZATION_JAVANIO_CSR_NT_ID                                                       = 9000;
const int SERIALIZATION_JAVANIO_HOMOGEN_NT_ID                                                   = 10010;
const int SERIALIZATION_JAVANIO_AOS_NT_ID                                                       = 10020;
const int SERIALIZATION_JAVANIO_SOA_NT_ID                                                       = 10030;
const int SERIALIZATION_JAVANIO_PACKEDSYMMETRIC_NT_ID                                           = 10040;
const int SERIALIZATION_JAVANIO_PACKEDTRIANGULAR_NT_ID                                          = 10050;
const int SERIALIZATION_JAVANIO_HOMOGEN_TENSOR_ID                                               = 21000;
// added by Harp-DAAL
const int SERIALIZATION_JAVANIO_HARP_TENSOR_ID                                                  = 22000;
const int SERIALIZATION_JAVANIO_HARP_NT_ID                                                      = 23000;
const int SERIALIZATION_JAVANIO_HARPDAAL_NT_ID                                                  = 24000;

} // namespace daal


#define IMPLEMENT_SERIALIZABLE_TAG(Class,Tag) \
    int Class<Tag>::serializationTag() { return Tag; } \
    int Class<Tag>::getSerializationTag() const { return Class<Tag>::serializationTag(); }

#define IMPLEMENT_SERIALIZABLE_TAGT(Class,Tag) \
    template<> int Class<Tag>::serializationTag() { return Tag; } \
    template<> int Class<Tag>::getSerializationTag() const { return Class<Tag>::serializationTag(); }
