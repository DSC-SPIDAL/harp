package edu.iu.svm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.iu.harp.resource.Writable;

public class HarpString extends Writable {
	public String s;

	public String get() {
		return s;
	}

	public void set(String s1) {
		s = s1;
	}

	@Override
  	public void write(DataOutput out) throws IOException {
    	out.writeUTF(s);
  	}

  	@Override
  	public void read(DataInput in) throws IOException {
    	s = in.readUTF();
  	}
	
	@Override
  	public int getNumWriteBytes() {
    	return 4 + 2 * s.length();
  	}

  	@Override
  	public void clear() {
  		s = null;
  	}
}