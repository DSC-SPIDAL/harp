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
private:
    int numBin;
    vector<Bin> bins; 
    double* label;    
};


histogram::histogram(int numberofbins) {
    numBin=bin;
    bins=new Bin[numberofbins];
 
}





