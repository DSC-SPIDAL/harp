/* file: cholesky_dense_batch.cpp */
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
!  Content:
!    C++ example of Cholesky decomposition
!******************************************************************************/

/**
 * <a name="DAAL-EXAMPLE-CPP-CHOLESKY_BATCH"></a>
 * \example cholesky_dense_batch.cpp
 */

#include "daal.h"
#include "service.h"

#include "offload.h"

using namespace std;
using namespace daal;
using namespace daal::algorithms;

/* Input data set parameters */
string datasetFileName = "../data/batch/cholesky.csv";

void serializeNumericTable(NumericTablePtr dataTable, byte **buffer, size_t *length);
NumericTablePtr deserializeNumericTable(byte *buffer, size_t size);
void serializeResult(cholesky::ResultPtr result, byte **buffer, size_t *length);
cholesky::ResultPtr deserializeResult(byte *buffer, size_t length);

int main(int argc, char *argv[])
{
    checkArguments(argc, argv, 1, &datasetFileName);

    /* Initialize FileDataSource<CSVFeatureManager> to retrieve the input data from a .csv file */
    FileDataSource<CSVFeatureManager> dataSource(datasetFileName, DataSource::doAllocateNumericTable,
                                                 DataSource::doDictionaryFromContext);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock();


    byte *bufferin,*bufferout,*bufferout_card;
    size_t lengthin,lengthout;

    NumericTablePtr dataTable = dataSource.getNumericTable();
    serializeNumericTable(dataTable, &bufferin, &lengthin);

        _Offload_status x;
        OFFLOAD_STATUS_INIT(x);

    #pragma offload target(mic:0) status(x) in(bufferin:length(lengthin)) in(lengthin) out(lengthout) nocopy(bufferout_card)
    {
        if (_Offload_get_device_number() < 0) {
            printf("optional offload ran on CPU\n");
        } else {
            printf("optional offload ran on COPROCESSOR\n");
        }

        NumericTablePtr dataTable_card = deserializeNumericTable(bufferin, lengthin);

        /* Create an algorithm to compute Cholesky decomposition using the default method */
        cholesky::Batch<> algorithm;

        /* Set input objects for the algorithm */
        algorithm.input.set(cholesky::data, dataTable_card);

        /* Compute Cholesky decomposition */
        algorithm.compute();

        /* Get computed Cholesky decomposition */
        cholesky::ResultPtr res_card = algorithm.getResult();

        printNumericTable(res_card->get(cholesky::choleskyFactor));

        serializeResult(res_card, &bufferout_card, &lengthout);

    }
    if (x.result == OFFLOAD_SUCCESS) {
        printf("optional offload was successful\n");
    } else {
        printf("optional offload failed\n");
    }

    bufferout=(byte *)malloc(sizeof(byte)*lengthout);
    for(int i=0;i<lengthout;i++) bufferout[i]=0;

    #pragma offload target(mic:0) out(bufferout:length(lengthout)) nocopy(bufferout_card,lengthout)
    {
        for(int i=0;i<lengthout;i++) bufferout[i]=bufferout_card[i];
    }

    cholesky::ResultPtr res = deserializeResult(bufferout, lengthout);
    printNumericTable(res->get(cholesky::choleskyFactor));

    return 0;
}

void serializeNumericTable(NumericTablePtr dataTable, byte **buffer, size_t *length)
{
    /* Create a data archive to serialize the numeric table */
    InputDataArchive dataArch;

    /* Serialize the numeric table into the data archive */
    dataTable->serialize(dataArch);

    /* Get the length of the serialized data in bytes */
    *length = dataArch.getSizeOfArchive();

    /* Store the serialized data in an array */
    *buffer = new byte[*length];
    dataArch.copyArchiveToArray(*buffer, *length);
}

NumericTablePtr deserializeNumericTable(byte *buffer, size_t length)
{
    /* Create a data archive to deserialize the numeric table */
    OutputDataArchive dataArch(buffer, length);

    /* Create a numeric table object */
    NumericTablePtr dataTable = NumericTablePtr( new HomogenNumericTable<float>() );

    /* Deserialize the numeric table from the data archive */
    dataTable->deserialize(dataArch);

    return dataTable;
}

void serializeResult(cholesky::ResultPtr result, byte **buffer, size_t *length)
{
    /* Create a data archive to serialize the numeric table */
    InputDataArchive dataArch;

    /* Serialize the numeric table into the data archive */
    result->serialize(dataArch);

    /* Get the length of the serialized data in bytes */
    *length = dataArch.getSizeOfArchive();

    /* Store the serialized data in an array */
    *buffer = new byte[*length];
    dataArch.copyArchiveToArray(*buffer, *length);
}

cholesky::ResultPtr deserializeResult(byte *buffer, size_t length)
{
    /* Create a data archive to deserialize the numeric table */
    OutputDataArchive dataArch(buffer, length);

    /* Create a numeric table object */
    cholesky::ResultPtr dataTable = cholesky::ResultPtr(new cholesky::Result);

    /* Deserialize the numeric table from the data archive */
    dataTable->deserialize(dataArch);

    return dataTable;
}
