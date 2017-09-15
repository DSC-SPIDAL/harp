/* file: lowordermoments_types.i */
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

#include "daal.h"

using namespace daal;
using namespace daal::algorithms;

#include "low_order_moments/JMethod.h"

#define DefaultDense    com_intel_daal_algorithms_low_order_moments_Method_DefaultDense
#define SinglePassDense com_intel_daal_algorithms_low_order_moments_Method_SinglePassDense
#define SumDense        com_intel_daal_algorithms_low_order_moments_Method_SumDense
#define FastCSR         com_intel_daal_algorithms_low_order_moments_Method_FastCSR
#define SinglePassCSR   com_intel_daal_algorithms_low_order_moments_Method_SinglePassCSR
#define SumCSR          com_intel_daal_algorithms_low_order_moments_Method_SumCSR

typedef low_order_moments::Batch<float, low_order_moments::defaultDense>     lom_of_s_dd;
typedef low_order_moments::Batch<float, low_order_moments::singlePassDense>  lom_of_s_pd;
typedef low_order_moments::Batch<float, low_order_moments::sumDense>         lom_of_s_sd;
typedef low_order_moments::Batch<double, low_order_moments::defaultDense>    lom_of_d_dd;
typedef low_order_moments::Batch<double, low_order_moments::singlePassDense> lom_of_d_pd;
typedef low_order_moments::Batch<double, low_order_moments::sumDense>        lom_of_d_sd;
typedef low_order_moments::Batch<float, low_order_moments::fastCSR>          lom_of_s_dc;
typedef low_order_moments::Batch<float, low_order_moments::singlePassCSR>    lom_of_s_pc;
typedef low_order_moments::Batch<float, low_order_moments::sumCSR>           lom_of_s_sc;
typedef low_order_moments::Batch<double, low_order_moments::fastCSR>         lom_of_d_dc;
typedef low_order_moments::Batch<double, low_order_moments::singlePassCSR>   lom_of_d_pc;
typedef low_order_moments::Batch<double, low_order_moments::sumCSR>          lom_of_d_sc;

typedef low_order_moments::Online<float, low_order_moments::defaultDense>     lom_on_s_dd;
typedef low_order_moments::Online<float, low_order_moments::singlePassDense>  lom_on_s_pd;
typedef low_order_moments::Online<float, low_order_moments::sumDense>         lom_on_s_sd;
typedef low_order_moments::Online<double, low_order_moments::defaultDense>    lom_on_d_dd;
typedef low_order_moments::Online<double, low_order_moments::singlePassDense> lom_on_d_pd;
typedef low_order_moments::Online<double, low_order_moments::sumDense>        lom_on_d_sd;
typedef low_order_moments::Online<float, low_order_moments::fastCSR>          lom_on_s_dc;
typedef low_order_moments::Online<float, low_order_moments::singlePassCSR>    lom_on_s_pc;
typedef low_order_moments::Online<float, low_order_moments::sumCSR>           lom_on_s_sc;
typedef low_order_moments::Online<double, low_order_moments::fastCSR>         lom_on_d_dc;
typedef low_order_moments::Online<double, low_order_moments::singlePassCSR>   lom_on_d_pc;
typedef low_order_moments::Online<double, low_order_moments::sumCSR>          lom_on_d_sc;

typedef low_order_moments::Distributed<step1Local, float, low_order_moments::defaultDense>     lom_dl_s_dd;
typedef low_order_moments::Distributed<step1Local, float, low_order_moments::singlePassDense>  lom_dl_s_pd;
typedef low_order_moments::Distributed<step1Local, float, low_order_moments::sumDense>         lom_dl_s_sd;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::defaultDense>    lom_dl_d_dd;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::singlePassDense> lom_dl_d_pd;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::sumDense>        lom_dl_d_sd;
typedef low_order_moments::Distributed<step1Local, float, low_order_moments::fastCSR>          lom_dl_s_dc;
typedef low_order_moments::Distributed<step1Local, float, low_order_moments::singlePassCSR>    lom_dl_s_pc;
typedef low_order_moments::Distributed<step1Local, float, low_order_moments::sumCSR>           lom_dl_s_sc;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::fastCSR>         lom_dl_d_dc;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::singlePassCSR>   lom_dl_d_pc;
typedef low_order_moments::Distributed<step1Local, double, low_order_moments::sumCSR>          lom_dl_d_sc;

typedef low_order_moments::Distributed<step2Master, float, low_order_moments::defaultDense>     lom_dm_s_dd;
typedef low_order_moments::Distributed<step2Master, float, low_order_moments::singlePassDense>  lom_dm_s_pd;
typedef low_order_moments::Distributed<step2Master, float, low_order_moments::sumDense>         lom_dm_s_sd;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::defaultDense>    lom_dm_d_dd;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::singlePassDense> lom_dm_d_pd;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::sumDense>        lom_dm_d_sd;
typedef low_order_moments::Distributed<step2Master, float, low_order_moments::fastCSR>          lom_dm_s_dc;
typedef low_order_moments::Distributed<step2Master, float, low_order_moments::singlePassCSR>    lom_dm_s_pc;
typedef low_order_moments::Distributed<step2Master, float, low_order_moments::sumCSR>           lom_dm_s_sc;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::fastCSR>         lom_dm_d_dc;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::singlePassCSR>   lom_dm_d_pc;
typedef low_order_moments::Distributed<step2Master, double, low_order_moments::sumCSR>          lom_dm_d_sc;
