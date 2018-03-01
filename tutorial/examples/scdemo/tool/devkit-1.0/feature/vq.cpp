#include <iostream>
#include <fstream>
#include <vector>
#include <limits>
#include <cstdarg>
#include "item.hpp"
#include "vldsift.hpp"
#include "sbow.hpp"
#include <errno.h>

using namespace std;

enum { DEBUG = 0, INFO, WARN, ERROR, FATAL, SIGNAL }; // log levels
int loglevel = INFO;

#define verify(_x)						\
  do {								\
    if (!(_x)) {						\
      LOG(FATAL, "Assertion failure in %s line %d of %s: %s\n", \
	  __FUNCTION__, __LINE__, __FILE__, #_x);		\
    }								\
  } while (0)

void LOG(int level, const char *fmt, ...)
{
  static const char *level_str[] = {
    "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "SIGNAL"
  };
  assert(loglevel >= DEBUG);
  assert(loglevel <= SIGNAL);
  if (level >= loglevel) {
    va_list args;
    fprintf(stderr, "[%s] ", level_str[level]);
    if (level == ERROR) {
      fprintf(stderr, "(errno: %s) ", strerror(errno));
    }
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
  }
  if (level == FATAL) {
    exit(1);
  }
}

void printUsageAndExit() { 
  cerr << "This program vector quantizes sift features according to a code book.\n"
       << "STDIN: binary key sift pairs \n STDOUT: binary key bow pairs\n"
       << "-codebook codebook_path" << endl;
  exit(1);
}

char* codebook_path=0;
int D=0;
bool parseArgs(int argc, char** argv) {
  //get rid of program name
  argc--; argv++;

  while(argc > 0)
    {
      if(!strcmp(*argv, "-codebook")){
	argc--; argv++; 
	if(argc == 0) {
	  fprintf(stderr, "must specify -codebook\n"); 
	  return false;
	}
	else {
	  codebook_path = *argv;
	}
      }
      argc--; argv++; 
    }

  if ( codebook_path == 0 ) {
    return false;
  }
  return true;
}

void read_codebook(const char* codebook_path, vector< vector<float> >& cb) {
  ifstream fin(codebook_path);
  if( !fin.good() ){
    LOG(FATAL, "bad codebook path!");
  }
  cerr << "reading codebook." << endl;
  string line;
  cb.clear();
  int D = -1;
  while( getline(fin, line) ) {
    cb.push_back(vector<float>());
    istringstream iss(line);
    float v = 0;
    while( iss >> v ) {
      cb.back().push_back(v);
    }
    if( D != -1 ) {
      verify(cb.back().size()==D);
    } else {
      D = cb.back().size();
      verify( D > 0 );
    }
  }
  cerr << "read codebook. size:" << cb.size() << " dim:" << D << endl;
}

double l2sqr( float* x, float* y, int dim  ) {
  double s = 0;
  for ( int i = 0; i < dim; i++ ) {
    double d = x[i] - y[i];
    s += d * d;
  }
  return s;
}

int vq(vector<vector<float> >& cb, float* v ) {
  double min_dist=std::numeric_limits<float>::max();;
  int min_ind=-1;
  for( int i = 0 ; i < cb.size(); i++ ) { 
    double dist = l2sqr( &cb[i][0], v, cb[i].size() );
    if( dist < min_dist ) {
      min_dist = dist;
      min_ind = i;
    }
  }
  verify(min_ind >= 0 && min_ind < cb.size() );
  return min_ind;
}

int main (int argc, char *argv[])
{

  if( !parseArgs(argc, argv) ) {
    printUsageAndExit();
  }

  vector<vector<float> > cb;
  read_codebook(codebook_path, cb);
  
    while(!feof(stdin)) {
      string key_str, value_str;
      int ret =ItemIO::readItem(stdin, key_str);
      if(ret == READITEM_EOF ) {
	exit(0);
      }
      if(ret!= READITEM_OK ) {
	cerr << "input key error " << endl;
	exit(1);
      }
      ret = ItemIO::readItem(stdin, value_str);
      if( ret != READITEM_OK ) {
	exit(1);
      }
      Points_vldsift points;
      if ( !points.fromBinary(value_str) ) {
	cerr << "sift format error" << endl;
      }
      //cerr << "# sift points in the bag: " << points.vecs.size() << endl;
      SBOW bag;
      for( size_t i = 0; i < points.vecs.size(); i++ ) {
	Point_vldsift &p = points.vecs[i];
	u_int32_t w =  vq(cb,p.desc);
	SWord sw;
	sw.index = w;
	sw.x = p.x;
	sw.y = p.y;
	sw.scale = p.scale;
	sw.norm = p.norm;
	sw.ori = 0.0;
	//cerr << "point " << i << ": " << w << endl;
	bag.words.push_back(sw);
      }

      ret = ItemIO::writeItem(stdout, key_str);
      if( ret != WRITEITEM_OK ) {
	cerr << "write failed " << endl;
	exit(1);
      }
      ret = ItemIO::writeItem(stdout, bag.toBinary());
      if( ret != WRITEITEM_OK ) {
	cerr << "write failed " << endl;
	exit(1);
      }
    }

    return 0;
}

