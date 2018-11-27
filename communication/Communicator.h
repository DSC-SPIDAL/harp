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

            int comThreads;

            //Using separate task queues for each thread pool to prevent unnecessary locking
            //----------------------------------//
            ctpl::thread_pool *threadPool;
            std::queue<std::future<void>> asyncTasks;
            std::mutex asyncTasksMutex;
            //----------------------------------//

            int communicationTag = 0;

        public:

            Communicator(TYPE workerId, TYPE worldSize, int comThreads = 1);

            void barrier();

            void allGather(harp::ds::Table<TYPE> *table);

            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation);

            void broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId);

            template<class TAB_TYPE>
            void rotate(harp::ds::Table<TAB_TYPE> *table, int sendTag = -1, int recvTag = -1);

            template<class TAB_TYPE>
            void asyncRotate(harp::ds::Table<TAB_TYPE> *table, int pid);

            void wait();
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
