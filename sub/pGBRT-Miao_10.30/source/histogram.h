#ifndef HISTOGRAM_H
#define HISTOGRAM_H

#include <iostream>
#include <vector>
#include "feature_data.h"

using namespace std;

Struct Bin{
    double label;
    double value;
    int number;
};

class histogram{
public:
    histogram(int numberofbins);      
    ~histogram();
    void update_histogram();
    double getResidule();
    double setHistogram(int n, int i);
    Bin mergeHistogram(Bin a,Bin b);
    Bin addElement(Bin a,int element);
   
private:
    int numBin;
    vector<Bin> bins; 
    double label;    
};

histogram::histogram(int numberofbins) {
    numBin=bin;
    bins=new Bin[numberofbins];
 
}

histogram::~histogram() {
    // delete all 1-d arrays: qid, label, node, pred, residual, idealdcg
	
	// delete all 2-d arrays: rawfeatures, sortedfeatures, sortedindices
    for (int i=0; i<numBin; i++) {
           bins.pop();
           Bin[i] = NULL;
        
    }
}




histogram::update_histogram(){
    for (int i=0;i<numBin;i++){
    	
    }

}


histogram::merge_histogram(Bin a,Bin b){
   
   return NULL;
}

histogram::addElement(Bin* a,int element){
   double cur=a->   

   return Null;
}
