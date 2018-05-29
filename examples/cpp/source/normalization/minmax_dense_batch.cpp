/* file: minmax_dense_batch.cpp */
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
!  Content:
!    C++ example of min-max normalization algorithm.
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-MINMAX_DENSE_BATCH"></a>
 * \example minmax_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;
using namespace daal::algorithms::normalization;

/* Input data set parameters */
string datasetName = "../data/batch/normalization.csv";

int main()
{
    /* Retrieve the input data */
    FileDataSource<CSVFeatureManager> dataSource(datasetName, DataSource::doAllocateNumericTable, DataSource::doDictionaryFromContext);
    dataSource.loadDataBlock();

    NumericTablePtr data = dataSource.getNumericTable();

    /* Create an algorithm */
    minmax::Batch<float, minmax::defaultDense> algorithm;

    /* Set lower and upper bounds for the algorithm */
    algorithm.parameter.lowerBound = -1.0;
    algorithm.parameter.upperBound =  1.0;

    /* Set an input object for the algorithm */
    algorithm.input.set(minmax::data, data);

    /* Compute min-max normalization function */
    algorithm.compute();

    /* Print the results of stage */
    minmax::ResultPtr res = algorithm.getResult();

    printNumericTable(data, "First 10 rows of the input data:", 10);
    printNumericTable(res->get(minmax::normalizedData), "First 10 rows of the min-max normalization result:", 10);

    return 0;
}
