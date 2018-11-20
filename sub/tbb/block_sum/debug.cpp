/*!
 * Copyright 2015-2018 by Contributors
 * \file common.cc
 * \brief Enable all kinds of global variables in common.
 */
#include "debug.h"

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

const char* HumanDate() {
    static char buffer_[9];
    time_t time_value = time(NULL);
    struct tm *pnow;
#if !defined(_WIN32)
    struct tm now;
    pnow = localtime_r(&time_value, &now);
#else
    pnow = localtime(&time_value);  // NOLINT(*)
#endif
    snprintf(buffer_, sizeof(buffer_), "%02d:%02d:%02d",
             pnow->tm_hour, pnow->tm_min, pnow->tm_sec);
    return buffer_;
}


void loginfo(std::string msg){
    std::cout << "[" << HumanDate() << "] " << msg;
}

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


