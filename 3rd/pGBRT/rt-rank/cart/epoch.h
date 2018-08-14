#ifndef AM_RT_EPOCH_H
#define AM_RT_EPOCH_H

#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

class epoch{
 public:
  struct timeval t1, t2;

  epoch() {}

  void start() {
    gettimeofday(&t1,NULL);
  }

  int elapsed() {
    gettimeofday(&t2,NULL);
    return t2.tv_sec - t1.tv_sec;

  }
};

#endif //AM_RT_EPOCH_H
