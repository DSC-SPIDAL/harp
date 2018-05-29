/* file: compression_types.i */
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

#include "JCompressionLevel.h"
#include "JCompressionMethod.h"
#include "daal.h"

#define DefaultLevel com_intel_daal_data_management_compression_CompressionLevel_DefaultLevelValue
#define Level0 com_intel_daal_data_management_compression_CompressionLevel_Level0Value
#define Level1 com_intel_daal_data_management_compression_CompressionLevel_Level1Value
#define Level2 com_intel_daal_data_management_compression_CompressionLevel_Level2Value
#define Level3 com_intel_daal_data_management_compression_CompressionLevel_Level3Value
#define Level4 com_intel_daal_data_management_compression_CompressionLevel_Level4Value
#define Level5 com_intel_daal_data_management_compression_CompressionLevel_Level5Value
#define Level6 com_intel_daal_data_management_compression_CompressionLevel_Level6Value
#define Level7 com_intel_daal_data_management_compression_CompressionLevel_Level7Value
#define Level8 com_intel_daal_data_management_compression_CompressionLevel_Level8Value
#define Level9 com_intel_daal_data_management_compression_CompressionLevel_Level9Value

#define Zlib com_intel_daal_data_management_compression_CompressionMethod_Zlib
#define Lzo com_intel_daal_data_management_compression_CompressionMethod_Lzo
#define Rle com_intel_daal_data_management_compression_CompressionMethod_Rle
#define Bzip2 com_intel_daal_data_management_compression_CompressionMethod_Bzip2
