//main.cpp

#include "main.h"

#define REG
//#define FAST

int main(int argc, char* argv[]) {
  int i;
  srand(time(NULL));

  // get command line args
  args_t myargs;
  init_args(myargs);
  if (!get_args(argc, argv, myargs)) {
    printf("usage: [-options] train.txt test.txt output.txt [test.txt output.txt]*\n");
    return 0;
  }

  // load data from input files
  data_t train;
  vec_data_t test;
  if (!load_data(train, test, myargs)) {
    printf("could not load data files\n"); 
    return 0;
  }

  for (int round = 0; round < myargs.rounds; round++) 
  {
    // allocate memory for predictions
    preds_t train_preds;
    vec_preds_t test_preds;
    init_vec(train, train_preds, test, test_preds);

    // if enabled, get data from stdin
    if (myargs.read_targets)
      read_targets(train, myargs);
    if (myargs.read_weights)
      read_weights(train, myargs);
    if (myargs.readcosts)
      read_costs(myargs);
   
    // ***do algorithm*** //

    //fast_dt* fdt=new fast_dt(train,myargs);
    //return 0;
    //cout<<"---"<<endl;

    // single regression tree
    int N = train.size();
    if (myargs.alg == ALG_BOOST || myargs.alg == ALG_REGRESSION) {
      for (int T = 0; T < myargs.trees; T++) {
	// make tree
	#ifdef REG
	dt_node* t = new dt_node(train, myargs, myargs.depth, 1, myargs.kfeatures, false, myargs);
	#else
	fast_dt* t=new fast_dt(train,myargs);
	#endif

	// get classification on training data
	#ifdef REG
	double train_rmse = dt_node::classify_all(train, t, train_preds, myargs);
	#else
	double train_rmse = fast_dt::classify_all(train, t, train_preds, myargs);
	#endif

	// update targets
	for (i=0; i<N; i++)
	  train[i]->target = train[i]->label - train_preds[i];
	if (myargs.verbose) fprintf(stderr, "%d,%f", T, (float)train_rmse);
      
	// classify test data
	for (i=0; i<myargs.num_test; i++) {
	  #ifdef REG
	  double test_rmse = dt_node::classify_all(test[i], t, test_preds[i], myargs);
	  #else
	  double test_rmse = fast_dt::classify_all(test[i], t, test_preds[i], myargs);
	  #endif
	  // write output every 50 trees
	  if (T % 50 == 0)
	    tuple::write_to_file(test_preds[i], test[i], myargs.test_outs[i]);
	  if (myargs.verbose) fprintf(stderr, ",%f", (float)test_rmse);
	}      

	// if combining with python, print predictions of this tree to stdout
	if (myargs.read_targets)
	  for(i=0; i<N; i++)
	    cout << train_preds[i] << endl;
	if (myargs.read_targets)
	  for (i=0; i<myargs.num_test; i++)
	    for(int j = 0; j < test_preds[i].size(); j++)
	      cout << test_preds[i][j] << endl;
	if (myargs.print_features){
	  t->print_features();
	  cout<<endl;
	}
   
	// delete tree
	delete t;
	if (myargs.verbose) fprintf(stderr, "\n");
      }

      // write final predictions
      for (i=0; i<myargs.num_test; i++) {
	tuple::write_to_file(test_preds[i], test[i], myargs.test_outs[i]);
      }
    }

    // forest
    if (myargs.alg == ALG_FOREST) {
      random_forest_p(train, test, train_preds, test_preds, myargs);
      //cout << "rmse = " << rmse(test_preds[0], test[0]) << endl;
      fprintf(stderr, "rmse=%f\n", (float)(rmse(test_preds[0], test[0])));

	// if combining with python, print predictions of this tree to stdout
      if (myargs.read_targets) {
	for(i=0; i<N; i++)
	  cout << train_preds[i] << endl;
      	for (i=0; i<myargs.num_test; i++)
	  for(int j = 0; j < test_preds[i].size(); j++)
	    cout << test_preds[i][j] << endl;
      }
    }
  }

  // free memory and exit
  free_memory(myargs, train, test);
  return 0;
}
