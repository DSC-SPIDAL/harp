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

            void sendAndRecv(const void *buffSend, int sendSize, void *buffRecv, int recvSize, int sendTo, int recvFrom,
                             MPI_Datatype mpiDatatype) {
                MPI_Request mpi_request;
                MPI_Isend(buffSend, sendSize, mpiDatatype, sendTo, 0, MPI_COMM_WORLD, &mpi_request);
                MPI_Recv(buffRecv, recvSize, mpiDatatype, recvFrom, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                MPI_Wait(&mpi_request, MPI_STATUSES_IGNORE);
            }

            MPI_Datatype getDataType(size_t type_hash) {//todo possible hash collisions?
                if (type_hash == typeid(int).hash_code()) {
                    return MPI_INT;
                } else if (type_hash == typeid(double).hash_code()) {
                    return MPI_DOUBLE;
                } else if (type_hash == typeid(float).hash_code()) {
                    return MPI_FLOAT;
                } else {
                    throw "Unknown data type";
                }
            }

        public:

            Communicator(int workerId, int worldSize);

            void barrier() {
                MPI_Barrier(MPI_COMM_WORLD);
            }

            template<class TYPE>
            void allGather(harp::ds::Table<TYPE> *table) {

            }

            template<class TYPE>
            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation) {
                MPI_Datatype dataType = this->getDataType(typeid(TYPE).hash_code());
                for (auto p : table->getPartitions()) {//keys are ordered
                    auto *data = new TYPE[p.second->getSize()];
                    MPI_Allreduce(
                            p.second->getData(),
                            data,
                            p.second->getSize(),
                            dataType,
                            operation,
                            MPI_COMM_WORLD
                    );
                    p.second->setData(data);
                }
            }

            template<class TYPE>
            void broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
                //determining number of partitions to bcast
                int partitionCount;
                if (bcastWorkerId == this->workerId) {
                    partitionCount = static_cast<int>(table->getPartitionCount());
                }
                MPI_Bcast(&partitionCount, 1, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);

                //broadcasting partition ids and sizes
                long partitionIds[partitionCount * 2];// [id, size]
                int index = 0;
                if (bcastWorkerId == this->workerId) {
                    for (const auto p : table->getPartitions()) {
                        partitionIds[index++] = p.first;
                        partitionIds[index++] = p.second->getSize();
                    }
                }
                MPI_Bcast(&partitionIds, partitionCount * 2, MPI_LONG, bcastWorkerId, MPI_COMM_WORLD);

                MPI_Datatype dataType = this->getDataType(
                        typeid(TYPE).hash_code());

                //now receiving partitions
                for (long i = 0; i < partitionCount * 2; i += 2) {
                    int partitionId = static_cast<int>(partitionIds[i]);
                    long partitionSize = partitionIds[i + 1];
                    if (partitionSize > 0) {
                        auto *data = new TYPE[partitionSize];//(TYPE *) malloc(sizeof(TYPE) * partitionSize);
                        if (bcastWorkerId == this->workerId) {
                            data = table->getPartition(partitionId)->getData();
                        }
                        MPI_Bcast(data, partitionSize, dataType, bcastWorkerId, MPI_COMM_WORLD);
                        if (bcastWorkerId != this->workerId) {
                            auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                            table->addPartition(newPartition);
                        }
                    }
                }
            }

            template<class TYPE>
            void rotate(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
                //todo assuming MPI_Send doesn't block, change later if that is false

                MPI_Datatype dataType = this->getDataType(typeid(TYPE).hash_code());

                int sendTo = (this->workerId + 1) % this->worldSize;
                int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

                //exchange NUMBER OF PARTITIONS
                int numOfPartitionsToSend = static_cast<int>(table->getPartitionCount());
                int numOfPartitionsToRecv = 0;
                this->sendAndRecv(&numOfPartitionsToSend, 1, &numOfPartitionsToRecv, 1, sendTo, receiveFrom, MPI_INT);

                printf("Worker %d will send %d partitions and receive %d partitions\n", workerId, numOfPartitionsToSend,
                       numOfPartitionsToRecv);

                //exchange PARTITION SIZES
                long partitionIdsToSend[numOfPartitionsToSend * 2];// [id, size]
                long partitionIdsToRecv[numOfPartitionsToRecv * 2];// [id, size]
                int index = 0;
                for (const auto p : table->getPartitions()) {
                    partitionIdsToSend[index++] = p.first;
                    partitionIdsToSend[index++] = p.second->getSize();
                }
                this->sendAndRecv(&partitionIdsToSend, numOfPartitionsToSend * 2,
                                  &partitionIdsToRecv, numOfPartitionsToRecv * 2,
                                  sendTo,
                                  receiveFrom,
                                  MPI_LONG);

                //sending DATA
                MPI_Request dataSendRequests[numOfPartitionsToSend];
                for (long i = 0; i < numOfPartitionsToSend * 2; i += 2) {
                    int partitionId = static_cast<int>(partitionIdsToSend[i]);
                    long partitionSize = partitionIdsToSend[i + 1];
                    TYPE *data = table->getPartition(partitionId)->getData();
                    MPI_Isend(data, partitionSize, dataType, sendTo, partitionId, MPI_COMM_WORLD,
                              &dataSendRequests[i / 2]);
                }

                //receiving DATA
                for (long i = 0; i < numOfPartitionsToRecv * 2; i += 2) {
                    int partitionId = static_cast<int>(partitionIdsToRecv[i]);
                    long partitionSize = partitionIdsToRecv[i + 1];
                    auto *data = new TYPE[partitionSize];//(TYPE *) malloc(sizeof(TYPE) * partitionSize);
                    MPI_Recv(data, partitionSize, dataType, receiveFrom, partitionId, MPI_COMM_WORLD,
                             MPI_STATUS_IGNORE);
                    auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                    table->removePartition(partitionId);//todo clear memory
                    table->addPartition(newPartition);
                }

                MPI_Waitall(numOfPartitionsToSend, dataSendRequests, MPI_STATUS_IGNORE);
            }
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
