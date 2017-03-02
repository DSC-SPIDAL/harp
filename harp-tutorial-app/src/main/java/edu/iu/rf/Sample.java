package edu.iu.rf;

import java.util.*;

public class Sample {
	public int label;
	public ArrayList<Float> features;

	public Sample(int label, ArrayList<Float> features) {
		this.label = label;
		this.features = features;
	}
}