#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"
#include "../../kernels/HarpKernels.h"
#include <fstream>


using namespace harp;

void printTable(harp::ds::Table<float> *table) {
    for (auto p : table->getPartitions()) {
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << static_cast<float *>(p.second->getData())[j] << ",";
        }
        std::cout << std::endl;
    }
}

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator *comm) {
        if (workerId == 0) {
            //generate only if doesn't exist
            std::ifstream censtream("/tmp/harp/kmeans/centroids");
            if (!censtream.good()) {
                printf("Generating data in node 0\n");
                util::generateKMeansData("/tmp/harp/kmeans", 10000, 4, worldSize, 10);
            }

            //running non distributed version in node 0
            auto *points = new harp::ds::Table<float>(0);
            for (int i = 0; i < worldSize; i++) {
                util::readKMeansDataFromFile("/tmp/harp/kmeans/" + std::to_string(i), 4, points,
                                             static_cast<int>(points->getPartitionCount()));
            }

            auto *centroids = new harp::ds::Table<float>(1);
            util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", 4, centroids);

            harp::kernels::kmeans(centroids, points, 4, 1);
            printTable(centroids);

            harp::ds::util::deleteTable(points, true);
            harp::ds::util::deleteTable(centroids, true);
        }
        comm->barrier();

        //running distributed version


    }
};

//todo see c++ template meta programming
int main(int argc, char *argv[]) {
    KMeansWorker kMeansWorker;
    kMeansWorker.init(argc, argv);
    kMeansWorker.start();
    return 0;
}