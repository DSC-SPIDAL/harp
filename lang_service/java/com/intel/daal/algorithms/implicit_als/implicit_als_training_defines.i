/* file: implicit_als_training_defines.i */
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

#include "implicit_als/training/JModelInputId.h"
#include "implicit_als/training/JMasterInputId.h"
#include "implicit_als/training/JTrainingResultId.h"
#include "implicit_als/training/JNumericTableInputId.h"
#include "implicit_als/training/JDistributedPartialResultStep1Id.h"
#include "implicit_als/training/JDistributedPartialResultStep2Id.h"
#include "implicit_als/training/JDistributedPartialResultStep3Id.h"
#include "implicit_als/training/JDistributedPartialResultStep4Id.h"

#include "implicit_als/training/JPartialModelInputId.h"
#include "implicit_als/training/JStep3LocalCollectionInputId.h"
#include "implicit_als/training/JStep3LocalNumericTableInputId.h"
#include "implicit_als/training/JStep4LocalPartialModelsInputId.h"
#include "implicit_als/training/JStep4LocalNumericTableInputId.h"

#include "implicit_als/training/JTrainingMethod.h"

#include "common_defines.i"

#define dataId         com_intel_daal_algorithms_implicit_als_training_NumericTableInputId_dataId
#define inputModelId   com_intel_daal_algorithms_implicit_als_training_ModelInputId_inputModelId
#define modelId        com_intel_daal_algorithms_implicit_als_training_TrainingResultId_modelId

#define step1InputModelId        com_intel_daal_algorithms_implicit_als_training_Step1LocalInputId_step1InputModelId
#define inputOfStep3FromStep2Id  com_intel_daal_algorithms_implicit_als_training_Step3LocalInputId_inputOfStep3FromStep2Id

#define FastCSR             com_intel_daal_algorithms_implicit_als_training_TrainingMethod_fastCSRId
#define DefaultDense        com_intel_daal_algorithms_implicit_als_training_TrainingMethod_defaultDenseId

#define inputOfStep2FromStep1Id     com_intel_daal_algorithms_implicit_als_training_MasterInputId_inputOfStep2FromStep1Id

#define partialModelId              com_intel_daal_algorithms_implicit_als_training_PartialModelInputId_partialModelId

#define partialModelBlocksToNodeId  com_intel_daal_algorithms_implicit_als_training_Step3LocalCollectionInputId_partialModelBlocksToNodeId
#define offsetId                    com_intel_daal_algorithms_implicit_als_training_Step3LocalNumericTableInputId_offsetId

#define partialDataId               com_intel_daal_algorithms_implicit_als_training_Step4LocalNumericTableInputId_partialDataId
#define inputOfStep4FromStep2Id     com_intel_daal_algorithms_implicit_als_training_Step4LocalNumericTableInputId_inputOfStep4FromStep2Id

#define partialModelsId             com_intel_daal_algorithms_implicit_als_training_Step4LocalPartialModelsInputId_partialModelsId
