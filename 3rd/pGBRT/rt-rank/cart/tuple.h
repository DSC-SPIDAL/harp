#ifndef AM_ML_TUPLE_H
#define AM_ML_TUPLE_H

#include <iostream>
#include <string>
#include <fstream>
#include <vector>
#include <string.h>
#include <cmath>
#include <algorithm>
#include <map>

using namespace std;

#define MY_DBL_MAX 99999999999999.999999
#define MY_DBL_MIN (-MY_DBL_MAX)
#define UNKNOWN MY_DBL_MIN

class tuple // represents a data instance
{
public:
  double* features;
  double label;
  double weight;
  int qid;
  double pred;
  double target;

  static vector< vector<int> >* sidx;
  static void make_sidx();

 tuple(int num_features, int z, double* init) : weight(1.0), label(-1), qid(-1), pred(-1), target(-1) {
    features = new double[num_features];
    for (int i = 0; i < num_features; i++) 
      //features[i] = UNKNOWN;
      //features[i] = 1;
      if (!init)
	//features[i] = z ? UNKNOWN : 0;
	//features[i] = (z > 0) ? UNKNOWN : (z < 0) ? 1 : 0;
	features[i] = (z > 0) ? UNKNOWN : (z < 0) ? 999999999 : -99999999;
      else
	features[i] = init[i];
  }
  ~tuple() {delete[] features;}

  // populate vector with tuples from input file
  static int read_input(vector<tuple*>& data, char* file, int num_features, bool training, int missing, char* missing_file); 

  // modifiers
  static void add_weight(tuple* t, double w) { t->weight = w; }
  static void set_label(tuple* t, double l) { t->label = l; }
  static void set_pred(tuple* t, double p) { t->pred = p; }
  static void set_qid(tuple* t, int q) { t->qid = q; }

  void set_target(double t) { target = t; }

  // free memory
  static void delete_data(vector<tuple*>& data) {
    for (int i = 0; i < data.size(); i++)
      delete data[i];
  }

  void write_to_file(ofstream& out) {
    out << pred << " qid:" << qid << " blah" << endl;
  }

  static void write_to_file(const vector<double>& preds, const vector<tuple*>& data, char* outfile) {
    ofstream out(outfile);
    int i, N=preds.size();
    for(i=0; i<N; i++) 
      out << preds[i] << " qid:" << data[i]->qid << " blah" << endl;
  }

  static double* read_default_features(char* file, int f) {
    if (file == NULL) return NULL;
    double* missing_values = new double[f];
    ifstream in(file);
    double v;
    for (int i = 0; i < f; i++) {
      in >> v;
      missing_values[i] = v;
    }
    return missing_values;
  }
};


#endif //AM_ML_TUPLE_H
