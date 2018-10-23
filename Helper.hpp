#ifndef HELPER_H
#define HELPER_H

#include <stdio.h>
#include <sys/time.h>

#define DUMMY_VAL 93620

namespace utility {

    double timer();

    // double timer() {
    //
    //     struct timeval tp;
    //     gettimeofday(&tp, NULL);
    //     return ((double) (tp.tv_sec) + 1e-6 * tp.tv_usec);
    //
    // }

}

#endif 
