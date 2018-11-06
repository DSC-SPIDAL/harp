#include <iostream>
#include "data_structures/inculdes.h"
#include "worker/Worker.h"
#include <time.h>

using namespace std;
using namespace harp::ds;
using namespace harp::com;

void printParitions(int worker, Table<int> *tab) {
    string pri = to_string(worker) + "[";
    for (auto f:tab->getPartitions()) {
        int *arr = f.second->getData();

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
        Table<int> tab(1);
        srand(workerId + time(NULL));
        int numOfPartitions = rand() % 4;
        for (int p = 0; p < numOfPartitions; p++) {
            int partitionSize = rand() % 10;
            int *data = (int *) malloc(partitionSize * sizeof(int));
            for (int j = 0; j < partitionSize; j++) {
                data[j] = rand() % 10;
            }
            Partition<int> partition(p, data, partitionSize);
            tab.addPartition(&partition);
        }

        printParitions(workerId, &tab);
        comm->barrier();
        comm->rotate<int>(&tab, 0);
        comm->barrier();
        printParitions(workerId, &tab);
    }
};

int main() {
    MyWorker worker;
    worker.init();
    worker.start();
    return 0;
}




