package edu.iu.sahad.rotation3;

import edu.iu.harp.schdynamic.Task;


public class AggregationTask implements Task<ColorCountPairs,  Long> {

	public AggregationTask( ){
	}

	@Override
	public Long run(ColorCountPairs input) throws Exception {
		long count = 0;
		ColorCountPairs ccp = input;
		for(int i = 0; i< ccp.getCounts().size(); i++){
			count +=  ccp.getCounts().getLong(i);
		}
		return count;
	}
}