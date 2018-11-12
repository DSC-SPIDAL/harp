#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"


using namespace harp;

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator *comm) {
        if (workerId == 0) {
            printf("Generating data in node 0");
            generateKMeansData("/tmp/harp/kmeans", 10000, 4, 4, 10);
        }
        comm->barrier();
        

    }
};


int main(int argc, char *argv[]) {
    KMeansWorker kMeansWorker;
    kMeansWorker.init(argc, argv);
    kMeansWorker.start();
    return 0;
}