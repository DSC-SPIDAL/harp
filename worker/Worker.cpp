#include <iostream>
#include "Worker.h"
#include "mpi.h"

namespace harp {

    void Worker::init(int argc, char *argv[]) {
        MPI_Init(NULL, NULL);

        MPI_Comm_size(MPI_COMM_WORLD, &this->worldSize);

        MPI_Comm_rank(MPI_COMM_WORLD, &this->workerId);
    }

    void Worker::start() {
        com::Communicator<int> comm(this->workerId, this->worldSize);
        this->execute(&comm);
        MPI_Finalize();
    }
}