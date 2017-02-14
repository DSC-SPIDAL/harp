package edu.iu.mlr;

import edu.iu.harp.schdynamic.Task;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class MergeTask implements Task<Range, Object> {
    private Table<DoubleArray> sTable;
    private double[][] local;

    public MergeTask(double[][] L) {
        local = L;
    }

    public void setTable(Table<DoubleArray> S) {
        sTable = S;
    }

    @Override
    public Object run(Range r) throws Exception {
        double[] sum = ((DoubleArray)(sTable.getPartition(0)).get()).get();
        for (int i = 0; i < local.length; ++i) {
            for (int j = r.start; j < r.end; ++j) {
                sum[j] += local[i][j];
                local[i][j] = 0.0;
            }
        }
        return null;
    }
}
