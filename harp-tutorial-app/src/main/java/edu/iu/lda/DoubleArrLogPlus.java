package edu.iu.lda;

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.resource.DoubleArray;
import edu.umd.cloud9.math.LogMath;

public class DoubleArrLogPlus  extends
PartitionCombiner<DoubleArray> {

	@Override
	public PartitionStatus combine(DoubleArray curPar, DoubleArray newPar) {
		double[] doubles1 = curPar.get();
	    int size1 = curPar.size();
	    double[] doubles2 = newPar.get();
	    int size2 = newPar.size();
	    if (size1 != size2) {
	      // throw new Exception("size1: " + size1
	      // + ", size2: " + size2);
	      return PartitionStatus.COMBINE_FAILED;
	    }
	    for (int i = 0; i < size2; i++) {
	    	doubles1[i] = Utils.logAdd(doubles1[i] , doubles2[i]);
	    }
	    return PartitionStatus.COMBINED;
	    
	}
	

}
