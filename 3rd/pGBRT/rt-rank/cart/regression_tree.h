#ifndef AM_DECISION_TREE_ML_H
#define AM_DECISION_TREE_ML_H

#include "tuple.h"
#include "args.h"

#include <map>
using namespace std;

double static mymax(double a, double b) {return a > b ? a : b; }
double static mymin(double a, double b) {return a < b ? a : b; }
inline double static squared(double a) {return a * a; }

class dt_node
{
public:
  enum {YES, NO, MISSING, CHILDTYPES};

  dt_node* child[CHILDTYPES];
  int feature;
  double value; // feature and value this node splits on
  bool leaf;
  double pred; // what this node predicts if leaf node

  args_t& args;

  dt_node(const vector<tuple*>& data, args_t& myargs, int maxdepth, int depth, int fk, bool par, args_t& margs) : leaf(0), feature(0), value(UNKNOWN), pred(-1), args(myargs){
    
    //printf("starting node: size %d\n", data.size());
    
    // initialize fields
    int i;
    for (i = 0; i < CHILDTYPES; i++)
      child[i] = 0;
    
    // get prediction
    pred = (args.pred == ALG_MEAN) ? average(data) : mode(data);

    // check if leaf node
    if (depth == maxdepth || same(data)) {
      leaf = true;
      return;
    }
 
    // find criterion to split data, if cant make this leaf node
    int f_split; double v_split;
    if (!find_split(data, myargs.features, f_split, v_split, fk, par, args)) {
      leaf = true;
      return;   
    }

    // split data into 3 parts, based on criteria found
    vector<tuple*> child_data[CHILDTYPES];
    split_data(data, child_data, f_split, v_split, args);
    //printf("split: %d %f, Y:%d N:%d  M:%d\n", f_split, (float)v_split, child_data[YES].size(), child_data[NO].size(), child_data[MISSING].size());

    if (!(child_data[YES].size() && child_data[NO].size())) {
      leaf = true;
      return;
    }

    // remember where we splitted, and recurse
    feature = f_split;
    value = v_split;
    child[YES] = new dt_node(child_data[YES], myargs, maxdepth, depth+1, fk, par, args);
    child[NO] = new dt_node(child_data[NO], myargs, maxdepth, depth+1, fk, par, args);

    // update cost for feature chosen to split on
    args.fcosts[feature]=0;

    if (child_data[MISSING].size())
      child[MISSING] = new dt_node(child_data[MISSING], myargs, maxdepth, depth+1, fk, par, args);
    else
      child[MISSING] = 0;
  }
 
  static bool find_split(vector<tuple*> data, int NF, int& fsplit, double& vsplit, int K, bool par, const args_t& args);
  static double classify(const tuple* const instance, const dt_node* const dt);
  static double boosted_classify(const vector< dt_node* >&, const tuple* const, double alpha);
  static bool entropy_split(data_t data, int NF, int& fsplit, double& vsplit, int K, bool par);

  // input: vector of tuples (data), children vector, feature and value to split on
  // output: split tuples in data into appropriate children vectors
  static void split_data(const vector<tuple*>& data, vector<tuple*> child[CHILDTYPES], int f, double v, const args_t& args) {
    int n = data.size(), i;

    for (i = 0; i < CHILDTYPES; i++)
      while(child[i].size())
	child[i].pop_back();

    if(0)
    for (i = 0; i < n; i++) {
      if (data[i]->features[f] == UNKNOWN)
	child[MISSING].push_back(data[i]);
      else if (data[i]->features[f] <= v)
	child[YES].push_back(data[i]);
      else
	child[NO].push_back(data[i]);
    }


    if (args.ones==1){
      for (i = 0; i < n; i++) 
	if (data[i]->features[f] == UNKNOWN)
	  //child[MISSING].push_back(data[i]);
	  child[NO].push_back(data[i]);
	else if (data[i]->features[f] == 0)
	  child[NO].push_back(data[i]);
	else if (data[i]->features[f] <= v)
	  child[YES].push_back(data[i]);
	else
	  child[NO].push_back(data[i]);
    }
    else if (args.missing==1){
      for (i = 0; i < n; i++)
	if (data[i]->features[f] == UNKNOWN || data[i]->features[f]==0)
	  child[MISSING].push_back(data[i]);
	else if (data[i]->features[f] <= v)
	  child[YES].push_back(data[i]);
	else
	    child[NO].push_back(data[i]);
    }
    else
      for (i = 0; i < n; i++)
	if (data[i]->features[f] == UNKNOWN)
	  child[MISSING].push_back(data[i]);
	else if (data[i]->features[f] == 0)
	  child[YES].push_back(data[i]);
	else if (data[i]->features[f] <= v)
	  child[YES].push_back(data[i]);
	else
	  child[NO].push_back(data[i]);
  }

  // destructor: free up allocated memory
  ~dt_node() { 
    if (!leaf) {
      delete child[YES], delete child[NO]; 
      if (child[MISSING] != 0)
	delete child[MISSING];
    }
  }

  // input: vector of tuples
  // output: bool indicating if all instances in data share same label
  static bool same(const vector<tuple*>& d) {
    double first = d[0]->target;
    for (int i = 1; i < d.size(); i++)
      if (d[i]->target != first)
      //if (abs(d[i]->label - first) > 0.00001)
	return false;
    return true;
  }

  // input: list of tuples
  // output: average of classes
  static double average(const vector<tuple*>& data) {
    double sum = 0.0;
    int n = data.size();
    for (int i = 0; i < n; i++)
      sum += data[i]->target * 1.0;
    return sum * 1.0 / n;
  }

  static double mode(const vector<tuple*>& data) {
    double best, max_count = -1;
    map<double, int> count;
    for (int i = 0; i < data.size(); i++) {
      double t = data[i]->target;
      count[t]++;
      int t_count = count[t];
      if (t_count > max_count)
	max_count = t_count, best = t;
    }
    return best;
  }

  // classify test points on tree, return rmse
  static double classify_all(const data_t& test, const dt_node* const tree, preds_t& preds, const args_t& args) {
    int i, N = test.size();
    double r = 0.0;
    for (i=0; i<N; i++) {
      preds[i] += args.alpha * dt_node::classify(test[i], tree);
      r += squared(test[i]->label - preds[i]);
    }
    return sqrt(1.0 / N * r);
  }

  // print features the tree has split on
  void print_features(){
    cout<<feature<<" ";
    if(child[YES]!=0)child[YES]->print_features();else cout<<"X ";
    if(child[NO]!=0)child[NO]->print_features();else cout<<"X ";
  }

};

#endif //AM_DECISION_TREE_ML_H
