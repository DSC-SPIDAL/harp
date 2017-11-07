#ifndef _SBOW_H__
#define _SBOW_H__
#include <sstream>
#include <string>
#include <vector>
#include <map>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <algorithm>
#include <iostream>
#include <cstdio>
using namespace std;

//spatial bow

struct SWord {
  u_int32_t index;
  float x; // [0,1]
  float y; // [0,1]
  short scale; // extra info
  float norm; // extra info
  float ori; // extra info
};

struct SBOW {
  vector<SWord> words;
  bool fromBinary(const string& s ) {
    size_t len = s.length();
    if( len == 0 ) {
      return true;
    }
    if( len % sizeof(SWord) != 0 || len < 0 ) {
      return false;
    }
    int num_words = len/sizeof(SWord);
    words.resize(num_words);
    memcpy(&words[0],s.data(), len);
    return true;
  }
  string toBinary() {
    string s;
    if( words.size() > 0 ) {
      s.append((char*)&words[0],sizeof(SWord)*words.size());
    }
    return s;
  }
  string toASCII() {
    ostringstream oss;
    oss << "# of points:" << words.size() << endl;
    for (size_t i = 0; i < words.size(); i++) {
      oss << words[i].index  << ' ' << words[i].x << " " << words[i].y << " " << words[i].scale << " " << words[i].norm << " " << endl;
    }
    return oss.str();
  }
  string toASCII_oneline() {
    ostringstream oss;
    for (size_t i = 0; i < words.size(); i++) {
      oss << words[i].index  << ' ' << words[i].x << " " << words[i].y << " " << words[i].scale << " " << words[i].norm << " ";
    }
    return oss.str();
  }
  bool fromASCII(istream& in) {
    int n=0;
    SWord w;
    in>>n;
    if( in.eof() ) {
      return true;
    }
    for( int i = 0 ; i< n; i++ ) {
      in >> w.x >> w.y >> w.index;
    }
    words.push_back(w);
    if( !in.good() ){
      return false;
    }
    return true;
  }
};

#endif
