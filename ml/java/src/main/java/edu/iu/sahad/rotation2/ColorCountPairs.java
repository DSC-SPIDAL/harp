package edu.iu.sahad.rotation2;

import edu.iu.harp.keyval.Value;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ColorCountPairs  extends Value {
	protected static final Log LOG = LogFactory
			    .getLog(ColorCountPairs.class);
	
	private IntArrayList colors;
	private LongArrayList counts;
	// the data structure is (color0, count0) (color1, count1) ...
	
	public ColorCountPairs(){
		colors = new IntArrayList();
		counts = new LongArrayList();
	}
	
	public void addAPair(int color, long count){
		colors.add(color);
		counts.add(count);
	}
	public int getSize(){
		return this.colors.size();
	}
	
	public IntArrayList getColors() {
		return colors;
	}

	public void setColors(IntArrayList colors) {
		this.colors = colors;
	}

	public LongArrayList getCounts() {
		return counts;
	}

	public void setCounts(LongArrayList counts) {
		this.counts = counts;
	}

	public void copyTo(Value value){
		IntArrayList othercolors = ((ColorCountPairs) value).getColors();
		LongArrayList othercounts = ((ColorCountPairs) value).getCounts();

		for(int i=0; i<this.colors.size(); i++){
			othercolors.add(this.colors.getInt(i));
			othercounts.add(this.counts.getLong(i));
		}
	}
	
	@Override
	public int getNumWriteBytes() {
		// TODO Auto-generated method stub
		// int type takes 4 bytes; long type takes 8 bytes
		// size = 4;  arraylist consists of int(4) and long(8)
		return 4 + this.colors.size() * 12;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		 out.writeInt(this.colors.size());
		 for(int i=0; i<this.colors.size(); i++) {
		      out.writeInt(this.colors.getInt(i));
		      out.writeLong(this.counts.getLong(i));
		   }
	}

	@Override
	public void read(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		int size = in.readInt();
	    for (int i = 0; i < size; i++) {
	      this.colors.add(in.readInt());
	      this.counts.add(in.readLong());
	    }
	}
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		this.colors.clear();
		this.counts.clear();
	}
}
