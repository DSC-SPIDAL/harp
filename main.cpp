#include <iostream>
#include "vector"
//#include <mpi.h>
#include "data_structures/Partition.h"
#include "data_structures/Partition.cpp"
#include "data_structures/Array.h"
#include "data_structures/Array.cpp"

using namespace std;
using namespace harp;

int main() {


    int a[4] = {1, 2, 3, 4};


    ds::Array<int> intArr(a, 4, -1);
    ds::Partition<ds::Array<int>> p(12, &intArr);

    p.getData()->getData()[2] = 256;

    cout << p.getData()->getData()[3] << endl;

    return 0;
}




