package edu.iu.sahad.rotation;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ColorTask implements Task<Partition<IntArray>,  Map<Integer, ColorCountPairs>> {
	private int numColor;
	private  Random rand;
	public ColorTask( int numColor, Random rand ){
		this.rand=rand;
		this.numColor=numColor;
	}

	@Override
	public Map<Integer, ColorCountPairs> run(Partition<IntArray> input) throws Exception {
		// TODO Auto-generated method stub
		Partition<IntArray> graphPartition = (Partition<IntArray>) input;
		int partitionID = graphPartition.id();
		
		int colorBit = SCUtils.power(2, rand.nextInt(numColor));
		//int colorBit = SCUtils.power(2, partitionID % numColor );
		
		ColorCountPairs ccp = new ColorCountPairs();
		ccp.addAPair(colorBit, 1);
		
		 Map<Integer, ColorCountPairs> mp = new HashMap();
         
	     mp.put(partitionID, ccp);
	     return mp;
	}
}
