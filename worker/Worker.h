#ifndef HARPC_WORKER_H
#define HARPC_WORKER_H

#include "../communication/Communicator.h"
#include "../communication/Communicator.cpp"

namespace harp {
    class Worker {
    protected:
        int workerId;
        int worldSize;

        int comThreads = 1;//no of communication threads
    public:
        void init(int argc, char *argv[]);

        void start();

        virtual void execute(com::Communicator<int> *comm) = 0;

        void setCommThreads(int comThreads);
    };
}

#endif //HARPC_WORKER_H
