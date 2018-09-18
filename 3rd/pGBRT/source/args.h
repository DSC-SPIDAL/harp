/*
Copyright (c) 2011, Washington University in St. Louis
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY 
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#ifndef ARGS_H
#define ARGS_H

#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <ostream>
#include <iostream>

using namespace std;

struct args_t {
    // processors
    int myid;
    int numProcs;
    bool isRoot;
    
	// data sets
	const char *trainFile; int sizeTrainFile;
	const char *validFile; bool useValidSet; int sizeValidFile;
	const char *testFile; bool useTestSet; int sizeTestFile;
	
	// data set attributes
	int numFeatures;
    
    // binning
    // bool preBinned;
    // int numBins;
    // bool useRebinning; int rebinningFreq;
	
	// ranking metrics
	bool computeRankingMetrics;

	// trees/boosting
	int maxDepth;
	int numTrees;
	double learningRate;
	
	// subsampling
    // bool useFriedmanSubsampling; float friedmanSubsampleRate;
    
    // timing
    bool time;
};

static void print_help() {
    printf(
        "Usage: pgboost TRAIN_FILE TRAIN_SIZE N_FEATURES DEPTH N_TREES RATE [OPTIONS]\n"
        "Trains gradient boosted regression tree ensembles.\n\n"
        
        "TRAIN_FILE  training file\n"
        "TRAIN_SIZE  number of instances in training file\n"
        "N_FEATURES  number of features in data sets\n"
        "DEPTH       maximum regression tree depth\n"
        "N_TREES     number of regression trees to learn\n"
        "RATE        learning rate/stepsize\n\n"
        
        "-V, -E      validation/testing files\n"
        "-v, -e      number of instances in validation/testing files\n"
        "-m          compute and print ranking-specific metrics, ERR and NDCG\n"
        "-t          print timing information (\"#timer EVENT ELAPSED_TIME\")\n"
        "-h          show help message\n\n"
        
        "Required input includes a training data set (filename, size, number of \n"
        "features) in SVM^rank or SVM^light format and gradient boosting parameters\n"
        "(tree depth, number of trees, learning rate). Output alternates by line\n"
        "between a depth first traversal of the current regression tree and current\n"
        "metrics computed on data sets.\n\n"
        
        "Version 0.9, August 2011. For additional documentation and the latest release,\n"
        "please visit <http://sites.google.com/site/pgbrt>. Report bugs to \n"
        "<swtyree@wustl.edu>.\n"
    );
}

static void initialize_args(args_t& a) {
    // processors
    a.myid = -1;
    a.numProcs = -1;
    a.isRoot = false;
    
    // data sets
	a.trainFile = NULL; a.sizeTrainFile = 0;
	a.validFile = NULL; a.useValidSet = false; a.sizeValidFile = 0;
    a.testFile = NULL; a.useTestSet = false; a.sizeTestFile = 0;
    
    // data set attributes
    a.numFeatures = -1;
    
    // binning
    // a.preBinned = false;
    // a.numBins = -1;
    // a.useRebinning = false; a.rebinningFreq = -1;
	
	// ranking metrics
    a.computeRankingMetrics = false;
	
	// trees/boosting
	a.maxDepth = -1;
	a.numTrees = -1;
	a.learningRate = -1.;
	
	// subsampling
    // a.useFriedmanSubsampling = false; a.friedmanSubsampleRate = -1.0f;
    
    // timing
    a.time = false;
}

static bool get_args(int argc, char* argv[], args_t& args, int myid, int numProcs) {
    // set processor info
    args.myid = myid;
    args.numProcs = numProcs;
    args.isRoot = (myid == 0);
    
    // parse options
	int c; opterr = false;
	while ((c = getopt (argc, argv, "V:v:E:e:rmth")) != -1) {
	switch (c) {
	    // data sets
        // case 'T':   args.trainFile = optarg; break;
        // case 't':   args.sizeTrainFile = atoi(optarg); break;
	    case 'V':   args.validFile = optarg; args.useValidSet = true; break;
        case 'v':	args.sizeValidFile = atoi(optarg);
                    if (args.sizeValidFile <= 0) {
                        if (args.isRoot) fprintf(stderr, "Validation file size should be a positive integer: %s\n", optarg); 
                        return false;
                    }
                    break;
        case 'E':   args.testFile = optarg; args.useTestSet = true; break;
	    case 'e':	args.sizeTestFile = atoi(optarg);
            	    if (args.sizeTestFile <= 0) {
                        if (args.isRoot) fprintf(stderr, "Test file size should be a positive integer: %s\n", optarg); 
                        return false;
                    }
                    break;
	    
	    // data set attributes
        // case 'f':   args.numFeatures = atoi(optarg); break;
        // case 'r':    args.isRankingSet = true; break;
	    
	    // binning
        // case 'p':   args.preBinned = true; break;
        // case 'B':   args.numBins = atoi(optarg); break;
        // case 'b':   args.useRebinning = true; args.rebinningFreq = atoi(optarg); break;
	    
	    // ranking metrics
	    case 'm':	args.computeRankingMetrics = true; break;
		
	    // trees/boosting
        // case 'd':   args.maxDepth = atoi(optarg); break;
        // case 'n':   args.numTrees = atoi(optarg); break;
        // case 'a':    args.learningRate = atof(optarg); break;
	    
	    // subsampling
        // case 's':   args.useFriedmanSubsampling = true; args.friedmanSubsampleRate = atof(optarg); break;
        
        // timing
        case 't':   args.time = true;
                    break;
        
        // help
        case 'h':   return false;
        
	    // unknown / invalid
        case '?':
            switch (optopt) {
                case 'V':
                case 'v':
                case 'E':
                case 'e':
                    if (args.isRoot) fprintf (stderr, "Option -%c requires an argument.\n", optopt);
                    return false;
            }
		    if (isprint (optopt)) // a printable character
		        if (args.isRoot) fprintf(stderr, "Unknown option '-%c'.\n", optopt);
		    else // an unprintable character
		        if (args.isRoot) fprintf(stderr, "Unknown option character `\\x%x'.\n", optopt);
		    return false;
		
		// unmatched option or other error (shouldn't occur)
	    default:
	        if (isprint (c)) // a printable character
		        if (args.isRoot) fprintf(stderr, "Unknown option '-%c'.\n", c);
		    else // an unprintable character
		        if (args.isRoot) fprintf(stderr, "Unknown option character `\\x%x'.\n", c);
	        return false;
	} }
	
	// check non-option arguments
	if (argc-optind > 6) {
        if (args.isRoot) fprintf(stderr, "Too many arguments.\n");
        return false;
    } else if (argc-optind < 6) {
        if (args.isRoot) fprintf(stderr, "Too few arguments.\n");
        return false;
    }
    
    // record non-option arguments
    args.trainFile = argv[optind];
    
    args.sizeTrainFile = atoi(argv[optind+1]);
    if (args.sizeTrainFile <= 0) {
        if (args.isRoot) fprintf(stderr, "Training file size should be a positive integer: %s\n", argv[optind+1]); 
        return false;
    }
    
    args.numFeatures = atoi(argv[optind+2]);
    if (args.numFeatures <= 0) {
        if (args.isRoot) fprintf(stderr, "Number of features should be a positive integer: %s\n", argv[optind+2]); 
        return false;
    } else if (args.numProcs > args.numFeatures) {
        if (args.isRoot) fprintf(stderr, "Number of processors (%d) should not exceed the number of features (%d)\n", args.numProcs, args.numFeatures); 
        return false;
    }
    
    args.maxDepth = atoi(argv[optind+3]);
    if (args.maxDepth <= 0) {
        if (args.isRoot) fprintf(stderr, "Maximum tree depth should be a positive integer: %s\n", argv[optind+3]); 
        return false;
    }
    
    args.numTrees = atoi(argv[optind+4]);
    if (args.numTrees <= 0) {
        if (args.isRoot) fprintf(stderr, "Number of trees should be a positive integer: %s\n", argv[optind+4]); 
        return false;
    }
    
    args.learningRate = atof(argv[optind+5]);
    if (args.learningRate <= 0.) {
        if (args.isRoot) fprintf(stderr, "Learning rate should be a positive number: %s\n", argv[optind+5]); 
        return false;
    }
    
	// return
	return true;
}

static void print_args(args_t& args) {
	cout << "#**************Configuration**************" << endl;
	cout << "#Number of processors: " << args.numProcs << endl;
	
	// data sets
	cout << "#Training file: " << args.trainFile << " (" << args.sizeTrainFile << " lines)" << endl;
	cout << "#Validation file: " << (args.useValidSet ? args.validFile : "(none)") << " (" << args.sizeValidFile << " lines)" << endl;
	cout << "#Test file: " << (args.useTestSet ? args.testFile : "(none)") << " (" << args.sizeTestFile << " lines)" << endl;
	cout << "#Number of features: " << args.numFeatures << endl;
	
    // trees/boosting
	cout << "#Maximum depth: ";
		cout << args.maxDepth << endl;
	cout << "#Number of trees: " << args.numTrees << endl;
	cout << "#Learning rate: " << args.learningRate << endl;
    
	cout << "#*****************************************" << endl;
}

#endif
