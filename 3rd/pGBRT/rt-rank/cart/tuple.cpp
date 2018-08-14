#include "tuple.h"

// input: file is of format "label qid:int feature1:value1 f2:v2 f5v5 ...."
// output: creation of tuples (one for each line in input file), into data vector
int tuple::read_input(vector<tuple*>& data, char* file, int num_features, bool training, int missing, char* missing_file) 
{
  ifstream input(file);
  if (input.fail())
    return 0;

  static double* init_values = init_values = tuple::read_default_features(missing_file, num_features);
  if(0)
  for (int j = 0; j < num_features; j++)
    cout << init_values[j] << endl;

  string strline;
  int idx=0;
  while( getline(input, strline) ) {
    tuple* t = new tuple(num_features, missing, init_values);
    if (t == NULL)
      cout << "out of memory" << endl;

    char* line = strdup(strline.c_str()); // easier to get a line as string, but to use strtok on a char*
    char* tok = NULL;

    if (training) { // if this is a training set, extract label (first item)
      tok = strtok(line, " ");
      t->label = atof(tok);
      t->target = t->label;
      t->weight = 1;
    }

    int first = 1;
    while (tok = strtok(training ? NULL : tok ? NULL : line, " \n")) { // tok is feature:value
      string bit = tok;
      int colon_index = bit.find(":");
      string feature = bit.substr(0, colon_index);
      string value = bit.substr(colon_index+1, bit.length()-colon_index-1);

      if (first) { t->qid = atoi(value.c_str()); first = 0; continue; } // first token is query
      
      int f = atoi(feature.c_str());
      double v = (double)atof(value.c_str());
      if (v!=0) // added
      t->features[f] = v;
    }
    t->features[0]=idx++;
    free(line);
    data.push_back(t);
  }
  
  return 1;
}

void tuple::make_sidx() {
}
