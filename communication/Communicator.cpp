#include "Communicator.h"
#include "mpi.h"
#include <map>
#include "future"

namespace harp {
    namespace com {

//        template<class TYPE>
//        void Communicator::sendAndRecv(const void *buffSend, int sendSize, void *buffRecv, int recvSize, int sendTo,
//                                       int recvFrom,
//                                       MPI_Datatype mpiDatatype) {
//            MPI_Request mpi_request;
//            MPI_Isend(buffSend, sendSize, mpiDatatype, sendTo, 0, MPI_COMM_WORLD, &mpi_request);
//            MPI_Recv(buffRecv, recvSize, mpiDatatype, recvFrom, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
//            MPI_Wait(&mpi_request, MPI_STATUSES_IGNORE);
//        }

        template<class TYPE>
        Communicator<TYPE>::Communicator(TYPE workerId, TYPE worldSize) {
            this->workerId = workerId;
            this->worldSize = worldSize;
        }

        template<class TYPE>
        void Communicator<TYPE>::allGather(harp::ds::Table<TYPE> *table) {

        }

        template<class TYPE>
        void Communicator<TYPE>::allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation) {
            MPI_Datatype dataType = getMPIDataType(table->getDataType());
            for (auto p : table->getPartitions()) {//keys are ordered todo not anymore ordered
                auto *data = createArray(table->getDataType(), p.second->getSize());
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
        void Communicator<TYPE>::broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
            //determining number of partitions to bcast
            int partitionCount;
            if (bcastWorkerId == this->workerId) {
                partitionCount = static_cast<int>(table->getPartitionCount());
            }
            MPI_Bcast(&partitionCount, 1, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);

            //broadcasting partition ids and sizes
            int partitionIds[partitionCount * 2];// [id, size]
            int index = 0;
            if (bcastWorkerId == this->workerId) {
                for (const auto p : table->getPartitions()) {
                    partitionIds[index++] = p.first;
                    partitionIds[index++] = p.second->getSize();
                }
            }
            MPI_Bcast(&partitionIds, partitionCount * 2, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);

            MPI_Datatype dataType = getMPIDataType(table->getDataType());

            //now receiving partitions
            for (long i = 0; i < partitionCount * 2; i += 2) {
                int partitionId = partitionIds[i];
                int partitionSize = partitionIds[i + 1];
                if (partitionSize > 0) {
                    auto *data = createArray(table->getDataType(),
                                             partitionSize);
                    if (bcastWorkerId == this->workerId) {
                        data = table->getPartition(partitionId)->getData();
                    }
                    MPI_Bcast(data, partitionSize, dataType, bcastWorkerId, MPI_COMM_WORLD);
                    if (bcastWorkerId != this->workerId) {
                        auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data,
                                                                           partitionSize,
                                                                           table->getDataType());
                        table->addPartition(newPartition);
                    }
                }
            }
        }

        template<class CLS_TYPE>
        template<class TYPE>
        void Communicator<CLS_TYPE>::rotate(harp::ds::Table<TYPE> *table) {
            MPI_Datatype dataType = getMPIDataType<TYPE>();

            int sendTo = (this->workerId + 1) % this->worldSize;
            int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

            //exchange NUMBER OF PARTITIONS
            int numOfPartitionsToSend = static_cast<int>(table->getPartitionCount());
            int numOfPartitionsToRecv = 0;

            MPI_Sendrecv(
                    &numOfPartitionsToSend, 1, MPI_INT, sendTo, 0,
                    &numOfPartitionsToRecv, 1, MPI_INT, receiveFrom, 0,
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );
            //sendAndRecv(&numOfPartitionsToSend, 1, &numOfPartitionsToRecv, 1, sendTo, receiveFrom, MPI_INT);

//            printf("Worker %d will send %d partitions and receive %d partitions\n", workerId, numOfPartitionsToSend,
//                   numOfPartitionsToRecv);

            //exchange PARTITION SIZES
            int partitionIdsToSend[numOfPartitionsToSend * 2];// [id, size]
            int partitionIdsToRecv[numOfPartitionsToRecv * 2];// [id, size]
            int index = 0;
            for (const auto p : *table->getPartitions()) {
                partitionIdsToSend[index++] = p.first;
                partitionIdsToSend[index++] = p.second->getSize();
            }
            sendAndRecv(&partitionIdsToSend, numOfPartitionsToSend * 2,
                        &partitionIdsToRecv, numOfPartitionsToRecv * 2,
                        sendTo,
                        receiveFrom,
                        MPI_INT);

            //sending DATA
            MPI_Request dataSendRequests[numOfPartitionsToSend];
            for (long i = 0; i < numOfPartitionsToSend * 2; i += 2) {
                int partitionId = partitionIdsToSend[i];
                int partitionSize = partitionIdsToSend[i + 1];
                auto *data = table->getPartition(partitionId)->getData();
                MPI_Isend(data, partitionSize, dataType, sendTo, partitionId, MPI_COMM_WORLD,
                          &dataSendRequests[i / 2]);
            }

            //table->clear();

            auto *recvTab = new harp::ds::Table<TYPE>(table->getId());

            //receiving DATA
            for (long i = 0; i < numOfPartitionsToRecv * 2; i += 2) {
                int partitionId = partitionIdsToRecv[i];
                int partitionSize = partitionIdsToRecv[i + 1];
                auto *data = new TYPE[partitionSize];
                MPI_Recv(data, partitionSize, dataType, receiveFrom, partitionId, MPI_COMM_WORLD,
                         MPI_STATUS_IGNORE);
                auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                recvTab->addPartition(newPartition);
            }

            MPI_Waitall(numOfPartitionsToSend, dataSendRequests, MPI_STATUS_IGNORE);

            //delete table;
            table->swap(recvTab);
            harp::ds::util::deleteTable(recvTab, false);
        }

        template<class TYPE>
        void Communicator<TYPE>::barrier() {
            MPI_Barrier(MPI_COMM_WORLD);
        }

        template<class TYPE>
        void
        Communicator<TYPE>::sendAndRecv(const void *buffSend, int sendSize, void *buffRecv, int recvSize, int sendTo,
                                        int recvFrom, MPI_Datatype mpiDatatype) {
            MPI_Request mpi_request[2];
            MPI_Isend(buffSend, sendSize, mpiDatatype, sendTo, 0, MPI_COMM_WORLD, &mpi_request[0]);
            MPI_Irecv(buffRecv, recvSize, mpiDatatype, recvFrom, 0, MPI_COMM_WORLD, &mpi_request[1]);
            MPI_Waitall(2, mpi_request, MPI_STATUSES_IGNORE);
        }

        template<class TYPE>
        template<class TAB_TYPE, class ITERATOR>
        void Communicator<TYPE>::asyncRotate(ds::Table<TAB_TYPE> *table, ITERATOR &iterator) {

            auto partition = iterator->second;//take partition out
            table->getPartitions()->erase(iterator);//erase partition

            auto *rotatingTable = new harp::ds::Table<TAB_TYPE>(table->getId());//create new table for rotation
            rotatingTable->addPartition(partition);
            auto handle = std::async(std::launch::async, [rotatingTable, table, this]() {
                rotate(rotatingTable);
                for (auto p:*rotatingTable->getPartitions()) {
                    table->addToPendingPartitions(p.second);
                }
            });

            handle.get();
            //table->getPartitions()->erase(iterator);

        }
    }
}