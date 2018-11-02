#ifndef HARPC_WORKER_H
#define HARPC_WORKER_H

namespace harp {
    class Worker {
    private:
        int workerId;
    public:
        void init();

        void start();

        virtual void execute(int workerId) = 0;
    };
}

#endif //HARPC_WORKER_H
