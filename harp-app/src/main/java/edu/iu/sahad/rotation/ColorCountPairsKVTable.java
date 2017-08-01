package edu.iu.sahad.rotation;
import edu.iu.harp.keyval.Int2ValKVTable;

public class ColorCountPairsKVTable  extends
	Int2ValKVTable<ColorCountPairs, ColorCountPairsKVPartition> {
	
	public ColorCountPairsKVTable(int tableID) {
		    super( tableID, new ColorCountPairsCombiner(), 
		    		ColorCountPairs.class,ColorCountPairsKVPartition.class);		    
	}

}
