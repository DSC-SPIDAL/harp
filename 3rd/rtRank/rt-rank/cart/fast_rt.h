#ifndef AM_FAST_DT_H
#define AM_FAST_DT_H

#include "tuple.h"
#include "args.h"

#include <map>
#include <set>
using namespace std;

//#define ONEPASS
#define MULTPASS

typedef int attr_t;
typedef pair<double,int> attr_idx_t;
typedef vector<attr_idx_t> attr_list_t;
typedef vector<attr_list_t*> attr_all_t;
typedef int node_id;
typedef vector< pair<double,int> > class_list_t; // class, index

typedef pair<double,double> split_info; // split value, impurity
typedef map<node_id,split_info> node_splits;
typedef pair<attr_t,double> idxcut; // attribute(feature), split value
typedef map<node_id,idxcut> node_idxcut;

typedef int idx_t;
typedef vector<idx_t> idxlist_t;
typedef map<node_id,idxlist_t> node_idxlist_t;
typedef vector<idxlist_t> attr_to_idx_t;

typedef map<node_id,double> node_d;
typedef map<node_id,int> node_i;

struct fast_node{
  double pred;
  int attr;
  double value;
  node_id left,right,missing;
  fast_node():pred(0),left(0),right(0),missing(0){}
};

class fast_dt{
public:
  attr_all_t attr_all;
  class_list_t class_list;
  int NF,N,NN,curnode,maxdepth;
  const vector<tuple*>& data_r;
  vector<fast_node> tree;
  set<node_id> curnodes;
  map<node_id,int> leafnode;
  node_idxlist_t node_idxlist;
  attr_to_idx_t attr_idxlist;
  map<node_id,int> leafdepth;
  vector< vector<double> > attr_class;

  map< node_id,set<idx_t> > idx_in_node;
  map< node_id,vector<idx_t> > order;

  // make decision tree from the training data
  fast_dt(const vector<tuple*>& data,const args_t& args):data_r(data){
    tree.push_back(fast_node());
    tree.push_back(fast_node());
    N=NN=data.size(),NF=args.features,curnode=1,maxdepth=args.depth;
    presort(data,args);
    partition();
    set_node_preds();
    freemem();
  }

  // sort each attribute in seperate lists, and make class list
  void presort(const vector<tuple*>& data,const args_t& args){

    epoch e;
    e.start();

    int i,j;
    for(i=0;i<N;i++){
      class_list.push_back(pair<double,int>(data[i]->target,curnode));
      node_idxlist[1].push_back(i);
    }
    for(i=0;i<NF;i++){
      attr_all.push_back(make_attr_list(i,data));
      attr_idxlist.push_back(idxlist_t(N));
      vector<double> classes;
      idx_in_node[i]=set<idx_t>();
      cout<<i<<endl;
      for(j=0;j<N;j++)
	{
	//attr_idxlist[i][(*attr_all[i])[j].second]=j;
	//attr_idxlist[i][j]=(*attr_all[i])[j].second;
	  //if(i)
	    //idx_in_node[i].insert((*attr_all[i])[j].second);
	classes.push_back(get_value(i,j));
      }
      attr_class.push_back(classes);
    }
    curnodes.insert(curnode);
    leafdepth[curnode]=1;
    curnode++;

    cout << "done presorting ->" << e.elapsed() << endl;
  }

  // free memory
  void freemem(){
    for(int A=0;A<NF;A++)
      delete attr_all[A];
  }

  // sort the data by attribute, keep track of the indices
  attr_list_t* make_attr_list(attr_t A,const vector<tuple*>& data){
    attr_list_t* attr_list=new attr_list_t;
    for(int i=0;i<N;i++)
      attr_list->push_back(pair<double,int>(data[i]->features[A],i));
    sort(attr_list->begin(),attr_list->end());
    return attr_list;
  }

  // do algorithm
  void partition(){
    while(1){
      vector<node_splits> all_splits;
      for(int a=1;a<NF;a++) //note: index starts at 1 here
	all_splits.push_back(find_splits(a));
      node_idxcut ncuts=combine_splits(all_splits);
      if(!update_labels(ncuts))
	break;
    }
  }

  // find the best split on attribute A for every leaf node
  // use dynamic programming to calculate expected squared loss for every split value
  node_splits find_splits(attr_t A){

    #ifdef ONEPASS
    return best_splits_in_A(A);
    #endif

    node_splits ns;
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr){
      node_id n=*itr;
      ns[n]=best_split_with_AN(A,n);
    }
    return ns;
  }

  // given all the best splits for each attribute on every node
  // select the best attribute to split on for each node
  node_idxcut combine_splits(vector<node_splits>& all){
    node_idxcut m;
    bool find=false;
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr){
      node_id n=*itr;
      double minloss=MY_DBL_MAX;
      //if(leafnode[n]) continue;
      m[n]=idxcut(-1,-1);
      for(int a=0;a<all.size();a++) {
	if(all[a][n].second<minloss && all[a][n].second!=-1){// && all[a][n].second>=0){
	  find=true;
	  minloss=all[a][n].second;
	  m[n]=idxcut(a+1,all[a][n].first);
	}
      }
      if(m[n].first<0)
	leafnode[n]=1;
    }
    //if(!find) {
    //cout<<"couldnt split anywhere"<<endl;
    //}
    return m;
  }

  // given the feature,split values on each node, make new ones
  bool update_labels(node_idxcut ncuts){
    int i;
    map<int,int> nextid;
    bool updated=false;
    for(i=0;i<N;i++){
      node_id n=class_list[i].second;
      if(leafnode[n])//a<0) // cant split this node (should add someway to remember)
	continue;
      if(leafdepth[n]>=maxdepth){
	leafnode[n]=true;
	continue;
      }
      attr_t a=ncuts[n].first;
      double cut=ncuts[n].second;
      // split node, make 2 new nodes in tree
      if(nextid[n]==0){
	tree.push_back(fast_node());
	tree.push_back(fast_node());
	tree[n].left=curnode+0;
	tree[n].right=curnode+1;
	tree[n].attr=a;
	tree[n].value=cut;
	nextid[n]=curnode;
	curnodes.insert(curnode+0);
	curnodes.insert(curnode+1);
	leafdepth[curnode+0]=leafdepth[n]+1;
	leafdepth[curnode+1]=leafdepth[n]+1;
	if(leafdepth[curnode+0]>=maxdepth){
	  leafnode[curnode+0]=1,leafnode[curnode+1]=1;
	  //curnodes.erase(curnode+0),curnodes.erase(curnode+1);
	}
	curnodes.erase(n);
	curnode+=2;
      }
      // put each point in this node into one of the 2 new nodes
      n=nextid[n]+((data_r[i]->features[a]<=cut)?0:1);
      node_idxlist[n].push_back(i);
      class_list[i].second=n;
      updated=true;
    } 
    return updated;
  }

  // now each data point has been assigned to a leaf node
  // set the prediction of each leaf node is the avg of the data point
  void set_node_preds(){
    map<node_id,double> sum;
    map<node_id,int> counter;
    N=data_r.size(); // redundant?
    for(int i=0;i<N;i++){
      node_id n=class_list[i].second;
      sum[n]+=class_list[i].first;
      counter[n]++;
    }
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr)
      tree[*itr].pred=1.0*sum[*itr]/counter[*itr];
  }

  // remove points in leaf node from all the attribute lists
  // prototype: wont work right now
  void remove_leaf_points(node_id nid){
    idxlist_t& idxlist=node_idxlist[nid];
    for(int a=0;a<NF;a++){
      idxlist_t aidxlist;
      int i;
      for(i=0;i<idxlist.size();i++){
	int idx=idxlist[i];
	aidxlist.push_back(attr_idxlist[a][idx]);
      }
      sort(aidxlist.begin(),aidxlist.end());
      int sz=aidxlist.size();
      for(i=0;i<sz;i++){
	int idx=aidxlist[sz-1-i];
	(*attr_all[a]).erase((*attr_all[a]).begin()+idx);
      }
    }
    int NN=(*attr_all[1]).size();
  }

  // given a data point, what class does the tree predict
  double classify(const tuple* const point) const{
    node_id nid=1;
    while(1){
      if(tree[nid].left==0) // at a leaf node
	return tree[nid].pred;
      attr_t a=tree[nid].attr;
      double cut=tree[nid].value;
      nid=(point->features[a]<=cut)?tree[nid].left:tree[nid].right;
    }
    return -1;
  }

  // classify all the data points
  static double classify_all(const data_t& test,const fast_dt* tree,preds_t& preds, const args_t& args){
    int i,N=test.size();
    double r=0.0;
    for(i=0;i<N;i++){
      preds[i]+=args.alpha*tree->classify(test[i]);
      r+=squared(test[i]->label-preds[i]);
    }
    return r;
  }

  // given an attribute and node, return the best value to split on and its loss value
  pair<double,double> best_split_with_AN(attr_t A, node_id nid){ 
    if(leafnode[nid]) return pair<double,double>(-1,-1);
    // get impurity (squared loss) for missing data
    double M=0.0;
    int missing=0,i,missingn=0;
    int nn; // number of nonmissing data points in node
    double minloss=MY_DBL_MAX;
    
    attr_list_t* a_list=attr_all[A];
    while((*a_list)[missing].first==UNKNOWN && missing<N-1){
      if(get_nodeid(A,missing)==nid){
	//M+=get_value(A,missing);
	missingn++;
      }
      missing++;
    }
    if (missing==N-1) // all data is missing
      return pair<double,double>(-1,-1);
    /*
    if (missingn){
      double mbar=M*1.0/missingn;
      M=0.0;
      for (i=0;i<missing;i++)
	if(get_nodeid(A,i)==nid)
	  M+=(get_value(A,i)-mbar)*(get_value(A,i)-mbar);
    }
*/
    //int nn = N - missing; // number of data points that arent missing
  
    // we have impurity = E_{i=0..n} (yi - ybar)^2  (E = summation)
    // this factors to impurity = E(yi^2) + (n+1)ybar^2 - 2*(n+1)*ybar^2
    // we want the impurity for left and right splits for all splits k
    // let ybl = ybar left, ybr = ybar right
    //     s = E_{i=0..k} yi^2
    //     r = E_{i=k+1..n} yi^2
    double ybl, ybr, s, r, L, R, I, ywl, ywr, WL, WR;
    ybl = ybr = s = r = ywl = ywr = WL = WR = 0.0;
    nn=0;
    int numleft=0,numright=0;
    double vs,fs;
    
    // put all data into right side R
    // get ybar right and r
    int start=missing;
    double first=-1;
    bool same=true;
    for (i=start;i<N;i++) {
      int idx=(*a_list)[i].second;
      node_id tnode=class_list[idx].second;
      if(tnode!=nid)continue;
      double value=value_of_index(i,A);
      r+=value*value;
      ybr+=value;
      ywr+=value;
      WR+=1;
      numright++;
      nn++;
      if(first==-1)
	first=(*a_list)[i].first;
      else if(same)
	same=first==(*a_list)[i].first;
    }
    ybr /= 1.0 * nn;
    //r += 0.0000000001// for precision errors
    if(nn==1 || same) return split_info(-1,-1);

    /*
    double first=(*attr_all[A])[start].first;
    bool same=true;
    for(i=start+1;i<N;i++){
      int idx=(*attr_all[A])[i].second;
      node_id tnode=class_list[idx].second;
      if(tnode==nid && first!=(*a_list)[i].first)
	same=false;
    }
    //if(nn==1 || same)
      //return pair<double,double>(-1,-1);
    */



    // for every i
    // put yi into left side, remove it from the right side, and calculate squared lost
    for (i=start;i<N-1;i++) {
      int idx=(*attr_all[A])[i].second;
      node_id tnode=class_list[idx].second;
      if (tnode!=nid)continue;
      numleft++;
      numright--;      
      int j=numleft-1;
      //double yn = data_r[idx]->target;
      double yn=attr_class[A][i];
      //double w = data_r[idx]->weight;     
      double w=1.0;
      s += w *  yn * yn;
      r -= w * yn * yn;
      WL += w;
      WR -= w; 
      ywr -= yn*w;
      ywl += yn*w;
      //if (r < 0 && r > -0.000001) r = 0;
      //if (r < 0) r = 0;
      ybl = (j*ybl + yn) / (j+1.0);
      ybr = ((nn-j)*ybr - yn) / (nn-j-1.0);
      L = s + WL*ybl*ybl - 2*ybl*ywl;
      R = r + WR*ybr*ybr - 2*ybr*ywr;
      // precision errors?
      //if (L < 0 && L > -0.0000001) L = 0; 
      //if (R < 0 && R > -0.0000001) R = 0;
      //if (R < 0) R = 0;
      //if (L < 0) L = 0;
      // do not consider splitting here if data is the same as next
      node_id nextid=next_idx(A,i,nid);
      if(nextid>=0 && (*a_list)[i].first==(*a_list)[nextid].first)
	continue;
      I=L+R+M;
      if (I<minloss) {
	minloss=I;
	vs=(*a_list)[i].first;
	if(nextid>=0)
	  vs=((*a_list)[i].first+(*a_list)[nextid].first)/2.0;
      }
    }

    if(minloss!=MY_DBL_MAX) 
      return pair<double,double>(vs,minloss);
    else
      return pair<double,double>(-1,-1);
  }

  // given an attribute and node, return the best value to split on and its loss value
  node_splits best_splits_in_A(attr_t A){
    node_splits splits;
    attr_list_t* a_list=attr_all[A];
    // get impurity for missing data on each node
    int i,missing=0;
    node_d M,minloss,mbar;
    node_i missingn;
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr){
      node_id nid=*itr;
      minloss[nid]=MY_DBL_MAX;
      splits[nid]=split_info(-1,MY_DBL_MAX);
    }
    while((*a_list)[missing].first==UNKNOWN && missing<N-1){
      node_id nid=get_nodeid(A,missing);
      //M[nid]+=get_value(A,missing);
      missingn[nid]++;
      missing++;
    }
    if (missing==N-1) // all data is missing
      return splits;
    /*for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr){
      node_id nid=*itr;
      if (missingn[nid]){
	mbar[nid]=M[nid]*1.0/missingn[nid];
	M[nid]=0.0;
      }
    }
    for(i=0;i<missing;i++){
      node_id nid=get_nodeid(A,i);
      if(missingn[nid])
	M[nid]+=(get_value(A,i)-mbar[nid])*(get_value(A,i)-mbar[nid]);
    }*/
    // put all data into right side R
    // get ybar right and r
    node_d ybl,ybr,s,r,L,R,I,ywl,ywr,WL,WR,vs,fs;
    node_i nn,numleft,numright;
    int start=missing;
    for(i=start;i<N;i++){
      node_id nid=get_nodeid(A,i);
      double value=value_of_index(i,A);
      r[nid]+=value*value;
      ybr[nid]+=value;
      ywr[nid]+=value;
      WR[nid]+=1;
      numright[nid]++;
      nn[nid]++;
    }
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr)
      ybr[*itr]/=1.0*nn[*itr];
    //r += 0.0000000001// for precision errors
    // base cases
    node_d first;
    node_i firstmet,finddiff;
    for(i=start;i<N;i++){
      node_id nid=get_nodeid(A,i);
      if(!firstmet[nid]){
	firstmet[nid]=1;
	first[nid]=(*a_list)[i].first;
      }
      else if((*a_list)[i].first!=first[nid])
	finddiff[nid]=1;
    }
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr){
      node_id nid=*itr;
      if(nn[nid]==1 || !finddiff[nid])
	splits[nid]=split_info(-1,-1);
    }
    // for every i
    // put yi into left side, remove it from the right side, and calculate squared lost
    for(i=start;i<N-1;i++){
      int idx=(*attr_all[A])[i].second;
      node_id nid=class_list[idx].second;
      //node_id tnode=class_list[idx].second;
      //node_id nid=get_nodeid(A,i);
      if(leafnode[nid] || splits[nid].second==-1) continue;
      numleft[nid]++;
      numright[nid]--;      
      int j=numleft[nid]-1;
      //double yn=data_r[idx]->target; //prefetch this!
      double yn=attr_class[A][i];
      //double w=data_r[idx]->weight;     
      double w=1.0;
      double ynn=yn*yn;
      s[nid]+=ynn;
      r[nid]-=ynn;
      WL[nid]+=1.0;
      WR[nid]-=1.0; 
      ywr[nid]-=yn;
      ywl[nid]+=yn;
      //if (r[nid] < 0 && r[nid] > -0.000001) r[nid] = 0;
      //if (r[nid] < 0) r[nid] = 0;
      ybl[nid]=(j*ybl[nid]+yn)/(j+1.0);
      ybr[nid]=((nn[nid]-j)*ybr[nid]-yn)/(nn[nid]-j-1.0);
      L[nid]=s[nid]+WL[nid]*ybl[nid]*ybl[nid]-2*ybl[nid]*ywl[nid];
      R[nid]=r[nid]+WR[nid]*ybr[nid]*ybr[nid]-2*ybr[nid]*ywr[nid];
      // precision errors?
      //if (L[nid] < 0 && L[nid] > -0.0000001) L[nid] = 0; 
      //if (R[nid] < 0 && R[nid] > -0.0000001) R[nid] = 0;
      //if (R[nid] < 0) R[nid] = 0;
      //if (L[nid] < 0) L[nid] = 0;
      // do not consider splitting here if data is the same as next
      node_id nextid=next_idx(A,i,nid);
      if(nextid>=0 && (*a_list)[i].first==(*a_list)[nextid].first)
      continue;
      // record info if this split is lowest impurity so far
      I[nid]=L[nid]+R[nid]+M[nid];
      if (I[nid]<minloss[nid]){
	minloss[nid]=I[nid];
	vs[nid]=(*a_list)[i].first;
	//if(nextid>=0)
	//vs[nid]=((*a_list)[i].first+(*a_list)[nextid].first)/2.0;
	splits[nid]=split_info(vs[nid],minloss[nid]);
      }
    }
    return splits;
  }

  // small helper functions
  inline double value_of_index(int idx,attr_t a){
    int classidx=(*attr_all[a])[idx].second;
    return class_list[classidx].first;
  }
  inline node_id next_idx(attr_t A,int cur_idx,node_id nid){
    for(int i=cur_idx+1;i<N;i++)
      if(get_nodeid(A,i)==nid)
	return i;
    return -1;
  }
  inline double get_value(attr_t A,int idx){
    int classidx=(*attr_all[A])[idx].second;
    return class_list[classidx].first;
  }
  inline node_id get_nodeid(attr_t A, int idx){
    int cidx=(*attr_all[A])[idx].second;
    return class_list[cidx].second;
  }

  // debugging
  void print_class_list(){
    for(int i=0;i<N;i++)
      cout<<class_list[i].first<<" -> "<<class_list[i].second<<endl;
  }
  void print_attribute_list(int A){
    for(int i=0;i<N;i++)
      cout<<(*attr_all[A])[i].first<<" -> "<<(*attr_all[A])[i].second<<endl;
  }
  void print_splits(node_splits& ns){
    for(map<node_id,split_info>::iterator itr=ns.begin();itr!=ns.end();++itr)
      cout<<itr->first<<" -> "<<itr->second.first<<" "<<itr->second.second<<endl;;
  }
  void print_nodes(){
    for(set<node_id>::iterator itr=curnodes.begin();itr!=curnodes.end();++itr)
      cout<<(*itr)<<endl;
  }
  void print_node_idxcut(node_idxcut& ni){
    for(map<node_id,idxcut>::iterator itr=ni.begin();itr!=ni.end();++itr)
      cout<<itr->first<<" -> "<<itr->second.first<<" "<<itr->second.second<<endl;;
  }
};

#endif
