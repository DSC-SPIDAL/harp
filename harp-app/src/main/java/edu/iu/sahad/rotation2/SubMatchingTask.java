package edu.iu.sahad.rotation2;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.Task;

import java.util.HashMap;
import java.util.Map;

/*
 * 20160525
 * change a little big.
 * From going through graphdata to going through activeChild. Save some search time.
 */
public class SubMatchingTask implements Task<Partition<ColorCountPairsKVPartition>, SubMatchingTask.SubMatchingTaskOutput> {
	private Table<IntArray> graphData;
	private ColorCountPairsKVTable passiveChild;
	public SubMatchingTask(Table<IntArray> graphData, ColorCountPairsKVTable passiveChild){
		this.graphData= graphData;
		this.passiveChild=passiveChild;
	}
	public void setPassiveChild(ColorCountPairsKVTable pChild){
		this.passiveChild = pChild;
	}

	@Override
	public SubMatchingTaskOutput run(Partition<ColorCountPairsKVPartition> input) throws Exception {
		// TODO Auto-generated method stub
		Map<Integer, Long> colorCountMap;
		Partition<ColorCountPairsKVPartition> activeChild =  input;
		int key = activeChild.id();
		//get valuepairlist from activeChild

		ColorCountPairs activeValuelist = activeChild.get().getVal(key);
		ColorCountPairs passiveValuelist = null;

		colorCountMap = new HashMap<Integer, Long>();
		Partition<IntArray> graphpar= graphData.getPartition(key);
		for(int i = graphpar.get().start(); i < graphpar.get().size(); i++){
			int neighbor = graphpar.get().get()[i];
			passiveValuelist = passiveChild.getVal(neighbor);

			if(passiveValuelist == null){
				continue;
			}

			//compute the new result using these two valuepairlist
			for(int j = 0; j < activeValuelist.getColors().size(); j++){

				int activeColor =activeValuelist.getColors().getInt(j);
				long activeCount = activeValuelist.getCounts().getLong(j);
				for(int k =0; k < passiveValuelist.getColors().size(); k++){
					int passiveColor =  passiveValuelist.getColors().getInt(k);
					long passiveCount = passiveValuelist.getCounts().getLong(k);
					//System.out.println("key:"+key+", neighbor"+neighbor+"; color:"+activeColor +":"+activeCount+":"+passiveColor+":"+passiveCount);
					if ((activeColor & passiveColor) == 0)
		                  // color set intersection is empty
		             {
						int newColor = activeColor | passiveColor;
						if (!colorCountMap.containsKey(newColor)) {
							colorCountMap.put(new Integer(newColor),
									new Long(activeCount * passiveCount));
		                } else {
		                	long count = colorCountMap.get(newColor);
		                    count += activeCount * passiveCount;
		                    colorCountMap.put(new Integer(newColor),
		                                    new Long(count));
		                    }
		             }
				}
			}
		 }

		if(colorCountMap.isEmpty()){
			//System.out.println("submatchingtask return null");
			return null;
		}else{

			ColorCountPairs ccp = new ColorCountPairs();
			// System.out.println("key: "+key+":");
	        for (int color : colorCountMap.keySet()) {
	        	long count = colorCountMap.get(color);
	           // System.out.println(key+": "+color+":"+count+" ");
	            ccp.addAPair(color, count);
	        }

	        return new SubMatchingTaskOutput(key, ccp);
		}
	}

	class SubMatchingTaskOutput{
		int key;
		ColorCountPairs colorCountPairs;
		public SubMatchingTaskOutput(int partitionId, ColorCountPairs ccp){
			this.key = partitionId;
			this.colorCountPairs = ccp;
		}
	}
}