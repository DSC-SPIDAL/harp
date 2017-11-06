#ifndef _VLDSIFT_H__
#define _VLDSIFT_H__
#include <string>
#include <vector>
#include <sstream>

#define VLDSIFT_DIM 128
using namespace std;
struct Point_vldsift {
  float x;
  float y;
  short scale;
  float norm;
  float desc[VLDSIFT_DIM];
};

struct Points_vldsift {
  vector<Point_vldsift> vecs;
  bool fromBinary(const string& bin ) {
    size_t len = bin.length();
    int num_points = len/sizeof(Point_vldsift);
    if( len % sizeof(Point_vldsift) != 0 ) {
      return false;
    }
    vecs.resize(num_points);
    memcpy(&vecs[0], bin.data(), num_points * sizeof(Point_vldsift));
    return true;
  }
  string toASCII() {
    ostringstream oss;
    oss << vecs.size() << " " << VLDSIFT_DIM << endl;
    for ( size_t i = 0; i< vecs.size(); i++ ) {
      Point_vldsift& p = vecs[i];
      oss << p.x << " " << p.y << " " << p.scale << " " << p.norm << " " << endl;
      for ( int k = 0; k < VLDSIFT_DIM; k++ ) {
	oss << p.desc[k] << " ";
      }
      oss << endl;
    }
    return oss.str();
  }
  string toASCII_oneline() {
    ostringstream oss;
    for ( size_t i = 0; i< vecs.size(); i++ ) {
      Point_vldsift& p = vecs[i];
      oss << p.x << " " << p.y << " " << p.scale << " " << p.norm << " ";
      for ( int k = 0; k < VLDSIFT_DIM; k++ ) {
	oss << p.desc[k] << " ";
      }
    }
    return oss.str();
  }
  string toBinary() {
    string s;
    s.append((char*)&vecs[0], vecs.size()*sizeof(Point_vldsift));
    return s;
  }
};

#endif
