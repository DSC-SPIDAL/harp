fndef STATIC_TREE_H
#define STATIC_TREE_H

#include <math.h>
#include <ostream>
#include <fstream>
#include <iostream>
#include <time.h>
#include <stdlib.h>
#include <mpi.h>
#include "FeatureData.h"

using namespace std;

class StaticNode {
public:
    int feature;
    float split;
    double label, loss;
    
    int m_infty, m_s;
    float s;
    double l_infty, l_s;
};

class StaticTree {
public:

	StaticTree(int depth); 
        ~StaticTree();
        void clear();
	void startNextLayer();

private:
	StaticNode*** layers;
	int depth, layer, nodes;
	
    	int nodesAtDepth(int d);
    	void clearNode(StaticNode* node);
    	double classifyDataPoint(InstanceData* data, int p);
	void updateBestSplits(FeatureData* data, int f);
    	void printNode(int level, int i, double learningrate);

};
