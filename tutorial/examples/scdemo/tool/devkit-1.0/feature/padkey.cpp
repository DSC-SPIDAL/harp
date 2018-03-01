#include <cstdio>
#include <sstream>
#include <iostream>
#include "item.hpp"

using namespace std;

int main()
{
  string e("a");
  ItemIO::writeItem(stdout,e);
  
  stringstream iss;    
  char buffer[1024];
  while(!feof(stdin)) {
    size_t count = fread(buffer,1,1024,stdin);
    iss.write(buffer, count);
  }

  ItemIO::writeItem(stdout,iss.str());

}
