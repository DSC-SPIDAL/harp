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

#ifndef FEATURE_DATA_H
#define FEATURE_DATA_H

#include <ostream> // TODO eliminate unnecessary includes
#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>
#include <algorithm>
#include <ostream>
#include <stdlib.h>
#include <math.h>
#include "metrics.h"
#include "loss.h"

using namespace std;

class FeatureData { // represents a training data set distributed among processors feature-wise
public:
    // constructor/destructor
    FeatureData(int n, int numfeatures_, int isrankingset_, int myid_, int numprocs_);
    ~FeatureData();
    
	// reading and initialization
	bool read(const char* file);
    void initMetrics();
    void sort();
	
	// manage tuples
	void reset();
	
	// metrics
    void computeMetrics(double &rmse, double &err, double &ndcg);  // TODO for now, just compute using single-core, no comm
    
    // queries
    int getN();
    int getNumFeatures();
    int getNumQueries();
    int getNode(int i);
    void setNode(int i, int n);
    double getResidual(int i);
    float getFeature(int f, int i);
    float getSortedFeature(int f, int i);
    int getSortedIndex(int f, int i);
    
    int whoHasFeature(int f);
    bool isLocalFeature(int f);
    int localFeatureIndex(int gf);
    int globalFeatureIndex(int lf);
    
    // prediction
    void updatePred(int i, double p);
    void updateResiduals();
    
private:
	// dataset descriptors
	int N; // number of data instances
    int numfeatures; // number of features stored on this processor
    bool isrankingset; // whether qids should be expected in file, also whether to compute ranking metrics
    // int minfeature, maxfeature; // range of feature indices on this processor; mapped from [minf,maxf] to [1,numf]; 0 feature for convenience
    int myid, numprocs;
    int numqueries; // number of queries in the data set
    
	// static attributes
    int* qid; // query id of each instance
    float** rawfeatures; // feature values ordered by instance
    float** sortedfeatures; // feature values ordered by value
    int** sortedindices; // indices of original instances for each sorted feature
    double* label; // target label value of each instance
    
	// level-specific attributes
	int* node; // last node reached in tree, permits constant time classification at each level
	
	// prediction attributes
	double* pred; // current cumulative prediction for each instance
	double* residual;  // current cumulative residual for each instance
    
	// metric attributes
	double* idealdcg; // ideal dcg by query
	
	// methods
	bool processLine(int &linenum, ifstream &input, int i);
    bool parseFeatureValue(string &cfeature, string &cvalue);
    int computeNumFeatures(int nf, int numprocs, int myid);
};

FeatureData::FeatureData(int n, int numfeatures_, int isrankingset_, int myid_, int numprocs_) {
    // N, numfeatures, minfeature, maxfeature
    N = n;
    numfeatures = computeNumFeatures(numfeatures_, numprocs_, myid_);
    isrankingset = isrankingset_;
    // minfeature = minf;
    // maxfeature = maxf;
    myid = myid_;
    numprocs = numprocs_;
    numqueries = -1;
    
    // qid: limited init, read from file
    qid = new int[n];
    
    // rawfeatures: initialized to minimum value (for missing values)
    rawfeatures = new float*[numfeatures];
	for (int i=0; i<numfeatures; i++) {
        rawfeatures[i] = new float[n];
		for (int j=0; j<n; j++)
            rawfeatures[i][j] = 0.f; //-9999999.f; // TODO replace with better minimum value -- min float?
	}
	
	// sortedfeatures, sortedindices: limited init, completed during sort()
    sortedfeatures = new float*[numfeatures];
    sortedindices = new int*[numfeatures];
    for (int i=0; i<numfeatures; i++) {
        sortedfeatures[i] = new float[n];
        sortedindices[i] = new int[n];
	}
	
	// label: limited init, read from file
    label = new double[n];
	
	// node: initialized to 0
    node = new int[n];
    for (int i=0; i<n; i++)
		node[i] = 0;

	// pred: initialized to 0.f
    pred = new double[n];
	for (int i=0; i<n; i++)
		pred[i] = 0.;
	
	// residual: limited init, initialized to label value
    residual = new double[n];
	
	// idealdcg: no init, computed after file reading, if isrankingset
    idealdcg = NULL;
}

FeatureData::~FeatureData() {
    // delete all 1-d arrays: qid, label, node, pred, residual, idealdcg
	delete [] qid;
	delete [] label;
	delete [] node;
	delete [] pred;
	delete [] residual;
	delete [] idealdcg;
	
	// delete all 2-d arrays: rawfeatures, sortedfeatures, sortedindices
	for (int i=0; i<numfeatures; i++) {
        delete [] rawfeatures[i];
        rawfeatures[i] = NULL;
        
        delete [] sortedfeatures[i];
        sortedfeatures[i] = NULL;
        
        delete [] sortedindices[i];
        sortedindices[i] = NULL;
    }
    delete[] rawfeatures;
    delete[] sortedfeatures;
    delete[] sortedindices;
}

bool FeatureData::read(const char* file) {
	// open file, or return error
	ifstream input(file);
	if (input.fail()) {
	    fprintf(stderr, "Error: unable to open training file %s\n", file);
	    return false;
    }

	// process all data instances
    int linenum = 0;
	for (int i=0; i<N; i++) {
		bool success = processLine(linenum, input, i);
        if (not success) {
            fprintf(stderr, "Error: unable to load training file %s, line %d\n", file, linenum);
            return false;
        }
    }
		
	// indicate success
	return true;
}

bool FeatureData::processLine(int &linenum, ifstream &input, int i) {
    // setup for reading
	int cqid = -1;
	double clabel = -1;
    string cfeature, cvalue;
    
    // read line
	string strline;
	getline(input, strline);
    linenum++;
	
	// check for errors
	if (input.eof()) {
        fprintf(stderr, "Error: end-of-file reached before expected number of training examples were read\n");
        return false;
	} else if (input.fail()) {
	    fprintf(stderr, "Error: failure while reading training example\n");
        return false;
	}
	
	// setup for tokenizing
	char* line = strdup(strline.c_str());
	char* tok = NULL;

	// extract label (first item) and check
	if (not (tok = strtok(line, " "))) {
	    fprintf(stderr, "Error: malformed line in training file, missing label\n");
        return false;
	}
	clabel = atof(tok);
	label[i] = clabel;
	
	// get qid, or ignore if not isrankingset
	string qidstr ("qid");
    if (isrankingset) {
        if (not parseFeatureValue(cfeature, cvalue) or qidstr.compare(cfeature) != 0 or cvalue.empty()) {
            fprintf(stderr, "Error: malformed line in training file, missing qid\n");
            return false;
        }
    	cqid = atoi(cvalue.c_str());
        qid[i] = cqid;
        if (not parseFeatureValue(cfeature, cvalue)) return true;
        if (cvalue.empty()) {
            fprintf(stderr, "Error: invalid feature/value pair in training file\n");
            return false;
        }
    } else {
        if (not parseFeatureValue(cfeature, cvalue)) return true;
        if (cvalue.empty()) {
            fprintf(stderr, "Error: invalid feature/value pair in training file\n");
            return false;
        }
        if (qidstr.compare(cfeature)) { // qid present
            if (not parseFeatureValue(cfeature, cvalue)) return true;
            if (cvalue.empty()) {
                fprintf(stderr, "Error: invalid feature/value pair in training file\n");
                return false;
            }
        }
    }
    
	// get feature values
    int feature = -1;
    float value = -1.f;
    do {
        // validate pair
        if (cvalue.empty()) {
            fprintf(stderr, "Error: invalid feature/value pair in training file\n");
            return false;
        }
        
        // record feature
        feature = atoi(cfeature.c_str());
        // if (feature > maxfeature) break;
        // else if (feature >= minfeature) {
        //     value = (float) atof(cvalue.c_str());
        //     rawfeatures[feature-minfeature][i] = value;
        // }
		if (isLocalFeature(feature)) {
		    value = (float) atof(cvalue.c_str());
            int lf = localFeatureIndex(feature);
            if (lf > numfeatures) {
                fprintf(stderr, "Error: feature index %d out of expected range\n", feature);
                return false;
            } else rawfeatures[lf][i] = value;
		}
    } while (parseFeatureValue(cfeature, cvalue));

	// clean up
	free(line);
	
	// return
    return true;
}

bool FeatureData::parseFeatureValue(string &cfeature, string &cvalue) {
    // get token
    char* tok;
    if (not (tok = strtok(NULL, " \n"))) // line stored in state from previous call to strtok
        return false;
    
    // find colon
	string bit = tok;
	int colon_index = bit.find(":");
	
	// return empty value if colon is missing
	if (colon_index == bit.npos){
        cfeature = bit;
        cvalue = string();
        return true;
	}
	
	// split string
	cfeature = bit.substr(0, colon_index);
	cvalue = bit.substr(colon_index+1,bit.length()-colon_index-1);
	
    return true;
}

void FeatureData::reset() {
    // clear nodes before next tree
	for (int i=0; i<N; i++) {
		node[i] = 0;
	}
}

class FeatureValuePair {
public:
    int index;
	float value;
};

struct CompareFeatureValuePairs {
    bool operator() (FeatureValuePair* fv1, FeatureValuePair* fv2) {
        return (fv1->value < fv2->value);
    }
};

void FeatureData::sort() {
    // initialize FeatureValue array
    CompareFeatureValuePairs cfvp;
    FeatureValuePair** pairs = new FeatureValuePair*[N];
    for (int i=0; i<N; i++)
        pairs[i] = new FeatureValuePair();
    
    // sort each feature
	for (int f=0; f<numfeatures; f++) {
	    // load feature into pairs array
        for (int i=0; i<N; i++) {
            pairs[i]->index = i;
            pairs[i]->value = rawfeatures[f][i];
        }
	    
	    // sort pairs array
		std::sort(pairs, pairs + N, cfvp);
		
		// write feature to sortedfeatures, sortedindices
        for (int i=0; i<N; i++) {
            sortedfeatures[f][i] = pairs[i]->value;
            sortedindices[f][i] = pairs[i]->index;
        }
	}
	
	// delete FeatureValue array
    for (int i=0; i<N; i++) {
        delete pairs[i];
        pairs[i] = NULL;
    }
    delete [] pairs;
}

void FeatureData::initMetrics() {
    // if not ranking data set, return
    if (not isrankingset) return;
    
    // compute number of queries and initialize idealdcg
    numqueries = computeNumQueries(N, qid);
    idealdcg = new double[numqueries];
    
    // compute idealdcg for each query
    computeIdealDCG(N, qid, label, idealdcg);
}

void FeatureData::computeMetrics(double &rmse, double &err, double &ndcg) {
    // compute rmse
    rmse = sqrt(computeBoostingSE(N, label, pred) / (double) N);
    
    // if not ranking data set, return
    if (not isrankingset) return;
    
    // compute ranking metrics
    double rawerr, rawndcg;
    computeBoostingRankingMetrics(N, qid, pred, label, idealdcg, rawerr, rawndcg);
    err = rawerr / (double) numqueries;
    ndcg = rawndcg / (double) numqueries;
}

int FeatureData::getN() {
    return N;
}

int FeatureData::getNumFeatures() {
    return numfeatures;
}

int FeatureData::getNumQueries() {
    return numqueries;
}

// int FeatureData::getMinFeature() {
//     return minfeature;
// }

int FeatureData::whoHasFeature(int f) {
    return f % numprocs;
}

bool FeatureData::isLocalFeature(int f) {
    return whoHasFeature(f) == myid;
}

int FeatureData::localFeatureIndex(int gf) {
    return gf / numprocs;
}

int FeatureData::globalFeatureIndex(int lf) {
    return lf * numprocs + myid;
}

int FeatureData::computeNumFeatures(int nf, int numprocs, int myid) {
    return (nf / numprocs) + (nf % numprocs > myid);
}

int FeatureData::getNode(int i) {
    return node[i];
}

void FeatureData::setNode(int i, int n) {
    node[i] = n;
}

double FeatureData::getResidual(int i) {
    return residual[i];
}

float FeatureData::getFeature(int f, int i) {
    return rawfeatures[f][i];
}

float FeatureData::getSortedFeature(int f, int i) {
    return sortedfeatures[f][i];
}

int FeatureData::getSortedIndex(int f, int i) {
    return sortedindices[f][i];
}

void FeatureData::updatePred(int i, double p) {
    pred[i] += p;
}

void FeatureData::updateResiduals() {
    computegradient(N, label, pred, qid, residual);
}

#endif
