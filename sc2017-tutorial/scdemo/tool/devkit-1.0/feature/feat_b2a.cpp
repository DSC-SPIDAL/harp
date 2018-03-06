#include <iostream>
#include <string>
#include "item.hpp"
#include "sbow.hpp"
#include "vldsift.hpp"
#include <cstdarg>
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
  cerr << "usage: program_name keytype. keytype is sbow or vldsift" << endl
       << "stdin: binary key value pair ( image, spatial words ) or ( image, raw vldsift" << endl
       << "stdout: spatial words in ascii format. each line is a set of words" << endl;
  exit(1);
}

enum { KEY_SBOW, KEY_VLDSIFT };

int main(int argc, char** argv) {
  if ( argc <= 1 ) {
    printUsageAndExit();
  } 
  
  int key_type = 0;
  if( !strcmp(argv[1],"sbow") ) {
    key_type=KEY_SBOW;
  } else if( !strcmp(argv[1],"vldsift" ) ) {
    key_type=KEY_VLDSIFT;
  } else {
    cerr << "unknown key type" << endl;
    exit(1);
  }
  
  for( int n = 0; ;n++ ) {
    string key, value;
    int ret = ItemIO::readItem(stdin,key);
    if( ret==READITEM_EOF) {
      break;
    }
    if( ret==READITEM_ERR ) {
      LOG(FATAL,"input format error");
    }
    ret = ItemIO::readItem(stdin, value);
    if( ret != READITEM_OK ) {
      LOG(FATAL,"input format error");
    }
    if( key_type == KEY_SBOW ) {
      SBOW bag;
      if( !bag.fromBinary(value) ) {
	LOG(FATAL,"input format error");
      }
      cout << bag.toASCII_oneline() << endl;
    } else if( key_type == KEY_VLDSIFT ) {
      Points_vldsift points;
      if( !points.fromBinary(value) ) {
	LOG(FATAL,"input format error");	 
      } 
      cout << points.toASCII_oneline() << endl;
    }
  }
}

