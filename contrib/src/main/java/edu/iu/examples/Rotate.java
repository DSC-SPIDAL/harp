package edu.iu.examples;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;

public class Rotate extends CollectiveMapper<String, String, Object, Object> {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    double[] values = new double[10000];
    Int2IntMap rotate = new Int2IntArrayMap();
    for (int i = 0; i < 100; i++) {
      Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, 10000)));
      rotate("rotate", "rotate", mseTable, rotate);
    }
  }
}
