#include <iostream>
#include "vector"
#include <mpi.h>
#include "data_structures/Partition.h"
#include "data_structures/Partition.cpp"
#include "data_structures/Array.h"
#include "data_structures/Array.cpp"
#include "data_structures/Table.h"
#include "data_structures/Table.cpp"
#include "worker/Worker.h"
//#include "worker/Worker.cpp"

using namespace std;
using namespace harp::ds;

class MyWorker : public harp::Worker {

    void execute(int workerId) override {
        cout << "Working" << endl;
    }
};

int main() {


    int a[4] = {1, 2, 3, 4};

    Array<int> intArr(a, 4, -1);
    Partition<Array<int>> p(12, &intArr);

    Table<Array<int >> tab(1);
    tab.addPartition(&p);


    p.getData()->getData()[2] = 256;

    cout << p.getData()->getData()[3] << endl;

    MyWorker worker;
    worker.init();
    worker.start();

    return 0;
}




