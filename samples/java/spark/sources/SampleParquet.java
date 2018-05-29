/* file: SampleParquet.java */
/*******************************************************************************
* Copyright 2017-2018 Intel Corporation
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
 //  Content:
 //     Java sample of using Parquet Data Frame
 ////////////////////////////////////////////////////////////////////////////////
 */

package DAAL;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import java.nio.DoubleBuffer;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import scala.Tuple2;

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.*;

public class SampleParquet {
    private static final int rowsToPrint = 5;
    private static final int partitionsToPrint = 10;

    public static void main(String[] args) {
        DaalContext context = new DaalContext();

        /* Create JavaSparkContext that loads defaults from the system properties and the classpath and sets the name */
        JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("Spark Parquet"));

        SparkSession sparkSession = SparkSession.builder().getOrCreate();

        Dataset<Row> df = sparkSession.read().parquet("/Spark/Parquet/data/Parquet");

        JavaRDD<HomogenNumericTable> dataRDD = convertDataFrame(df, 4096);

        List<HomogenNumericTable> dfList = dataRDD.collect();

        int parts = dfList.size();

        System.out.println(dataRDD.count() + " tables");

        for (int i = 0; i < parts && i < partitionsToPrint; i++) {
            HomogenNumericTable table = dfList.get(i);
            int nColumns = (int)table.getNumberOfColumns();
            int nRows = (int)table.getNumberOfRows();
            if (nRows > rowsToPrint) {
                nRows = rowsToPrint;
            }
            DoubleBuffer dataDouble = DoubleBuffer.allocate(nColumns * nRows);
            dataDouble = table.getBlockOfRows(0, nRows, dataDouble);
            printDoubleBuffer(dataDouble, nColumns, nRows, "Print homogen data table as double:");
            table.releaseBlockOfRows(0, nRows, dataDouble);
        }

        context.dispose();
        sc.stop();
    }

    private static JavaRDD<HomogenNumericTable> convertDataFrame(Dataset<Row> df, final long maxRowsPerTable) {

        JavaRDD<HomogenNumericTable> dataRDD = df.rdd().toJavaRDD().mapPartitions(new FlatMapFunction<Iterator<Row>, HomogenNumericTable>() {
            public Iterator<HomogenNumericTable> call(Iterator<Row> it) {
                DaalContext localContext = new DaalContext();
                long maxRows = maxRowsPerTable;
                long curRow  = 0;
                ArrayList<HomogenNumericTable> tables = new ArrayList<HomogenNumericTable>();
                ArrayList<Row> rows = new ArrayList<Row>();
                while (it.hasNext()) {
                    rows.add(it.next());
                    curRow++;
                    if( curRow == maxRows || !(it.hasNext()) ) {
                        long nCols = rows.get(0).length();
                        double[] data = new double[(int)(curRow * nCols)];
                        for (int i = 0; i < (int)curRow; i++) {
                            Row row = rows.get(i);
                            for (int j = 0; j < (int)nCols; j++) {
                                data[i * (int)nCols + j] = row.getDouble(j);
                            }
                        }
                        HomogenNumericTable table = new HomogenNumericTable(localContext, data, nCols, curRow);
                        table.pack();
                        tables.add(table);
                        rows.clear();
                        curRow = 0;
                    }
                }
                localContext.dispose();
                return tables.iterator();
            }
        });

        return dataRDD;
    }

    private static void printDoubleBuffer(DoubleBuffer buf, long nColumns, long nRows, String message) {
        int step = (int) nColumns;
        System.out.println(message);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nColumns; j++) {
                System.out.format("%6.3f   ", buf.get(i * step + j));
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
