package edu.iu.sgd;

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.resource.LongArray;

public class LongArrMax extends
  PartitionCombiner<LongArray> {

  @Override
  public PartitionStatus combine(
    LongArray curPar, LongArray newPar) {
    long[] longs1 = curPar.get();
    int size1 = curPar.size();
    long[] longs2 = newPar.get();
    int size2 = newPar.size();
    if (size1 != size2) {
      return PartitionStatus.COMBINE_FAILED;
    }
    for (int i = 0; i < size1; i++) {
      if (longs1[i] < longs2[i]) {
        longs1[i] = longs2[i];
      }
    }
    return PartitionStatus.COMBINED;
  }
}
