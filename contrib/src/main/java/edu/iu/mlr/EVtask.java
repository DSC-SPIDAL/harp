package edu.iu.mlr;

import java.util.*;

import edu.iu.harp.partition.Table;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

public class EVtask
  implements Task<Partition, Object> {
  private Table<DoubleArray> wTable;
  private HashMap<Integer, ArrayList<String>> qrels;
  private ArrayList<Instance> data;
  private ArrayList<String> topics;

  public EVtask(Table<DoubleArray> A, ArrayList<Instance> D,
    ArrayList<String> T,
    HashMap<Integer, ArrayList<String>> Q) {
    qrels = Q;
    wTable = A;
    data = D;
    topics = T;
  }

  @Override
  public Object run(Partition par)
    throws Exception {
    String cat = topics.get(par.id());
    double[] cm = ((DoubleArray) par.get()).get();
    double[] W = ((DoubleArray) (wTable.getPartition(par.id())).get()).get();
    double p;
    double label;
    Instance inst;

    //cm[] as tp, fn, fp, tn
    for (int i = 0; i < data.size(); ++i) {
      inst = data.get(i);
      p = predict(W, inst.term);
      if (qrels.get(inst.id).contains(cat)){
        label = 1.0;
        if (p > 0.5){
            cm[0]++;
        }
        else{
            cm[1]++;
        }
      }   
      else{
        label = 0.0;
        if (p > 0.5){
            cm[2]++;
        }
        else{
            cm[3]++;
        }
      }
    }
    return null;
  }

  private static double sigmoid(double z) {
    return 1.0 / (1.0 + Math.exp(-z));
  }

  private static double predict(double W[],
    HashMap<Integer, Double> x) {
    double res = W[0]; // constant

    for (Map.Entry<Integer, Double> entry : x
      .entrySet()) {
      int key = entry.getKey();
      double value = entry.getValue();

      res += W[key] * value;
    }

    return sigmoid(res);
  }
}
