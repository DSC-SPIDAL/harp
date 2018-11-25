#include <iostream>
#include "../../data_structures/inculdes.h"
#include "../../worker/Worker.h"
#include <time.h>
#include "../generators/DataGenerator.h"
#include "../../kernels/HarpKernels.h"
#include <fstream>
#include <chrono>
#include <iomanip>
#include <thread>
#include "future"

#include "../../communication/Rotator.h"
#include "../../communication/Rotator.cpp"


using namespace harp;
using namespace harp::ds::util;
using namespace std::chrono;
using namespace std;
//
//template<class TYPE>
//void printTable(harp::ds::Table<TYPE> *table) {
//    for (auto p : *table->getPartitions()) {
//        std::cout << p.first << " : ";
//        for (int j = 0; j < p.second->getSize(); j++) {
//            std::cout << std::setprecision(10) << p.second->getData()[j] << ",";
//        }
//        std::cout << std::endl;
//    }
//}

bool debugCondition(int workerId) {
    return workerId == 1;
}

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator<int> *comm) {
        int iterations = 1;
        int numOfCentroids = 13;
        int vectorSize = 400;
        int numOfVectors = 10000;

        double serialDuration = 0;

        string logName = to_string(workerId) + ".txt";
        freopen(&logName[0], "w", stdout);

        std::cout << "Starting.." << std::endl;

        if (workerId == 0) {
            //generate only if doesn't exist
            std::ifstream censtream("/tmp/harp/kmeans/centroids");
            if (!censtream.good()) {
                printf("Generating data in node 0\n");
                util::generateKMeansData("/tmp/harp/kmeans", numOfVectors, vectorSize, worldSize, numOfCentroids);
            }

            //running non distributed version in node 0
            auto *points = new harp::ds::Table<double>(0);
            for (int i = 0; i < worldSize; i++) {
                util::readKMeansDataFromFile("/tmp/harp/kmeans/" + std::to_string(i), vectorSize, points,
                                             static_cast<int>(points->getPartitionCount()));
            }

//            cout << points->getPartitionCount() << endl;
//            harp::com::Rotator<double> rotator(comm, points);
//            rotator.rotate(1);
//            rotator.finalize();
//            cout << points->getPartitionCount() << endl;
//


            std::cout << points->getPartitionCount() << std::endl;

            auto *centroids = new harp::ds::Table<double>(1);
            util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize, centroids);

            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            harp::kernels::kmeans(centroids, points, vectorSize, iterations);
            high_resolution_clock::time_point t2 = high_resolution_clock::now();

            auto duration = duration_cast<microseconds>(t2 - t1).count();

            serialDuration = duration;
            std::cout << "Serial : " << duration << std::endl;

            printTable(centroids);

            deleteTable(points, true);
            deleteTable(centroids, true);

        }

        comm->barrier();

        //running distributed version

        //load centroids
        auto *centroids = new harp::ds::Table<double>(1);
        util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize,
                                     centroids);//todo load only required centroids




        auto *myCentroids = new harp::ds::Table<double>(1);
        for (auto c : *centroids->getPartitions()) {
            if (c.first % worldSize == workerId) {
                //modifying centroids to hold count
                auto *data = c.second->getData();
                auto *newDataWithCount = new double[c.second->getSize() + 1];

                for (int x = 0; x < c.second->getSize(); x++) {
                    newDataWithCount[x] = data[x];
                }
                newDataWithCount[c.second->getSize()] = 0;

                myCentroids->addPartition(
                        new harp::ds::Partition<double>(c.first, newDataWithCount, c.second->getSize() + 1));
            } else {
                delete c.second;
            }
        }

        //load points
        auto *points = new harp::ds::Table<double>(0);
        util::readKMeansDataFromFile("/tmp/harp/kmeans/" + std::to_string(workerId), vectorSize, points);


        high_resolution_clock::time_point t1 = high_resolution_clock::now();

        for (int it = 0; it < iterations; it++) {
            auto *minDistances = new double[points->getPartitionCount()];
            auto *closestCentroid = new int[points->getPartitionCount()];

            bool firstRound = true;// to prevent minDistance array initialization requirement

            //determining closest
            for (int cen = 0; cen < numOfCentroids;) {
                while (myCentroids->hasNext() && cen < numOfCentroids) {
                    auto *nextCent = myCentroids->nextPartition(true);
                    for (auto p:*points->getPartitions()) {
                        double distance = harp::math::partition::distance(p.second, 0, p.second->getSize(),
                                                                          nextCent, 0, nextCent->getSize() - 1);
                        if (firstRound || distance < minDistances[p.first]) {
                            minDistances[p.first] = distance;
                            closestCentroid[p.first] = nextCent->getId();
                        }
                    }
                    firstRound = false;
                    cen++;
                    std::cout << "Calling rotate on " << nextCent->getId() << std::endl;
                    comm->asyncRotate(myCentroids, nextCent->getId());
                }
                //comm->rotate(myCentroids);
                myCentroids->resetIterator();
            }

            //myCentroids->resetIterator();

            //wait for async rotations to complete
            comm->wait();

            harp::ds::util::resetTable<double>(myCentroids, 0);

            //building new centroids
            for (int cen = 0; cen < numOfCentroids;) {
                for (auto c:*myCentroids->getPartitions(true)) {
                    auto *cdata = c.second->getData();
                    for (auto p:*points->getPartitions()) {
                        if (closestCentroid[p.first] == c.first) {
                            cdata[c.second->getSize() - 1]++;
                            auto *pdata = p.second->getData();
                            for (int i = 0; i < p.second->getSize(); i++) {
                                cdata[i] += pdata[i];
                            }
                        }
                    }
                    cen++;
                }
                comm->rotate(myCentroids);
            }

            //calculating average
            for (auto c:*myCentroids->getPartitions()) {
                auto *data = c.second->getData();
                for (int j = 0; j < vectorSize; j++) {
                    data[j] /= data[vectorSize];
                }
            }

            delete[] minDistances;
            delete[] closestCentroid;
        }
        high_resolution_clock::time_point t2 = high_resolution_clock::now();
        auto duration = duration_cast<microseconds>(t2 - t1).count();
        if (workerId == 0) {
            std::cout << "Parallel : " << duration << std::endl;
            std::cout << "Speedup : " << serialDuration / duration << std::endl;
        }

        comm->barrier();
        printTable(myCentroids);

        deleteTable(myCentroids, true);
        deleteTable(points, true);
    }
};


int main(int argc, char *argv[]) {
    KMeansWorker kMeansWorker;
    kMeansWorker.init(argc, argv);
    kMeansWorker.start();
    return 0;
}