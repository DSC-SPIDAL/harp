package edu.iu.svm;

import java.util.StringTokenizer;
import java.util.HashSet;
import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;

public class HarpStringPlus
  extends PartitionCombiner<HarpString> {
  //@Override
  //public PartitionStatus combine(
  //  HarpString curPar, HarpString newPar) {
  //  String s1 = curPar.get();
  //  String s2 = newPar.get();
  //  s1 += s2;
  //  curPar.set(s1);
  //  return PartitionStatus.COMBINED;
  //}
  @Override
  public PartitionStatus combine(
    HarpString curPar, HarpString newPar) {
 
      HashSet<String> supportVectors = new HashSet<String>();
      String[] svString = curPar.get().split("\n");
      for (String line : svString) {
        if (!supportVectors.contains(line)) {
          supportVectors.add(line);
        }
      }
      svString = newPar.get().split("\n");
      for (String line : svString) {
        if (!supportVectors.contains(line)) {
          supportVectors.add(line);
        }
      }

	  StringBuilder sb = new StringBuilder(512*1024);
      for (String line : supportVectors) {
		  sb.append(line);
		  sb.append("\n");
	  }
	  String s1 = sb.toString();	

    curPar.set(s1);
    return PartitionStatus.COMBINED;
  }
 

}
