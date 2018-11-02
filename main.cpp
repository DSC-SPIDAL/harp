#include <iostream>
#include "data_structures/inculdes.h";
#include "worker/Worker.h"

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




