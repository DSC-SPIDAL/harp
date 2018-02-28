package edu.iu.sahad.rotation;

import edu.iu.harp.keyval.Value;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ColorCountPairs  extends Value {
	protected static final Log LOG = LogFactory
			    .getLog(ColorCountPairs.class);
	
	private ArrayList<Integer> colors;
	private ArrayList<Double> counts;	
	// the data structure is (color0, count0) (color1, count1) ...
	
	public ColorCountPairs(){
		colors = new ArrayList<>();
		counts = new ArrayList<>();
	}
	
	public void addAPair(int color, double count){
		colors.add(color);
		counts.add(count);
	}
	public int getSize(){
		return this.colors.size();
	}
	
	public ArrayList<Integer> getColors() {
		return colors;
	}

	public void setColors(ArrayList<Integer> colors) {
		this.colors = colors;
	}

	public ArrayList<Double> getCounts() {
		return counts;
	}

	public void setCounts(ArrayList<Double> counts) {
		this.counts = counts;
	}

	public void copyTo(Value value){
		ArrayList<Integer> othercolors = ((ColorCountPairs) value).getColors();
		ArrayList<Double> othercounts = ((ColorCountPairs) value).getCounts();

		for(int i=0; i<this.colors.size(); i++){
			othercolors.add(this.colors.get(i));
			othercounts.add(this.counts.get(i));
		}
	}
	
	@Override
	public int getNumWriteBytes() {
		// TODO Auto-generated method stub
		// int type takes 4 bytes; double type takes 8 bytes
		// size = 4;  arraylist consists of int(4) and double(8)
		return 4 + this.colors.size() * 12;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		 out.writeInt(this.colors.size());
		 for(int i=0; i<this.colors.size(); i++) {
		      out.writeInt(this.colors.get(i));
		      out.writeDouble(this.counts.get(i));
		   }
	}

	@Override
	public void read(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		int size = in.readInt();
	    for (int i = 0; i < size; i++) {
	      this.colors.add(in.readInt());
	      this.counts.add(in.readDouble());
	    }
	}
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		this.colors.clear();
		this.counts.clear();
	}
}
