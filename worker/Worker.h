#ifndef HARPC_WORKER_H
#define HARPC_WORKER_H

#include "../communication/Communicator.h"

namespace harp {
    class Worker {
    private:
        int workerId;
    public:
        void init();

        void start();

        virtual void execute(int workerId, com::Communicator *comm) = 0;
    };
}

#endif //HARPC_WORKER_H
