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

            void allGather(harp::ds::Table *table);

            void allReduce(harp::ds::Table *table, MPI_Op operation);

            void broadcast(harp::ds::Table *table, int bcastWorkerId);

            void rotate(harp::ds::Table *table);
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
