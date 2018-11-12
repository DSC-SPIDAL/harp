#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"
#include "../../kernels/math/HarpMath.h"
#include "../../kernels/kmeans/KMeans.h"


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


int main(int argc, char *argv[]) {
//    KMeansWorker kMeansWorker;
//    kMeansWorker.init(argc, argv);
//    kMeansWorker.start();
    auto *d1 = new float[3];
    d1[0] = 10;
    d1[1] = 2;
    d1[2] = 3;

    auto *d2 = new float[3];
    d2[0] = 13;
    d2[1] = 24;
    d2[2] = 31;

    auto *p1 = new ds::Partition(0, d1, 3, HP_FLOAT);
    auto *p2 = new ds::Partition(1, d2, 3, HP_FLOAT);

    auto *points = new harp::ds::Table(0, HP_FLOAT);
    points->addPartition(p1);
    points->addPartition(p2);

    auto *c = new float[3];
    c[0] = 4;
    c[1] = 5;
    c[2] = 7;

    auto *centroids = new harp::ds::Table(0, HP_FLOAT);
    centroids->addPartition(new ds::Partition(-1, c, 3, HP_FLOAT));


    harp::kernels::kmeans(centroids, points, 1);
    std::cout << centroids->getPartitionCount() << std::endl;
    for (auto p : centroids->getPartitions()) {
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << static_cast<float *>(p.second->getData())[j] << ",";
        }
    }

    std::cout << "" << std::endl;
    return 0;
}