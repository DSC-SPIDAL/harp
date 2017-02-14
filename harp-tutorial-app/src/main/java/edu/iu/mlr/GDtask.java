package edu.iu.mlr;

import java.util.*;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

public class GDtask implements Task<Partition, Object> {
    private double alpha;
    private HashMap<Integer, ArrayList<String>> qrels;
    private ArrayList<Instance> data;
    private ArrayList<String> topics;

    public GDtask(double A, ArrayList<Instance> D, ArrayList<String> T,
                  HashMap<Integer, ArrayList<String>> Q) {
        qrels = Q;
        alpha = A;
        data = D;
        topics = T;
    }

    @Override
    public Object run(Partition par) throws Exception {
        String cat = topics.get(par.id());
        double[] W = ((DoubleArray)par.get()).get();
        double p;
        double label;
        Instance inst;

        for (int i = 0; i < data.size(); ++i) {
            inst = data.get(i);
            p = predict(W, inst.term);
            if (qrels.get(inst.id).contains(cat))
                label = 1.0;
            else
                label = 0.0;

            W[0] += alpha * (label - p); // constant
            for(Map.Entry<Integer, Double> entry : inst.term.entrySet()) {
                int key = entry.getKey();
                double value = entry.getValue();

                W[key] += alpha * (label - p) * value;
            }
        }
        return null;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private static double predict(double W[], HashMap<Integer, Double> x) {
        double res = W[0]; // constant

        for(Map.Entry<Integer, Double> entry : x.entrySet()) {
            int key = entry.getKey();
            double value = entry.getValue();

            res += W[key] * value;
        }

        return sigmoid(res);
    }
}
