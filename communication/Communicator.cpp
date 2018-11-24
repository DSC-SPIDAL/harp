#include "Communicator.h"
#include "mpi.h"
#include <map>
#include "future"
#include "iomanip"

template<class TYPE>
void printTable(harp::ds::Table<TYPE> *table) {
    for (auto p : *table->getPartitions()) {
        std::cout << p.first << " : ";
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << std::setprecision(10) << p.second->getData()[j] << ",";
        }
        std::cout << std::endl;
    }
}

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
            this->threadPool = new ThreadPool(8);

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
                    &numOfPartitionsToSend, 1, MPI_INT, sendTo, table->getId(),
                    &numOfPartitionsToRecv, 1, MPI_INT, receiveFrom, table->getId(),
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );

//            printf("Worker %d will send %d partitions and receive %d partitions\n", workerId, numOfPartitionsToSend,
//                   numOfPartitionsToRecv);

            //exchange PARTITION METADATA
            int sendingMetaSize = 1 + (numOfPartitionsToSend * 2);// totalDataSize(1) + [{id, size}]
            int receivingMetaSize = 1 + (numOfPartitionsToRecv * 2);// totalDataSize(1) + [{id, size}]

            int partitionMetaToSend[sendingMetaSize];
            int partitionMetaToRecv[receivingMetaSize];

            int index = 1;
            int totalDataSize = 0;
            std::vector<TYPE> dataBuffer;//todo possible error: data buffer gets cleared immediately after returning this function

            for (const auto p : *table->getPartitions()) {
                partitionMetaToSend[index++] = p.first;
                partitionMetaToSend[index++] = p.second->getSize();
                totalDataSize += p.second->getSize();
                //todo prevent memory copying if possible
                std::copy(p.second->getData(), p.second->getData() + p.second->getSize(),
                          std::back_inserter(dataBuffer));
            }
            partitionMetaToSend[0] = totalDataSize;

            MPI_Sendrecv(
                    &partitionMetaToSend, sendingMetaSize, MPI_INT, sendTo, table->getId(),
                    &partitionMetaToRecv, receivingMetaSize, MPI_INT, receiveFrom, table->getId(),
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );

            //sending DATA
            //todo implement support for data arrays larger than INT_MAX
            MPI_Request dataSendRequest;
            MPI_Isend(&dataBuffer[0], totalDataSize, dataType, sendTo, table->getId(), MPI_COMM_WORLD,
                      &dataSendRequest);

//            MPI_Request dataSendRequests[numOfPartitionsToSend];
//            for (long i = 0; i < numOfPartitionsToSend * 2; i += 2) {
//                int partitionId = partitionMetaToSend[i];
//                int partitionSize = partitionMetaToSend[i + 1];
//                auto *data = table->getPartition(partitionId)->getData();
//                MPI_Isend(data, partitionSize, dataType, sendTo, table->getId(), MPI_COMM_WORLD,
//                          &dataSendRequests[i / 2]);
//            }

            //table->clear();

            auto *recvTab = new harp::ds::Table<TYPE>(table->getId());
            auto *recvBuffer = new TYPE[partitionMetaToRecv[0]];

            MPI_Recv(recvBuffer, partitionMetaToRecv[0], dataType, receiveFrom, table->getId(),
                     MPI_COMM_WORLD,
                     MPI_STATUS_IGNORE);

            int copiedCount = 0;
            for (long i = 1; i < receivingMetaSize; i += 2) {
                int partitionId = partitionMetaToRecv[i];
                int partitionSize = partitionMetaToRecv[i + 1];

                auto *data = new TYPE[partitionSize];
                std::copy(recvBuffer + copiedCount, recvBuffer + copiedCount + partitionSize, data);
                copiedCount += partitionSize;

                auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                recvTab->addPartition(newPartition);
            }

            //receiving DATA
//            for (long i = 0; i < numOfPartitionsToRecv * 2; i += 2) {
//                int partitionId = partitionMetaToRecv[i];
//                int partitionSize = partitionMetaToRecv[i + 1];
//                auto *data = new TYPE[partitionSize];
//                MPI_Recv(data, partitionSize, dataType, receiveFrom, table->getId(),
//                         MPI_COMM_WORLD,
//                         MPI_STATUS_IGNORE);
//                auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
//                recvTab->addPartition(newPartition);
//            }

            MPI_Wait(&dataSendRequest, MPI_STATUS_IGNORE);
//            MPI_Waitall(numOfPartitionsToSend, dataSendRequests, MPI_STATUS_IGNORE);

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
        template<class TAB_TYPE>
        void Communicator<TYPE>::asyncRotate(ds::Table<TAB_TYPE> *table, int pid) {
            auto partition = table->getPartition(pid);//take partition out
            table->removePartition(pid, false);
            //table->getPartitions()->erase(iterator->first);//erase partition
            auto *rotatingTable = new harp::ds::Table<TAB_TYPE>(table->getId());//create new table for rotation
            rotatingTable->addPartition(partition);


            auto handle = std::async(std::launch::async, [rotatingTable, table, this]() {
//                if (workerId == 0) {
//                    std::cout << "Sending : " << std::endl;
//                    printTable(rotatingTable);
//                }
                rotate(rotatingTable);
//                if (workerId == 0) {
//                    std::cout << "Received : " << std::endl;
//                    printTable(rotatingTable);
//                }
                for (auto p:*rotatingTable->getPartitions()) {
                    table->addToPendingPartitions(p.second);
                }
            });

        }
    }
}