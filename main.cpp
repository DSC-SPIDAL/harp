#include <iostream>
#include "harp.h"
#include <time.h>

using namespace std;
using namespace harp::ds;
using namespace harp::com;

void printPartitions(int worker, Table *tab) {
    string pri = to_string(worker) + "[";
    for (auto f:tab->getPartitions()) {
        int *arr = static_cast<int *>(f.second->getData());

        for (int j = 0; j < f.second->getSize(); j++) {
            pri += (to_string(arr[j]) + ",");
        }
        pri += "|";
    }
    pri += "]";
    cout << pri << endl;
}

class MyWorker : public harp::Worker {

    void execute(Communicator *comm) override {
        auto *tab = new Table(1, HP_INT);

        srand(workerId + time(NULL));
        int numOfPartitions = rand() % 40;
        for (int p = 0; p < numOfPartitions; p++) {
            int partitionSize = rand() % 1000000;
            int *data = new int[partitionSize];
            for (int j = 0; j < partitionSize; j++) {
                data[j] = rand() % 100;
            }
            auto *partition = new Partition(p, data, partitionSize, tab->getDataType());
            tab->addPartition(partition);
        }


        int *data = new int[2];
        data[0] = 1;

        auto *partition = new Partition(0, data, 2, HP_INT);
        tab->addPartition(partition);


        //printPartitions(workerId, tab);
        comm->barrier();
        comm->rotate(tab);
//        comm->barrier();
//        printPartitions(workerId, &tab);
//        comm->broadcast(&tab, 0);
        //comm->allReduce(&tab, MPI_SUM);
        comm->barrier();
        //printPartitions(workerId, tab);
        tab->clear();
    }
};

int main() {
    MyWorker worker;
    worker.init(0, nullptr);
    worker.start();
    return 0;
}




