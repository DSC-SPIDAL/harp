#ifndef HISTOGRAM_H
#define HISTOGRAM_H

#include <iostream>
#include <vector>
#include "feature_data.h"

using namespace std;

/*
 * The Bin structure contains the median value, the label and the number
 */
Struct Bin{
    double label;
    double value;
    int number;
};

/**
 * The historgram collect the bins of feature data.
 */
class histogram{
public:
    histogram(int numberofbins);      
    ~histogram();
    void update_histogram();
    double getResidule();
    double setHistogram(int n, int i);
    Bin mergeHistogram(Bin* a,Bin* b);
    Bin addElement(Bin* a,int element);
    double    
private:
    int numBin;
    vector<Bin> bins; 
    double* label;    
};


histogram::histogram(int numberofbins) {
    numBin=bin;
    bins=new Bin[numberofbins];
 
}

histogram::~histogram() {
    // delete all 1-d arrays: qid, label, node, pred, residual, idealdcg
	
    // delete all 2-d arrays: rawfeatures, sortedfeatures, sortedindices
    for (int i=0; i<numBin; i++){
         Bin newbin=bins.pop();
         newbin=NULL;
    }
    
    delete lable;
}




histogram::update_histogram(Bin* a,Bin* b){
    int number= a->number+b->number;
    
    
}


histogram::merge_histogram(Bin* a,Bin* b){
   int total_number=a->number+b->number;
   double total=(a->value*a->number+b->value*b->number);
   Bin new_bin=new Bin(a->label,total_number,total);
   return new_bin;
}

histogram::addElement(Bin* a,int element){
   double cur=a->value;   

   return NULL;
}

histogram::sum(Bin* a,Bin* b){

   return NULL;
} 
