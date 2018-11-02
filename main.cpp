#include <iostream>
#include "data_structures/inculdes.h"
#include "worker/Worker.h"
#include "communication/Communicator.cpp"

using namespace std;
using namespace harp::ds;

class MyWorker : public harp::Worker {

    void execute(int workerId, harp::com::Communicator *comm) override {
        cout << "woker " << workerId << " before barrier" << endl;
        comm->barrier();
        cout << "woker " << workerId << " after barrier" << endl;

        Table<Array<int >> tab(1);


        if (workerId == 0) {
            //p1
            int a[4] = {1, 2, 3, 4};
            Array<int> intArr(a, 4, -1);
            Partition<Array<int>> p(1, &intArr);
            tab.addPartition(&p);

            //p2
            int a2[4] = {1, 2, 3, 4};
            Array<int> intArr2(a2, 4, -1);
            Partition<Array<int>> p2(2, &intArr2);
            tab.addPartition(&p2);
        }


        comm->broadcast<Array<int >>(&tab, 0);
    }
};

int main() {
    MyWorker worker;
    worker.init();
    worker.start();

    return 0;
}




