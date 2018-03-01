#ifndef _ITEM_H__
#define _ITEM_H__

#include <cstdio>
#include <cstring>
#include <string>
#include <iostream>
//#include <stdint.h>
using namespace std;

#define READITEM_OK 0
#define READITEM_ERR 1
#define READITEM_EOF -1

#define WRITEITEM_OK 0
#define WRITEITEM_ERR 1

typedef unsigned int u_int32_t;

class ItemIO {
public:
  static int readItem(FILE* stream, string& s) {
    s.clear();
    u_int32_t len = 0;
    size_t c = fread(&len, sizeof(u_int32_t), 1, stdin);
    if( feof(stream) ) {
      return READITEM_EOF;
    }
    if ( ferror(stream)|| c < 1 ) {
      perror("input error!");
      return READITEM_ERR;
    }
    if( len == 0 ) {
      return READITEM_OK;
    }
    char* buf = new char[len];
    c = fread(buf,len,1, stdin);
    if( c < 1 || ferror(stream) || feof(stream) ) {
      perror("input error!");
      delete[] buf;
      return READITEM_ERR;
    }

    s.append(buf, len);
    delete[] buf;
    return READITEM_OK;
  }

  static int readItem(iostream& stream, string& s) {
    s.clear();
    u_int32_t len = 0;
    stream.read((char*)&len, sizeof(u_int32_t));
    if( stream.eof() ) {
      return READITEM_EOF;
    }
    if ( !stream.good() ) {
      cerr<< "input error!" << endl;
      return READITEM_ERR;
    }
    if( len == 0 ) {
      return READITEM_OK;
    }
    char* buf = new char[len];
    stream.read(buf,len);
    if( !stream.good() ) {
      cerr << "input error!";
      delete[] buf;
      return READITEM_ERR;
    }
    s.append(buf, len);
    delete[] buf;
    return READITEM_OK;
  }

  static int writeItem(FILE* stream, const string& s) {
    u_int32_t len = s.length();
    size_t c = fwrite(&len,sizeof(u_int32_t), 1, stream);
    if( c < 1 ) {
      return WRITEITEM_ERR;
    }
    if( len > 0 ) {
      c = fwrite((void*)s.data(), len, 1, stream);
      if( c < 1 ) {
	return WRITEITEM_ERR;
      }
    }
    return WRITEITEM_OK;
  }

  static int writeItem(iostream& stream, const string& s) {
    u_int32_t len = s.length();
    stream.write((char*)&len,sizeof(u_int32_t));
    if( !stream.good() ) {
      return WRITEITEM_ERR;
    }
    if( len > 0 ) {
      stream.write((char*)s.data(), len);
      if( !stream.good() ) {
	return WRITEITEM_ERR;
      }
    }
    return WRITEITEM_OK;
  }

  static int readItem(FILE* stream, int& x) {
    string s;
    int ret = readItem(stream, s);
    if( ret != READITEM_OK ) {
      return ret;
    }
    if( s.length() != sizeof(int) ) {
      return READITEM_ERR;
    }
    memcpy(&x, s.data(), sizeof(int));
    return ret;
  }

  static int writeItem(FILE* stream, int x ) {
    string s;
    s.append((char*)&x, sizeof(x));
    return writeItem(stream, s);
  }
  
  static int writeItem(FILE* stream, void* data, size_t len ) {
    string s;
    if( len > 0 ) {
      s.append((char*)data, len);
    }
    return writeItem(stream, s);
  }
};
#endif 
