package edu.iu.sahad.rotation3;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.schstatic.StaticScheduler;
import edu.iu.harp.schstatic.Task;

public class CloneTask extends Task<Partition<ColorCountPairsKVPartition>,  CloneTask.CloneTaskOutput> {

	public CloneTask( ){
	}

	@Override
	public CloneTaskOutput run(Partition<ColorCountPairsKVPartition> input) throws Exception {
		int key = input.id();
		ColorCountPairs ccp = input.get().getVal(key);
		ColorCountPairs newccp = new ColorCountPairs();
		ccp.copyTo(newccp);
		return new CloneTaskOutput(key, newccp);
	}
	class CloneTaskOutput{
		int key;
		ColorCountPairs colorCountPairs;
		public CloneTaskOutput(int key, ColorCountPairs ccp){
			this.key = key;
			colorCountPairs = ccp;
		}

	}
}