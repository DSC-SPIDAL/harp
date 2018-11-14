#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"
#include "../../kernels/HarpKernels.h"
#include <fstream>
#include <chrono>


using namespace harp;
using namespace harp::ds::util;
using namespace std::chrono;
using namespace std;

void printTable(harp::ds::Table<float> *table) {
    for (auto p : table->getPartitions()) {
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << static_cast<float *>(p.second->getData())[j] << ",";
        }
        std::cout << std::endl;
    }
}

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator<int> *comm) {
        int iterations = 1;
        int numOfCentroids = 10;
        int vectorSize = 4;

        if (workerId == 0) {
            //generate only if doesn't exist
            std::ifstream censtream("/tmp/harp/kmeans/centroids");
            if (!censtream.good()) {
                printf("Generating data in node 0\n");
                util::generateKMeansData("/tmp/harp/kmeans", 10000, 4, worldSize, numOfCentroids);
            }

            //running non distributed version in node 0
            auto *points = new harp::ds::Table<float>(0);
            for (int i = 0; i < worldSize; i++) {
                util::readKMeansDataFromFile("/tmp/harp/kmeans/" + std::to_string(i), vectorSize, points,
                                             static_cast<int>(points->getPartitionCount()));
            }

            std::cout << points->getPartitionCount() << std::endl;

            auto *centroids = new harp::ds::Table<float>(1);
            util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize, centroids);

            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            harp::kernels::kmeans(centroids, points, vectorSize, iterations);
            high_resolution_clock::time_point t2 = high_resolution_clock::now();

            auto duration = duration_cast<microseconds>(t2 - t1).count();
            std::cout << "Serial : " << duration << std::endl;

            printTable(centroids);

            deleteTable(points, true);
            deleteTable(centroids, true);
        }
        std::cout << std::endl;
        comm->barrier();

        //running distributed version

        //load centroids
        auto *centroids = new harp::ds::Table<float>(1);
        util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize,
                                     centroids);//todo load only required centroids

        auto *myCentroids = new harp::ds::Table<float>(1);
        for (auto c : centroids->getPartitions()) {
            if (c.first % worldSize == workerId) {
                myCentroids->addPartition(c.second);
            } else {
                delete c.second;
            }
        }
        deleteTable(centroids, false);

        //load points
        auto *points = new harp::ds::Table<float>(0);
        util::readKMeansDataFromFile("/tmp/harp/kmeans/" + std::to_string(workerId), vectorSize, points);

        auto *minDistances = new double[points->getPartitionCount()];
        auto *closestCentroid = new int[points->getPartitionCount()];

        bool firstRound = true;// to prevent minDistance array initialization requirement

        high_resolution_clock::time_point t1 = high_resolution_clock::now();

        for (int it = 0; it < iterations; it++) {
            //determining closest
            for (int cen = 0; cen < numOfCentroids;) {
                for (auto c:myCentroids->getPartitions()) {
                    for (auto p:points->getPartitions()) {
                        double distance = harp::math::partition::distance(p.second, c.second);
                        if (firstRound || distance < minDistances[p.first]) {
                            minDistances[p.first] = distance;
                            closestCentroid[p.first] = c.first;
                        }
                    }
                    firstRound = false;
                    cen++;
                }
                comm->rotate(myCentroids);
            }


            for (auto c:myCentroids->getPartitions()) {
                auto *data = c.second->getData();
                for (int i = 0; i < c.second->getSize(); i++) {
                    data[i] = 0;
                }
            }

            //building new centroids
            for (int cen = 0; cen < numOfCentroids;) {
                for (auto c:myCentroids->getPartitions()) {
                    auto *cdata = c.second->getData();
                    for (auto p:points->getPartitions()) {//todo optimize with map
                        auto *pdata = p.second->getData();
                        if (closestCentroid[p.first] == c.first) {
                            for (int i = 0; i < p.second->getSize(); i++) {
                                cdata[i] += pdata[i] / 10000;
                            }
                        }
                    }
                    cen++;
                }
                comm->rotate(myCentroids);
            }
        }
        high_resolution_clock::time_point t2 = high_resolution_clock::now();
        auto duration = duration_cast<microseconds>(t2 - t1).count();
        if (workerId == 0) {
            std::cout << "Parallel : " << duration << std::endl;
        }
        printTable(myCentroids);
    }
};


int main(int argc, char *argv[]) {
    KMeansWorker kMeansWorker;
    kMeansWorker.init(argc, argv);
    kMeansWorker.start();
    return 0;
}