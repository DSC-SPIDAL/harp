#include <iostream>
#include "data_structures/inculdes.h"
#include "worker/Worker.h"
#include "communication/Communicator.cpp"


//todo remove Array and directly use partition
using namespace std;
using namespace harp::ds;

class MyWorker : public harp::Worker {

    void execute(int workerId, harp::com::Communicator *comm) override {
        cout << "woker " << workerId << " before barrier" << endl;
        comm->barrier();
        cout << "woker " << workerId << " after barrier" << endl;

        Table<int> tab(1);


        if (workerId == 0) {
            //p1
            int a[4] = {1, 2, 3, 4};
            Partition<int> p(1, a, 4);
            tab.addPartition(&p);

            //p2
            int a2[4] = {1, 2, 3, 4};
            Partition<int> p2(2, a2, 4);
            tab.addPartition(&p2);
        }


        comm->broadcast<int>(&tab, 0);
    }
};

int main() {
    MyWorker worker;
    worker.init();
    worker.start();

    return 0;
}




