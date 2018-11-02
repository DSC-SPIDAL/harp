#include <iostream>
#include "add_x.h"
#include "vector"
//#include <mpi.h>
#include "data_structures/Partition.h"
#include "data_structures/Partition.cpp"
#include "data_structures/Array.h"
#include "data_structures/Array.cpp"

using namespace std;
using namespace harp;


namespace Yo {
    namespace X {
        template<class A, class B>
        class Thing {
        public:
            int a = 0;

            A doThings(A x, A y) {
                this->a = x;
                return x + y;
            }
        };
    }
}


void doThings(Yo::X::Thing<int, double> t) {
    t.doThings(5, 10);
}


int main() {


    int a[4] = {1, 2, 3, 4};


    ds::Array<int> intArr(a, 4, -1);
    ds::Partition<ds::Array<int>> p(12, &intArr);

    p.getData()->getData()[2] = 256;

    cout << p.getData()->getData()[3] << endl;

    return 0;
}




