#ifndef HARPC_WORKER_H
#define HARPC_WORKER_H

#include "../communication/Communicator.h"

namespace harp {
    class Worker {
    protected:
        int workerId;
        int worldSize;
    public:
        void init(int argc, char *argv[]);

        void start();

        virtual void execute(com::Communicator *comm) = 0;
    };
}

#endif //HARPC_WORKER_H
