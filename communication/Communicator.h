#ifndef HARPC_COMMUNICATOR_H
#define HARPC_COMMUNICATOR_H

#include <iostream>
#include "mpi.h"
#include "../data_structures/inculdes.h"
#include "../util/ThreadPool.h"
#include "future"

//todo doing implementation in header file due to templates problem
namespace harp {
    namespace com {
        template<class TYPE>
        class Communicator {
        private:
            TYPE workerId;
            TYPE worldSize;

            ctpl::thread_pool *threadPool;

            //todo group async tasks with a tag and allow to wait on all or on tag
            std::queue<std::future<void>> asyncTasks;

        public:

            Communicator(TYPE workerId, TYPE worldSize);

            void barrier();

            void allGather(harp::ds::Table<TYPE> *table);

            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation);

            void broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId);

            template<class TAB_TYPE>
            void rotate(harp::ds::Table<TAB_TYPE> *table);

            template<class TAB_TYPE>
            void asyncRotate(harp::ds::Table<TAB_TYPE> *table, int pid);

            void wait();
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
