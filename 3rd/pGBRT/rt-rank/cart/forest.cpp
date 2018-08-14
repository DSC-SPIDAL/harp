//forest.cpp

#include "forest.h"


void randsample(const vector<tuple*>& data, vector<tuple*>& s) {
  int n = data.size(), i;
  for (i=0; i < n; i++)
    s.push_back(data[rand() % n]);
}

void single_forest(const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds, args_t& args) {
  int kfeatures = args.kfeatures;
  int num_test = args.num_test;
  int i;

  data_t sample;
  randsample(train, sample);

  dt_node* tree = new dt_node(sample, args, args.depth, 1, kfeatures, false, args);
  for (i=0; i < num_test; i++)
    double rmse = dt_node::classify_all(test[i], tree, test_preds[i], args);

  double TRrmse = dt_node::classify_all(train, tree, train_preds, args);

  delete tree;
}

void multiple_forest(int trees, const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds_seg, args_t& args) {
  for (int t = 0; t < trees; t++) {
    single_forest(train, test, train_preds, test_preds_seg, args);
    fprintf(stderr, "%d\n", t);
  }
}

void random_forest(const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds, args_t& args) {
  int trees = args.trees;
  int numthreads = args.processors;
  int i, t;
  
  for (t = 0; t < trees; t++)
    single_forest(train, test, train_preds, test_preds, args);

  // average results of the trees
  for (i = 0; i < test.size(); i++)
    for (int j = 0; j < test[i].size(); j++)
      test_preds[i][j] /= trees;

  for (i = 0; i < test.size(); i++) {
    tuple::write_to_file(test_preds[i], test[i], args.test_outs[i]);
  }
}


void random_forest_p(const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds, args_t& args) {
  int trees = args.trees;
  int numthreads=args.processors, i, x, t;
  int fk = args.kfeatures;
  if (numthreads > trees)
    numthreads = trees;
  trees = (trees / numthreads) * numthreads;

  int trees_per_thread = trees / numthreads;  
  thread** threads = new thread*[numthreads];

  vector< vec_preds_t > seg;
  vector< preds_t > train_seg;
  for (i=0; i < numthreads; i++) {
    vec_preds_t p;
    init_pred_vec(test, p);
    seg.push_back(p);

    preds_t tp;
    init_pred(train, tp);
    train_seg.push_back(tp);
  }
 
  for (i=0;i<numthreads;i++)
    threads[i] = new thread(bind(multiple_forest, trees_per_thread, cref(train), cref(test), ref(train_seg[i]), ref(seg[i]), ref(args)));
  
  for (i=0;i<numthreads;i++){
    threads[i]->join();
    delete threads[i];
  }
  fprintf(stderr, "done threading\n");
  delete[] threads;

  // congregate results of the threads
  for (i = 0; i < numthreads; i++)
    for (t = 0; t < test.size(); t++)
      for (int j = 0; j < test[t].size(); j++)
	test_preds[t][j] += seg[i][t][j];

  for (i = 0; i < numthreads; i++)
      for (int j = 0; j < train.size(); j++)
	train_preds[j] += train_seg[i][j];

  // average results of the trees
  for (i = 0; i < test.size(); i++)
    for (int j = 0; j < test[i].size(); j++)
      test_preds[i][j] /= trees;

  for (int j = 0; j < train.size(); j++)
    train_preds[j] /= trees;

  // write results to file
  for (i = 0; i < test.size(); i++) {
    tuple::write_to_file(test_preds[i], test[i], args.test_outs[i]);
  }
}


