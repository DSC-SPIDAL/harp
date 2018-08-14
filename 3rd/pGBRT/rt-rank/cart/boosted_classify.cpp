#include "regression_tree.h"

// input:  a tuple, and decision tree
// output: class guess for the tuple based on the dt
double dt_node::classify(const tuple* const instance, const dt_node* const dt) {
  if (dt->leaf)
    return dt->pred;
  int f = dt->feature;
  double v = dt->value;
  if (instance->features[f] == UNKNOWN)
    if (dt->child[MISSING] == 0)
      return dt->pred;
    else
      return classify(instance, dt->child[MISSING]);
  else
    if (instance->features[f] <= v) 
      return classify(instance, dt->child[YES]);
    else
      return classify(instance, dt->child[NO]);
  return 0;
}

// input: a list of boosted decision trees, and a data instance
// output: the boosted classifer of that instance
double dt_node::boosted_classify(const vector<dt_node*>& bdt, const tuple* const instance, double alpha) {
  double r = 0.0;
  int nt = bdt.size();
  for (int i = 0; i < nt; i++) {
      r = r + alpha*classify(instance,bdt[i]);
  }
  return r;
}
