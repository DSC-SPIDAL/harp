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

#ifndef METRICS_H
#define METRICS_H

#include <iostream>
#include <vector>
#include <math.h>
#include <map>
#include <queue>

using namespace std;

/*
 * compute SE
 */
static double computeBoostingSE(int N, double *label, double *pred) {
	// computes SE between label and pred values
	double SE = 0.;
	for (int i=0; i<N; i++)
		SE += pow(label[i] - pred[i], 2.0);
	return SE;
}

/*
 * compute number of queries (assumes, per data format standard, that instances are ordered by qid)
 */
static int computeNumQueries(int N, int *qid) {
    // initialize count
    int nq = 1;
    int prevqid = qid[0];
    
    // count qids
    for (int i=1; i<N; i++) {
        nq += (qid[i] != prevqid);
        prevqid = qid[i];
    }
    
    return nq;
}

/*
 * compute ideal DCG
 */
struct Document {
	double pred;
	double label;
};

class CompareDocuments {
	public: bool operator() (Document d1, Document d2) { return (d1.pred < d2.pred); }
};

static float R(double y) {
	return (pow(2.0,y) - 1.0) / 16.0;
}

static void computeIdealDCG(int N, int *qid, double *label, double *idealdcg) {
	priority_queue<
		Document,vector<Document>,CompareDocuments> rank;
    int i = 0;
	int nq = 0;
	
	// iterate over queries
    while(i<N) {
		// get query id
		int currqid = qid[i];
		nq++;

		// add documents for that query to priority queue
		while (i<N and qid[i] == currqid) {
            Document doc;
			doc.pred = label[i];
			doc.label = label[i];
			rank.push(doc);
			i++;
		}

		// compute ideal ndcg
		double dcg = 0.;
		for (int j=1; !rank.empty() and j<=10; j++) {
			// get label of document
			Document doc = rank.top();
			rank.pop();

			// add to dcg, if in top 10
			dcg += (pow(2.0,doc.label) - 1.0) / (log2(1.0+j));
		}

		// delete remaining docs
		while (!rank.empty()) {
			rank.pop();
		}

		// set ideal dcg for this query
		idealdcg[nq-1] = dcg;
	}
}

/*
 * compute rawERR and rawNDCG
 */
 static void computeBoostingRankingMetrics(int N, int *qid, double *pred, double *label, double* idealdcg, double &rawerr, double &rawndcg) {
	priority_queue<
		Document,vector<Document>,CompareDocuments> rank;
	rawerr = 0.0;
	rawndcg = 0.0;

	int i = 0;
	int nq = 0;
	// iterate over queries
	while (i<N) {
		// get query id
		int currqid = qid[i];
        nq++;

		// add documents for that query to priority queue
		while (i<N and qid[i] == currqid) {
			Document doc;
			doc.pred = pred[i];
			doc.label = label[i];
			rank.push(doc);
			i++;
		}

		// compute err and ndcg
		double p = 1.0;
		double dcg = 0.0;
		for (int j=1; !rank.empty(); j++) {
			// get label of document
			Document doc = rank.top();
			rank.pop();

			// add to err
			rawerr += 1.0/j * R(doc.label) * p;
			p *= (1.0 - R(doc.label));

			// add to dcg, if in top 10
			if (j <= 10) dcg += (pow(2.0,doc.label) - 1.0) / (log2(1.0+j));
		}

		// add to ndcg
		if (idealdcg[nq-1] > 0)
			rawndcg += dcg / idealdcg[nq-1];
		else rawndcg += 1.0;
	}
}

#endif
