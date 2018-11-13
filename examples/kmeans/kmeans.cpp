#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"
#include "../../kernels/HarpKernels.h"


using namespace harp;

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator *comm) {
        if (workerId == 0) {
            printf("Generating data in node 0");
            util::generateKMeansData("/tmp/harp/kmeans", 10000, 4, 4, 10);
        }
        comm->barrier();


    }
};

//todo see c++ template meta programming
int main(int argc, char *argv[]) {
    // KMeansWorker kMeansWorker;
    // kMeansWorker.init(argc, argv);
    //kMeansWorker.start();

    auto *points = new harp::ds::Table<float>(0);
    srand(time(NULL));
    for (int i = 0; i < 10000; i++) {
        auto *d1 = new float[3];
        d1[0] = rand() % 100;
        d1[1] = rand() % 100;
        d1[2] = rand() % 100;

        auto *p1 = new ds::Partition<float>(i, d1, 3);
        points->addPartition(p1);
    }


    auto *centroids = new harp::ds::Table<float>(0);
    for (int i = 0; i < 100; i++) {
        auto *c = new float[3];
        c[0] = rand() % 100;
        c[1] = rand() % 100;
        c[2] = rand() % 100;
        centroids->addPartition(new ds::Partition<float>(i, c, 3));
    }

    harp::kernels::kmeans(centroids, points, 1);
    for (auto p : centroids->getPartitions()) {
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << static_cast<float *>(p.second->getData())[j] << ",";
        }
        std::cout << std::endl;
    }

    std::cout << "" << std::endl;
    return 0;
}