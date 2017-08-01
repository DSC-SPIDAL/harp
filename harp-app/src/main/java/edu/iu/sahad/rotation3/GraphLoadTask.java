package edu.iu.sahad.rotation3;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GraphLoadTask implements Task<String, ArrayList<Partition<IntArray>>>{
	protected static final Log LOG = LogFactory
		    .getLog(GraphLoadTask.class);
	 private Configuration conf;

	 public GraphLoadTask(Configuration conf){
		 this.conf = conf;
	 }
	 
	@Override
	public ArrayList<Partition<IntArray>> run(String input) throws Exception {
		// TODO Auto-generated method stub
		 String fileName = (String) input;
		 ArrayList<Partition<IntArray>> partialGraphDataList = new ArrayList<Partition<IntArray>>();
		 Path pointFilePath = new Path(fileName);
		 FileSystem fs =pointFilePath.getFileSystem(conf);
		 FSDataInputStream in = fs.open(pointFilePath);
		 BufferedReader br  = new BufferedReader(new InputStreamReader(in));
		 try {
		      String line ="";
		      while((line=br.readLine())!=null){
		          line = line.trim();
			  String splits[] = line.split("\\s+");
			  String keyText = splits[0];
			  int key = Integer.parseInt(keyText);
		
			  if( splits.length == 2){
			  	String valueText = splits[1];
		      
				String[] itr = valueText.split(",");
				int length = itr.length;
				int[] intValues = new int[length];
				for(int i=0; i< length; i++){
				    intValues[i]= Integer.parseInt(itr[i]);
			        }
				 Partition<IntArray> partialgraph = new Partition<IntArray>(key, new IntArray(intValues, 0, length));
				 partialGraphDataList.add(partialgraph);
			 }
		      }
		  } finally {
		      in.close();
		 }
		return partialGraphDataList;
	}

}
