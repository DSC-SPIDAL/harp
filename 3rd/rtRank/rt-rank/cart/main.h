//main.h

#include "args.h"
#include "regression_tree.h"
#include "tuple.h"
#include "forest.h"
#include "epoch.h"
#include "fast_rt.h"

#include <iostream>
#include <vector>
#include <cmath>
using namespace std;

#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>
using namespace boost;

// given a set of files, read them into train and test vectors
int load_data(vector<tuple*>& train, vector< vector<tuple*> >& test, const args_t& myargs) {
  int numfeatures = myargs.features;
  int missing = myargs.missing;
  char* missing_file = myargs.missing_file;

  //zeros->missing=0, missing=1, ones=-1
  int m=myargs.ones ? -1 : myargs.missing;

  fprintf(stderr, "loading training data...");
  if (!tuple::read_input(train, myargs.train_file, numfeatures, 1, m, missing_file))
    return 0;
  fprintf(stderr, "done\n");
  fprintf(stderr, "loading test data...");
  for (int i=0; i<myargs.num_test; i++) {
    vector<tuple*> t;
    if (!tuple::read_input(t, myargs.test_files[i], numfeatures, 1, m, missing_file))
      return 0;
    test.push_back(t);
  }
  fprintf(stderr, "done\n");
  return 1;
}

// read targets from stdin, assign them to the respective tuple
void read_targets(vector<tuple*>& train, const args_t& myargs) {
  int N = train.size(), i;
  int r = myargs.read_targets;
  for (i=0; i < N; i++) {
    double t;
    cin >> t;
    train[i]->set_target(t);
    train[i]->label = t;
  }
}

void read_weights(vector<tuple*>& train, const args_t& myargs) {
  int i, N=train.size();
  for (i=0; i<N; i++) {
    double w; cin >> w;
    train[i]->weight = w;
  }
}

void read_costs(args_t& myargs){
  int i,f = myargs.features;
  //cerr<<"reading "<<f<<" costs"<<endl;
  //cerr<<"read: ";
  for(i=0;i<f;i++){
    double cost;cin>>cost;
    myargs.fcosts[i]=cost;
    //cerr<<myargs.fcosts[i]<<" ";
  }
  //cerr<<endl;
}

// free all the memory we used up
void free_memory(const args_t& myargs, vector<tuple*>& train, vector< vector<tuple*> >& test) {
  int i;
  tuple::delete_data(train);
  for (i=0; i < myargs.num_test; i++)
    tuple::delete_data(test[i]);
}


// given preds and labels, get rmse
double rmse(const vector<double>& preds, const vector<tuple*>& data) {
  double r = 0;
  int i, N = data.size();
  for (i=0; i<N; i++)
    r += squared(data[i]->label - preds[i]); 
  return sqrt(1.0 / N * r);
}

// init vectors to 0
void init_vec(const vector<tuple*>& train, vector<double>& train_preds, const vector< vector<tuple*> >& test, vector< vector<double> >& test_preds) {
  int i;
  for (i = 0; i < test.size(); i++) {
    vector<double> p;
    for (int j = 0; j < test[i].size(); j++)
      p.push_back(0.0);
    test_preds.push_back(p);
  }
  for (i = 0; i < train.size(); i++)
    train_preds.push_back(0.0);
}

/*void init_pred_vec(const vec_data_t& test, vec_preds_t& preds) {
  for (int t = 0; t < test.size(); t++) {
    preds_t p;
    for (int i = 0; i < test[t].size(); i++)
      p.push_back(0);
    preds.push_back(p);
  }
  }*/
