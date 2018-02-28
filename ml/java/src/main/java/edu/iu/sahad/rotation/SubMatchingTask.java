package edu.iu.sahad.rotation;

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
public class SubMatchingTask implements Task<Partition<ColorCountPairsKVPartition>, Map<Integer, ColorCountPairs> > {
	private Table<IntArray> graphData;
	private ColorCountPairsKVTable passiveChild;
	public SubMatchingTask(Table<IntArray> graphData, ColorCountPairsKVTable passiveChild){
		this.graphData= graphData;
		this.passiveChild=passiveChild;
	}

	@Override
	public Map<Integer, ColorCountPairs> run(Partition<ColorCountPairsKVPartition> input) throws Exception {
		// TODO Auto-generated method stub
		Map<Integer, Double> colorCountMap;
		Partition<ColorCountPairsKVPartition> activeChild =  input;
		int key = activeChild.id();
		//get valuepairlist from activeChild

		ColorCountPairs activeValuelist = activeChild.get().getVal(key);
		ColorCountPairs passiveValuelist = null;
		
		colorCountMap = new HashMap<Integer, Double>();
		Partition<IntArray> graphpar= graphData.getPartition(key);
		for(int i = graphpar.get().start(); i < graphpar.get().size(); i++){
			int neighbor = graphpar.get().get()[i];
			passiveValuelist = passiveChild.getVal(neighbor);
			
			if(passiveValuelist == null){
				continue;
			}
			
			//compute the new result using these two valuepairlist
			for(int j = 0; j < activeValuelist.getColors().size(); j++){

				int activeColor =activeValuelist.getColors().get(j);
				double activeCount = activeValuelist.getCounts().get(j);
				for(int k =0; k < passiveValuelist.getColors().size(); k++){
					int passiveColor =  passiveValuelist.getColors().get(k);
					double passiveCount =passiveValuelist.getCounts().get(k);
					//System.out.println("key:"+key+", neighbor"+neighbor+"; color:"+activeColor +":"+activeCount+":"+passiveColor+":"+passiveCount);
					if ((activeColor & passiveColor) == 0)
		                  // color set intersection is empty
		             {
						int newColor = activeColor | passiveColor;
						if (!colorCountMap.containsKey(newColor)) {
							colorCountMap.put(new Integer(newColor), 
									new Double(activeCount * passiveCount));
		                } else {
		                	double count = colorCountMap.get(newColor);
		                    count += activeCount * passiveCount;
		                    colorCountMap.put(new Integer(newColor),
		                                    new Double(count));
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
	        	double count = colorCountMap.get(color);
	           // System.out.println(key+": "+color+":"+count+" ");
	            ccp.addAPair(color, count);
	        }
	        Map<Integer, ColorCountPairs> mp = new HashMap();
	         
	        mp.put(key, ccp);
	        return mp;
		}
	}

}
