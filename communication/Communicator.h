#ifndef HARPC_COMMUNICATOR_H
#define HARPC_COMMUNICATOR_H

#include <iostream>
#include "mpi.h"
#include "../data_structures/inculdes.h"


//todo doing implementation in header file due to templates problem
namespace harp {
    namespace com {
        template<class TYPE>
        class Communicator {
        private:
            TYPE workerId;
            TYPE worldSize;

            void sendAndRecv(const void *buffSend, int sendSize, void *buffRecv, int recvSize, int sendTo, int recvFrom,
                             MPI_Datatype mpiDatatype);

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
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
