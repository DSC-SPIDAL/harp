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

#ifndef INSTANCE_DATA_H
#define INSTANCE_DATA_H

#include <ostream> // TODO eliminate unnecessary includes
#include <iostream>
#include <fstream>
#include <math.h>
#include <string.h>
#include <algorithm>
#include <ostream>
#include <stdlib.h>
#include <math.h>
#include <mpi.h>
#include "metrics.h"
#include "loss.h"

using namespace std;

class InstanceData { // represents a test data set distributed among processors instance-wise
public:
    // constructor/destructor
    InstanceData(int n, int numfeatures_, bool isrankingset_, int mini, int maxi);
    ~InstanceData();
    
	// reading and initialization
	bool read(const char* file, int filesize);
    void initMetrics();
	
	// metrics
    void computeMetrics(double &rmse, double &err, double &ndcg);
    
    // queries
    int getN();
    int getNumFeatures();
    int getNumQueries();
    float getFeature(int f, int i);
    
    // prediction
    void updatePred(int i, double p);
    
private:
	// dataset descriptors
	int N; // number of data instances
    int numfeatures; // number of features stored on this processor
    bool isrankingset; // whether qids should be expected in file, also whether to compute ranking metrics
    int numqueries; // number of queries in the data set
    int minindex, maxindex;
    
	// static attributes
    vector<float>** features; // feature values
    int* qid; // query id of each instance
    vector<int>* qidtemp;
    double* label; // target label value of each instance
    vector<double>* labeltemp;
	
	// prediction attributes
	double* pred; // current cumulative prediction for each instance
    
	// metric attributes
	double* idealdcg; // ideal dcg by query
	
	// methods
    bool processLineHeader(int &linenum, ifstream &input, char* &line, double &label, int &qid);
    bool storeLine(char* line, int i, double label, int qid);
    bool parseFeatureValue(string &cfeature, string &cvalue);
};

InstanceData::InstanceData(int n, int numfeatures_, bool isrankingset_, int mini, int maxi) {
	// N, numfeatures
    N = n;
    numfeatures = numfeatures_;
    isrankingset = isrankingset_;
    numqueries = -1;
    minindex = mini;
    maxindex = maxi;
    
    // qid: limited init, read from file
    qid = NULL;
    qidtemp = new vector<int>(n,0);
	
	// features: limited init, defaulted to minimum value (for missing values) and read from file
	features = new vector<float>*[numfeatures];
    for (int i=0; i<numfeatures; i++)
        features[i] = new vector<float>(n,-9999999.f);
	
	// label: limited init, read from file
    label = NULL;
    labeltemp = new vector<double>(n,0.f);
    
    // pred: initialized to 0.f
    pred = NULL;
    
    // idealdcg: no init, computed after file reading, if isrankingset
    idealdcg = NULL;
}

InstanceData::~InstanceData() {
    delete [] qid; delete qidtemp;
    delete [] label; delete labeltemp;
    delete [] pred;
	delete [] idealdcg;

	for (int i=0; i<numfeatures; i++) {
		delete features[i];
        features[i] = NULL;
	}
	delete [] features;
}

bool InstanceData::read(const char* file, int filesize) {
    // open file, or return error
	ifstream input(file);
	if (input.fail()) {
	    fprintf(stderr, "Error: unable to open validation/test file %s\n", file);
	    return false;
    }
    
    // track line number for error messages
    int linenum = 0;
	
	// skip to my section
	string strline;
    int idx=0;
	for (int i=0; i<minindex-1; i++) {
		getline(input, strline); // skip to section (minindex-1, minindex+N)
        linenum++;
		if (input.eof()) {
            fprintf(stderr, "Error: end-of-file reached before expected number of validation/test examples were read.\n");
            fprintf(stderr, "Error: unable to load validation/test file %s\n", file);
            return false;
    	} else if (input.fail()) {
    	    fprintf(stderr, "Error: failure while reading validation/test example.\n");
    	    fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
            return false;
    	}
	}
	
	// variables
	char* line = NULL;
    double currlabel;
    int prevqid = -1, currqid;
    
	// get previous qid
	if (minindex > 0)
	    if (not processLineHeader(linenum, input, line, currlabel, prevqid)) { // just read the qid, don't store anything
    	    fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
	        return false;
        }
    free(line);
	
	// skip the remainder of previous query and store allotted data instances
    int j = 0;
    for (int i=0; i<N; i++) {
        // get label and qid
       if (not processLineHeader(linenum, input, line, currlabel, currqid)) {
           fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
           return false;
       }
        
        // if ranking set and still previous query, continue
        if (isrankingset and currqid == prevqid) {
            free(line);
            continue;
        }
        // otherwise store line
        bool success = storeLine(line, j++, currlabel, currqid);
        if (not success) {
            fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
            return false;
        }
    }
    
    // continue until the entire final query has been read
    int finalqid = currqid;
    while (isrankingset and maxindex+1 < filesize) {
        // get label and qid
        if (not processLineHeader(linenum, input, line, currlabel, currqid)) {
            fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
            return false;
        }
        
        // done if next query
        if (currqid != finalqid) {
            free(line);
            break;
        }
        
        // allocate space for new instance
        if (j >= N) {
            for (int f=0; f<numfeatures; f++)
                features[f]->push_back(0.f);
            qidtemp->push_back(0);
            labeltemp->push_back(0.f);
        }
        
        // store line
        bool success = storeLine(line, j++, currlabel, currqid);
        if (not success) {
            fprintf(stderr, "Error: unable to load validation/test file %s, line %d\n", file, linenum);
            return false;
        }
    }
	
	// update N
    N = j;
	
	// convert qid to array
    qid = new int[N];
    for (int i=0; i<N; i++)
        qid[i] = qidtemp->at(i);
    delete qidtemp;
    qidtemp = NULL;
    
    // convert label to array
    label = new double[N];
    for (int i=0; i<N; i++)
        label[i] = labeltemp->at(i);
    delete labeltemp;
    labeltemp = NULL;
    
    // initialized pred
    pred = new double[N];
    for (int i=0; i<N; i++)
        pred[i] = 0.0;
	
	// indicate success
	return true;
}

bool InstanceData::processLineHeader(int &linenum, ifstream &input, char* &line, double &label, int &qid) {
    // setup for reading
    int cqid = -1;
	float clabel = -1;
	string cfeature, cvalue;
    
    // read line
	string strline;
	getline(input, strline);
    linenum++;
	
	// check for errors
	if (input.eof()) {
        fprintf(stderr, "Error: end-of-file reached before expected number of validation/test examples were read\n");
        return false;
	} else if (input.fail()) {
	    fprintf(stderr, "Error: failure while reading validation/test example\n");
        return false;
	}
	
	// convert line
	line = strdup(strline.c_str());
	char* tok = NULL;

	// extract label
	if (not (tok = strtok(line, " "))) {
	    fprintf(stderr, "Error: malformed line in validation/test file, missing label\n");
        return false;
	}
	label = atof(tok);
	
	// extract qid
    if (isrankingset) {
        string qidstr ("qid");
        if (not parseFeatureValue(cfeature, cvalue) or qidstr.compare(cfeature) != 0 or cvalue.empty()) {
            fprintf(stderr, "Error: malformed line in validation/test file, missing qid\n");
            return false;
        }
    	qid = atoi(cvalue.c_str());
    }
    
    // return
    return true;
}

bool InstanceData::storeLine(char* line, int i, double label, int qid) { // uses tokenizer from call to processLineHeader()
    // store label and qid
    labeltemp->at(i) = label;
    if (isrankingset) qidtemp->at(i) = qid;
    
    // get feature values
	string cfeature, cvalue;
    int feature = -1;
    float value = -1.f;
    
    // ignore qid, if present and not isrankingset
    string qidstr ("qid");
    if (not parseFeatureValue(cfeature, cvalue)) return true;
    if (not isrankingset and qidstr.compare(cfeature)) // qid is present
        if (not parseFeatureValue(cfeature, cvalue)) return true;
        
	do {
	    // check value
	    if (cvalue.empty()) {
	        fprintf(stderr, "Error: invalid feature/value pair in validation/test file\n");
            return false;
	    }
	    
	    // record feature
        feature = atoi(cfeature.c_str());
        if (feature < 0 or feature > getNumFeatures()) {
            fprintf(stderr, "Error: feature index %d out of expected range\n", feature);
            return false;
        }
		value = (float) atof(cvalue.c_str());
        features[feature]->at(i) = value;
        // TODO: faster?? consider dropping this if statement and using 
	    // mine = (f >= minfeature and f <= maxfeature); features[f*mine]->value = v*mine;
    } while (parseFeatureValue(cfeature, cvalue));

	// clean up
	free(line);
	
	// return
    return true;
}

bool InstanceData::parseFeatureValue(string &cfeature, string &cvalue) {
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

int InstanceData::getN() {
    return N;
}

int InstanceData::getNumFeatures() {
    return numfeatures;
}

int InstanceData::getNumQueries() {
    return numqueries;
}

float InstanceData::getFeature(int f, int i) {
    return features[f]->at(i);
}

void InstanceData::initMetrics() {
    // if not ranking data set, return
    if (not isrankingset) return;

    // compute number of queries and initialize idealdcg
    numqueries = computeNumQueries(N, qid);
    idealdcg = new double[numqueries];

    // compute idealdcg for each query
    computeIdealDCG(N, qid, label, idealdcg);
}

void InstanceData::computeMetrics(double &rmse, double &err, double &ndcg) {
    // uses MPI_Reduce to compute across all processors,
    // however results are only valid at root (myid==0)
    
    // compute rmse
    double se = computeBoostingSE(N, label, pred);
    
    // compute ranking metrics
    double rawerr, rawndcg; int nq;
    if (isrankingset) {
        computeBoostingRankingMetrics(N, qid, pred, label, idealdcg, rawerr, rawndcg);
        nq = getNumQueries();
    }
    
    // reduce sums to master
    double buffer[] = {se, (double) getN(), rawerr, rawndcg, (double) nq};
    double recv_buffer[] = {-1.0, -1.0, -1.0, -1.0, -1.0};
    MPI_Reduce(&buffer, &recv_buffer, 5, MPI_DOUBLE, MPI_SUM, 0, MPI_COMM_WORLD);
    
    // compute
    rmse = sqrt(recv_buffer[0] / recv_buffer[1]);
    if (not isrankingset) return;
    err = recv_buffer[2] / recv_buffer[4];
    ndcg = recv_buffer[3] / recv_buffer[4];
}

void InstanceData::updatePred(int i, double p) {
    pred[i] += p;
}

#endif
