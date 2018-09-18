
#include <algorithm>
using namespace std;
 
#include "regression_tree.h"
#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>
using namespace boost;


int feature_to_split_on;
bool mysortpred(const tuple* d1, const tuple* d2) {
  return d1->features[feature_to_split_on] < d2->features[feature_to_split_on];
}
void sort_data_by_feature(vector<tuple*>& data, int f) {
  feature_to_split_on = f;
  sort(data.begin(), data.end(), mysortpred);
}


bool mysortpred2(const pair<tuple*, int> tk1, const pair<tuple*, int> tk2) {
  return tk1.first->features[tk1.second] < tk2.first->features[tk2.second];
}
/////////



//double best_fc_in_feature(vector<tuple*> data, int f, pair<int, double>& fc) {
double best_fc_in_feature(vector<tuple*> data, int f, int& fs, double& vs, const args_t& args) {  //if (!XX)
  vector< pair<tuple*, int> > tk;
  int z;

  for (z = 0; z < data.size(); z++)
    tk.push_back( pair<tuple*,int>(data[z], f) );
  sort(tk.begin(), tk.end(), mysortpred2);
  for (z = 0; z < data.size(); z++)
    data[z] = tk[z].first;

  int n = data.size(), i;
  double min = MY_DBL_MAX;

  // get impurity (squared loss) for missing data
  double M = 0.0;
  int missing = 0;
  while (data[missing]->features[f] == UNKNOWN && missing < n-1) {
    M += data[missing]->target * 1.0;
    missing++;
  }
  if (missing == n-1) // all data is missing
    return MY_DBL_MAX;
  if (missing) {
    double mbar = M * 1.0 / missing;
    M = 0.0;
    for (i = 0; i < missing; i++)
      M += (data[i]->target - mbar) * (data[i]->target - mbar);
  }
  int nn = n - missing; // number of data points that arent missing
  
  // we have impurity = E_{i=0..n} (yi - ybar)^2  (E = summation)
  // this factors to impurty = E(yi^2) + (n+1)ybar^2 - 2*(n+1)*ybar^2
  // we want the impurity for left and right splits for all splits k
  // let ybl = ybar left, ybr = ybar right
  //     s = E_{i=0..k} yi^2
  //     r = E_{i=k+1..n} yi^2
  double ybl, ybr, s, r, L, R, I, ywl, ywr, WL, WR;
  ybl = ybr = s = r = ywl = ywr = WL = WR = 0.0;
  
  // put all data into right side R
  // get ybar right and r
  int start = missing;
  for (i = start; i < n; i++) {
    r += data[i]->target * data[i]->target * data[i]->weight;
    ybr += data[i]->target;
    ywr += data[i]->target * data[i]->weight;
    WR += data[i]->weight;
  }
  ybr /= 1.0 * nn;

  //r += 0.0000000001 for precision errors
  
  // for every i
  // put yi into left side, remove it from the right side, and calculate squared lost
  for (i = start; i < n-1; i++) {
    int j = i - missing; 
    double yn = data[i]->target;
    double w = data[i]->weight;

    s += w *  yn * yn;
    r -= w * yn * yn;
    WL += w;
    WR -= w;
    
    ywr -= yn*w;
    ywl += yn*w;

   
    if (r < 0 && r > -0.000001) r = 0;
    if (r < 0) r = 0;
    
    ybl = (j*ybl + yn) / (j+1.0);
    ybr = ((nn-j)*ybr - yn) / (nn-j-1.0);

      
    L = s + WL*ybl*ybl - 2*ybl*ywl;
    R = r + WR*ybr*ybr - 2*ybr*ywr;

    
    //L = s + (j+1)*ybl*ybl - 2*(j+1)*ybl*ybl;
    //R = r + (nn-j-1.0)*ybr*ybr - 2*(nn-j-1.0)*ybr*ybr;
    
    // precision errors?
    if (L < 0 && L > -0.0000001) L = 0; 
    if (R < 0 && R > -0.0000001) R = 0;
    if (R < 0) R = 0;
    if (L < 0) L = 0;

    if(0)
    if (L < 0 || R < 0 || r < 0)
      printf("Problem %lf %lf %lf\n", L, R, r);
    
    // do not consider splitting here if data is the same as next
    if (data[i]->features[f] == data[i+1]->features[f])
      continue;
    
    I = L + R;// + M;
    I = L + R + M + args.fpenalty*args.fcosts[f];


    if (I < min) {
      min = I;
      fs = f;
      vs = (data[i]->features[f] + data[i+1]->features[f]) / 2;
    }
  }

  return min;
}


// find best feature to split on in range [start, end)
// store results int I, fs, vs : impurity, feature, value
void find_split_in_range(vector<tuple*> data, int start, int  end, int& fs, double& vs, double& I, vector<bool> skip, const args_t& args) {
  double min = MY_DBL_MAX;

  for (int i = start; i < end; i++) {
    int f = i + 1;
    if (skip[f]) continue;

    int fi;
    double vi, Ii;
    Ii = best_fc_in_feature(data, f, fi, vi, args);
    if (Ii < min) {
      min = Ii;
      fs = fi;
      vs = vi;
    }
  }

  I = min;
}

bool find_split_p(vector<tuple*> data, int NF, int& f_split, double& v_split, vector<bool>& skip, const args_t& args) {

  f_split = -1;
  double min = MY_DBL_MAX;
  int n = data.size(), i;

  pair<int, double>* fc = new pair<int,double>[NF];
  double* I = new double[NF];

  int numthreads = args.processors;
  thread** threads = new thread*[numthreads];
  
  int* F = new int[numthreads];
  double* V = new double[numthreads];
  double* Imp = new double[numthreads];

  for (i = 0; i < numthreads; i++)
    threads[i] = new thread(bind(find_split_in_range, data, i*(NF-1)/numthreads, (i+1)*(NF-1)/numthreads, ref(F[i]), ref(V[i]), ref(Imp[i]), ref(skip), cref(args)));

  for (i = 0; i < numthreads; i++) {
    threads[i]->join(); 
    delete threads[i];
  }
  delete[] threads;

  for (i = 0; i < numthreads;i++)
    if (Imp[i] < min) {
      min = Imp[i];
      f_split = F[i];
      v_split = V[i];
    }

  delete[] fc;  delete[] I;  delete[] V;  delete[] Imp; delete[] F;
  return min != MY_DBL_MAX;
}

///////////////////////

bool dt_node::entropy_split(data_t data, int NF, int& f_split, double& v_split, int K, bool par) {
  f_split = -1;
  double min = MY_DBL_MAX;
  int n = data.size(), i;
  
  vector<bool> skip;

  //min E(i=1..k) pi * log(1/pi)
  
  for (i = 0; i <= NF; i++)
    skip.push_back( (K > 0) ? true : false);

  for (i = 0; i < K; i++) {
    int f;
    do
      f = rand() % (NF-2) + 1;
    while (!skip[f]);
    skip[f] = false;
  }


  //if (K <= 0)
  //return find_split_p(data, NF, f_split, v_split, skip);



  for (int f = 1; f < NF; f++) {
    sort_data_by_feature(data,f);

    //if (skip[f]) continue;

    /*
 vector< pair<tuple*, int> > tk;
  int z;

  for (z = 0; z < data.size(); z++)
  tk.push_back( pair<tuple*,int>(data[z], f) );
  sort(tk.begin(), tk.end(), mysortpred2);


  for (z = 0; z < data.size(); z++)
  data[z] = tk[z].first;
    */


  int num_c = 7;
  vector<int> c_left, c_right;
  vector<double> p_left, p_right;
  vector<int> freq;
  int n_left = 0, n_right = 0;
  for (i=0;i<num_c;i++) {
    c_left.push_back(0);
    c_right.push_back(0);
    p_left.push_back(0.0);
    p_right.push_back(0.0);
    freq.push_back(0);
  }



    // get impurity (entropy) for missing data
    double M = 0.0, L, R;
    int missing = 0;
    while (data[missing]->features[f] == UNKNOWN && missing < n-1) {
      freq[(int)data[missing]->target]++;
      missing++;
    }
    if (missing == n-1) // all data is missing
      continue;

    int nn = n - missing; // number of data points that arent missing

    // entropy

    // put all data into right side R
    int start = missing;
    for (i = start; i < n; i++) {
      c_right[(int)data[i]->target]++;
      n_right++;
      freq[(int)data[i]->target]++;
    }
    for (i = 0; i < num_c; i++) {
      if (freq[i])
	p_right[i] = c_right[i] / freq[i];
      else
	p_right[i] = 0;
      if (freq[i])
	R += p_right[i] * log(1.0 / p_right[i]);
    }

    if (missing) {
      M = (1.0 * missing / n) * log(1.0 * n / missing);
      //cout << "missing data!" << endl;
    }
    L = 0.0;

    // for every i
    // put yi into left side, remove it from the right side, and calculate squared lost
    for (i = start; i < n-1; i++) {
      int j = i - missing; 
      int yn = (int)data[i]->target;

      n_right--;
      n_left++;

      c_left[yn]++;
      c_right[yn]--;

      p_left[yn] += 1.0 / freq[yn];
      p_right[yn] -= 1.0 / freq[yn];

      // do not consider splitting here if data is the same as next
      if (data[i]->features[f] == data[i+1]->features[f])
	continue;

      L = 0.0;
      int k;
      for (k = 0; k < num_c; k++) {
	if (c_left[k])
	  L += (1.0 * c_left[k] / n) * log(1.0 * n / c_left[k]); 
      }
      R = 0.0;
      for (k = 0; k < num_c; k++) {
	if (c_right[k])
	  R += (1.0 * c_right[k] / n) * log(1.0 * n / c_right[k]);   
      }

      double I = 1.0*n_left/n * L + 1.0*n_right/n * R + 1.0*missing/n * M;
      //I = 1.0*n_left/n * 

      /*
      L = 0.0, R = 0.0;
      for (i = 0; i < num_c; i++)
	L += p_left;
      */

      if (I < min) {
	min = I;
	f_split = f;
	v_split = (data[i]->features[f] + data[i+1]->features[f])/2;
      }
    }
  }

  return min != MY_DBL_MAX;
}






//////////////////////


bool dt_node::find_split(vector<tuple*> data, int NF, int& f_split, double& v_split, int K, bool par, const args_t& args) {
  if (args.loss == ALG_ENTROPY)
    return entropy_split(data, NF, f_split, v_split, K, par);

  f_split = -1;
  double min = MY_DBL_MAX;
  int n = data.size(), i;
  
  vector<bool> skip;

  //K = NF/2;

  // pick K random features to split on, if specified
  for (i = 0; i <= NF; i++)
    skip.push_back( (K > 0) ? true : false);
  for (i = 0; i < K; i++) {
    int f;
    do
      f = rand() % (NF-2) + 1;
    while (!skip[f]);
    skip[f] = false;
  }

  //if (K >= 10)
  if (args.alg != ALG_FOREST && args.processors!=1)
    return find_split_p(data, NF, f_split, v_split, skip, args);

  for (int f = 1; f < NF; f++) {
    if (skip[f]) continue;

    double bestf=MY_DBL_MAX;
    double bestv=-1;

    // sort data
    vector< pair<tuple*, int> > tk;
    int z;
    for (z = 0; z < data.size(); z++)
      tk.push_back( pair<tuple*,int>(data[z], f) );
    sort(tk.begin(), tk.end(), mysortpred2);
    for (z = 0; z < data.size(); z++)
      data[z] = tk[z].first;

    // get impurity (squared loss) for missing data
    double M = 0.0;
    int missing = 0;
    while (data[missing]->features[f] == UNKNOWN && missing < n-1) {
      M += data[missing]->target * 1.0;
      missing++;
    }
    if (missing == n-1) // all data is missing
      continue;
    if (missing) {
      double mbar = M * 1.0 / missing;
      M = 0.0;
      for (i = 0; i < missing; i++)
	M += (data[i]->target - mbar) * (data[i]->target - mbar);
    }
    int nn = n - missing; // number of data points that arent missing

    // we have impurity = E_{i=0..n} (yi - ybar)^2  (E = summation)
    // this factors to impurty = E(yi^2) + (n+1)ybar^2 - 2*(n+1)*ybar^2
    // we want the impurity for left and right splits for all splits k
    // let ybl = ybar left, ybr = ybar right
    //     s = E_{i=0..k} yi^2
    //     r = E_{i=k+1..n} yi^2
    double ybl, ybr, s, r, L, R, I, ywl, ywr, WL, WR;
    ybl = ybr = s = r = ywl = ywr = WL = WR = 0.0;

    // put all data into right side R
    // get ybar right and r
    int start = missing;
    for (i = start; i < n; i++) {
      r += data[i]->target * data[i]->target * data[i]->weight;
      ybr += data[i]->target;
      ywr += data[i]->target * data[i]->weight;
      WR += data[i]->weight;
    }
    ybr /= 1.0 * nn;

    // for every i
    // put yi into left side, remove it from the right side, and calculate squared lost
    for (i = start; i < n-1; i++) {
      int j = i - missing; 
      double yn = data[i]->target;
      double w = data[i]->weight;

      s += w *  yn * yn;
      r -= w * yn * yn;
      WL += w;
      WR -= w;

      ywr -= yn*w;
      ywl += yn*w;

      if (r < 0 && r > -0.000001) r = 0;

      ybl = (j*ybl + yn) / (j+1.0);
      ybr = ((nn-j)*ybr - yn) / (nn-j-1.0);
      
      L = s + WL*ybl*ybl - 2*ybl*ywl;
      R = r + WR*ybr*ybr - 2*ybr*ywr;

      //L = s + WL*(j+1)*ybl*ybl - 2*(j+1)*ybl*ywl;
      //R = r + WR*(nn-j-1.0)*ybr*ybr - 2*(nn-j-1.0)*ybr*ywr;
      //L = s + (j+1)*ybl*ybl - 2*(j+1)*ybl*ybl;
      //R = r + (nn-j-1.0)*ybr*ybr - 2*(nn-j-1.0)*ybr*ybr;

      // precision errors?
      if (L < 0 && L > -0.0000001) L = 0; 
      if (R < 0 && R > -0.0000001) R = 0;

      // do not consider splitting here if data is the same as next
      if (data[i]->features[f] == data[i+1]->features[f])
	continue;

      I = L + R + M + args.fpenalty*args.fcosts[f];

      if(I<bestf){
	bestf=I;
	bestv= (data[i]->features[f] + data[i+1]->features[f])/2;
      }

      if (I < min) {
	min = I;
	f_split = f;
	v_split = (data[i]->features[f] + data[i+1]->features[f])/2;
      }
    }
  }

  return min != MY_DBL_MAX;
}
