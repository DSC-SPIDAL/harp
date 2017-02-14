package edu.iu.lda;

import java.util.List;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;

public class Utils {
	
	  public static double logAdd(double a, double b) {
		  if(a == Double.NEGATIVE_INFINITY && b == Double.NEGATIVE_INFINITY){
			  return a;
		  }else{
		  
		  if (a < b) {
		      return b + Math.log(1 + Math.exp(a - b));
		    } else {
		      return a + Math.log(1 + Math.exp(b - a));
		    }
		  }
	  }
	  
	  public static void printIntTable(List<OneDoc> docs) {
		  System.out.println(docs.getClass().getName());
		  for(int i=0; i< docs.size(); i++){
			  OneDoc doc = docs.get(i);
			  List<Integer> idlist = doc.idList;
			  List<Integer> occurlist = doc.occurrenceList;
			  System.out.println("Id=" + doc.docId +": ");
			  for(int j=0; j<idlist.size(); j++){
				  System.out.print(idlist.get(j)+":"+occurlist.get(j)+" ");
			  }
			  System.out.println();
		  }
	  }
	  public static void printIntTable(Table<IntArray> dataTable) {
		  for( Partition<IntArray> ap: dataTable.getPartitions()){
			  int res[] = ap.get().get();
			  System.out.print("ID: "+ap.id() + ":");
			  for(int i=0; i<res.length;i++)
				  System.out.print(res[i]+"\t");
			  System.out.println();
		  }
	  }
	 public static void printDoubleTable(Table<DoubleArray> dataTable) {
		  for( Partition<DoubleArray> ap: dataTable.getPartitions()){
			  double res[] = ap.get().get();
			  System.out.print("ID: "+ap.id() + ":");
			  for(int i=0; i<res.length;i++)
				  System.out.print(res[i]+"\t");
			  System.out.println();
		  }
	  }

}
