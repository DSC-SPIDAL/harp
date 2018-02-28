package edu.iu.daal_qr;

import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data_source.*;

import java.nio.FloatBuffer;

public class Service{
public static void printNumericTable(String header, NumericTable nt, long nPrintedRows, long nPrintedCols) {
        long nNtCols = nt.getNumberOfColumns();
        long nNtRows = nt.getNumberOfRows();
        long nRows = nNtRows;
        long nCols = nNtCols;

        if (nPrintedRows > 0) {
            nRows = Math.min(nNtRows, nPrintedRows);
        }

        FloatBuffer result = FloatBuffer.allocate((int) (nNtCols * nRows));
        result = nt.getBlockOfRows(0, nRows, result);

        if (nPrintedCols > 0) {
            nCols = Math.min(nNtCols, nPrintedCols);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(header);
        builder.append("\n");
        for (long i = 0; i < nRows; i++) {
            for (long j = 0; j < nCols; j++) {
                String tmp = String.format("%-6.3f   ", result.get((int) (i * nNtCols + j)));
                builder.append(tmp);
            }
            builder.append("\n");
        }
        System.out.println(builder.toString());
    }

    public static void printNumericTable(String header, CSRNumericTable nt, long nPrintedRows, long nPrintedCols) {
        long[] rowOffsets = nt.getRowOffsetsArray();
        long[] colIndices = nt.getColIndicesArray();
        float[] values = nt.getFloatArray();

        long nNtCols = nt.getNumberOfColumns();
        long nNtRows = nt.getNumberOfRows();
        long nRows = nNtRows;
        long nCols = nNtCols;

        if (nPrintedRows > 0) {
            nRows = Math.min(nNtRows, nPrintedRows);
        }

        if (nPrintedCols > 0) {
            nCols = Math.min(nNtCols, nPrintedCols);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(header);
        builder.append("\n");

        float[] oneDenseRow = new float[(int) nCols];
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                oneDenseRow[j] = 0;
            }
            int nElementsInRow = (int) (rowOffsets[i + 1] - rowOffsets[i]);
            for (int k = 0; k < nElementsInRow; k++) {
                oneDenseRow[(int) (colIndices[(int) (rowOffsets[i] - 1 + k)] - 1)] = values[(int) (rowOffsets[i] - 1
                        + k)];
            }
            for (int j = 0; j < nCols; j++) {
                String tmp = String.format("%-6.3f   ", oneDenseRow[j]);
                builder.append(tmp);
            }
            builder.append("\n");
        }
        System.out.println(builder.toString());
    }


    public static void printNumericTable(String header, NumericTable nt, long nRows) {
        printNumericTable(header, nt, nRows, nt.getNumberOfColumns());
    }

    public static void printNumericTable(String header, NumericTable nt) {
        printNumericTable(header, nt, nt.getNumberOfRows());
    }

    public static void printNumericTable(String header, CSRNumericTable nt, long nRows) {
        printNumericTable(header, nt, nRows, nt.getNumberOfColumns());
    }

    public static void printNumericTable(String header, CSRNumericTable nt) {
        printNumericTable(header, nt, nt.getNumberOfRows());
    }
}