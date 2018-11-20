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

#include "../../communication/Rotator.h"
#include "../../communication/Rotator.cpp"


using namespace harp;
using namespace harp::ds::util;
using namespace std::chrono;
using namespace std;

template<class TYPE>
void printTable(harp::ds::Table<TYPE> *table) {
    for (auto p : *table->getPartitions()) {
        std::cout << p.first << " : ";
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << std::setprecision(10) << p.second->getData()[j] << ",";
        }
        std::cout << std::endl;
    }
}

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator<int> *comm) {
//        auto *points = new harp::ds::Table<double>(0);
//        for (int j = 0; j < 3; j++) {
//            auto *data = new double[5];
//            for (int i = 0; i < 5; i++) {
//                data[i] = workerId;
//            }
//            points->addPartition(new harp::ds::Partition<double>(j, data, 5));
//        }
//
//        printTable(points);


        int iterations = 1;
        int numOfCentroids = 10;
        int vectorSize = 4;
        int numOfVectors = 10000;

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

            printTable(centroids);

            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            harp::kernels::kmeans(centroids, points, vectorSize, iterations);
            high_resolution_clock::time_point t2 = high_resolution_clock::now();

            auto duration = duration_cast<microseconds>(t2 - t1).count();
            std::cout << "Serial : " << duration << std::endl;

            printTable(centroids);

            deleteTable(points, true);
            deleteTable(centroids, true);

        }


        cout << endl;
        comm->barrier();

        //running distributed version

        //load centroids
        auto *centroids = new harp::ds::Table<double>(1);
        util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize,
                                     centroids);//todo load only required centroids




        auto *myCentroids = new harp::ds::Table<double>(1);
        for (auto c : *centroids->getPartitions()) {
            if (c.first % worldSize == workerId) {
                myCentroids->addPartition(c.second);
            } else {
                delete c.second;
            }
        }

        auto *myCentroidPointCount = new harp::ds::Table<int>(0);
        for (auto p:*myCentroids->getPartitions()) {
            auto *count = new int[1];
            myCentroidPointCount->addPartition(new harp::ds::Partition<int>(p.first, count, 1));
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
                for (auto c = myCentroids->getPartitions(true)->begin(); c != myCentroids->getPartitions()->end();) {
                    for (auto p:*points->getPartitions()) {
                        double distance = harp::math::partition::distance(p.second, c->second);
                        if (firstRound || distance < minDistances[p.first]) {
                            minDistances[p.first] = distance;
                            closestCentroid[p.first] = c->first;
                        }
                    }
                    firstRound = false;
                    cen++;
                    //comm->asyncRotate(myCentroids, c);
                    c++;
                }
                comm->rotate(myCentroids);
                comm->rotate(myCentroidPointCount);
            }

            harp::ds::util::resetTable<double>(myCentroids, 0);
            harp::ds::util::resetTable<int>(myCentroidPointCount, 0);

            //building new centroids

            for (int cen = 0; cen < numOfCentroids;) {
                for (auto c:*myCentroids->getPartitions()) {
                    auto *cdata = c.second->getData();
                    auto *count = myCentroidPointCount->getPartition(c.first)->getData();
                    for (auto p:*points->getPartitions()) {
                        if (closestCentroid[p.first] == c.first) {
                            count[0]++;
                            auto *pdata = p.second->getData();
                            for (int i = 0; i < p.second->getSize(); i++) {
                                cdata[i] += pdata[i];
                            }
                        }
                    }
                    cen++;
                }
                comm->rotate(myCentroids);
                comm->rotate(myCentroidPointCount);
            }

            //calculating average
            for (auto c:*myCentroids->getPartitions()) {
                auto *count = myCentroidPointCount->getPartition(c.first)->getData();
                for (int j = 0; j < vectorSize; j++) {
                    c.second->getData()[j] /= count[0];
                }
            }

            delete[] minDistances;
            delete[] closestCentroid;
        }
        high_resolution_clock::time_point t2 = high_resolution_clock::now();
        auto duration = duration_cast<microseconds>(t2 - t1).count();
        if (workerId == 0) {
            std::cout << "Parallel : " << duration << std::endl;
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