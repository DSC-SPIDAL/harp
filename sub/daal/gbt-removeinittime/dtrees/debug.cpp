/*!
 * Copyright 2015-2018 by Contributors
 * \file common.cc
 * \brief Enable all kinds of global variables in common.
 */
#include "debug.h"

namespace daal
{
namespace algorithms
{
namespace dtrees
{
namespace internal
{


void startVtune(std::string tagfilename, int waittime){
    static bool isInit = false;

    if (!isInit){
        std::ofstream write;
        write.open(tagfilename);
        write << "okay" << std::endl;
        write.close();
        isInit = true;

        //sleep for 1 sec
        std::this_thread::sleep_for(std::chrono::milliseconds(waittime));
    }
}


#ifdef USE_DEBUG
void printmsg(std::string msg){
    std::cout << "MSG:" << msg << "\n";
}

void printInt(std::string msg, int val){
    std::ostringstream stringStream;
    stringStream << msg << ":" << val;
    printmsg(stringStream.str());
}
void printVec(std::string msg, const std::vector<unsigned int>& vec){
    std::ostringstream stringStream;
    stringStream << msg ;
    for(int i=0; i< vec.size(); i++){
    stringStream << vec[i] << ",";
    }
    printmsg(stringStream.str());
}
void printVec(std::string msg, const std::vector<int>& vec){
    std::ostringstream stringStream;
    stringStream << msg ;
    for(int i=0; i< vec.size(); i++){
    stringStream << vec[i] << ",";
    }
    printmsg(stringStream.str());
}

#else
void printmsg(std::string msg){}
void printInt(std::string msg, int val){}
void printVec(std::string msg, const std::vector<unsigned int>& vec){}
void printVec(std::string msg, const std::vector<int>& vec){}

#endif

} 
}
}
}
