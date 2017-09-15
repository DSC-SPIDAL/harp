/* file: implicit_als_train_init_parameter.cpp */
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
//++
//  Implementation of auxiliary implicit als methods.
//--
*/

#include "implicit_als_train_init_parameter.h"

using namespace daal::data_management;
using namespace daal::services;

namespace daal
{
namespace algorithms
{
namespace implicit_als
{
namespace training
{
namespace init
{
namespace internal
{
SharedPtr<HomogenNumericTable<int> > getPartition(const init::DistributedParameter *parameter)
{
    NumericTable *partitionTable = parameter->partition.get();
    size_t nRows = partitionTable->getNumberOfRows();
    size_t nParts = nRows - 1;
    BlockDescriptor<int> block;
    if (nRows == 1)
    {
        partitionTable->getBlockOfRows(0, nRows, readOnly, block);
        nParts = *(block.getBlockPtr());
        partitionTable->releaseBlockOfRows(block);
    }
    SharedPtr<HomogenNumericTable<int> > nt(new HomogenNumericTable<int>(1, nParts + 1, NumericTable::doAllocate));
    int *partition = nt->getArray();
    if (nRows == 1)
    {
        size_t nUsersInPart = parameter->fullNUsers / nParts;
        partition[0] = 0;
        for (size_t i = 1; i < nParts; i++)
        {
            partition[i] = partition[i - 1] + nUsersInPart;
        }
        partition[nParts] = parameter->fullNUsers;
    }
    else
    {
        partitionTable->getBlockOfRows(0, nRows, readOnly, block);
        int *srcPartition = block.getBlockPtr();
        for (size_t i = 0; i < nParts + 1; i++)
        {
            partition[i] = srcPartition[i];
        }
        partitionTable->releaseBlockOfRows(block);
    }
    return nt;
}

}// namespace internal
}// namespace init
}// namespace training
}// namespace implicit_als
}// namespace algorithms
}// namespace daal
