/*
 * Copyright 2013-2016 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.daal_nn;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import java.nio.DoubleBuffer;


public class Service{
public static Tensor readTensorFromCSV(DaalContext context, String datasetFileName, boolean allowOneColumn) {
        FileDataSource dataSource = new FileDataSource(context, datasetFileName,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        dataSource.loadDataBlock();
        NumericTable nt = dataSource.getNumericTable();
        int nRows = (int)nt.getNumberOfRows();
        int nCols = (int)nt.getNumberOfColumns();
        if (nCols > 1 || allowOneColumn) {
            long[] dims = {nRows, nCols};
            float[] data = new float[nRows * nCols];
            DoubleBuffer buffer = DoubleBuffer.allocate(nRows * nCols);
            buffer = nt.getBlockOfRows(0, nRows, buffer);
            for (int i = 0; i < nRows * nCols; i++) {
                data[i] = (float)buffer.get(i);
            }

            return new HomogenTensor(context, dims, data);
        } else {
            long[] dims = {nRows};
            float[] data = new float[nRows];
            DoubleBuffer buffer = DoubleBuffer.allocate(nRows);
            buffer = nt.getBlockOfRows(0, nRows, buffer);
            for (int i = 0; i < nRows; i++) {
                data[i] = (float)buffer.get(i);
            }

            return new HomogenTensor(context, dims, data);
        }
    }

public static Tensor readTensorFromNumericTable(DaalContext context, NumericTable nt, boolean allowOneColumn) {
        int nRows = (int)nt.getNumberOfRows();
        int nCols = (int)nt.getNumberOfColumns();
        if (nCols > 1 || allowOneColumn) {
            long[] dims = {nRows, nCols};
            float[] data = new float[nRows * nCols];
            DoubleBuffer buffer = DoubleBuffer.allocate(nRows * nCols);
            buffer = nt.getBlockOfRows(0, nRows, buffer);
            for (int i = 0; i < nRows * nCols; i++) {
                data[i] = (float)buffer.get(i);
            }

            return new HomogenTensor(context, dims, data);
        } else {
            long[] dims = {nRows};
            float[] data = new float[nRows];
            DoubleBuffer buffer = DoubleBuffer.allocate(nRows);
            buffer = nt.getBlockOfRows(0, nRows, buffer);
            for (int i = 0; i < nRows; i++) {
                data[i] = (float)buffer.get(i);
            }

            return new HomogenTensor(context, dims, data);
        }
    }

public static NumericTable readNumericTableFromCsv(DaalContext context, String datasetFileName, boolean allowOneColumn) {
       FileDataSource dataSource = new FileDataSource(context, datasetFileName,
                DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
        dataSource.loadDataBlock();
        NumericTable nt = dataSource.getNumericTable();
        return nt;
    }

public static Tensor getNextSubtensor(DaalContext context, Tensor inputTensor, int startPos, int nElements) {
    long[] dims = inputTensor.getDimensions();
    dims[0] = nElements;

    int nDims = dims.length;
    int bufLength = nElements;

    for(int i = 1; i < nDims; i++) {
        bufLength *= dims[i];
    }

    DoubleBuffer dataDoubleSubtensor = DoubleBuffer.allocate(bufLength);

    long fDims[] = {};
    inputTensor.getSubtensor(fDims, (long)startPos, (long)nElements, dataDoubleSubtensor);

    double[] subtensorData = new double[bufLength];
    dataDoubleSubtensor.get(subtensorData);

    inputTensor.releaseSubtensor(fDims, (long)startPos, (long)nElements, dataDoubleSubtensor);

    return new HomogenTensor(context, dims, subtensorData);
}

 public static void printTensors(String header1, String header2, String message,
                                    Tensor dataTensor1, Tensor dataTensor2, int nPrintedRows) {
        long[] dims1 = dataTensor1.getDimensions();
        int nRows1 = (int)dims1[0];
        if (nPrintedRows == 0 || nRows1 < nPrintedRows) nPrintedRows = nRows1;

        int nCols1 = 1;
        for (int i = 1; i < dims1.length; i++) {
            nCols1 *= dims1[i];
        }

        long[] dims2 = dataTensor2.getDimensions();
        int nRows2 = (int)dims2[0];
        if (nPrintedRows == 0 || nRows2 < nPrintedRows) nPrintedRows = nRows2;

        int nCols2 = 1;
        for (int i = 1; i < dims2.length; i++) {
            nCols2 *= dims2[i];
        }

        long[] fixed = {};

        DoubleBuffer result1 = DoubleBuffer.allocate(nRows1 * nCols1);
        result1 = dataTensor1.getSubtensor(fixed, 0, nPrintedRows, result1);

        DoubleBuffer result2 = DoubleBuffer.allocate(nRows2 * nCols2);
        result2 = dataTensor2.getSubtensor(fixed, 0, nPrintedRows, result2);

        StringBuilder builder = new StringBuilder();
        builder.append(message);
        builder.append("\n");
        builder.append(header1 + "\t" + header2 + "\n");
        for (long i = 0; i < nPrintedRows; i++) {
            for (long j = 0; j < nCols1; j++) {
                String tmp = String.format("%-6.3f   ", result1.get((int) (i * nCols1 + j)));
                builder.append(tmp);
            }
            builder.append("\t");
            for (long j = 0; j < nCols2; j++) {
                String tmp = String.format("%-6.3f   ", result2.get((int) (i * nCols2 + j)));
                builder.append(tmp);
            }
            builder.append("\n");
        }
        System.out.println(builder.toString());
    }
}
