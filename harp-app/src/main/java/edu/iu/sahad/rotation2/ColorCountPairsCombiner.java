package edu.iu.sahad.rotation2;
import edu.iu.harp.keyval.ValCombiner;
import edu.iu.harp.keyval.ValStatus;

public class ColorCountPairsCombiner extends ValCombiner<ColorCountPairs> {

	@Override
	public ValStatus combine(ColorCountPairs curVal, ColorCountPairs newVal) {
		boolean flag=false;// curVal doesn't exists the same color 
		for(int j = 0; j<newVal.getColors().size();j++){
			flag = false;
			for(int i=0; i<curVal.getColors().size(); i++){
				if(curVal.getColors().get(i) ==  newVal.getColors().get(j)){
					curVal.getCounts().set(i, curVal.getCounts().get(i) + newVal.getCounts().get(j));
					flag = true;
					break;
				}
			}
			
			if(flag == false){
				curVal.getColors().add(newVal.getColors().get(j));
				curVal.getCounts().add(newVal.getCounts().get(j));
			}
		}
		
		return ValStatus.COMBINED;
	}
}