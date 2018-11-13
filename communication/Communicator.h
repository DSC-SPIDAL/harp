#ifndef HARPC_COMMUNICATOR_H
#define HARPC_COMMUNICATOR_H

#include <iostream>
#include "mpi.h"
#include "../data_structures/inculdes.h"


//todo doing implementation in header file due to templates problem
namespace harp {
    namespace com {
        class Communicator {
        private:
            int workerId;
            int worldSize;
        public:

            Communicator(int workerId, int worldSize);

            void barrier();

            template<class TYPE>
            void allGather(harp::ds::Table<TYPE> *table);

            template<class TYPE>
            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation);

            template<class TYPE>
            void broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId);

            template<class TYPE>
            void rotate(harp::ds::Table<TYPE> *table);
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
