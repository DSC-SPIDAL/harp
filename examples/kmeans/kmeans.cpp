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

#include "../../util/Timing.h"

#include "../../util/Print.h"

using namespace harp;
using namespace harp::ds::util;
using namespace harp::util::timing;
using namespace std::chrono;
using namespace std;

bool debugCondition(int workerId) {
    return workerId == 1;
}

int tagCounter = 0;

const int TIME_BEFORE_SERIAL = tagCounter++;
const int TIME_AFTER_SERIAL = tagCounter++;

const int TIME_BEFORE_WAIT = tagCounter++;
const int TIME_AFTER_WAIT = tagCounter++;

const int TIME_PARALLEL_TOTAL_START = tagCounter++;
const int TIME_PARALLEL_TOTAL_END = tagCounter++;

const int TIME_ASYNC_ROTATE_BEGIN = tagCounter++;
const int TIME_ASYNC_ROTATE_END = tagCounter++;

class KMeansWorker : public harp::Worker {

    void execute(com::Communicator<int> *comm) {


//        auto *tab = new harp::ds::Table<int>(0);
//        for (int x = 0; x < workerId + 1; x++) {
//            auto *arr = new int[worldSize + workerId + x];
//            for (int l = 0; l < worldSize + workerId + x; l++) {
//                arr[l] = workerId;
//            }
//            auto *part = new harp::ds::Partition<int>(x, arr, worldSize + workerId + x);
//            tab->addPartition(part);
//        }
//
//        comm->allGather(tab);
//        if (workerId == 1) {
//            printTable(tab);
//        }
//
//        return;
        int iterations = 5;
        int numOfCentroids = 250;
        int vectorSize = 1000;
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

            std::cout << points->getPartitionCount() << std::endl;

            auto *centroids = new harp::ds::Table<double>(1);
            util::readKMeansDataFromFile("/tmp/harp/kmeans/centroids", vectorSize, centroids);

            record(TIME_BEFORE_SERIAL);
            harp::kernels::kmeans(centroids, points, vectorSize, iterations);
            record(TIME_AFTER_SERIAL);

            std::cout << "Serial : " << diff(TIME_BEFORE_SERIAL, TIME_AFTER_SERIAL) << std::endl;


            //printTable(centroids);

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

        record(TIME_PARALLEL_TOTAL_START);

        for (int it = 0; it < iterations; it++) {
            auto *minDistances = new double[points->getPartitionCount()];
            auto *closestCentroid = new int[points->getPartitionCount()];

            bool firstRound = true;// to prevent minDistance array initialization requirement

            //determining closest

            int cen = 0;
            while (cen < numOfCentroids && myCentroids->hasNext(true)) {
                auto *nextCent = myCentroids->nextPartition();
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
                //std::cout << "Calling rotate on " << nextCent->getId() << std::endl;
                //record(TIME_ASYNC_ROTATE_BEGIN);
                comm->asyncRotate(myCentroids, nextCent->getId());
                //record(TIME_ASYNC_ROTATE_END);
                //diff(TIME_ASYNC_ROTATE_BEGIN, TIME_ASYNC_ROTATE_END, true);
            }

            //wait for async communications to complete
            //record(TIME_BEFORE_WAIT);
            comm->wait();
            //record(TIME_AFTER_WAIT);

            //std::cout << "Wait time 1st rot : " << diff(TIME_BEFORE_WAIT, TIME_AFTER_WAIT, true) << std::endl;

            harp::ds::util::resetTable<double>(myCentroids, 0);

            //building new centroids
            cen = 0;
            while (cen < numOfCentroids && myCentroids->hasNext(true)) {
                auto *nextCent = myCentroids->nextPartition();
                auto *cdata = nextCent->getData();
                for (auto p:*points->getPartitions()) {
                    if (closestCentroid[p.first] == nextCent->getId()) {
                        cdata[nextCent->getSize() - 1]++;
                        auto *pdata = p.second->getData();
                        for (int i = 0; i < p.second->getSize(); i++) {
                            cdata[i] += pdata[i];
                        }
                    }
                }
                cen++;
                //record(TIME_ASYNC_ROTATE_BEGIN);
                comm->asyncRotate(myCentroids, nextCent->getId());
                //record(TIME_ASYNC_ROTATE_END);
                //diff(TIME_ASYNC_ROTATE_BEGIN, TIME_ASYNC_ROTATE_END, true);
            }

            //record(TIME_BEFORE_WAIT);
            comm->wait();
            //record(TIME_AFTER_WAIT);

            //std::cout << "Wait time 2nd rot : " << diff(TIME_BEFORE_WAIT, TIME_AFTER_WAIT, true) << std::endl;

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
        record(TIME_PARALLEL_TOTAL_END);
        if (workerId == 0) {
            std::cout << "Parallel : " << diff(TIME_PARALLEL_TOTAL_START, TIME_PARALLEL_TOTAL_END) << std::endl;
            std::cout << "Speedup : " << diff(TIME_BEFORE_SERIAL, TIME_AFTER_SERIAL) /
                                         diff(TIME_PARALLEL_TOTAL_START, TIME_PARALLEL_TOTAL_END) << std::endl;
        }

//        std::cout << "Avg async rotation time : " << average(TIME_ASYNC_ROTATE_BEGIN, TIME_ASYNC_ROTATE_END)
//                  << std::endl;
//        std::cout << "Avg Wait time : " << average(TIME_BEFORE_WAIT, TIME_AFTER_WAIT) << std::endl;
//        std::cout << "Total Communication time : " << total(11, 12) << std::endl;
//        std::cout << "Total Computation time : " << diff(TIME_PARALLEL_TOTAL_START, TIME_PARALLEL_TOTAL_END) -
//                                                    total(TIME_ASYNC_ROTATE_BEGIN, TIME_ASYNC_ROTATE_END) -
//                                                    diff(TIME_BEFORE_WAIT, TIME_AFTER_WAIT) << std::endl;

        comm->barrier();
        printTable(myCentroids);

        deleteTable(myCentroids, true);
        deleteTable(points, true);
    }
};


int main(int argc, char *argv[]) {
    KMeansWorker kMeansWorker;
    kMeansWorker.init(argc, argv);
    kMeansWorker.setCommThreads(1);
    kMeansWorker.start();
    return 0;
}