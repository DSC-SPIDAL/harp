#include <iostream>
#include "Worker.h"
#include "mpi.h"

namespace harp {

    void Worker::init(int argc, char *argv[]) {
        int provided;
        MPI_Init_thread(&argc, &argv, MPI_THREAD_MULTIPLE, &provided);
        if (provided < MPI_THREAD_MULTIPLE) {
            printf("ERROR: The MPI library does not have full thread support\n");
            MPI_Abort(MPI_COMM_WORLD, 1);
        }
        //MPI_Init(NULL, NULL);

        MPI_Comm_size(MPI_COMM_WORLD, &this->worldSize);

        MPI_Comm_rank(MPI_COMM_WORLD, &this->workerId);
    }

    void Worker::start() {
        com::Communicator<int> comm(this->workerId, this->worldSize);
        this->execute(&comm);
        MPI_Finalize();
    }

    void Worker::setCommThreads(int comThreads) {
        this->comThreads = comThreads;
    }
}