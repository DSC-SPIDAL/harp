//parse command line arguments
#ifndef AM_RT_ARGS_H
#define AM_RT_ARGS_H

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "tuple.h"
#include <vector>
using namespace std;

typedef vector<tuple*> data_t;
typedef vector< vector<tuple*> > vec_data_t;
typedef vector<double> preds_t;
typedef vector< vector<double> > vec_preds_t;

enum alg_t { ALG_BOOST, ALG_FOREST, ALG_REGRESSION };
enum alg_loss { ALG_SQUARED_LOSS, ALG_ENTROPY };
enum alg_pred { ALG_MEAN, ALG_MODE };

struct args_t
{
  int features;
  int trees;
  int processors;
  double alpha;
  int depth;
  int kfeatures;
  char* train_file;
  vector<char*> test_files;
  vector<char*> test_outs;
  int num_test;
  alg_t alg;
  int verbose;
  bool read_targets;
  bool read_weights;
  int rounds;
  alg_loss loss;
  alg_pred pred;
  int missing;
  char* missing_file;
  int ones;
  int readcosts;
  vector<float> fcosts;
  float fpenalty;
  int print_features;
  int restart_tree;
};

static void init_args(args_t& a) {
  a.processors = 1;
  a.trees = 1;
  a.alpha = 1.0;
  a.features = 701;
  a.depth = 1000;
  a.num_test = 1;
  a.kfeatures = -1;
  a.alg = ALG_REGRESSION;
  a.verbose = 0;
  a.rounds = 1;
  a.read_targets = false;
  a.read_weights = false;
  a.loss = ALG_SQUARED_LOSS;
  a.pred = ALG_MEAN;
  a.missing = 1;
  a.missing_file = NULL;
  a.ones = 0;
  a.readcosts=0;
  a.fpenalty=0;
  a.print_features=0;
  a.restart_tree=0;
}


static void init_pred_vec(const vec_data_t& test, vec_preds_t& preds) {
  for (int t = 0; t < test.size(); t++) {
    preds_t p;
    for (int i = 0; i < test[t].size(); i++)
      p.push_back(0);
    preds.push_back(p);
  }
}

static void init_pred(const data_t& train, preds_t& preds) {
  for (int t = 0; t < train.size(); t++) {
    preds.push_back(0);
  }
}


static int get_args(int argc, char* argv[], args_t& args) {
  int index, c, i=0;

  // option arguments
  opterr = 0;
  while ((c = getopt (argc, argv, "a:cd:ef:i:k:t:l:mop:r:svwzBFR")) != -1)
    switch (c) {
      case 'a': args.alpha = atof(optarg); break;
      case 'c': args.readcosts=1; break;
      case 'd':	args.depth = atoi(optarg); break;
      case 'e': args.loss = ALG_ENTROPY; break;
      case 'o': args.ones = 1; break;
      case 'l': args.fpenalty = atof(optarg); break;
      case 't':	args.trees = atoi(optarg); break;
      case 'p':	args.processors = atoi(optarg); break;
      case 'f':	args.features = atoi(optarg)+1; break;
      case 'k':	args.kfeatures = atoi(optarg); break;
      case 'r': args.rounds = atoi(optarg); 
	        args.read_targets = true; 
		break;
      case 's': args.print_features=1; break;
      case 'i': args.missing_file = optarg; break;
      case 'B': args.alg = ALG_BOOST; break;
      case 'F':	args.alg = ALG_FOREST; break;
      case 'R': args.alg = ALG_REGRESSION; break;
      case 'm': args.pred = ALG_MODE; break;
      case 'v': args.verbose = 1; break;
      case 'w': args.read_weights = true; break;
      case 'z': args.missing = 0; break;
      case '?':
	if (optopt == 'c')
	  fprintf (stderr, "Option -%c requires an argument.\n", optopt);
	else if (isprint (optopt))
	  fprintf (stderr, "Unknown option `-%c'.\n", optopt);
	else
	  fprintf (stderr, "Unknown option character `\\x%x'.\n", optopt);
	return 0;
      default:
	return 0;
      }

  // non option arguments
  if (argc-optind < 3)
    return 0;
  for (index = optind; index < argc; index++) {
    if (i==0) args.train_file = argv[index];
    else if (i%2) args.test_files.push_back(argv[index]);
    else args.test_outs.push_back(argv[index]);
    i++;
  }
  args.num_test = args.test_files.size();
  args.fcosts=vector<float>(args.features); //test

  return 1;
}

#endif
