#ifndef HARPC_WORKER_H
#define HARPC_WORKER_H

#include "../communication/Communicator.h"

namespace harp {
    class Worker {
    protected:
        int workerId;
        int worldSize;

        int comThreads = 1;//no of communication threads
    public:
        void init(int argc, char *argv[]) {
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

        void start() {
            com::Communicator comm(this->workerId, this->worldSize);
            this->execute(&comm);
            MPI_Finalize();
        }

        virtual void execute(com::Communicator *comm) = 0;

        void setCommThreads(int comThreads) {
            this->comThreads = comThreads;
        }
    };
}

#endif //HARPC_WORKER_H
