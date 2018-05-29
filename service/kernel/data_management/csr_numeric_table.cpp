/** file csr_numeric_table.cpp */
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

#include "csr_numeric_table.h"

namespace daal
{
namespace data_management
{
namespace interface1
{


#define DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(T)                                                  \
template<>                                                                                          \
CSRBlockDescriptor<T>::CSRBlockDescriptor() : _rows_capacity(0), _values_capacity(0),               \
    _ncols(0), _nrows(0), _rowsOffset(0), _rwFlag(0), _rawPtr(0), _pPtr(0), _nvalues(0) {}

DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(float         )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(double        )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(int           )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(unsigned int  )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(DAAL_INT64    )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(DAAL_UINT64   )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(char          )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(unsigned char )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(short         )
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(unsigned short)
DAAL_IMPL_CSRBLOCKDESCRIPTORCONSTRUCTOR(unsigned long )

}
}
}
