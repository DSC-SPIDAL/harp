#ifndef HISTOGRAM_H
#define HISTOGRAM_H

struct Bin{
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
    Bin* merge(Bin* left,Bin* right);
private:
    int numBin;
    vector<Bin> bins;
    double* label;
};


histogram::histogram(int numberofbins) {
    numBin=numberofbins;
    bins=vector<Bin>();
    cout<<"build the bins"<<endl;
}

histogram::~histogram(){

}

Bin* histogram::merge(Bin* left, Bin* right){
    int count1=left->number;
    int count2=right->number;
    double val1=left->value;
    double val2=right->value;
    Bin* result=new Bin();

    result->number=count1+count2;
    result->value=(val1+val2)/(result->number);
    return result;
}

#endif


