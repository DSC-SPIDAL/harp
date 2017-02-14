package edu.iu.mlr;

import java.util.*;

public class Instance {
    public int id;
    public HashMap<Integer,Double> term;

    public Instance(int id, HashMap<Integer,Double> term) {
        this.id = id;
        this.term = term;
    }
}
