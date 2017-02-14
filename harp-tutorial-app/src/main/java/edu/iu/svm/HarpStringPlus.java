package edu.iu.svm;

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;

public class HarpStringPlus extends PartitionCombiner<HarpString> {
	@Override
	public PartitionStatus combine(HarpString curPar, HarpString newPar) {
		String s1 = curPar.get();
		String s2 = newPar.get();
		s1 += s2;
		curPar.set(s1);
		return PartitionStatus.COMBINED;
	}
}